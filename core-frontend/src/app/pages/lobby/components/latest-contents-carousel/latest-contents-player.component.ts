import { AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Share } from '@capacitor/share';
import { HttpService } from 'src/app/services/http.service';
import { DeviceIdService } from 'src/app/services/device-id.service';
import { HapticService } from 'src/app/services/haptic.service';
import { MediaAudioService } from 'src/app/services/media-audio.service';
import { CommentModalComponent } from 'src/app/pages/star-page/modals/comment-modal.component';
import { ContentStats, LatestContentItem, compactNumber, toContentStats } from '../../latest-contents-logic';

/**
 * 로비 숏폼 전체 화면 플레이어 (2-33차 — 요청서 "콘텐츠 선택 시 전체 화면 재생", 2-34차 오른쪽 액션 열 추가).
 * 로비 카드의 영상을 누르면 모달로 뜬다. 재생·소리는 로비와 같은 MediaAudioService (2-34차) —
 * 소리 켬 상태면 소리와 함께 재생하고, 사용자가 음소거해 뒀거나 거부되면 "Tap for sound" 버튼을 보여 준다.
 *
 * 오른쪽 중간~아래 세로 열: 조회수 · 좋아요 · 댓글 · 저장 · 공유 + 숫자 (2-34차 1-4).
 * - 좋아요·댓글·공유는 스타 페이지와 같은 기존 API(/like/toggle, 댓글 모달, /share/add)
 * - 조회수·저장은 /api/super/content/view·save. 숫자는 열릴 때 /content/stats 로 받는다(목록은 60초 캐시라 늦다)
 * - 숫자 조회가 실패하면 열 전체를 숨기고 재생만 한다
 * 이동은 모달을 닫으면서 { navigate } 로 돌려주고 로비가 라우팅한다 (모달 위에서 라우팅하면 모달이 남는다).
 */
