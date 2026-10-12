import {
  LATEST_DEFAULT_INTERVAL_SEC,
  LATEST_HEIGHT_MAX_PX,
  LATEST_HEIGHT_MIN_PX,
  LATEST_MANUAL_PAUSE_MS,
  canAutoAdvance,
  clampIndex,
  eventKey,
  halfContentHeight,
  indexFromScroll,
  nextIndex,
  normalizeIntervalMs,
  prevIndex,
  shouldBindSrc,
} from './latest-contents-logic';

describe('latest-contents-logic (2-33차 로비 숏폼)', () => {
  describe('nextIndex / prevIndex', () => {
    it('다음 카드로 넘어가고 마지막에서는 첫 카드로 순환한다', () => {
      expect(nextIndex(0, 3)).toBe(1);
      expect(nextIndex(2, 3)).toBe(0);
    });

    it('이전 카드로 가고 첫 카드에서는 마지막으로 순환한다', () => {
      expect(prevIndex(1, 3)).toBe(0);
      expect(prevIndex(0, 3)).toBe(2);
    });

    it('카드가 0장이거나 1장이면 항상 0', () => {
      expect(nextIndex(0, 0)).toBe(0);
      expect(nextIndex(0, 1)).toBe(0);
      expect(prevIndex(0, 1)).toBe(0);
    });
  });

  describe('clampIndex', () => {
    it('음수·NaN 은 0, 길이 이상은 마지막 인덱스', () => {
      expect(clampIndex(-1, 3)).toBe(0);
      expect(clampIndex(NaN, 3)).toBe(0);
      expect(clampIndex(5, 3)).toBe(2);
      expect(clampIndex(1, 3)).toBe(1);
    });
  });

  describe('indexFromScroll', () => {
    const step = 240;

    it('스크롤 위치를 가장 가까운 카드로 반올림한다', () => {
      expect(indexFromScroll(0, step, 5)).toBe(0);
      expect(indexFromScroll(1.4 * step, step, 5)).toBe(1);
      expect(indexFromScroll(1.6 * step, step, 5)).toBe(2);
    });

    it('끝을 넘긴 위치는 마지막 카드, step 0 은 0', () => {
      expect(indexFromScroll(99 * step, step, 5)).toBe(4);
      expect(indexFromScroll(300, 0, 5)).toBe(0);
    });
  });

  describe('shouldBindSrc', () => {
    it('활성 카드와 양옆 1장만 src 를 붙인다 (마지막↔첫 카드도 이웃)', () => {
      expect(shouldBindSrc(0, 0, 5)).toBeTrue();
      expect(shouldBindSrc(1, 0, 5)).toBeTrue();
      expect(shouldBindSrc(4, 0, 5)).toBeTrue();
      expect(shouldBindSrc(2, 0, 5)).toBeFalse();
      expect(shouldBindSrc(3, 0, 5)).toBeFalse();
    });

    it('카드가 2장이면 둘 다, radius 0 이면 활성 카드만', () => {
      expect(shouldBindSrc(0, 1, 2)).toBeTrue();
      expect(shouldBindSrc(1, 1, 2)).toBeTrue();
      expect(shouldBindSrc(1, 0, 5, 0)).toBeFalse();
      expect(shouldBindSrc(0, 0, 5, 0)).toBeTrue();
    });

    it('빈 목록·범위 밖 인덱스는 false', () => {
      expect(shouldBindSrc(0, 0, 0)).toBeFalse();
      expect(shouldBindSrc(7, 0, 5)).toBeFalse();
    });
  });

  describe('canAutoAdvance', () => {
    it('수동 조작이 없었으면 바로 움직인다', () => {
      expect(canAutoAdvance(1000, null)).toBeTrue();
    });

    it('수동 조작 뒤 6초 안에는 멈추고 6초가 되면 다시 움직인다', () => {
      const last = 10000;
      expect(canAutoAdvance(last + LATEST_MANUAL_PAUSE_MS - 1, last)).toBeFalse();
      expect(canAutoAdvance(last + LATEST_MANUAL_PAUSE_MS, last)).toBeTrue();
    });
  });

  describe('normalizeIntervalMs', () => {
    it('3~10초는 그대로 ms 로, 문자열 숫자도 받는다', () => {
      expect(normalizeIntervalMs(4)).toBe(4000);
      expect(normalizeIntervalMs('7')).toBe(7000);
      expect(normalizeIntervalMs(10)).toBe(10000);
    });

    it('범위 밖·비숫자는 기본 4초', () => {
      expect(normalizeIntervalMs(2)).toBe(LATEST_DEFAULT_INTERVAL_SEC * 1000);
      expect(normalizeIntervalMs(11)).toBe(LATEST_DEFAULT_INTERVAL_SEC * 1000);
      expect(normalizeIntervalMs(undefined)).toBe(LATEST_DEFAULT_INTERVAL_SEC * 1000);
      expect(normalizeIntervalMs('abc')).toBe(LATEST_DEFAULT_INTERVAL_SEC * 1000);
    });
  });

  describe('halfContentHeight', () => {
    it('헤더를 뺀 높이의 절반', () => {
      expect(halfContentHeight(800, 56)).toBe(372);
      expect(halfContentHeight(800, 0)).toBe(400);
    });

    it('하한·상한으로 자르고 비정상 입력은 하한', () => {
      expect(halfContentHeight(300, 56)).toBe(LATEST_HEIGHT_MIN_PX);
      expect(halfContentHeight(2000, 0)).toBe(LATEST_HEIGHT_MAX_PX);
      expect(halfContentHeight(NaN, 56)).toBe(LATEST_HEIGHT_MIN_PX);
      expect(halfContentHeight(0, 0)).toBe(LATEST_HEIGHT_MIN_PX);
    });
  });

  it('eventKey 는 편성 ID 와 종류로 안정적인 문자열을 만든다', () => {
    expect(eventKey(12, 'PLAY')).toBe('12:PLAY');
    expect(eventKey(12, 'PLAY')).toBe(eventKey(12, 'PLAY'));
    expect(eventKey(12, 'CLICK')).not.toBe(eventKey(12, 'PLAY'));
  });
});
