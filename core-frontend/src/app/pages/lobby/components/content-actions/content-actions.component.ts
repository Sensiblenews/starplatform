import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Share } from '@capacitor/share';
import { HttpService } from 'src/app/services/http.service';
import { DeviceIdService } from 'src/app/services/device-id.service';
import { HapticService } from 'src/app/services/haptic.service';
import { CommentModalComponent } from 'src/app/pages/star-page/modals/comment-modal.component';
import { ContentStats, compactNumber, toContentStats } from '../../latest-contents-logic';

/**
 * 숏폼 오른쪽 세로 액션 열: 조회수 · 좋아요 · 댓글 · 저장 · 공유 + 숫자 (2-34차 1-4).
 * 2-34 후속: 전체 화면 플레이어뿐 아니라 로비 Star Shorts 카드에도 같은 열을 붙인다 — 두 곳이 이 컴포넌트를 쓴다.
 *
 * - 좋아요·댓글·공유는 스타 페이지와 같은 기존 API(/like/toggle, 댓글 모달, /share/add)
 * - 조회수·저장은 /api/super/content/view·save. 숫자는 게시물이 바뀔 때 /content/stats 로 받는다(목록은 60초 캐시라 늦다)
 * - 숫자 조회가 실패하면 열을 숨긴다
 * - 댓글 모달이 떠 있는 동안 busyChange(true) — 로비 카드는 그동안 자동 이동을 멈춘다
 */
@Component({
  selector: 'app-content-actions',
  template: `
    <div class="ca" [class.ca-card]="size === 'card'" *ngIf="stats as s" (click)="$event.stopPropagation()">
      <div class="ca-item is-static" aria-label="Views">
        <ion-icon name="eye-outline"></ion-icon>
        <span>{{ format(s.viewCnt) }}</span>
      </div>
      <button class="ca-item" [class.is-on]="s.liked" (click)="toggleLike()" aria-label="Like">
        <ion-icon [name]="s.liked ? 'heart' : 'heart-outline'"></ion-icon>
        <span>{{ format(s.likeCnt) }}</span>
      </button>
      <button class="ca-item" (click)="openComments()" aria-label="Comments">
        <ion-icon name="chatbubble-ellipses-outline"></ion-icon>
        <span>{{ format(s.commentCnt) }}</span>
      </button>
      <button class="ca-item" [class.is-saved]="s.saved" (click)="toggleSave()" aria-label="Save">
        <ion-icon [name]="s.saved ? 'bookmark' : 'bookmark-outline'"></ion-icon>
        <span>{{ format(s.saveCnt) }}</span>
      </button>
      <button class="ca-item" (click)="share()" aria-label="Share">
        <ion-icon name="arrow-redo-outline"></ion-icon>
        <span>{{ format(s.shareCnt) }}</span>
      </button>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ca { display: flex; flex-direction: column; align-items: center; gap: 14px; }
    .ca-item {
      display: flex; flex-direction: column; align-items: center; gap: 3px;
      min-width: 48px; padding: 2px; border: 0; background: transparent; color: #fff;
    }
    .ca-item ion-icon { font-size: 28px; filter: drop-shadow(0 1px 3px rgba(0,0,0,.55)); }
    .ca-item span { font-size: 12px; font-weight: 700; line-height: 1.1; text-shadow: 0 1px 3px #000; }
    /* 로비 카드용 — 카드 높이 안에 들어가도록 한 단계 작게 */
    .ca-card { gap: 8px; }
    .ca-card .ca-item { min-width: 36px; padding: 1px; gap: 2px; }
    .ca-card .ca-item ion-icon { font-size: 22px; }
    .ca-card .ca-item span { font-size: 10px; }
    .ca-item.is-on ion-icon { color: #ff4d6d; }
    .ca-item.is-saved ion-icon { color: #ffd34f; }
  `],
})
export class ContentActionsComponent implements OnChanges {
  /** 게시물 ID (WH_STAR_CONTENT.CON_ID) */
  @Input() conId: number | null = null;
  /** full = 전체 화면 플레이어, card = 로비 Star Shorts 카드(작게) */
  @Input() size: 'full' | 'card' = 'full';
  /** 댓글 모달이 열리고 닫힐 때 */
  @Output() busyChange = new EventEmitter<boolean>();

  /** 오른쪽 숫자. 불러오기 전·실패 시 null → 열을 숨긴다 */
  stats: ContentStats | null = null;

  private deviceId = '';
  private deviceReady: Promise<void>;
  /** 이 화면에서 조회를 이미 보고한 게시물 (서버도 기기·30분 단위로 거른다) */
  private readonly viewed = new Set<number>();
  /** 좋아요·저장 연타 방지 — 응답 전 재요청을 막는다 */
  private likeBusy = false;
  private saveBusy = false;
  /** 게시물이 빠르게 바뀔 때 늦게 온 숫자 응답을 버리기 위한 번호 */
  private loadSeq = 0;

