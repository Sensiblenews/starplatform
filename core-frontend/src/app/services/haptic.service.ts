import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Haptics, ImpactStyle } from '@capacitor/haptics';

/** 연속 호출을 억제하는 최소 간격 */
const COOLDOWN_MS = 400;

/**
 * 햅틱 피드백 단일 창구.
 *
 * 이전에는 로비·스타페이지·랭킹 모달·GlobalFeedbackService가 각자
 * `Haptics.impact({ style: ImpactStyle.Light })` 를 직접 불렀다. 두 가지 문제가 있었다.
 *
 * 1. 실패가 은폐됐다. `try { import(...).then(() => Haptics.impact(...)) } catch {}`
 *    형태라 try 가 Promise 바깥에 있어 rejection 을 잡지 못했고, .catch() 도 없어
 *    unhandled rejection 으로 사라졌다. 로그가 한 줄도 남지 않아 원인 추적이 막혔다.
 *
 * 2. 쿨다운이 GlobalFeedbackService 에만 있었다. 로비·스타페이지의 조회수 틱은
 *    신규 조회 1건당 1회씩 상한 없이 호출했다.
 *
 * iOS 진동 이력 (클라이언트 보고: "iOS 에서 진동이 전혀 없다")
 *
 * - impact(Light) → vibrate(60ms) → selectionChanged 순으로 바꿔 봤지만 어느 것도
 *   실기기에서 확인되지 않았다. 각 단계의 근거였던 "플러그인 iOS 구현이 generator 를
 *   붙잡지 않아 울리기 전에 해제된다"는 이론은 검증된 적이 없다. 같은 플러그인의
 *   impact 는 일반적인 Capacitor 앱에서 iOS 에 정상 동작한다.
 * - selectionChanged 는 iOS 햅틱 중 가장 약해서, "전혀 없다"는 보고에 대해
 *   체감이 더 작은 쪽으로 간 셈이었다. impact(Medium) 으로 되돌린다.
 * - 코드로 확인되지 않는 원인이 남아 있다. iPad 는 햅틱 하드웨어가 없고,
 *   설정 > 소리 및 햅틱 > 시스템 햅틱이 꺼져 있으면 UIFeedbackGenerator 는
 *   아무것도 하지 않는다. 실기기 확인 전에는 코드를 더 바꾸지 않는다.
 */
@Injectable({ providedIn: 'root' })
export class HapticService {
  private lastFiredAt = 0;

  /** 쿨다운 판정. 부수효과가 없어 그대로 단위 테스트한다 */
  static isWithinCooldown(lastFiredAt: number, now: number, cooldownMs: number = COOLDOWN_MS): boolean {
    return now - lastFiredAt < cooldownMs;
  }

  /**
   * 짧은 탭 피드백. 사용자 조작과 이벤트 알림 모두 이 메서드 하나를 쓴다.
   * 쿨다운에 걸리거나 네이티브가 아니면 조용히 넘어간다.
   */
  async tap(now: number = Date.now()): Promise<void> {
    if (!Capacitor.isNativePlatform()) return;
    if (HapticService.isWithinCooldown(this.lastFiredAt, now)) return;

    this.lastFiredAt = now;
    await this.fire();
  }

  /**
   * 양쪽 다 Heavy (2-31차 후속, 사용자 지시 "impact 를 크게").
   * 이전: Android Light(짧은 진동으로 충분하다고 봄) / iOS Medium(Light 가 약하다는 보고로 한 단계 올림).
   * 그래도 약하다는 보고가 이어져 가장 센 단계로 통일한다.
   */
  private async fire(): Promise<void> {
    const style = ImpactStyle.Heavy;
    try {
      await Haptics.impact({ style });
    } catch (e) {
      console.warn('[Haptic] 진동 재생 실패', e);
    }
  }
}
