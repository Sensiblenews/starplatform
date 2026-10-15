import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

/** /api/super/star/feed/upload-status 응답 */
interface UploadStatus {
  state: 'QUEUED' | 'RUNNING' | 'DONE' | 'FAILED' | 'UNKNOWN';
  msg?: string;
}

/**
 * 스타 피드 동영상 업로드 (2-34차 1-3, 상한 95MB).
 *
 * 영상은 base64 JSON 이 아니라 파일 그대로(multipart) 보낸다 — base64 는 1.33배로 불어 Cloudflare 본문 한도를 넘고,
 * 큰 파일을 data URL 로 읽으면 WebView 메모리도 크게 먹는다.
 * 서버는 저장만 하고 바로 작업 ID 를 돌려주며, 압축(H.264)·썸네일·게시물 생성은 백그라운드에서 한다.
 * 쓰기 모달은 전송이 끝나면 닫히고, 이 서비스가 앱 전역에서 작업 상태를 폴링해 결과를 토스트로 알린다.
 */
@Injectable({ providedIn: 'root' })
export class FeedUploadService {
  /** 앱·서버가 같은 값을 쓴다 (SuperAppService.FEED_VIDEO_MAX_BYTES) */
  static readonly MAX_VIDEO_SIZE_MB = 95;

  /** 상태 폴링 간격·최대 대기 (서버 변환 타임아웃 10분 + 대기열 여유) */
  private static readonly POLL_MS = 3000;
  private static readonly POLL_MAX_MS = 20 * 60 * 1000;

  private readonly apiBaseURL = environment.apiBaseURL;

  constructor(
    private http: HttpClient,
    private toastCtrl: ToastController,
  ) { }

  /** 업로드 요청 — 진행률 이벤트를 그대로 흘려보낸다. 응답 본문은 { result, jobId | msg } */
  uploadStarVideo(form: FormData): Observable<HttpEvent<any>> {
    const req = new HttpRequest('POST', `${this.apiBaseURL}/api/super/star/feed/add-media`, form, {
      reportProgress: true,
    });
    return this.http.request(req);
  }

  /** 진행률 이벤트 → 0~100. 진행률이 아니면 null */
  static progressOf(event: HttpEvent<any>): number | null {
    if (event.type !== HttpEventType.UploadProgress) return null;
    if (!event.total) return null;
    return Math.min(100, Math.round((event.loaded / event.total) * 100));
  }

  /**
   * 서버 변환이 끝날 때까지 상태를 폴링하고 결과를 토스트로 알린다. 모달이 닫힌 뒤에도 돈다.
   * onDone 은 게시물이 만들어졌을 때(검수 대기) 한 번 불린다 — 화면 새로고침용
   */
  watchJob(jobId: string, onDone?: () => void): void {
    const startedAt = Date.now();
    const poll = () => {
      this.http.get<UploadStatus>(`${this.apiBaseURL}/api/super/star/feed/upload-status?jobId=${encodeURIComponent(jobId)}`)
        .subscribe({
          next: res => {
            const state = res ? res.state : 'UNKNOWN';
            if (state === 'DONE') {
              this.toast('Your video is ready and waiting for review.', 'success');
              if (onDone) onDone();
              return;
            }
            if (state === 'FAILED') {
              this.toast(res.msg || 'Video processing failed. Please try again.', 'danger');
              return;
            }
            if (state === 'UNKNOWN') {
              this.toast('Video processing was interrupted. Please check your page and upload again if needed.', 'warning');
              return;
            }
            this.next(poll, startedAt);
          },
          // 일시적인 네트워크 오류는 작업 실패가 아니다 — 조금 뒤 다시 묻는다
          error: () => this.next(poll, startedAt),
        });
    };
    setTimeout(poll, FeedUploadService.POLL_MS);
  }

  private next(poll: () => void, startedAt: number): void {
    if (Date.now() - startedAt > FeedUploadService.POLL_MAX_MS) {
      this.toast('Video processing is taking longer than usual. Please check your page later.', 'warning');
      return;
    }
    setTimeout(poll, FeedUploadService.POLL_MS);
  }

  private async toast(message: string, color: string): Promise<void> {
    const toast = await this.toastCtrl.create({ message, duration: 4000, position: 'bottom', color });
    await toast.present();
  }
}