@Component({
  selector: 'app-latest-contents-player',
  template: `
    <div class="player" *ngIf="item">
      <video #video class="player-video" controls playsinline loop
        [attr.src]="item.mediaUrl" [attr.poster]="item.thumbUrl || null"
        (playing)="onPlaying()"></video>

      <button class="player-close" (click)="close()" aria-label="Close">
        <ion-icon name="close"></ion-icon>
      </button>

      <button class="player-unmute" *ngIf="needsUnmute" (click)="unmute()">
        <ion-icon name="volume-mute"></ion-icon> Tap for sound
      </button>

      <div class="player-actions" *ngIf="stats as s">
        <div class="player-action is-static" aria-label="Views">
          <ion-icon name="eye-outline"></ion-icon>
          <span>{{ format(s.viewCnt) }}</span>
        </div>
        <button class="player-action" [class.is-on]="s.liked" (click)="toggleLike()" aria-label="Like">
          <ion-icon [name]="s.liked ? 'heart' : 'heart-outline'"></ion-icon>
          <span>{{ format(s.likeCnt) }}</span>
        </button>
        <button class="player-action" (click)="openComments()" aria-label="Comments">
          <ion-icon name="chatbubble-ellipses-outline"></ion-icon>
          <span>{{ format(s.commentCnt) }}</span>
        </button>
        <button class="player-action" [class.is-saved]="s.saved" (click)="toggleSave()" aria-label="Save">
          <ion-icon [name]="s.saved ? 'bookmark' : 'bookmark-outline'"></ion-icon>
          <span>{{ format(s.saveCnt) }}</span>
        </button>
        <button class="player-action" (click)="share()" aria-label="Share">
          <ion-icon name="arrow-redo-outline"></ion-icon>
          <span>{{ format(s.shareCnt) }}</span>
        </button>
      </div>

      <div class="player-footer">
        <div class="player-author" (click)="goProfile()">
          <img class="player-avatar" [src]="item.starImage || defaultAvatar" (error)="handleImageError($event)" alt="" />
          <div class="player-author-text">
            <div class="player-name">{{ item.starName }}</div>
            <div class="player-sub">StarPage <ion-icon name="chevron-forward"></ion-icon></div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; height: 100%; background: #000; }
    .player { position: relative; width: 100%; height: 100%; background: #000; display: flex; align-items: center; justify-content: center; }
    .player-video { width: 100%; height: 100%; object-fit: contain; background: #000; }
    .player-close {
      position: absolute; top: calc(12px + env(safe-area-inset-top)); right: 12px; z-index: 2;
      width: 40px; height: 40px; border-radius: 50%; border: none; background: rgba(0,0,0,.55); color: #fff;
      display: flex; align-items: center; justify-content: center; font-size: 24px;
    }
    .player-unmute {
      position: absolute; top: calc(14px + env(safe-area-inset-top)); left: 12px; z-index: 2;
      display: inline-flex; align-items: center; gap: 6px; padding: 8px 12px; border-radius: 18px; border: none;
      background: rgba(0,0,0,.6); color: #fff; font-size: 13px; font-weight: 700;
      ion-icon { font-size: 16px; }
    }
    /* 오른쪽 중간~아래 세로 액션 열. 네이티브 컨트롤 바·게시자 줄 위에서 끝나게 아래 여백을 둔다 */
    .player-actions {
      position: absolute; right: 8px; z-index: 3;
      bottom: calc(132px + env(safe-area-inset-bottom));
      display: flex; flex-direction: column; align-items: center; gap: 14px;
    }
    .player-action {
      display: flex; flex-direction: column; align-items: center; gap: 3px;
      min-width: 48px; padding: 2px; border: 0; background: transparent; color: #fff;
      ion-icon { font-size: 28px; filter: drop-shadow(0 1px 3px rgba(0,0,0,.55)); }
      span { font-size: 12px; font-weight: 700; line-height: 1.1; text-shadow: 0 1px 3px #000; }
    }
    .player-action.is-on ion-icon { color: #ff4d6d; }
    .player-action.is-saved ion-icon { color: #ffd34f; }
    .player-footer {
      position: absolute; left: 0; right: 0; bottom: 0; z-index: 2;
      /* 네이티브 컨트롤 바 위에 놓이도록 아래 여백을 둔다. 오른쪽은 액션 열 자리 */
      padding: 14px 72px calc(64px + env(safe-area-inset-bottom)) 16px;
      display: flex; align-items: center; gap: 12px;
      background: linear-gradient(to top, rgba(0,0,0,.7), rgba(0,0,0,0));
      pointer-events: none;
      > * { pointer-events: auto; }
    }
    .player-author { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; color: #fff; }
    .player-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; border: 2px solid #fff; background: #333; flex-shrink: 0; }
    .player-author-text { min-width: 0; }
    .player-name { font-size: 16px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-shadow: 0 1px 2px rgba(0,0,0,.6); }
    .player-sub { display: inline-flex; align-items: center; gap: 2px; font-size: 12px; color: rgba(255,255,255,.75); ion-icon { font-size: 11px; } }
  `],
})
export class LatestContentsPlayerComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() item: LatestContentItem;
  @ViewChild('video') video: ElementRef<HTMLVideoElement>;

  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  isPlaying = false;
  /** 소리 켠 자동 재생이 거부돼 음소거로 돌고 있는 상태 */
  needsUnmute = false;
  /** 오른쪽 숫자. 불러오기 전·실패 시 null → 액션 열을 숨긴다 */
  stats: ContentStats | null = null;

  private deviceId = '';
  private viewReported = false;
  /** 좋아요·저장 연타 방지 — 응답 전 재요청을 막는다 */
  private likeBusy = false;
  private saveBusy = false;

  constructor(
    private modalCtrl: ModalController,
    private http: HttpService,
    private deviceIdService: DeviceIdService,
    private haptic: HapticService,
    private audio: MediaAudioService,
  ) { }

  ngOnInit(): void {
    const load = (deviceId: string) => {
      this.deviceId = this.effectiveId(deviceId);
      this.loadStats();
    };
    const cached = this.deviceIdService.get();
    if (cached) {
      load(cached);
    } else {
      this.deviceIdService.resolve().then(id => load(id || '')).catch(() => load(''));
    }
  }

  /** 스타 페이지 getEffectiveDeviceId 와 같은 규칙 — 스타 로그인 중이면 스타 ID 로 좋아요·저장한다 */
  private effectiveId(deviceId: string): string {
    try {
      const starId = localStorage.getItem('starId');
      if (localStorage.getItem('isStar') === 'true' && starId) return starId;
    } catch (e) { /* 저장소 접근 불가 — 기기 ID 사용 */ }
    return deviceId;
  }

  ngAfterViewInit(): void {
    const video = this.video ? this.video.nativeElement : null;
    if (!video) return;
    // 로비 영상은 모달을 열기 전에 로비가 멈춘다. 서비스가 이 영상을 재생 담당으로 잡고 소리 상태를 맞춘다.
    // 끝까지 거부되면 포스터와 네이티브 컨트롤로 사용자가 직접 재생한다
    this.audio.play(video).then(() => {
      this.needsUnmute = this.audio.isEffectivelyMuted();
    });
  }

  ngOnDestroy(): void {
    const video = this.video ? this.video.nativeElement : null;
    if (video) {
      this.audio.pause(video);
      video.removeAttribute('src');
      try { video.load(); } catch (e) { /* 이미 비어 있음 */ }
    }
  }

  format(value: number): string {
    return compactNumber(value);
  }

  // ===== 숫자 =====

  private loadStats(): void {
    if (!this.item) return;
    this.http.post<any>('/api/super/content/stats', { conId: this.item.conId, deviceId: this.deviceId }).subscribe({
      next: res => {
        if (res && res.result === 'OK') this.stats = toContentStats(res.stats);
      },
      error: () => { /* 숫자 없이 재생만 */ },
    });
  }

  /** 실제 재생이 시작되면 조회 1회 보고. 서버가 기기·30분 단위로 중복을 거른다 */
  onPlaying(): void {
    this.isPlaying = true;
    if (this.viewReported || !this.item || !this.deviceId) return;
    this.viewReported = true;
    this.http.post<any>('/api/super/content/view', { conId: this.item.conId, deviceId: this.deviceId }).subscribe({
      next: res => {
        if (res && res.counted && this.stats) this.stats.viewCnt += 1;
      },
      error: () => { },
    });
  }

  // ===== 액션 =====

  /** 좋아요 — 스타 페이지 글 좋아요와 같은 API(기기 단위 토글). 응답 전에 화면을 먼저 바꾸고 실패 시 되돌린다 */
  toggleLike(): void {
    const s = this.stats;
    if (!s || this.likeBusy || !this.deviceId) return;
    this.likeBusy = true;
    this.haptic.tap();
    const before = { liked: s.liked, likeCnt: s.likeCnt };
    s.liked = !s.liked;
    s.likeCnt = Math.max(0, s.likeCnt + (s.liked ? 1 : -1));
    this.http.post<any>('/api/super/like/toggle', { targetType: 'FEED', targetId: this.item.conId, deviceId: this.deviceId }).subscribe({
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

  /** 댓글 — 스타 페이지와 같은 댓글 모달을 위에 띄운다. 닫을 때 받은 개수로 숫자 갱신 */
  async openComments(): Promise<void> {
    if (!this.stats || !this.deviceId) return;
    const modal = await this.modalCtrl.create({
      component: CommentModalComponent,
      componentProps: { type: 'feed', targetId: this.item.conId, deviceId: this.deviceId },
      initialBreakpoint: 1,
      breakpoints: [0, 1],
    });
    await modal.present();
    const { data } = await modal.onDidDismiss();
    if (this.stats && data && typeof data.count === 'number') this.stats.commentCnt = data.count;
  }

  /** 저장 / 저장 취소. 저장 수는 서버가 돌려준 실제 값으로 맞춘다 */
  toggleSave(): void {
    const s = this.stats;
    if (!s || this.saveBusy || !this.deviceId) return;
    this.saveBusy = true;
    this.haptic.tap();
    const before = { saved: s.saved, saveCnt: s.saveCnt };
    s.saved = !s.saved;
    s.saveCnt = Math.max(0, s.saveCnt + (s.saved ? 1 : -1));
    this.http.post<any>('/api/super/content/save', { conId: this.item.conId, deviceId: this.deviceId, saved: s.saved }).subscribe({
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
    const link = `https://witch-hunting.com/post/${this.item.conId}`;
    try {
      await Share.share({ title: 'Star Platform', url: link });
      this.http.post('/api/super/share/add', { targetType: 'FEED', targetId: this.item.conId }).subscribe({ next: () => { }, error: () => { } });
      if (this.stats) this.stats.shareCnt += 1;
    } catch (e) {
      // 사용자가 공유 시트를 닫은 경우 — 조용히 무시
    }
  }

  // ===== 소리·이동 =====

  /** "Tap for sound" — 사용자가 음소거해 뒀으면 앱 전체 소리를 다시 켠다 (로비 카드 아이콘과 같은 상태) */
  unmute(): void {
    const video = this.video ? this.video.nativeElement : null;
    if (!video) return;
    if (!this.audio.soundOn) {
      this.audio.toggleSound();
    } else {
      // 소리 켬인데 막혀 있던 경우 — 이 터치가 제스처라 바로 켠다
      video.muted = false;
      video.defaultMuted = false;
      video.play().catch(() => { });
    }
    this.needsUnmute = false;
  }

  close(): void {
    this.modalCtrl.dismiss();
  }

  goProfile(): void {
    this.modalCtrl.dismiss({ navigate: ['/star', this.item.prsId], reason: 'PROFILE_CLICK' });
  }

  handleImageError(event: any): void {
    if (event && event.target) event.target.src = this.defaultAvatar;
  }
}
