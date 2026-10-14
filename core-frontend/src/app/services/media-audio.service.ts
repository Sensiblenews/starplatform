import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * 로비 영상 재생·소리 관리 (2-34차 1-3 "숏폼·세로형·가로형 자동재생 시 자동 사운드").
 *
 * - 앱 전체에서 재생 중인 영상은 하나뿐이다. 다른 영상이 play() 를 요청하면 앞의 것을 멈춘다
 *   (Star Shorts·세로형·가로형·전체 화면 플레이어가 같은 서비스를 쓴다)
 * - 기본은 소리 켬. 사용자가 음소거 아이콘을 누르면 앱 전체가 음소거로 바뀌고 앱을 다시 켤 때까지 유지된다
 * - Capacitor 웹뷰는 사용자 제스처 없는 소리 재생을 허용한다 (Android Bridge.setMediaPlaybackRequiresUserGesture(false),
 *   iOS mediaTypesRequiringUserActionForPlayback = []). 그래도 거부되면(NotAllowedError — 개발용 브라우저 등)
 *   음소거로 재생하고 첫 터치에 소리를 켠다
 * - 소리를 막을 때는 volume 이 아니라 muted 를 쓴다 (iOS 웹뷰는 volume 을 무시한다 — 2-31차 틱 소리 사례)
 */
@Injectable({ providedIn: 'root' })
export class MediaAudioService {
  /** 사용자가 고른 소리 상태. true = 소리 켬 (기본) */
  readonly soundOn$ = new BehaviorSubject<boolean>(true);

  private current: HTMLVideoElement | null = null;
  /** 소리 켠 재생이 거부돼 첫 터치를 기다리는 중 */
  private waitingForGesture = false;
  private readonly unlockEvents = ['pointerdown', 'touchend', 'click'];

  constructor(private ngZone: NgZone) { }

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
