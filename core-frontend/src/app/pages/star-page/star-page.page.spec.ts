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
      const merged = page.mergeFeed([makeFeed(1), makeFeed(2)], true, 10);
      expect(merged.length).toBe(2);
      expect(merged[0].CON_ID).toBe(1);
    });

    it('같은 콘텐츠는 기존 객체를 재사용한다 (trackBy가 DOM을 유지하도록)', () => {
      const existing = makeFeed(1);
      existing.isLoaded = true;
      const page = makePage([existing]);

      const merged = page.mergeFeed([makeFeed(1)], false, 10);

      expect(merged[0]).toBe(existing);
    });

    it('이미 로딩된 이미지의 isLoaded를 되돌리지 않는다 (되돌리면 다시 페이드인하며 깜빡인다)', () => {
      const existing = makeFeed(1);
      existing.isLoaded = true;
      const page = makePage([existing]);

      const merged = page.mergeFeed([makeFeed(1)], false, 10);

      expect(merged[0].isLoaded).toBeTrue();
    });

    it('서버에서 바뀐 값은 반영한다', () => {
      const existing = makeFeed(1, { LIKE_CNT: 3 });
      existing.isLoaded = true;
      existing.likeCount = 3;
      const page = makePage([existing]);

      const merged = page.mergeFeed([makeFeed(1, { LIKE_CNT: 9, IS_LIKED: 1 })], false, 10);

      expect(merged[0].likeCount).toBe(9);
      expect(merged[0].hasLiked).toBeTrue();
      expect(merged[0].isLoaded).toBeTrue();
    });

    it('새로 올라온 콘텐츠는 새 객체로 들어온다', () => {
      const page = makePage([makeFeed(1)]);
      const merged = page.mergeFeed([makeFeed(2), makeFeed(1)], false, 10);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([2, 1]);
    });

    it('요청한 만큼 꽉 채워 왔으면 그 뒤쪽은 남긴다 (무한 스크롤로 본 뒤가 잘리지 않게)', () => {
      const page = makePage([makeFeed(1), makeFeed(2), makeFeed(3)]);

      // 한도 2건을 꽉 채워 받았으니 3번은 아직 뒤에 있는 것이다
      const merged = page.mergeFeed([makeFeed(1), makeFeed(2)], false, 2);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([1, 2, 3]);
    });

    it('한도보다 적게 왔으면 없는 항목은 삭제된 것으로 본다', () => {
      const page = makePage([makeFeed(1), makeFeed(2), makeFeed(3)]);

      // 한도 10건인데 2건만 왔다 = 서버가 가진 전부. 3번은 삭제됐다
      const merged = page.mergeFeed([makeFeed(1), makeFeed(2)], false, 10);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([1, 2]);
    });

    it('하나뿐인 글을 지우면 목록이 빈다 (지운 글이 되살아나지 않는다)', () => {
      // 위치 기반으로만 꼬리를 보존하면 previous.slice(0)이 통째로 살아나
      // 방금 지운 글이 화면에 그대로 남는다
      const page = makePage([makeFeed(1)]);

      const merged = page.mergeFeed([], false, 10);

      expect(merged.length).toBe(0);
    });

    it('마지막 글을 지워도 되살아나지 않는다', () => {
      const page = makePage([makeFeed(1), makeFeed(2)]);

      const merged = page.mergeFeed([makeFeed(1)], false, 10);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([1]);
    });

    it('검수 대기 → 승인 전환이 병합에서 반영된다', () => {
      // 대기 상태로 화면에 떠 있던 항목 (작성자 본인이라 토큰으로 보고 있었다)
      const page0 = makePage();
      const existing = page0.prepareFeedItem(
        makeFeed(1, { MDR_STATUS: 'PENDING', image: null, pendingImageToken: 'TOKEN1' }));
      existing.isLoaded = true;

      const page = makePage([existing]);

      // 관리자가 승인한 뒤의 서버 응답 (주소가 내려오고 토큰은 없다)
      const merged = page.mergeFeed(
        [makeFeed(1, { MDR_STATUS: 'APPROVED', image: 'https://cdn/1.jpg' })], false, 10);

      expect(merged[0].MDR_STATUS).toBe('APPROVED');
      expect(merged[0].image).toBe('https://cdn/1.jpg');
      expect(merged[0].pendingImageUrl).toBeNull();
      expect(merged[0].isUnderReview).toBeFalse();
    });

    it('광고 슬롯은 병합 대상에서 뺀다 (insertAdSlots가 다시 꽂는다)', () => {
      const page = makePage([makeFeed(1), { isAd: true, adId: 1 }, makeFeed(2)]);

      const merged = page.mergeFeed([makeFeed(1), makeFeed(2)], false, 10);

      expect(merged.some((m: any) => m.isAd)).toBeFalse();
      expect(merged.length).toBe(2);
    });

    it('갱신에서 사라진 항목은 목록에서도 빠진다 (삭제된 글이 남지 않게)', () => {
      const page = makePage([makeFeed(1), makeFeed(2)]);

      const merged = page.mergeFeed([makeFeed(2)], false, 10);

      expect(merged.map((m: any) => m.CON_ID)).toEqual([2]);
    });
  });
});
