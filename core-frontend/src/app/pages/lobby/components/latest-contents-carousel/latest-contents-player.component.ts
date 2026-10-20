import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { MediaAudioService } from 'src/app/services/media-audio.service';
import { LatestContentItem, videoPoster } from '../../latest-contents-logic';
import { ContentActionsComponent } from '../content-actions/content-actions.component';

/**
 * 로비 숏폼 전체 화면 플레이어 (2-33차 — 요청서 "콘텐츠 선택 시 전체 화면 재생", 2-34차 오른쪽 액션 열 추가).
 * 로비 카드의 영상을 누르면 모달로 뜬다. 재생·소리는 로비와 같은 MediaAudioService (2-34차) —
 * 소리 켬 상태면 소리와 함께 재생하고, 사용자가 음소거해 뒀거나 거부되면 "Tap for sound" 버튼을 보여 준다.
 *
 * 2-34 후속(클라이언트 피드백):
 * - 네이티브 컨트롤(가운데 재생·정지 오버레이)이 너무 오래 보여 없앴다. 열리자마자 재생하고, 화면을 누르면 일시정지/재개
 * - 오른쪽 액션 열(조회수·좋아요·댓글·저장·공유)은 로비 카드와 같은 ContentActionsComponent.
 *   맨 아래가 왼쪽 프로필 이름 줄까지 내려오도록 위치를 낮췄다
 * 이동은 모달을 닫으면서 { navigate } 로 돌려주고 로비가 라우팅한다 (모달 위에서 라우팅하면 모달이 남는다).
 */
@Component({
  selector: 'app-latest-contents-player',
  template: `
    <div class="player" *ngIf="item">
      <video #video class="player-video no-native-controls" playsinline loop webkit-playsinline disablepictureinpicture disableremoteplayback x-webkit-airplay="deny"
        [attr.src]="item.mediaUrl" [attr.poster]="videoPoster(item)"
        (click)="togglePause()" (playing)="onPlaying()" (pause)="isPaused = true"></video>

      <!-- 사용자가 직접 멈췄을 때만 가운데 재생 표시 (자동 재생 중에는 아무것도 덮지 않는다) -->
      <div class="player-paused" *ngIf="isPaused && userPaused" (click)="togglePause()">
        <ion-icon name="play"></ion-icon>
      </div>

      <button class="player-close" (click)="close()" aria-label="Close">
        <ion-icon name="close"></ion-icon>
      </button>

      <button class="player-unmute" *ngIf="needsUnmute" (click)="unmute()">
        <ion-icon name="volume-mute"></ion-icon> Tap for sound
      </button>

      <app-content-actions class="player-actions" [conId]="item.conId" size="full"></app-content-actions>

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
    .player-paused {
      position: absolute; inset: 0; z-index: 1; display: flex; align-items: center; justify-content: center;
      ion-icon { font-size: 64px; color: rgba(255,255,255,.9); filter: drop-shadow(0 2px 6px rgba(0,0,0,.5)); }
    }
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
    /* 오른쪽 세로 액션 열 — 맨 아래(공유 숫자)가 왼쪽 프로필 이름 줄과 같은 높이.
       이름 줄 하단 = 푸터 아래 여백(24px+안전 영역) + 아바타 높이 40px 의 절반 */
    .player-actions {
      position: absolute; right: 8px; z-index: 3;
      bottom: calc(44px + env(safe-area-inset-bottom));
    }
    .player-footer {
      position: absolute; left: 0; right: 0; bottom: 0; z-index: 2;
      /* 오른쪽은 액션 열 자리 */
      padding: 14px 72px calc(24px + env(safe-area-inset-bottom)) 16px;
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
export class LatestContentsPlayerComponent implements AfterViewInit, OnDestroy {
  /** 썸네일 없는 영상의 poster (2-35차 — Android 기본 회색 배너 방지) */
  readonly videoPoster = videoPoster;

  @Input() item: LatestContentItem;
  @ViewChild('video') video: ElementRef<HTMLVideoElement>;
  @ViewChild(ContentActionsComponent) actions: ContentActionsComponent;

  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  /** 소리 켠 자동 재생이 거부돼 음소거로 돌고 있는 상태 */
  needsUnmute = false;
  isPaused = false;
  /** 사용자가 화면을 눌러 멈췄는지 (자동 재생 대기·버퍼링 중에는 재생 표시를 띄우지 않는다) */
  userPaused = false;

  constructor(
    private modalCtrl: ModalController,
    private audio: MediaAudioService,
  ) { }

  ngAfterViewInit(): void {
    const video = this.videoEl();
    if (!video) return;
    // 로비 영상은 모달을 열기 전에 로비가 멈춘다. 서비스가 이 영상을 재생 담당으로 잡고 소리 상태를 맞춘다
    this.audio.play(video).then(ok => {
      this.needsUnmute = this.audio.isEffectivelyMuted();
      // 저전력 모드 등으로 끝내 막히면 가운데 재생 표시를 띄워 사용자가 누르게 한다
      if (!ok) {
        this.isPaused = true;
        this.userPaused = true;
      }
    });
  }

  ngOnDestroy(): void {
    const video = this.videoEl();
    if (video) {
      this.audio.pause(video);
      video.removeAttribute('src');
      try { video.load(); } catch (e) { /* 이미 비어 있음 */ }
    }
  }

  onPlaying(): void {
    this.isPaused = false;
    this.userPaused = false;
    if (this.actions) this.actions.reportView();
  }

  /** 화면 터치 — 일시정지 / 재개 (네이티브 컨트롤 대신) */
  togglePause(): void {
    const video = this.videoEl();
    if (!video) return;
    if (video.paused) {
      this.audio.play(video);
    } else {
      this.userPaused = true;
      this.audio.pause(video);
    }
  }

  /** "Tap for sound" — 사용자가 음소거해 뒀으면 앱 전체 소리를 다시 켠다 */
  unmute(): void {
    const video = this.videoEl();
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

  private videoEl(): HTMLVideoElement | null {
    return this.video ? this.video.nativeElement : null;
  }
}
