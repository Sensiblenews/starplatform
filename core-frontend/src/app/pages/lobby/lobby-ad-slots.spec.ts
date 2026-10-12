import {
  LOBBY_AD_SLOT_COUNT,
  buildAdSlotPositions,
  emptyAdSlotFilled,
  slotIndexFromAdLoaded
} from './lobby-ad-slots';

describe('lobby-ad-slots', () => {
  describe('emptyAdSlotFilled', () => {
    it('슬롯 수만큼 false 로 채운다', () => {
      expect(emptyAdSlotFilled()).toEqual([false, false, false]);
      expect(emptyAdSlotFilled().length).toBe(LOBBY_AD_SLOT_COUNT);
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
    });

    it('범위 밖·정수 아님은 -1', () => {
      expect(slotIndexFromAdLoaded({ slot: 3 })).toBe(-1);
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

    it('슬롯 수를 넘는 항목은 무시한다', () => {
      expect(buildAdSlotPositions([10, 20, 30, 40], header, 0).length).toBe(3);
    });
  });
});
