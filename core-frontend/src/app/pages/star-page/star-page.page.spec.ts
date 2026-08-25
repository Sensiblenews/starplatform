import { StarPagePage } from './star-page.page';

/**
 * 피드 목록 병합 로직 단위 테스트 (2-26차).
 *
 * StarPagePage는 주입 대상이 15개라 TestBed로 띄우면 테스트가 무거워지고
 * 정작 검증하려는 배열 병합 규칙과는 상관없는 부분에서 깨진다.
 * 여기서 다루는 메서드는 this.feedList 외에 의존하는 것이 없으므로
 * 생성자를 거치지 않고 프로토타입만 빌려 검증한다.
 */
function makePage(feedList: any[] = []): any {
  const page: any = Object.create(StarPagePage.prototype);
  page.feedList = feedList;
  return page;
}

function makeFeed(conId: number, overrides: any = {}): any {
  return Object.assign({
    CON_ID: conId,
    CON_BODY: `본문 ${conId}`,
    MEDIA_TYPE: 'PHOTO',
    image: `https://cdn/${conId}.jpg`,
    LIKE_CNT: 0,
    COMMENT_CNT: 0,
    IS_LIKED: 0
  }, overrides);
}

describe('StarPagePage — 피드 목록', () => {

  describe('trackByFeed (항목 추적 키)', () => {
    it('콘텐츠는 CON_ID로, 광고 슬롯은 adId로 구분한다', () => {
      const page = makePage();
      expect(page.trackByFeed(0, makeFeed(12))).toBe('con-12');
      expect(page.trackByFeed(1, { isAd: true, adId: 2 })).toBe('ad-2');
    });

    it('같은 콘텐츠는 객체가 달라도 같은 키를 돌려준다', () => {
      const page = makePage();
      expect(page.trackByFeed(0, makeFeed(5))).toBe(page.trackByFeed(3, makeFeed(5)));
    });
  });

  describe('prepareFeedItem (초기 상태 세팅)', () => {
    it('미디어가 있으면 로딩 전(isLoaded=false)으로 둔다', () => {
      const page = makePage();
      expect(page.prepareFeedItem(makeFeed(1)).isLoaded).toBeFalse();
    });

    it('미디어가 없는 텍스트 피드는 바로 로딩 완료로 본다 (스피너가 남지 않게)', () => {
      const page = makePage();
      const text = makeFeed(1, { MEDIA_TYPE: 'PHOTO', image: null });
      expect(page.prepareFeedItem(text).isLoaded).toBeTrue();
    });

    it('동영상은 음소거 상태로 시작한다', () => {
      const page = makePage();
      const video = makeFeed(1, { MEDIA_TYPE: 'VIDEO' });
      expect(page.prepareFeedItem(video).isMuted).toBeTrue();
    });
  });

  describe('mergeFeed (복귀 후 백그라운드 갱신)', () => {
    it('첫 로드는 받은 목록을 그대로 쓴다', () => {
      const page = makePage();
      const merged = page.mergeFeed([makeFeed(1), makeFeed(2)], true);
      expect(merged.length).toBe(2);
      expect(merged[0].CON_ID).toBe(1);
    });

    it('같은 콘텐츠는 기존 객체를 재사용한다 (trackBy가 DOM을 유지하도록)', () => {
      const existing = makeFeed(1);
      existing.isLoaded = true;
      const page = makePage([existing]);

      const merged = page.mergeFeed([makeFeed(1)], false);

      expect(merged[0]).toBe(existing);
    });

    it('이미 로딩된 이미지의 isLoaded를 되돌리지 않는다 (되돌리면 다시 페이드인하며 깜빡인다)', () => {
      const existing = makeFeed(1);
      existing.isLoaded = true;
      const page = makePage([existing]);

      const merged = page.mergeFeed([makeFeed(1)], false);

      expect(merged[0].isLoaded).toBeTrue();
    });

    it('서버에서 바뀐 값은 반영한다', () => {
      const existing = makeFeed(1, { LIKE_CNT: 3 });
      existing.isLoaded = true;
      existing.likeCount = 3;
      const page = makePage([existing]);

      const merged = page.mergeFeed([makeFeed(1, { LIKE_CNT: 9, IS_LIKED: 1 })], false);

      expect(merged[0].likeCount).toBe(9);
      expect(merged[0].hasLiked).toBeTrue();
      expect(merged[0].isLoaded).toBeTrue();
    });

    it('새로 올라온 콘텐츠는 새 객체로 들어온다', () => {
      const page = makePage([makeFeed(1)]);
      const merged = page.mergeFeed([makeFeed(2), makeFeed(1)], false);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([2, 1]);
    });

    it('다시 받은 범위 밖의 항목은 남긴다 (무한 스크롤로 본 뒤쪽이 잘리지 않게)', () => {
      const page = makePage([makeFeed(1), makeFeed(2), makeFeed(3)]);

      // 갱신은 앞 2건만 다시 받아온 상황
      const merged = page.mergeFeed([makeFeed(1), makeFeed(2)], false);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([1, 2, 3]);
    });

    it('광고 슬롯은 병합 대상에서 뺀다 (insertAdSlots가 다시 꽂는다)', () => {
      const page = makePage([makeFeed(1), { isAd: true, adId: 1 }, makeFeed(2)]);

      const merged = page.mergeFeed([makeFeed(1), makeFeed(2)], false);

      expect(merged.some((m: any) => m.isAd)).toBeFalse();
      expect(merged.length).toBe(2);
    });

    it('갱신에서 사라진 항목은 목록에서도 빠진다 (삭제된 글이 남지 않게)', () => {
      const page = makePage([makeFeed(1), makeFeed(2)]);

      const merged = page.mergeFeed([makeFeed(2)], false);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([2]);
    });
  });
});