  constructor(
    private modalCtrl: ModalController,
    private http: HttpService,
    private deviceIdService: DeviceIdService,
    private haptic: HapticService,
  ) {
    const cached = this.deviceIdService.get();
    this.deviceReady = cached
      ? Promise.resolve(this.setDevice(cached))
      : this.deviceIdService.resolve().then(id => this.setDevice(id || '')).catch(() => this.setDevice(''));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['conId']) {
      this.stats = null;
      this.deviceReady.then(() => this.loadStats());
    }
  }

  format(value: number): string {
    return compactNumber(value);
  }

  /** 실제 재생이 시작되면 부른다 — 게시물당 1회 조회 보고 */
  reportView(): void {
    const conId = this.conId;
    if (!conId || this.viewed.has(conId)) return;
    this.viewed.add(conId);
    this.deviceReady.then(() => {
      if (!this.deviceId) return;
      this.http.post<any>('/api/super/content/view', { conId, deviceId: this.deviceId }).subscribe({
        next: res => {
          if (res && res.counted && this.stats && this.conId === conId) this.stats.viewCnt += 1;
        },
        error: () => { },
      });
    });
  }

  /** 스타 페이지 getEffectiveDeviceId 와 같은 규칙 — 스타 로그인 중이면 스타 ID 로 좋아요·저장한다 */
  private setDevice(deviceId: string): void {
    try {
      const starId = localStorage.getItem('starId');
      if (localStorage.getItem('isStar') === 'true' && starId) {
        this.deviceId = starId;
        return;
      }
    } catch (e) { /* 저장소 접근 불가 — 기기 ID 사용 */ }
    this.deviceId = deviceId;
  }

  private loadStats(): void {
    const conId = this.conId;
    if (!conId) return;
    const seq = ++this.loadSeq;
    this.http.post<any>('/api/super/content/stats', { conId, deviceId: this.deviceId }).subscribe({
      next: res => {
        if (seq !== this.loadSeq) return;
        if (res && res.result === 'OK') this.stats = toContentStats(res.stats);
      },
      error: () => { /* 숫자 없이 재생만 */ },
    });
  }

  /** 좋아요 — 응답 전에 화면을 먼저 바꾸고 실패 시 되돌린다 */
  toggleLike(): void {
    const s = this.stats;
    const conId = this.conId;
    if (!s || !conId || this.likeBusy || !this.deviceId) return;
    this.likeBusy = true;
    this.haptic.tap();
    const before = { liked: s.liked, likeCnt: s.likeCnt };
    s.liked = !s.liked;
    s.likeCnt = Math.max(0, s.likeCnt + (s.liked ? 1 : -1));
    this.http.post<any>('/api/super/like/toggle', { targetType: 'FEED', targetId: conId, deviceId: this.deviceId }).subscribe({
      next: res => {
        this.likeBusy = false;
        if (!res || res.result !== 'OK') {
          s.liked = before.liked;
          s.likeCnt = before.likeCnt;
        } else if (typeof res.isLiked === 'boolean' && res.isLiked !== s.liked) {
          // 다른 화면에서 이미 눌러 둔 경우 — 서버 상태에 맞춘다
          s.liked = res.isLiked;
          s.likeCnt = Math.max(0, before.likeCnt + (res.isLiked === before.liked ? 0 : (res.isLiked ? 1 : -1)));
        }
      },
      error: () => {
        this.likeBusy = false;
        s.liked = before.liked;
        s.likeCnt = before.likeCnt;
      },
    });
  }

  /** 댓글 — 스타 페이지와 같은 댓글 모달. 닫을 때 받은 개수로 숫자 갱신 */
  async openComments(): Promise<void> {
    const conId = this.conId;
    if (!this.stats || !conId || !this.deviceId) return;
    this.busyChange.emit(true);
    const modal = await this.modalCtrl.create({
      component: CommentModalComponent,
      componentProps: { type: 'feed', targetId: conId, deviceId: this.deviceId },
      initialBreakpoint: 1,
      breakpoints: [0, 1],
    });
    await modal.present();
    const { data } = await modal.onDidDismiss();
    this.busyChange.emit(false);
    if (this.stats && this.conId === conId && data && typeof data.count === 'number') this.stats.commentCnt = data.count;
  }

  /** 저장 / 저장 취소. 저장 수는 서버가 돌려준 실제 값으로 맞춘다 */
  toggleSave(): void {
    const s = this.stats;
    const conId = this.conId;
    if (!s || !conId || this.saveBusy || !this.deviceId) return;
    this.saveBusy = true;
    this.haptic.tap();
    const before = { saved: s.saved, saveCnt: s.saveCnt };
    s.saved = !s.saved;
    s.saveCnt = Math.max(0, s.saveCnt + (s.saved ? 1 : -1));
    this.http.post<any>('/api/super/content/save', { conId, deviceId: this.deviceId, saved: s.saved }).subscribe({
      next: res => {
        this.saveBusy = false;
        if (res && res.result === 'OK') {
          const fixed = toContentStats({ saveCnt: res.saveCnt, saved: res.saved });
          s.saved = fixed.saved;
          s.saveCnt = fixed.saveCnt;
        } else {
          s.saved = before.saved;
          s.saveCnt = before.saveCnt;
        }
      },
      error: () => {
        this.saveBusy = false;
        s.saved = before.saved;
        s.saveCnt = before.saveCnt;
      },
    });
  }

  /** 공유 — 게시물 상세와 같은 공개 주소(/post/{id})와 집계 API */
  async share(): Promise<void> {
    const conId = this.conId;
    if (!conId) return;
    this.busyChange.emit(true);
    try {
      await Share.share({ title: 'Star Platform', url: `https://witch-hunting.com/post/${conId}` });
      this.http.post('/api/super/share/add', { targetType: 'FEED', targetId: conId }).subscribe({ next: () => { }, error: () => { } });
      if (this.stats && this.conId === conId) this.stats.shareCnt += 1;
    } catch (e) {
      // 사용자가 공유 시트를 닫은 경우 — 조용히 무시
    } finally {
      this.busyChange.emit(false);
    }
  }
}
