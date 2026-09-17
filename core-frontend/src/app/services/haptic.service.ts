import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Haptics, ImpactStyle } from '@capacitor/haptics';

/** 연속 호출을 억제하는 최소 간격 */
const COOLDOWN_MS = 400;
/**
 * iOS 선택 피드백 생성기를 다시 예열하는 간격.
 * 마지막 재생 후 이만큼 지나면 Taptic Engine 이 식었다고 보고 새로 예열한다.
 */
const IOS_REARM_AFTER_MS = 30 * 1000;

/**
 * 햅틱 피드백 단일 창구.
 *
 * 이전에는 로비·스타페이지·랭킹 모달·GlobalFeedbackService가 각자
 * `Haptics.impact({ style: ImpactStyle.Light })` 를 직접 불렀다. 세 가지 문제가 있었다.
 *
 * 1. iOS에서 이벤트성 진동이 울리지 않았다.
 *    플러그인의 iOS impact 구현(Haptics.swift)은 generator 를 지역 변수로 만들고
 *    prepare() 없이 impactOccurred() 를 부른 뒤 즉시 해제한다. Taptic Engine 이
 *    유휴 상태면 깨어나기 전에 generator 가 사라져 아무 일도 일어나지 않는다.
 *    사용자가 화면을 만지는 중에는 시스템 UI 가 엔진을 덥혀놔 우연히 울리지만,
 *    30초 폴링 결과를 최대 29초 뒤에 재생하는 조회수 틱은 항상 식은 상태다.
 *    같은 파일의 selectionStart/Changed 는 generator 를 프로퍼티로 보관하고
 *    prepare() 를 호출한다 — impact 에만 그 처리가 빠져 있다.
 *    안드로이드 구현은 Vibrator 를 직접 돌려 예열 개념이 없다. 그래서 같은 코드인데
 *    안드로이드만 동작했다.
 *
 * 2. 실패가 은폐됐다. `try { import(...).then(() => Haptics.impact(...)) } catch {}`
 *    형태라 try 가 Promise 바깥에 있어 rejection 을 잡지 못했고, .catch() 도 없어
 *    unhandled rejection 으로 사라졌다. 로그가 한 줄도 남지 않아 원인 추적이 막혔다.
 *
 * 3. 쿨다운이 GlobalFeedbackService 에만 있었다. 로비·스타페이지의 조회수 틱은
 *    신규 조회 1건당 1회씩 상한 없이 호출했다.
 */
@Injectable({ providedIn: 'root' })
export class HapticService {
  private lastFiredAt = 0;
  /** iOS 선택 피드백 생성기를 예열해 둔 시각. 0이면 아직 예열 전 */
  private iosArmedAt = 0;

  /** 쿨다운 판정. 부수효과가 없어 그대로 단위 테스트한다 */
  static isWithinCooldown(lastFiredAt: number, now: number, cooldownMs: number = COOLDOWN_MS): boolean {
    return now - lastFiredAt < cooldownMs;
  }

  /**
   * iOS 선택 피드백 생성기를 다시 예열해야 하는지. 부수효과가 없어 그대로 단위 테스트한다.
   * armedAt 이 0이면 아직 한 번도 예열하지 않은 상태다.
   */
  static needsRearm(armedAt: number, now: number, rearmAfterMs: number = IOS_REARM_AFTER_MS): boolean {
    if (armedAt === 0) return true;
    return now - armedAt > rearmAfterMs;
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
   * iOS 는 impact 대신 vibrate 를 쓴다. 플러그인의 iOS vibrate 는 CHHapticEngine 을
   * 직접 만들어 start() 한 뒤 재생하므로 예열 의존이 없고 세기도 충분하다.
   *
   * ⚠️ 그 구현의 resetHandler 클로저가 engine 을 강하게 캡처해 호출마다 엔진 객체가
   * 하나씩 남는다. 위 쿨다운이 호출 빈도를 막는 유일한 방어선이므로 없애지 말 것.
   *
   * 안드로이드는 impact 가 정상 동작하므로 기존 체감을 그대로 둔다.
   */
  /**
   * iOS 는 selectionStart + selectionChanged 를 쓴다.
   *
   * 이 플러그인에서 iOS 쪽 경로가 셋인데, 제대로 동작하는 것은 이 하나뿐이다.
   *
   * - impact: generator 를 지역 변수로 만들고 prepare() 없이 바로 쏜 뒤 해제한다.
   *   Taptic Engine 이 식어 있으면 깨어나기 전에 generator 가 사라진다.
   * - vibrate(duration): CHHapticEngine 과 player 를 지역 변수로 만들고 start() 한 뒤
   *   함수가 끝난다. engine 은 resetHandler 클로저의 순환 참조로 겨우 살아남지만
   *   player 를 붙잡는 곳이 없어, 짧은 패턴은 재생을 마치기 전에 풀려난다.
   *   (2-29차에 impact 대신 이걸로 바꿨다가 여전히 안 울려서 다시 짚었다)
   * - selectionStart/selectionChanged: generator 를 프로퍼티로 보관하고 prepare() 를
   *   부른다. 재생할 때마다 prepare() 를 다시 불러 예열 상태를 유지한다.
   *
   * 안드로이드 impact 는 Vibrator 를 직접 돌려 예열 개념이 없으므로 그대로 둔다.
   */
  private async fire(): Promise<void> {
    try {
      if (Capacitor.getPlatform() !== 'ios') {
        await Haptics.impact({ style: ImpactStyle.Light });
        return;
      }

      // 오래 쉬었으면 엔진이 식었다고 보고 다시 예열한다.
      // selectionChanged 는 보관된 generator 가 없으면 조용히 아무것도 하지 않는다.
      if (HapticService.needsRearm(this.iosArmedAt, this.lastFiredAt)) {
        await Haptics.selectionStart();
        this.iosArmedAt = this.lastFiredAt;
      }
      await Haptics.selectionChanged();
    } catch (e) {
      console.warn('[Haptic] 진동 재생 실패', e);
    }
  }
}
