import { Injectable } from '@angular/core';

/** 효과음 파일 경로 */
const TICK_SRC = 'assets/sounds/tick.mp3';
/** 재생 음량. 귀에 거슬리지 않는 선 */
const VOLUME = 0.65;
/**
 * 연속 재생을 억제하는 최소 간격.
 * HapticService 와 같은 값이다 — 진동과 소리가 한 쌍으로 움직여야 어긋나 보이지 않는다.
 */
const COOLDOWN_MS = 400;
/** 잠금 해제를 시도할 사용자 조작들 */
const UNLOCK_EVENTS = ['touchend', 'click', 'keydown'];

/**
 * 조회수 틱 효과음 단일 창구.
 *
 * 이전에는 로비·스타페이지가 재생할 때마다 `new Audio(...)` 로 새 엘리먼트를 만들고
 * 바로 play() 했다. iOS 웹뷰는 사용자 조작 없이 시작하는 오디오 재생을 막기 때문에,
 * 터치 직후에 나는 소리는 재생되고 조회수 자동 증가처럼 최대 29초 뒤 타이머로 터지는
 * 소리는 차단됐다. 클라이언트가 보고한 "소리가 났다 안 났다" 가 이 둘의 차이다.
 *
 * 해결: 엘리먼트를 하나만 만들어 재사용하고, 사용자의 첫 조작 때 그 엘리먼트를 한 번
 * 무음으로 재생해 잠금을 풀어둔다. iOS 의 제약은 엘리먼트 단위라, 한 번 풀린 엘리먼트는
 * 이후 타이머에서 재생해도 막히지 않는다. 매번 새로 만들면 이 효과를 볼 수 없다.
 *
 * ⚠️ 무음 모드에서는 여전히 소리가 나지 않는다. 웹뷰 오디오의 한계이며 코드로 못 넘는다.
 */
@Injectable({ providedIn: 'root' })
export class TickSoundService {
  private audio = new Audio(TICK_SRC);
  private unlocked = false;
  private lastPlayedAt = 0;
  private removeUnlockListeners: Array<() => void> = [];

  constructor() {
    this.audio.preload = 'auto';
    this.audio.volume = VOLUME;
    this.armUnlock();
  }

  /** 쿨다운 판정. 부수효과가 없어 그대로 단위 테스트한다 */
  static isWithinCooldown(lastPlayedAt: number, now: number, cooldownMs: number = COOLDOWN_MS): boolean {
    return now - lastPlayedAt < cooldownMs;
  }

  /** 우측 상단 토글 값. 읽지 못하면 켜진 것으로 본다 */
  static isSoundOn(): boolean {
    try {
      return localStorage.getItem('soundOn') !== 'false';
    } catch {
      return true;
    }
  }

  /**
   * 효과음을 재생한다. 토글이 꺼져 있거나 쿨다운에 걸리면 조용히 넘어간다.
   * @param now 시각 주입 — 테스트에서 고정값을 넣는다
   */
  async play(now: number = Date.now()): Promise<void> {
    if (!TickSoundService.isSoundOn()) return;
    if (TickSoundService.isWithinCooldown(this.lastPlayedAt, now)) return;

    this.lastPlayedAt = now;
    try {
      // 이전 재생이 남아 있으면 처음으로 되감는다
      this.audio.pause();
      this.audio.currentTime = 0;
      await this.audio.play();
    } catch (e) {
      console.warn('[Tick] 효과음 재생 실패', e);
    }
  }

  /** 사용자의 첫 조작을 기다렸다가 잠금을 푼다 */
  private armUnlock(): void {
    if (typeof document === 'undefined') return;

    const handler = () => { void this.unlock(); };
    UNLOCK_EVENTS.forEach(type => {
      document.addEventListener(type, handler, true);
      this.removeUnlockListeners.push(() => document.removeEventListener(type, handler, true));
    });
  }

  /**
   * 같은 엘리먼트를 무음으로 한 번 재생했다 멈춰 iOS 의 재생 잠금을 푼다.
   * 실패하면 리스너를 남겨 다음 조작 때 다시 시도한다.
   */
  private async unlock(): Promise<void> {
    if (this.unlocked) return;

    const volume = this.audio.volume;
    try {
      this.audio.volume = 0;
      await this.audio.play();
      this.audio.pause();
      this.audio.currentTime = 0;
      this.unlocked = true;
      this.removeUnlockListeners.forEach(remove => remove());
      this.removeUnlockListeners = [];
    } catch {
      // 아직 안 풀렸다. 다음 사용자 조작 때 다시 시도한다.
    } finally {
      this.audio.volume = volume;
    }
  }
}
