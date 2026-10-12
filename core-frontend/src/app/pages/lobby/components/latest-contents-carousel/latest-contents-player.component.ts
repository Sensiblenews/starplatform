import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { LatestContentItem } from '../../latest-contents-logic';

/**
 * 로비 숏폼 전체 화면 플레이어 (2-33차 — 요청서 "콘텐츠 선택 시 전체 화면 재생").
 * 로비 카드의 영상을 누르면 모달로 뜬다. 소리 켜고 자동 재생을 시도하고, 거부되면 음소거로 재생한 뒤
 * "Tap for sound" 버튼을 보여 준다. 닫기 외에 게시자 StarPage 로 가는 버튼만 둔다(게시물 상세 버튼은 사용자 요청으로 제외).
 * 이동은 모달을 닫으면서 { navigate } 로 돌려주고 로비가 라우팅한다 (모달 위에서 라우팅하면 모달이 남는다).
 */
@Component({
  selector: 'app-latest-contents-player',
  template: `
    <div class="player" *ngIf="item">
      <video #video class="player-video" controls playsinline loop
        [attr.src]="item.mediaUrl" [attr.poster]="item.thumbUrl || null"
        (playing)="isPlaying = true"></video>

      <button class="player-close" (click)="close()" aria-label="Close">
        <ion-icon name="close"></ion-icon>
      </button>

      <button class="player-unmute" *ngIf="needsUnmute" (click)="unmute()">
        <ion-icon name="volume-mute"></ion-icon> Tap for sound
      </button>

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
    .player-footer {
      position: absolute; left: 0; right: 0; bottom: 0; z-index: 2;
      /* 네이티브 컨트롤 바 위에 놓이도록 아래 여백을 둔다 */
      padding: 14px 16px calc(64px + env(safe-area-inset-bottom));
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
  @Input() item: LatestContentItem;
  @ViewChild('video') video: ElementRef<HTMLVideoElement>;

  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  isPlaying = false;
  /** 소리 켠 자동 재생이 거부돼 음소거로 돌고 있는 상태 */
  needsUnmute = false;

  constructor(private modalCtrl: ModalController) { }

  ngAfterViewInit(): void {
    const video = this.video ? this.video.nativeElement : null;
    if (!video) return;
    // 사용자가 카드를 눌러 열었으므로 소리 켠 재생을 먼저 시도한다 (탭 직후라 대부분 허용된다).
    // 거부되면(iOS 일부) 음소거로 재생하고 버튼으로 소리를 켜게 한다
    video.muted = false;
    const attempt = video.play();
    if (attempt && typeof attempt.catch === 'function') {
      attempt.catch(() => {
        video.muted = true;
        this.needsUnmute = true;
        video.play().catch(() => { /* 포스터와 네이티브 컨트롤로 사용자가 직접 재생 */ });
      });
    }
  }

  ngOnDestroy(): void {
    const video = this.video ? this.video.nativeElement : null;
    if (video) {
      video.pause();
      video.removeAttribute('src');
      try { video.load(); } catch (e) { /* 이미 비어 있음 */ }
    }
  }

  unmute(): void {
    const video = this.video ? this.video.nativeElement : null;
    if (!video) return;
    video.muted = false;
    this.needsUnmute = false;
    video.play().catch(() => { });
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
