import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { HttpService } from './http.service';

// 서버 응답 모양 (POST /api/super/dm/*)
export interface DmRoom {
  peerId: string;
  peerName: string;
  peerImage: string | null;
  lastType: 'TEXT' | 'IMAGE' | 'VIDEO';
  lastText: string | null;
  lastDate: number | string;
  unread: number;
}

export interface DmMessage {
  msgId: number;
  senderId: string;
  contentType: 'TEXT' | 'IMAGE' | 'VIDEO';
  text: string | null;
  fileUrl?: string;   // 상대 경로 + 단기 토큰. fileUrl()로 절대 주소를 만든다
  thumbUrl?: string;
  sendDate: number | string;
  readDate: number | string | null;
  expireAt: number | string;
}

export interface DmPeer {
  id: string;
  name: string;
  image: string | null;
}

/**
 * 1:1 메신저 API + 미읽음 상태 (2-29차).
 * 모든 요청은 localStorage의 starId·starToken을 body에 실어 보내고, 서버가 본인 여부를 검증한다.
 * 모달 열기는 순환 import를 피하려고 각 모달 파일의 open* 헬퍼가 맡는다.
 */
@Injectable({ providedIn: 'root' })
export class DmService {

  /** 앱 안 점 표시용 미읽음 수 (로비 헤더 아바타). 앱 아이콘 뱃지는 푸시 badge=1 + 복귀 시 clear로 따로 돈다 */
  readonly unread$ = new BehaviorSubject<number>(0);
  /** 내 프로필 사진 (로비 헤더 아바타). unread-count 응답에 실려 온다 */
  readonly myImage$ = new BehaviorSubject<string>('');

  constructor(private http: HttpService) { }

  /** 스타 소유자로 로그인돼 있는가 (토큰까지 있어야 API가 통과한다) */
  get isSignedIn(): boolean {
    return localStorage.getItem('isStar') === 'true'
      && !!localStorage.getItem('starId')
      && !!localStorage.getItem('starToken');
  }

  get myId(): string {
    return localStorage.getItem('starId') || '';
  }

  /** 상대 페이지에서 채팅 아이콘을 보여줄 조건: 내가 스타로 로그인 + 내 페이지가 아님 */
  canMessage(pageStarId: string): boolean {
    return this.isSignedIn && !!pageStarId && pageStarId !== this.myId;
  }

  /** 서버가 준 상대 경로(/api/super/dm/file?t=...) → 절대 주소 */
  fileUrl(relative: string | undefined | null): string {
    if (!relative) return '';
    return environment.apiBaseURL + relative;
  }

  /**
   * 미읽음 수·내 프로필 사진 갱신. 사진은 토큰이 없어도(매직 로그인) 받아오므로 starId만 있으면 요청한다.
   * 서버는 토큰이 맞을 때만 미읽음을 센다.
   */
  refreshUnread(): void {
    if (localStorage.getItem('isStar') !== 'true' || !this.myId) {
      this.unread$.next(0);
      return;
    }
    this.post('/api/super/dm/unread-count', {}).subscribe({
      next: (res: any) => {
        if (res && res.result === 'OK') {
          this.unread$.next(Number(res.unread) || 0);
          if (res.myImage) this.myImage$.next(String(res.myImage));
        } else {
          console.warn('[dm] unread-count failed:', res && res.msg);
        }
      },
      error: (err) => console.warn('[dm] unread-count error:', err)
    });
  }

  rooms(): Observable<any> {
    return this.post('/api/super/dm/rooms', {});
  }

  messages(peerId: string): Observable<any> {
    return this.post('/api/super/dm/messages', { peerId });
  }

  sendText(peerId: string, text: string): Observable<any> {
    return this.post('/api/super/dm/send', { peerId, text });
  }

  sendFile(peerId: string, kind: 'IMAGE' | 'VIDEO', base64: string, mime: string): Observable<any> {
    return this.post('/api/super/dm/upload', { peerId, kind, base64, mime });
  }

  markRead(peerId: string): Observable<any> {
    return this.post('/api/super/dm/read', { peerId });
  }

  private post(path: string, body: any): Observable<any> {
    const auth = {
      starId: localStorage.getItem('starId') || '',
      starToken: localStorage.getItem('starToken') || '',
    };
    return this.http.post(path, { ...auth, ...body }, { needToken: true, contentType: 'json' });
  }
}
