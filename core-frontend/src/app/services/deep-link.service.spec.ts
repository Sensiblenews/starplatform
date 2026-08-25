import { TestBed } from '@angular/core/testing';
import { DeepLinkService } from './deep-link.service';

describe('DeepLinkService', () => {
  let service: DeepLinkService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DeepLinkService);
  });

  it('생성된다', () => {
    expect(service).toBeTruthy();
  });

  describe('화이트리스트 경로는 앱 내부로 라우팅한다', () => {
    it('스타 페이지', () => {
      expect(service.resolve('https://witch-hunting.com/star/bts'))
        .toEqual({ kind: 'route', url: '/star/bts' });
    });

    it('피드 상세', () => {
      expect(service.resolve('https://witch-hunting.com/feed-detail/12'))
        .toEqual({ kind: 'route', url: '/feed-detail/12' });
    });

    it('/post/{id}는 피드 상세로 매핑된다', () => {
      expect(service.resolve('https://witch-hunting.com/post/12'))
        .toEqual({ kind: 'route', url: '/feed-detail/12' });
    });

    it('레거시 /witch 접두사를 제거한다', () => {
      expect(service.resolve('https://witch-hunting.com/witch/star/bts'))
        .toEqual({ kind: 'route', url: '/star/bts' });
    });

    it('커스텀 스킴도 같은 규칙을 탄다', () => {
      expect(service.resolve('witchhunting://star/bts'))
        .toEqual({ kind: 'route', url: '/star/bts' });
    });

    it('쿼리 파라미터를 유실하지 않는다', () => {
      expect(service.resolve('https://witch-hunting.com/star/bts?ref=123&utm_source=sns'))
        .toEqual({ kind: 'route', url: '/star/bts?ref=123&utm_source=sns' });
    });

    it('id 뒤 잉여 경로는 무시한다', () => {
      expect(service.resolve('https://witch-hunting.com/star/bts/extra'))
        .toEqual({ kind: 'route', url: '/star/bts' });
    });
  });

  describe('제외 경로는 브라우저로 넘긴다', () => {
    ['/privacy', '/terms', '/witch/privacy', '/witch/terms'].forEach(path => {
      it(path, () => {
        const url = 'https://witch-hunting.com' + path;
        expect(service.resolve(url)).toEqual({ kind: 'external', url });
      });
    });
  });

  describe('화이트리스트 밖은 전부 브라우저로 넘긴다', () => {
    it('웹 랜딩 루트', () => {
      const url = 'https://witch-hunting.com/';
      expect(service.resolve(url)).toEqual({ kind: 'external', url });
    });

    it('외부 도메인', () => {
      const url = 'https://youtube.com/watch?v=1';
      expect(service.resolve(url)).toEqual({ kind: 'external', url });
    });

    it('서비스 도메인의 미등록 경로', () => {
      const url = 'https://witch-hunting.com/sitemap.xml';
      expect(service.resolve(url)).toEqual({ kind: 'external', url });
    });

    it('id가 없는 화이트리스트 프리픽스', () => {
      const url = 'https://witch-hunting.com/star/';
      expect(service.resolve(url)).toEqual({ kind: 'external', url });
    });

    it('제외 경로와 접두사만 같은 경로도 통과시키지 않는다', () => {
      const url = 'https://witch-hunting.com/privacy-policy';
      expect(service.resolve(url)).toEqual({ kind: 'external', url });
    });
  });

  describe('매직 로그인', () => {
    it('starId와 email이 있으면 매직 로그인으로 판정한다', () => {
      expect(service.resolve('witchhunting://claim-verify?starId=7&email=a@b.com&pw=pass'))
        .toEqual({ kind: 'magic-login', starId: '7', email: 'a@b.com', pw: 'pass' });
    });

    it('필수 파라미터가 없으면 무시한다', () => {
      expect(service.resolve('witchhunting://claim-verify?starId=7'))
        .toEqual({ kind: 'ignore' });
    });
  });

  describe('처리 대상이 아닌 입력', () => {
    it('URL로 파싱되지 않으면 무시한다', () => {
      expect(service.resolve('not a url')).toEqual({ kind: 'ignore' });
    });

    it('http/https가 아닌 스킴은 무시한다', () => {
      expect(service.resolve('mailto:a@b.com')).toEqual({ kind: 'ignore' });
    });
  });
});
