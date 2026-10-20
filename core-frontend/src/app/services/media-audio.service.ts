import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';

/**
 * 로비 영상 재생·소리 관리 (2-34차 1-3 "숏폼·세로형·가로형 자동재생 시 자동 사운드").
 *
 * - 앱 전체에서 소리를 맡은(play) 영상은 하나뿐이다. 다른 영상이 play() 를 요청하면 앞의 것을 멈춘다
 *   (Star Shorts·세로형·가로형·전체 화면 플레이어가 같은 서비스를 쓴다)
 * - 예외: 가로형 Hot Content 는 보이는 2장을 함께 재생한다(2-34 후속). 두 번째 장은 playMuted 로 소리 없이 돈다
 * - 기본은 소리 켬. 사용자가 음소거 아이콘을 누르면 앱 전체가 음소거로 바뀌고 앱을 다시 켤 때까지 유지된다
 * - Capacitor 웹뷰는 사용자 제스처 없는 소리 재생을 허용한다 (Android Bridge.setMediaPlaybackRequiresUserGesture(false),
 *   iOS mediaTypesRequiringUserActionForPlayback = []). 그래도 거부되면(NotAllowedError — 개발용 브라우저 등)
 *   음소거로 재생하고 첫 터치에 소리를 켠다
 * - 소리를 막을 때는 volume 이 아니라 muted 를 쓴다 (iOS 웹뷰는 volume 을 무시한다 — 2-31차 틱 소리 사례)
 * - 2-35차: 스타페이지·콘텐츠 상세도 이 서비스로 소리 켜고 재생한다. 앱이 백그라운드로 가면 재생 담당 영상을 멈추고
 *   돌아오면 다시 튼다 (로비는 자체 생명주기로도 멈추고 켠다 — 중복 호출은 무해)
 */
@Injectable({ providedIn: 'root' })
export class MediaAudioService {
  /** 사용자가 고른 소리 상태. true = 소리 켬 (기본) */
  readonly soundOn$ = new BehaviorSubject<boolean>(true);

  private current: HTMLVideoElement | null = null;
  /** 소리 켠 재생이 거부돼 첫 터치를 기다리는 중 */
  private waitingForGesture = false;
  private readonly unlockEvents = ['pointerdown', 'touchend', 'click'];

  /** 백그라운드로 가며 이 서비스가 멈춘 영상 — 돌아오면 다시 튼다 */
  private pausedForBackground: HTMLVideoElement | null = null;

  constructor(private ngZone: NgZone) {
    if (Capacitor.isNativePlatform()) {
      App.addListener('appStateChange', ({ isActive }) => this.ngZone.run(() => this.onAppState(isActive)));
    }
  }

  private onAppState(isActive: boolean): void {
    if (!isActive) {
      const video = this.current;
      if (video && !video.paused) {
        video.pause();
        this.pausedForBackground = video;
      }
      return;
    }
    const video = this.pausedForBackground;
    this.pausedForBackground = null;
    // 그 사이 다른 영상이 재생을 맡았거나 화면에서 사라졌으면 건드리지 않는다
    if (video && video.isConnected && this.current === video && video.paused) {
      this.play(video);
    }
  }

  get soundOn(): boolean {
    return this.soundOn$.value;
  }

  /** 지금 재생을 맡은 영상인지 */
  isCurrent(video: HTMLVideoElement | null | undefined): boolean {
    return !!video && video === this.current;
  }

  /**
   * 이 영상을 재생한다. 다른 영상이 재생 중이면 멈춘다.
   * 소리 켬 상태면 소리와 함께 시도하고, 거부되면 음소거로 재생한 뒤 첫 터치에 소리를 켠다.
   * 음소거로도 거부되면 false (호출한 쪽이 재생 버튼을 띄운다). AbortError(곧바로 다른 영상으로 넘어감)는 true 로 본다
   */
  async play(video: HTMLVideoElement): Promise<boolean> {
    if (this.current && this.current !== video && !this.current.paused) {
      this.current.pause();
    }
    this.current = video;

    const wantSound = this.soundOn && !this.waitingForGesture;
    this.applyMuted(video, !wantSound);
    try {
      await video.play();
      return true;
    } catch (err: any) {
      if (err && err.name === 'AbortError') return true;
      if (!wantSound || this.current !== video) return false;
      // 소리 켠 재생이 막혔다 — 음소거로 다시 시도하고 첫 터치에 소리를 켠다
      this.applyMuted(video, true);
      this.armGestureUnlock();
      try {
        await video.play();
        return true;
      } catch (e: any) {
        return !!(e && e.name === 'AbortError');
      }
    }
  }

  /**
   * 소리 없이 함께 재생 (가로형 두 번째 카드). 재생 담당(current)은 바꾸지 않고 다른 영상도 멈추지 않는다.
   * 이 영상이 재생 담당이었다면 담당에서 뺀다 — 소리는 첫 번째 카드만 낸다
   */
  playMuted(video: HTMLVideoElement): void {
    if (this.current === video) this.current = null;
    this.applyMuted(video, true);
    if (video.paused) video.play().catch(() => { /* 자동재생 거부 — 포스터가 남는다 */ });
  }

  /** 이 영상을 멈추고 재생 담당에서 뺀다 (화면 밖·로비 이탈) */
  pause(video: HTMLVideoElement | null | undefined): void {
    if (!video) return;
    if (!video.paused) video.pause();
    if (this.current === video) this.current = null;
  }

  /** 음소거 아이콘 — 앱 전체 소리 상태를 뒤집고 지금 재생 중인 영상에 바로 적용한다 */
  toggleSound(): void {
    const next = !this.soundOn;
    this.soundOn$.next(next);
    if (next) this.waitingForGesture = false; // 아이콘 터치 자체가 사용자 제스처
    if (this.current) {
      this.applyMuted(this.current, !next);
      if (next && this.current.paused) this.current.play().catch(() => { });
    }
  }

  /** 화면에 그릴 음소거 상태 — 소리 켬이어도 첫 터치를 기다리는 중이면 음소거로 보인다 */
  isEffectivelyMuted(): boolean {
    return !this.soundOn || this.waitingForGesture;
  }

  private applyMuted(video: HTMLVideoElement, muted: boolean): void {
    // iOS 는 속성(attribute)만으로는 부족하고 프로퍼티까지 맞춰야 한다
    video.muted = muted;
    video.defaultMuted = muted;
  }

  private armGestureUnlock(): void {
    if (this.waitingForGesture) return;
    this.waitingForGesture = true;
    this.ngZone.runOutsideAngular(() => {
      this.unlockEvents.forEach(type => document.addEventListener(type, this.onGesture, { capture: true, passive: true }));
    });
  }

  private onGesture = () => {
    this.unlockEvents.forEach(type => document.removeEventListener(type, this.onGesture, { capture: true } as any));
    this.ngZone.run(() => {
      this.waitingForGesture = false;
      const video = this.current;
      if (!video || !this.soundOn) return;
      // 제스처 안에서 바로 소리를 켠다 — 이 호출이 늦으면 iOS 가 다시 막는다
      this.applyMuted(video, false);
      video.play().catch(() => {
        // 그래도 막히면 음소거로 계속 보여 주고 다음 터치를 기다린다
        this.applyMuted(video, true);
        video.play().catch(() => { });
        this.armGestureUnlock();
      });
    });
  };
}
