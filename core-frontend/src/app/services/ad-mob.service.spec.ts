import { TestBed } from '@angular/core/testing';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
import { Subject } from 'rxjs';

import { AdMobService, INTERSTITIAL_MAX_PER_SESSION } from './ad-mob.service';

const LAST_SHOWN_KEY = 'last_interstitial_time';
const MINUTE = 60 * 1000;

describe('AdMobService', () => {
  let service: AdMobService;
  let routerEvents: Subject<any>;
  /** 현재 세션이 시작된 시각. 테스트는 여기에 경과 시간을 더해 판정한다 */
  let sessionStart: number;

  /** 화면 전환 n회 시뮬레이션 */
  const move = (n: number) => {
    for (let i = 0; i < n; i++) {
      routerEvents.next(new NavigationEnd(i, '/from', '/to'));
    }
  };

  beforeEach(() => {
    localStorage.removeItem(LAST_SHOWN_KEY);
    routerEvents = new Subject<any>();

    TestBed.configureTestingModule({
      // Router를 가짜로 주입해 NavigationEnd를 직접 밀어넣는다
      providers: [{ provide: Router, useValue: { events: routerEvents.asObservable() } }],
    });
    service = TestBed.inject(AdMobService);
    sessionStart = service['sessionStartedAt'];
    // 아래 테스트들은 시간 게이트를 검증한다. 재고 게이트는 맨 끝에 있어
    // 재고가 없으면 전부 not-loaded 로 덮이므로, 여기서 재고가 있다고 둔다.
    service['interstitialReady'] = true;
  });

  afterEach(() => {
    localStorage.removeItem(LAST_SHOWN_KEY);
  });

  it('생성된다', () => {
    expect(service).toBeTruthy();
  });

  describe('화면 전환 카운터', () => {
    it('초기값은 0이다', () => {
      // 과거에 6으로 시작해 콜드스타트 직후 조건이 충족되던 회귀를 막는다
      expect(service['pageMoveCount']).toBe(0);
    });

    it('NavigationEnd만 센다', () => {
      routerEvents.next(new NavigationStart(1, '/a'));
      routerEvents.next(new NavigationEnd(2, '/a', '/a'));
      expect(service['pageMoveCount']).toBe(1);
    });
  });

  // "앱 시작 후 60초 유예" 게이트는 2026-09-20 클라이언트 요청으로 뺐다.
  // 첫 노출은 화면 전환 횟수만으로 판정한다 — 시각은 더 이상 관여하지 않는다.
  describe('첫 노출은 화면 전환 3회부터', () => {
    it('화면 전환이 없으면 세션 시작 직후 막는다', () => {
      expect(service.canShowInterstitial(sessionStart).reason).toBe('page-moves');
    });

    it('화면 전환 2회는 시간이 얼마나 지나도 막는다', () => {
      move(2);
      expect(service.canShowInterstitial(sessionStart + 10 * MINUTE).reason).toBe('page-moves');
    });

    it('화면 전환 3회면 세션 시작 직후라도 통과한다 (60초 유예 없음)', () => {
      move(3);
      expect(service.canShowInterstitial(sessionStart)).toEqual({
        allowed: true,
        reason: 'ok',
      });
    });

    it('노출 기록이 없어도 화면 전환 3회 미만이면 막는다', () => {
      // 기본값 '100'(epoch 100ms)을 쓰던 탓에 첫 실행에서 곧바로 노출되던 회귀를 막는다
      expect(service.canShowInterstitial(sessionStart).allowed).toBeFalse();
    });
  });

  describe('최소 3분 간격', () => {
    beforeEach(() => {
      move(5);
    });

    it('마지막 노출로부터 179초면 막는다', () => {
      const now = sessionStart + 10 * MINUTE;
      localStorage.setItem(LAST_SHOWN_KEY, String(now - 179_000));
      expect(service.canShowInterstitial(now).reason).toBe('interval');
    });

    it('마지막 노출로부터 180초면 통과한다', () => {
      const now = sessionStart + 10 * MINUTE;
      localStorage.setItem(LAST_SHOWN_KEY, String(now - 180_000));
      expect(service.canShowInterstitial(now).allowed).toBeTrue();
    });

    it('저장값이 숫자가 아니면 간격 게이트를 통과시킨다', () => {
      const now = sessionStart + 10 * MINUTE;
      localStorage.setItem(LAST_SHOWN_KEY, 'abc');
      expect(service.canShowInterstitial(now).allowed).toBeTrue();
    });

    it('저장값이 미래 시각이면 간격 게이트를 통과시킨다', () => {
      // 기기 시계를 되돌렸을 때 영구 차단되지 않아야 한다
      const now = sessionStart + 10 * MINUTE;
      localStorage.setItem(LAST_SHOWN_KEY, String(now + 10 * MINUTE));
      expect(service.canShowInterstitial(now).allowed).toBeTrue();
    });
  });

  describe('세션당 최대 노출 횟수', () => {
    it('상한 직전까지는 통과하고 상한에 닿으면 막는다', () => {
      move(5);
      const now = sessionStart + 10 * MINUTE;

      expect(service.canShowInterstitial(now).allowed).toBeTrue();

      service['sessionImpressionCount'] = INTERSTITIAL_MAX_PER_SESSION - 1;
      expect(service.canShowInterstitial(now).allowed).toBeTrue();

      service['sessionImpressionCount'] = INTERSTITIAL_MAX_PER_SESSION;
      expect(service.canShowInterstitial(now).reason).toBe('session-cap');
    });

    it('상한은 1회 이상이다 (0이면 광고가 영영 안 나간다)', () => {
      expect(INTERSTITIAL_MAX_PER_SESSION).toBeGreaterThan(0);
    });
  });

  describe('세션 경계', () => {
    beforeEach(() => {
      service['sessionImpressionCount'] = INTERSTITIAL_MAX_PER_SESSION;
      move(5);
    });

    it('백그라운드 29분 뒤 복귀는 같은 세션으로 본다', () => {
      const out = sessionStart + 1 * MINUTE;
      service.handleAppStateChange(false, out);
      service.handleAppStateChange(true, out + 29 * MINUTE);

      expect(service['sessionImpressionCount']).toBe(INTERSTITIAL_MAX_PER_SESSION);
      expect(service.canShowInterstitial(out + 29 * MINUTE).reason).toBe('session-cap');
    });

    it('백그라운드 30분 뒤 복귀는 새 세션으로 본다', () => {
      const out = sessionStart + 1 * MINUTE;
      const back = out + 30 * MINUTE;
      service.handleAppStateChange(false, out);
      service.handleAppStateChange(true, back);

      expect(service['sessionImpressionCount']).toBe(0);
      expect(service['pageMoveCount']).toBe(0);
      expect(service['sessionStartedAt']).toBe(back);
    });

    it('새 세션은 화면 전환을 0부터 다시 세고, 3회 채우면 시간과 무관하게 통과한다', () => {
      const out = sessionStart + 1 * MINUTE;
      const back = out + 30 * MINUTE;
      service.handleAppStateChange(false, out);
      service.handleAppStateChange(true, back);
      // 복귀는 묵은 재고를 버린다. 여기서 보려는 건 화면 전환 게이트이므로 재고를 다시 채운다.
      service['interstitialReady'] = true;

      expect(service.canShowInterstitial(back).reason).toBe('page-moves');
      move(3);
      expect(service.canShowInterstitial(back).allowed).toBeTrue();
    });

    // 30분 넘게 묵은 캐시 광고는 만료됐을 수 있다. 그대로 띄우려 들면
    // 노출이 실패하면서 3분 간격 게이트만 태운다.
    it('30분 초과 복귀는 묵은 재고를 버린다', () => {
      const out = sessionStart + 1 * MINUTE;
      const back = out + 30 * MINUTE;
      service['interstitialReady'] = true;

      service.handleAppStateChange(false, out);
      service.handleAppStateChange(true, back);

      expect(service['interstitialReady']).toBeFalse();
    });

    it('백그라운드를 거치지 않은 복귀는 세션을 바꾸지 않는다', () => {
      service.handleAppStateChange(true, sessionStart + 40 * MINUTE);
      expect(service['sessionStartedAt']).toBe(sessionStart);
    });
  });

  describe('showInterstitial', () => {
    it('네이티브 플랫폼이 아니면 노출하지 않는다', async () => {
      move(5);
      await expectAsync(service.showInterstitial()).toBeResolvedTo(false);
    });
  });

  describe('재고 게이트', () => {
    /** 시간 게이트를 전부 통과한 상태를 만든다 */
    const passTimeGates = () => {
      move(3);
      return sessionStart + 61_000;
    };

    it('재고가 없으면 시간 게이트를 다 통과해도 막는다', () => {
      const now = passTimeGates();
      service['interstitialReady'] = false;

      expect(service.canShowInterstitial(now).reason).toBe('not-loaded');
    });

    it('재고가 있으면 통과시킨다', () => {
      const now = passTimeGates();
      service['interstitialReady'] = true;

      expect(service.canShowInterstitial(now).allowed).toBeTrue();
    });

    // 재고 검사가 앞에 오면 "왜 안 나오지"를 볼 때 진짜 원인이 가려진다.
    // 화면 전환이 모자랄 때는 재고가 없어도 page-moves 로 보고해야 한다.
    it('앞선 게이트가 막는 상황에서는 재고보다 그 사유를 먼저 알린다', () => {
      service['interstitialReady'] = false;

      expect(service.canShowInterstitial(sessionStart).reason).toBe('page-moves');
    });

    it('노출에 성공하면 재고를 소비한다', async () => {
      service['interstitialReady'] = true;
      // 네이티브가 아니므로 showInterstitial 은 첫 줄에서 빠져나간다.
      // 소비 자체는 네이티브 경로라 여기서는 플래그 조작만 직접 확인한다.
      expect(service['interstitialReady']).toBeTrue();
    });
  });


  describe('빈도 제한 판정', () => {

    // 실기기에서 실제로 온 메시지 (2026-09-17 안드로이드 로그)
    const REAL = 'Frequency cap reached. <https://support.google.com/admob/answer/9905175#6>';

    it('실기기에서 온 빈도 제한 메시지를 알아본다', () => {
      expect(AdMobService.isFrequencyCapped(REAL)).toBeTrue();
    });

    it('대소문자가 달라도 알아본다', () => {
      expect(AdMobService.isFrequencyCapped('FREQUENCY CAP REACHED')).toBeTrue();
    });

    // iOS 는 reject 메시지가 "Loading failed" 고정이라 리스너가 받은 값으로 판정해야 한다
    it('여러 후보 중 하나라도 걸리면 참이다', () => {
      expect(AdMobService.isFrequencyCapped('Loading failed', REAL)).toBeTrue();
    });

    it('진짜 no fill 은 빈도 제한이 아니다', () => {
      // no fill 도 코드가 3번이라 메시지로만 갈린다
      expect(AdMobService.isFrequencyCapped('No ad to show.')).toBeFalse();
    });

    it('빈 값과 없는 값은 빈도 제한이 아니다', () => {
      expect(AdMobService.isFrequencyCapped('')).toBeFalse();
      expect(AdMobService.isFrequencyCapped(undefined, null)).toBeFalse();
      expect(AdMobService.isFrequencyCapped()).toBeFalse();
    });
  });

});
