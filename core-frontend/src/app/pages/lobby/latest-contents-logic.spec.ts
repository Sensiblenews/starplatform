import {
  LATEST_DEFAULT_INTERVAL_SEC,
  LATEST_MANUAL_PAUSE_MS,
  LATEST_SWIPE_THRESHOLD_PX,
  canAutoAdvance,
  compactNumber,
  clampIndex,
  eventKey,
  nextIndex,
  normalizeIntervalMs,
  prevIndex,
  swipeDirection,
  toContentStats,
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

  describe('swipeDirection', () => {
    it('왼쪽으로 끌면 next, 오른쪽으로 끌면 prev', () => {
      expect(swipeDirection(-80)).toBe('next');
      expect(swipeDirection(80)).toBe('prev');
    });

    it('임계값 미만·NaN 은 null (탭으로 본다)', () => {
      expect(swipeDirection(-(LATEST_SWIPE_THRESHOLD_PX - 1))).toBeNull();
      expect(swipeDirection(0)).toBeNull();
      expect(swipeDirection(NaN)).toBeNull();
      expect(swipeDirection(-LATEST_SWIPE_THRESHOLD_PX)).toBe('next');
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

  it('eventKey 는 편성 ID 와 종류로 안정적인 문자열을 만든다', () => {
    expect(eventKey(12, 'PLAY')).toBe('12:PLAY');
    expect(eventKey(12, 'PLAY')).toBe(eventKey(12, 'PLAY'));
    expect(eventKey(12, 'CLICK')).not.toBe(eventKey(12, 'PLAY'));
  });
});

describe('latest-contents-logic (2-34차 플레이어 숫자)', () => {
  describe('compactNumber', () => {
    it('999 까지는 그대로', () => {
      expect(compactNumber(0)).toBe('0');
      expect(compactNumber(7)).toBe('7');
      expect(compactNumber(999)).toBe('999');
    });

    it('천 단위는 K, 소수 첫째 자리는 내림', () => {
      expect(compactNumber(1000)).toBe('1K');
      expect(compactNumber(1250)).toBe('1.2K');
      expect(compactNumber(1999)).toBe('1.9K');
      expect(compactNumber(12500)).toBe('12K');
      expect(compactNumber(999999)).toBe('999K');
    });

    it('백만은 M, 십억은 B', () => {
      expect(compactNumber(1_500_000)).toBe('1.5M');
      expect(compactNumber(2_000_000_000)).toBe('2B');
    });

    it('문자열 숫자는 받고, 비숫자·음수는 0', () => {
      expect(compactNumber('1554')).toBe('1.5K');
      expect(compactNumber('abc')).toBe('0');
      expect(compactNumber(-5)).toBe('0');
      expect(compactNumber(undefined)).toBe('0');
    });
  });

  describe('toContentStats', () => {
    it('Y/N·문자열 숫자를 정리한다', () => {
      const s = toContentStats({ viewCnt: '12', likeCnt: 3, commentCnt: null, saveCnt: '-1', shareCnt: 'x', liked: 'Y', saved: 'N' });
      expect(s).toEqual({ viewCnt: 12, likeCnt: 3, commentCnt: 0, saveCnt: 0, shareCnt: 0, liked: true, saved: false });
    });

    it('응답이 없으면 전부 0·false', () => {
      expect(toContentStats(undefined)).toEqual({ viewCnt: 0, likeCnt: 0, commentCnt: 0, saveCnt: 0, shareCnt: 0, liked: false, saved: false });
    });
  });
});
