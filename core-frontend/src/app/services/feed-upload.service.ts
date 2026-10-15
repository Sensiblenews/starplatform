import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

/** 업로드 진행 이벤트 — progress(0~100) 또는 끝났을 때 서버 응답 body */
export interface UploadEvent {
  progress?: number;
  body?: any;
}

/** 업로드 실패 — status 0 은 네트워크 끊김, 413 은 프록시 본문 한도 초과 */
export interface UploadError {
  status: number;
}

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
 *
 * [2-34 후속 — Android 에서 업로드가 안 되던 문제] capacitor.config 의 CapacitorHttp(enabled) 가 XHR·fetch 를 네이티브
 * HTTP 로 바꿔치기하는데, 다른 서버로 가는 FormData 파일은 JS 에서 통째로 base64 로 읽어 네이티브로 넘긴다
 * (@capacitor/android native-bridge.js convertFormData). 수십 MB 영상이면 WebView 메모리·브리지에서 실패하고
 * 진행률 이벤트도 오지 않는다. 그래서 이 업로드만 Capacitor 가 보관해 둔 원래 XHR(window.CapacitorWebXMLHttpRequest)로
 * 보낸다 — 웹뷰가 파일을 스트리밍으로 올린다. 브라우저(패치 없음)에서는 보통 XHR 과 같다.
 * 서버 CORS 는 모든 출처를 허용하고(실측), multipart 는 단순 요청이라 사전 요청도 없다.
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

  /**
   * 업로드 요청 — 진행률({ progress })을 흘려보내고 끝나면 서버 응답({ body: { result, jobId | msg } }).
   * 실패는 UploadError({ status }) 로 error 를 낸다. 구독을 끊으면 전송을 취소한다
   */
  uploadStarVideo(form: FormData): Observable<UploadEvent> {
    const url = `${this.apiBaseURL}/api/super/star/feed/add-media`;
    return new Observable<UploadEvent>(sub => {
      const original = (window as any).CapacitorWebXMLHttpRequest;
      const proto = XMLHttpRequest.prototype;
      // Capacitor 가 바꿔치기한 메서드 대신 원래 메서드로 연다·보낸다·취소한다 (원본이 없으면 = 브라우저, 보통 XHR)
      const open = original && original.open ? original.open : proto.open;
      const send = original && original.send ? original.send : proto.send;
      const abort = original && original.abort ? original.abort : proto.abort;
      const xhr = new XMLHttpRequest();
      let finished = false;

      xhr.upload.onprogress = (e: ProgressEvent) => {
        if (e.lengthComputable && e.total > 0) {
          sub.next({ progress: Math.min(100, Math.round((e.loaded / e.total) * 100)) });
        }
      };
      xhr.onload = () => {
        finished = true;
        if (xhr.status < 200 || xhr.status >= 300) {
          sub.error({ status: xhr.status } as UploadError);
          return;
        }
        let body: any = null;
        try {
          body = JSON.parse(xhr.responseText);
        } catch (e) {
          body = null;
        }
        sub.next({ body });
        sub.complete();
      };
      xhr.onerror = () => {
        finished = true;
        sub.error({ status: 0 } as UploadError);
      };
      xhr.ontimeout = xhr.onerror;

      open.call(xhr, 'POST', url, true);
      send.call(xhr, form);
      return () => {
        if (!finished) {
          try { abort.call(xhr); } catch (e) { /* 이미 끝남 */ }
        }
      };
    });
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
