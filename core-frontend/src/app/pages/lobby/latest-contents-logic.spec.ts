import {
  LATEST_DEFAULT_INTERVAL_SEC,
  LATEST_FEED_AD_AFTER,
  LatestContentItem,
  buildLatestFeedRows,
  isVideoItem,
  pickActiveIndex,
  pickActiveIndexes,
  previewText,
  nextScrollLeft,
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
  BLANK_VIDEO_POSTER,
  videoPoster,
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

describe('latest-contents-logic (2-34차 세로형 Latest Content)', () => {
  const item = (n: number, mediaType: string | null = 'VIDEO'): LatestContentItem => ({
    lcId: n, conId: n, prsId: 'p' + n, starName: 'S' + n, starImage: null, title: null,
    mediaUrl: 'm' + n, thumbUrl: null, isFeatured: 'N', mediaType,
  });
  const items = (n: number) => Array.from({ length: n }, (_, i) => item(i + 1));
  const shape = (rows: ReturnType<typeof buildLatestFeedRows>) =>
    rows.map(r => r.kind === 'ad' ? 'ad' + r.slot : String(r.item.lcId)).join(',');

  it('광고 위치는 3·6·9번째 뒤 (2-34 후속 클라이언트 요청)', () => {
    expect(LATEST_FEED_AD_AFTER).toEqual([3, 6, 9]);
  });

  it('12개면 3·6·9 뒤에 슬롯 3·4·5 가 순서대로 들어간다', () => {
    expect(shape(buildLatestFeedRows(items(12), [3, 4, 5]))).toBe('1,2,3,ad3,4,5,6,ad4,7,8,9,ad5,10,11,12');
  });

  it('마지막 콘텐츠 뒤에는 광고를 붙이지 않는다 (콘텐츠 사이에만)', () => {
    expect(shape(buildLatestFeedRows(items(3), [3, 4, 5]))).toBe('1,2,3');
    expect(shape(buildLatestFeedRows(items(4), [3, 4, 5]))).toBe('1,2,3,ad3,4');
    expect(shape(buildLatestFeedRows(items(6), [3, 4, 5]))).toBe('1,2,3,ad3,4,5,6');
    expect(shape(buildLatestFeedRows(items(7), [3, 4, 5]))).toBe('1,2,3,ad3,4,5,6,ad4,7');
    expect(shape(buildLatestFeedRows(items(9), [3, 4, 5]))).toBe('1,2,3,ad3,4,5,6,ad4,7,8,9');
    expect(shape(buildLatestFeedRows(items(10), [3, 4, 5]))).toBe('1,2,3,ad3,4,5,6,ad4,7,8,9,ad5,10');
  });

  it('콘텐츠가 모자라면 그 뒤 광고는 넣지 않는다', () => {
    expect(shape(buildLatestFeedRows(items(2), [3, 4, 5]))).toBe('1,2');
  });

  it('빈 목록·없는 목록은 빈 줄', () => {
    expect(buildLatestFeedRows([], [3, 4, 5])).toEqual([]);
    expect(buildLatestFeedRows(null as any, [3, 4, 5])).toEqual([]);
  });

  it('isVideoItem — mediaType 이 없으면(2-33 숏폼) 영상, PHOTO 는 아님', () => {
    expect(isVideoItem(item(1, null))).toBe(true);
    expect(isVideoItem(item(1, 'VIDEO'))).toBe(true);
    expect(isVideoItem(item(1, 'PHOTO'))).toBe(false);
    expect(isVideoItem(undefined)).toBe(false);
  });

  it('pickActiveIndex — 가장 많이 보이는 영상, 동률은 앞, 절반 미만·사진은 제외', () => {
    expect(pickActiveIndex([0.2, 0.9, 0.6], [true, true, true])).toBe(1);
    expect(pickActiveIndex([0.8, 0.8], [true, true])).toBe(0);
    expect(pickActiveIndex([1, 0.7], [false, true])).toBe(1);
    expect(pickActiveIndex([0.4, 0.3], [true, true])).toBe(-1);
    expect(pickActiveIndex([], [])).toBe(-1);
  });
});

describe('latest-contents-logic (2-34 후속 가로형 2장 재생·자동 넘김)', () => {
  it('pickActiveIndexes — 절반 이상 보이는 영상을 앞에서부터 최대 2개', () => {
    expect(pickActiveIndexes([1, 0.7, 0.2], [true, true, true], 2)).toEqual([0, 1]);
    expect(pickActiveIndexes([1, 0.7, 0.6], [false, true, true], 2)).toEqual([1, 2]);
    expect(pickActiveIndexes([0.3, 0.4], [true, true], 2)).toEqual([]);
    expect(pickActiveIndexes([1, 1, 1], [true, true, true], 1)).toEqual([0]);
  });

  it('nextScrollLeft — 한 칸씩, 끝에서 처음으로', () => {
    expect(nextScrollLeft(0, 230, 1000)).toBe(230);
    expect(nextScrollLeft(900, 230, 1000)).toBe(1000);
    expect(nextScrollLeft(1000, 230, 1000)).toBe(0);
    expect(nextScrollLeft(999, 230, 1000)).toBe(0);
    expect(nextScrollLeft(0, 230, 0)).toBe(0);
  });
});

describe('previewText (2-34 후속 세로형 카드 글)', () => {
  it('빈 줄을 빼서 두 번째 줄에 말줄임표만 남지 않게', () => {
    expect(previewText('들꽃\n\nhttps://youtube.com/x')).toBe('들꽃\nhttps://youtube.com/x');
    expect(previewText('  a  \r\n\r\n\n b ')).toBe('a\nb');
  });

  it('비었으면 빈 문자열', () => {
    expect(previewText(null)).toBe('');
    expect(previewText('\n \n')).toBe('');
  });
});

describe('videoPoster (2-35차 썸네일 없는 영상)', () => {
  it('썸네일이 있으면 썸네일을 쓴다', () => {
    expect(videoPoster({ thumbUrl: 'https://x/t.jpg' })).toBe('https://x/t.jpg');
  });

  it('썸네일이 없거나 깨졌으면 투명 poster — Android 기본 회색 배너 방지', () => {
    expect(videoPoster({ thumbUrl: null })).toBe(BLANK_VIDEO_POSTER);
    expect(videoPoster({ thumbUrl: 'https://x/t.jpg', thumbFailed: true })).toBe(BLANK_VIDEO_POSTER);
    expect(videoPoster(null)).toBe(BLANK_VIDEO_POSTER);
  });
});
