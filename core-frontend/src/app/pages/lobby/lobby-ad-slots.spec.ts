import {
  LOBBY_AD_SLOT_COUNT,
  LOBBY_AD_SLOT_DEFAULT_HEIGHT,
  buildAdSlotPositions,
  LOBBY_AD_FADE_OFF_VELOCITY,
  LOBBY_AD_FADE_ON_VELOCITY,
  nextAdFadeState,
  defaultAdSlotHeights,
  emptyAdSlotFilled,
  slotHeightFromAdLoaded,
  slotIndexFromAdLoaded
} from './lobby-ad-slots';

describe('lobby-ad-slots', () => {
  describe('emptyAdSlotFilled', () => {
    it('슬롯 수만큼 false 로 채운다', () => {
      expect(emptyAdSlotFilled()).toEqual([false, false, false, false, false, false]);
      expect(emptyAdSlotFilled().length).toBe(LOBBY_AD_SLOT_COUNT);
    });
  });

  describe('slotHeightFromAdLoaded', () => {
    it('height 가 없으면(iOS·구 빌드) 기본 315', () => {
      expect(slotHeightFromAdLoaded(undefined)).toBe(LOBBY_AD_SLOT_DEFAULT_HEIGHT);
      expect(slotHeightFromAdLoaded({ slot: 1 })).toBe(315);
      expect(slotHeightFromAdLoaded({ slot: 1, height: 0 })).toBe(315);
      expect(slotHeightFromAdLoaded({ height: '300' })).toBe(315);
      expect(defaultAdSlotHeights()).toEqual([315, 315, 315, 315, 315, 315]);
    });

    it('측정값을 정수로 쓰고 범위 밖은 자른다', () => {
      expect(slotHeightFromAdLoaded({ slot: 0, height: 296.4 })).toBe(296);
      expect(slotHeightFromAdLoaded({ height: 120 })).toBe(200);
      expect(slotHeightFromAdLoaded({ height: 900 })).toBe(420);
    });
  });

  describe('slotIndexFromAdLoaded', () => {
    it('detail 이 없으면(구 네이티브) 첫 슬롯', () => {
      expect(slotIndexFromAdLoaded(undefined)).toBe(0);
      expect(slotIndexFromAdLoaded(null)).toBe(0);
      expect(slotIndexFromAdLoaded({})).toBe(0);
    });

    it('detail.slot 을 그대로 쓴다', () => {
      expect(slotIndexFromAdLoaded({ slot: 0 })).toBe(0);
      expect(slotIndexFromAdLoaded({ slot: 2 })).toBe(2);
      // 2-34차 세로형 Latest Content 안 슬롯
      expect(slotIndexFromAdLoaded({ slot: 3 })).toBe(3);
      expect(slotIndexFromAdLoaded({ slot: 5 })).toBe(5);
    });

    it('범위 밖·정수 아님은 -1', () => {
      expect(slotIndexFromAdLoaded({ slot: 6 })).toBe(-1);
      expect(slotIndexFromAdLoaded({ slot: -1 })).toBe(-1);
      expect(slotIndexFromAdLoaded({ slot: 1.5 })).toBe(0);
      expect(slotIndexFromAdLoaded({ slot: '1' })).toBe(0);
    });
  });

  describe('buildAdSlotPositions', () => {
    const header = 56;

    it('접힌 슬롯(null)은 보내지 않는다', () => {
      expect(buildAdSlotPositions([null, null, null], header, 0)).toEqual([]);
      expect(buildAdSlotPositions([null, 400, null], header, 0)).toEqual([
        { index: 1, y: 400, hideAbove: header }
      ]);
    });

    it('슬롯 1·2 는 헤더 하단, 슬롯 3 은 sticky 탭 바 하단에서 클리핑한다', () => {
      const tabBar = 140; // 탭 바가 상단에 고정된 상태
      expect(buildAdSlotPositions([300, 900, 1500], header, tabBar)).toEqual([
        { index: 0, y: 300, hideAbove: header },
        { index: 1, y: 900, hideAbove: header },
        { index: 2, y: 1500, hideAbove: tabBar }
      ]);
    });

    it('탭 바가 헤더보다 위로 가는 비정상 값은 헤더 하단으로 보정한다', () => {
      expect(buildAdSlotPositions([null, null, 1500], header, 10)).toEqual([
        { index: 2, y: 1500, hideAbove: header }
      ]);
    });

    it('세로형 안 슬롯(3~5)은 헤더 하단에서 클리핑한다', () => {
      const tabBar = 140;
      expect(buildAdSlotPositions([null, null, null, 700, null, 1200], header, tabBar)).toEqual([
        { index: 3, y: 700, hideAbove: header },
        { index: 5, y: 1200, hideAbove: header }
      ]);
    });

    it('슬롯 수를 넘는 항목은 무시한다', () => {
      expect(buildAdSlotPositions([10, 20, 30, 40, 50, 60, 70], header, 0).length).toBe(6);
    });

    it('억제된 슬롯은 펼쳐져 있어도 보내지 않는다 (TOP 100 교체 중 슬롯 3)', () => {
      const tabBar = 300;
      expect(buildAdSlotPositions([300, 900, 1500], header, tabBar, new Set([2]))).toEqual([
        { index: 0, y: 300, hideAbove: header },
        { index: 1, y: 900, hideAbove: header },
      ]);
      expect(buildAdSlotPositions([null, null, 1500], header, tabBar, new Set([2]))).toEqual([]);
    });
  });
  describe('nextAdFadeState', () => {
    it('임계치를 넘는 빠른 스크롤에서만 페이드 아웃한다', () => {
      expect(nextAdFadeState(0.4, false)).toBeFalse();
      expect(nextAdFadeState(LOBBY_AD_FADE_ON_VELOCITY, false)).toBeFalse();
      expect(nextAdFadeState(2.0, false)).toBeTrue();
      expect(nextAdFadeState(-2.0, false)).toBeTrue();
    });

    it('페이드 중에는 더 낮은 속도까지 유지한다 (히스테리시스)', () => {
      expect(nextAdFadeState(1.0, true)).toBeTrue();
      expect(nextAdFadeState(LOBBY_AD_FADE_OFF_VELOCITY, true)).toBeTrue();
      expect(nextAdFadeState(0.2, true)).toBeFalse();
      expect(nextAdFadeState(0, true)).toBeFalse();
    });

    it('속도를 모르면 상태를 유지한다', () => {
      expect(nextAdFadeState(undefined, true)).toBeTrue();
      expect(nextAdFadeState(NaN, false)).toBeFalse();
    });
  });
});
