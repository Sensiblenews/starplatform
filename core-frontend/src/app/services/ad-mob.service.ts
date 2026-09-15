import { Injectable } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import {
  AdMob,
  AdMobBannerSize,
  AdmobConsentStatus,
  BannerAdOptions,
  BannerAdPluginEvents,
  BannerAdPosition,
  BannerAdSize,
} from '@capacitor-community/admob';
import { App } from '@capacitor/app';
import { Capacitor, PluginListenerHandle } from '@capacitor/core';
import {
  BANNER_AD_ID,
  BANNER_AD_ID_IOS,
  INTERSTITIAL_AD_ID,
  INTERSTITIAL_AD_ID_IOS,
} from './../constants/Keys/AdMob';

// ==========================================
// [2-29차] 전면 광고 노출 정책
// ==========================================
// 기존 정책(30초 간격 + 화면 전환 6회, 세션 상한 없음)을 요청서 기준으로 교체한다.
// 값을 조정할 일이 생기면 아래 상수만 고치면 된다 — 판정 로직은 canShowInterstitial 한 곳뿐이다.

/** 노출 간 최소 간격 3분. 세션을 넘어 지켜져야 하므로 localStorage에 남긴다 */
const INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000;
/**
 * 세션당 최대 노출 횟수.
 *
 * 2-29차 요청서는 2회였으나 5회로 올렸다(2026-09-15). 2회는 10분 남짓의 보통 세션에서
 * 실질적인 상한으로 작동해, 3분 간격 게이트가 일하기도 전에 광고가 멈췄다.
 * 5회에 닿으려면 60초 유예 + 3분 간격 × 4 = 최소 13분을 써야 하므로,
 * 실질 상한은 간격 게이트가 쥐고 이 값은 안전장치로만 남는다.
 *
 * 요청서 명시값에서 벗어난 값이므로 클라이언트와 합의된 숫자로 유지할 것.
 */
export const INTERSTITIAL_MAX_PER_SESSION = 5;
/** 앱(또는 세션) 시작 후 이 시간 동안은 노출하지 않는다 */
const INTERSTITIAL_COLD_START_GRACE_MS = 60 * 1000;
/** 세션 시작 후 이 횟수만큼 화면을 옮기기 전에는 노출하지 않는다 */
const INTERSTITIAL_MIN_PAGE_MOVES = 3;
/** 백그라운드 체류가 이 시간을 넘기면 복귀 시 새 세션으로 본다 (Firebase Analytics 기본값과 동일) */
const SESSION_RESUME_GAP_MS = 30 * 60 * 1000;
/** 마지막 노출 시각 저장 키 */
const LAST_SHOWN_KEY = 'last_interstitial_time';

/** 노출 차단 사유. 로그와 테스트에서 어떤 게이트가 막았는지 구분하는 데 쓴다 */
export type InterstitialGateReason =
  | 'ok'
  | 'session-cap'
  | 'cold-start'
  | 'page-moves'
  | 'interval';

@Injectable({
  providedIn: 'root',
})
export class AdMobService {
  /** 세션 시작 이후 화면 전환 횟수 */
  private pageMoveCount = 0;
  /** 현재 세션 시작 시각 */
  private sessionStartedAt = Date.now();
  /** 현재 세션에서 실제로 노출한 전면 광고 수 */
  private sessionImpressionCount = 0;
  /** 백그라운드로 내려간 시각. 포그라운드 상태면 null */
  private backgroundedAt: number | null = null;
  private appStateListener: PluginListenerHandle | null = null;

