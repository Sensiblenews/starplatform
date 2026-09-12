import { TestBed } from '@angular/core/testing';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
import { Subject } from 'rxjs';

import { AdMobService } from './ad-mob.service';

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

  describe('앱 시작 직후 금지', () => {
    it('세션 시작 직후에는 막는다', () => {
      move(10);
      expect(service.canShowInterstitial(sessionStart).reason).toBe('cold-start');
    });

    it('59초는 화면을 아무리 옮겨도 막는다', () => {
      move(10);
      expect(service.canShowInterstitial(sessionStart + 59_000).reason).toBe('cold-start');
    });

    it('60초가 지나도 화면 전환이 3회 미만이면 막는다', () => {
      move(2);
      expect(service.canShowInterstitial(sessionStart + 61_000).reason).toBe('page-moves');
    });

    it('60초 경과 + 화면 전환 3회면 통과한다', () => {
      move(3);
      expect(service.canShowInterstitial(sessionStart + 61_000)).toEqual({
        allowed: true,
        reason: 'ok',
      });
    });

    it('노출 기록이 없어도 세션 시작 직후면 막는다', () => {
      // 기본값 '100'(epoch 100ms)을 쓰던 탓에 첫 실행에서 곧바로 노출되던 회귀를 막는다
      move(10);
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

  describe('세션당 최대 2회', () => {
    it('2회까지는 통과하고 3회째는 막는다', () => {
      move(5);
      const now = sessionStart + 10 * MINUTE;

      expect(service.canShowInterstitial(now).allowed).toBeTrue();

      service['sessionImpressionCount'] = 1;
      expect(service.canShowInterstitial(now).allowed).toBeTrue();

      service['sessionImpressionCount'] = 2;
      expect(service.canShowInterstitial(now).reason).toBe('session-cap');
    });
  });

  describe('세션 경계', () => {
    beforeEach(() => {
      service['sessionImpressionCount'] = 2;
      move(5);
    });

    it('백그라운드 29분 뒤 복귀는 같은 세션으로 본다', () => {
      const out = sessionStart + 1 * MINUTE;
      service.handleAppStateChange(false, out);
      service.handleAppStateChange(true, out + 29 * MINUTE);

      expect(service['sessionImpressionCount']).toBe(2);
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

    it('새 세션도 시작 직후 60초는 막는다', () => {
      const out = sessionStart + 1 * MINUTE;
      const back = out + 30 * MINUTE;
      service.handleAppStateChange(false, out);
      service.handleAppStateChange(true, back);
      move(5);

      expect(service.canShowInterstitial(back + 30_000).reason).toBe('cold-start');
      expect(service.canShowInterstitial(back + 61_000).allowed).toBeTrue();
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
});
