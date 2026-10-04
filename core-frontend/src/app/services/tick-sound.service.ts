import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';

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
 * 바로 play() 했다. 실패는 console.log 한 줄로 삼켜져 남지 않았고, soundOn 토글도
 * 로비·스타페이지에서는 무시되고 있었다. 판정과 재생을 여기로 모은다.
 *
 * ⚠️ 진단 정정. 처음에는 "iOS 웹뷰가 사용자 조작 없는 재생을 막아서 타이머 재생이
 * 차단된다"고 보고 아래 잠금 해제 로직을 넣었다. 그러나 Capacitor iOS 는 웹뷰를 만들 때
 * mediaTypesRequiringUserActionForPlayback 을 비워 두므로(CAPBridgeViewController.swift)
 * 자동재생 차단은 애초에 없었다. 잠금 해제는 무해해서 남겨 두지만, 클라이언트가 보고한
 * "소리가 났다 안 났다"의 원인은 아니다. 남은 후보는 무음 스위치와, 네이티브 동영상
 * 광고 재생 시 AdMob SDK 가 오디오 세션을 점유하는 경우다. 둘 다 실기기 확인이 필요하다.
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
    if (TickSoundService.needsUnlock()) this.armUnlock();
  }

  /**
   * 잠금 해제가 필요한 환경인지. Capacitor 네이티브(iOS·Android)는 웹뷰가 사용자 조작 없는
   * 재생을 허용하므로 필요 없다. 일반 모바일 브라우저에서만 의미가 있다.
   *
   * ⚠️ 2-31차 후속에서 확인한 버그: 예전에는 네이티브에서도 잠금 해제를 돌렸는데, 그 과정이
   * volume=0 으로 한 번 재생했다 멈추는 방식이었다. iOS 웹뷰는 미디어 볼륨을 1 로 잠가 두므로
   * volume=0 이 무시돼 **앱을 켜고 처음 화면을 터치하거나 스크롤하는 순간(touchend) 틱 소리가
   * 그대로 났다.** Android 는 volume=0 이 먹혀 조용했다. 클라이언트 보고 "첫 화면 터치·스크롤 시
   * 소리 — 안드로이드는 개선, iOS 는 그대로"의 정체가 이것이다.
   */
  static needsUnlock(): boolean {
    return !Capacitor.isNativePlatform();
  }

  /**
   * 무음 1회 재생으로 자동재생 잠금을 푼다. 소리를 막는 수단은 volume 이 아니라 muted 다 —
   * iOS 는 volume 을 무시하지만 muted 는 따른다. 순수 함수에 가깝게 떼어 테스트한다.
   * @return 잠금이 풀렸으면 true
   */
  static async silentPrime(audio: Pick<HTMLMediaElement, 'muted' | 'play' | 'pause' | 'currentTime'>): Promise<boolean> {
    const wasMuted = audio.muted;
    try {
      audio.muted = true;
      await audio.play();
      audio.pause();
      audio.currentTime = 0;
      return true;
    } catch {
      return false;
    } finally {
      audio.muted = wasMuted;
    }
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

  /** 사용자의 첫 조작을 기다렸다가 잠금을 푼다 (일반 브라우저 전용 — needsUnlock 참조) */
  private armUnlock(): void {
    if (typeof document === 'undefined') return;

    const handler = () => { void this.unlock(); };
    UNLOCK_EVENTS.forEach(type => {
      document.addEventListener(type, handler, true);
      this.removeUnlockListeners.push(() => document.removeEventListener(type, handler, true));
    });
  }

  /** 첫 조작에서 무음 재생으로 잠금을 푼다. 실패하면 리스너를 남겨 다음 조작 때 다시 시도한다 */
  private async unlock(): Promise<void> {
    if (this.unlocked) return;
    if (!(await TickSoundService.silentPrime(this.audio))) return; // 아직 안 풀렸다. 다음 조작 때 다시 시도
    this.unlocked = true;
    this.removeUnlockListeners.forEach(remove => remove());
    this.removeUnlockListeners = [];
  }
}