  constructor(private router: Router) {
    // 앱 내에서 라우팅이 끝날 때마다 카운트 +1
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.pageMoveCount++;
        this.logGate(`화면 이동 → ${event.urlAfterRedirects}`);
      }
    });
  }

  async initialize(): Promise<void> {
    AdMob.initialize({
      initializeForTesting: false,
    });

    // 서비스 생성 시점과 초기화 시점이 다를 수 있으므로 세션 시작을 여기서 다시 잡는다
    this.startNewSession(Date.now());
    this.exposeDebugHelper();
    this.logGate('광고 초기화 — 새 세션 시작');
    await this.registerAppStateListener();

    // ATT·동의 절차는 실패해도 광고 로드를 막지 않는다.
    // 이전에는 try/catch가 없어 이 구간에서 reject가 하나만 나와도 아래
    // loadInterstitial()이 통째로 건너뛰어졌다. 그 뒤로는 showInterstitial()의
    // 실패 경로가 재장전할 때까지 전면 광고가 한 장도 준비되지 않았다.
    // requestConsentInfo/showConsentForm은 네트워크와 UMP 설정에 의존해 실패가 잦다.
    try {
      const [trackingInfo, consentInfo] = await Promise.all([
        AdMob.trackingAuthorizationStatus(),
        AdMob.requestConsentInfo(),
      ]);

      if (trackingInfo.status === 'notDetermined') {
        await AdMob.requestTrackingAuthorization();
      }

      const authorizationStatus = await AdMob.trackingAuthorizationStatus();
      if (
        authorizationStatus.status === 'authorized' &&
        consentInfo.isConsentFormAvailable &&
        consentInfo.status === AdmobConsentStatus.REQUIRED
      ) {
        await AdMob.showConsentForm();
      }
    } catch (e) {
      console.error('[AD] ATT/동의 절차 실패 — 광고 로드는 그대로 진행한다', e);
    }

    await this.loadInterstitial();
  }

  async showBanner(): Promise<void> {
    const adId =
      Capacitor.getPlatform() === 'ios' ? BANNER_AD_ID_IOS : BANNER_AD_ID;
    const options: BannerAdOptions = {
      adId,
      adSize: BannerAdSize.ADAPTIVE_BANNER,
      position: BannerAdPosition.BOTTOM_CENTER,
      margin: 0,
    };
    AdMob.showBanner(options);
  }

  // 전면 광고 미리 로드하기 (보여주지는 않음)
  async loadInterstitial(): Promise<void> {
    if (!Capacitor.isNativePlatform()) return;

    const adId = Capacitor.getPlatform() === 'ios' ? INTERSTITIAL_AD_ID_IOS : INTERSTITIAL_AD_ID;

    try {
      // prepareInterstitial은 광고를 로드만 하고 메모리에 올려둔다
      await AdMob.prepareInterstitial({ adId });
      // "로드된 적이 없음"과 "로드는 됐는데 노출이 실패함"을 로그로 구분하기 위해 남긴다
      console.log('[AD] 전면 광고 로드 완료 (재고 있음)');
    } catch (e) {
      console.error('[AD] 전면 광고 로드 실패', e);
    }
  }

  // ==========================================
  // 진단 로그 (chrome://inspect 콘솔에서 확인)
  // ==========================================
  // canShowInterstitial은 첫 번째로 걸린 게이트만 돌려준다. 그것만으로는
  // "지금 뭐가 얼마나 모자란지"를 알 수 없어, 네 게이트의 현재값을 한 줄로 같이 찍는다.
  // 로그는 전부 '[AD]' 로 시작하므로 콘솔 필터에 AD 를 넣으면 이것만 보인다.

  /** 네 게이트의 현재값을 사람이 읽을 수 있는 한 줄로 만든다 */
  private gateSnapshot(now: number): string {
    const lastShown = this.readLastShownAt(now);
    const sinceShown = lastShown === Number.NEGATIVE_INFINITY
      ? '없음'
      : `${Math.round((now - lastShown) / 1000)}s`;

    return [
      `세션 ${this.sessionImpressionCount}/${INTERSTITIAL_MAX_PER_SESSION}회`,
      `경과 ${Math.round((now - this.sessionStartedAt) / 1000)}s/${INTERSTITIAL_COLD_START_GRACE_MS / 1000}s`,
      `이동 ${this.pageMoveCount}/${INTERSTITIAL_MIN_PAGE_MOVES}회`,
      `직전노출 ${sinceShown}/${INTERSTITIAL_MIN_INTERVAL_MS / 1000}s`,
    ].join(' · ');
  }

  /**
   * 지금 전면 광고를 띄울 수 있는지 콘솔에 한 줄 남긴다.
   * label 에는 무슨 행동 뒤인지 적는다(화면 이동, 노출 시도 등).
   */
  logGate(label: string, now: number = Date.now()): void {
    const gate = this.canShowInterstitial(now);
    const verdict = gate.allowed ? '노출 가능 ✅' : `차단 ❌ ${gate.reason}`;
    console.log(`[AD] ${label} | ${verdict} | ${this.gateSnapshot(now)}`);
  }

  /**
   * 노출 가능 여부만 판정한다. 부수효과 없이 시각을 주입받으므로 그대로 단위 테스트할 수 있다.
   * 게이트는 빠르게 탈락하는 순서로 본다.
   */
  canShowInterstitial(now: number = Date.now()): {
    allowed: boolean;
    reason: InterstitialGateReason;
  } {
    if (this.sessionImpressionCount >= INTERSTITIAL_MAX_PER_SESSION) {
      return { allowed: false, reason: 'session-cap' };
    }

    if (now - this.sessionStartedAt < INTERSTITIAL_COLD_START_GRACE_MS) {
      return { allowed: false, reason: 'cold-start' };
    }

    if (this.pageMoveCount < INTERSTITIAL_MIN_PAGE_MOVES) {
      return { allowed: false, reason: 'page-moves' };
    }

    if (now - this.readLastShownAt(now) < INTERSTITIAL_MIN_INTERVAL_MS) {
      return { allowed: false, reason: 'interval' };
    }

    return { allowed: true, reason: 'ok' };
  }

  /** @param trigger 어느 행동이 호출했는지. 로그에만 쓴다 */
  async showInterstitial(trigger: string = '알 수 없는 지점'): Promise<boolean> {
    if (!Capacitor.isNativePlatform()) {
      console.log(`[AD] 노출 시도 (${trigger}) | 차단 ❌ 네이티브 앱이 아님`);
      return false;
    }

    const now = Date.now();
    const gate = this.canShowInterstitial(now);
    this.logGate(`노출 시도 (${trigger})`, now);
    if (!gate.allowed) return false;

    try {
      await AdMob.showInterstitial();

      localStorage.setItem(LAST_SHOWN_KEY, String(now));
      this.sessionImpressionCount++;
      this.pageMoveCount = 0;

      // 다음 회차를 위해 곧바로 재장전
      this.loadInterstitial();

      // 노출 직후 상태를 같이 남긴다. 이동 카운터가 0으로 리셋되고 간격 3분이
      // 새로 시작하므로, 바로 다음 로그가 차단으로 바뀌는 것이 정상이다.
      this.logGate('노출 성공 🎬 — 이후 상태', Date.now());

      return true;
    } catch (e) {
      // 노출에 실패했으면 세션 카운터를 소모하지 않는다. 화면 흐름도 막지 않는다.
      // e를 반드시 같이 찍는다 — AdMob 에러(no fill·미준비·잘못된 광고 단위)를
      // 구분할 단서가 여기밖에 없다.
      console.warn('[AD] 노출 실패 — 재고 없음으로 보고 재장전한다', e);
      this.loadInterstitial();
      return false;
    }
  }

  /**
   * 백그라운드 전환·복귀를 반영한다. 리스너가 호출하며, 테스트는 이 메서드를 직접 부른다.
   * 복귀까지의 공백이 SESSION_RESUME_GAP_MS를 넘으면 새 세션으로 본다 —
   * 콜드스타트만 세션으로 잡으면 앱을 오래 띄워 두는 사용자는 상한이 소진된 채 광고가 멈춘다.
   */
  handleAppStateChange(isActive: boolean, now: number = Date.now()): void {
    if (!isActive) {
      this.backgroundedAt = now;
      return;
    }

    if (
      this.backgroundedAt !== null &&
      now - this.backgroundedAt >= SESSION_RESUME_GAP_MS
    ) {
      this.startNewSession(now);
      // 오래 묵은 캐시 광고는 만료됐을 수 있으므로 재장전
      this.loadInterstitial();
      this.logGate('백그라운드 30분 초과 복귀 — 새 세션 시작', now);
    }

    this.backgroundedAt = null;
  }

  /** 세션 경계를 새로 긋는다 */
  private startNewSession(now: number): void {
    this.sessionStartedAt = now;
    this.sessionImpressionCount = 0;
    this.pageMoveCount = 0;
  }

  /**
   * 마지막 노출 시각을 읽는다. 기록이 없거나 깨졌거나 미래 시각이면
   * "아직 노출한 적 없음"으로 보고 간격 게이트를 통과시킨다.
   * (첫 노출은 cold-start와 page-moves 게이트가 막는다)
   */
  private readLastShownAt(now: number): number {
    const raw = localStorage.getItem(LAST_SHOWN_KEY);
    const parsed = raw === null ? NaN : parseInt(raw, 10);
    const isUsable = Number.isFinite(parsed) && parsed > 0 && parsed <= now;
    return isUsable ? parsed : Number.NEGATIVE_INFINITY;
  }

  /**
   * chrome://inspect 콘솔에서 아무 때나 상태를 확인할 수 있게 전역 함수를 심는다.
   * 콘솔에 adDebug() 를 입력하면 현재 게이트 현황이 한 줄 찍히고 판정 객체가 반환된다.
   */
  private exposeDebugHelper(): void {
    (window as any).adDebug = () => {
      this.logGate('수동 확인 (adDebug)');
      return this.canShowInterstitial();
    };
  }

  private async registerAppStateListener(): Promise<void> {
    if (this.appStateListener) return; // 리스너 중복 등록 방지

    try {
      this.appStateListener = await App.addListener('appStateChange', ({ isActive }) => {
        this.handleAppStateChange(isActive);
      });
    } catch (e) {
      // 리스너 등록에 실패해도 콜드스타트 기준 세션은 그대로 동작한다
      console.error('[AD] 앱 상태 리스너 등록 실패', e);
    }
  }
}
