import { Injectable } from '@angular/core';
import { Browser } from '@capacitor/browser';

// 앱으로 열어야 하는 경로 화이트리스트. 여기에 없으면 전부 브라우저로 보낸다.
export const DEEP_LINK_PREFIXES = ['/post/', '/star/', '/feed-detail/'];

// 화이트리스트보다 먼저 검사하는 제외 경로. 약관·개인정보는 앱이 가로채지 않는다.
export const EXCLUDED_PREFIXES = ['/privacy', '/terms'];

// 유니버셜 링크로 들어올 수 있는 서비스 도메인
const APP_HOSTS = ['witch-hunting.com'];

// 레거시 컨텍스트 경로. 이전에 공유된 /witch/star/1 형태를 현재 경로로 되돌린다.
const LEGACY_PREFIX = '/witch';

// 커스텀 스킴 (웹 랜딩의 "Open in App" 버튼, 매직 로그인 메일이 사용)
const CUSTOM_SCHEME = 'witchhunting:';

export type DeepLinkDecision =
  // 앱 내부 라우팅 (쿼리스트링 포함 경로)
  | { kind: 'route'; url: string }
  // 스타 페이지 소유권 인증 링크
  | { kind: 'magic-login'; starId: string; email: string; pw: string }
  // 앱이 처리하지 않고 브라우저로 넘길 URL
  | { kind: 'external'; url: string }
  // 처리 대상 아님 (파싱 실패, 파라미터 부족 등)
  | { kind: 'ignore' };

@Injectable({ providedIn: 'root' })
export class DeepLinkService {

  // URL 하나를 받아 어떻게 처리할지만 결정한다. 실제 라우팅/오픈은 호출부가 수행한다.
  resolve(rawUrl: string): DeepLinkDecision {
    let parsed: URL;

    try {
      parsed = new URL(rawUrl);
    } catch (e) {
      return { kind: 'ignore' };
    }

    if (parsed.protocol === CUSTOM_SCHEME) {
      // witchhunting://claim-verify?starId=..&email=.. 형태
      if (parsed.hostname === 'claim-verify') {
        const starId = parsed.searchParams.get('starId');
        const email = parsed.searchParams.get('email');

        if (starId && email) {
          return { kind: 'magic-login', starId, email, pw: parsed.searchParams.get('pw') };
        }
        return { kind: 'ignore' };
      }

      // 커스텀 스킴은 host가 경로의 첫 조각이 된다 (witchhunting://star/1 → /star/1)
      return this.decideByPath('/' + parsed.hostname + parsed.pathname, parsed.search, rawUrl);
    }

    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
      return { kind: 'ignore' };
    }

    // 서비스 도메인이 아니면 볼 것도 없이 외부 링크
    if (APP_HOSTS.indexOf(parsed.hostname) === -1) {
      return { kind: 'external', url: rawUrl };
    }

    return this.decideByPath(parsed.pathname, parsed.search, rawUrl);
  }

  // 외부 URL은 OS가 제공하는 인앱 브라우저(SFSafariViewController / Chrome Custom Tabs)로 연다.
  // window.open('_system')을 쓰면 앱을 완전히 이탈해 스토어 심사에서 지적받는다.
  async openExternal(url: string): Promise<void> {
    try {
      await Browser.open({ url });
    } catch (e) {
      // 웹에서 실행 중이거나 플러그인을 쓸 수 없는 경우
      window.open(url, '_blank');
    }
  }

  // 처리 순서: 제외 경로 → 화이트리스트 → 나머지 전부 외부
  private decideByPath(rawPath: string, search: string, rawUrl: string): DeepLinkDecision {
    const path = this.stripLegacyPrefix(rawPath);

    const excluded = EXCLUDED_PREFIXES.some(p => path === p || path.indexOf(p + '/') === 0);
    if (excluded) {
      return { kind: 'external', url: rawUrl };
    }

    const prefix = DEEP_LINK_PREFIXES.find(p => path.indexOf(p) === 0);
    if (!prefix) {
      return { kind: 'external', url: rawUrl };
    }

    // 프리픽스 뒤 첫 조각만 id로 사용한다 (/star/1/extra → 1)
    const id = path.slice(prefix.length).split('/')[0];
    if (!id) {
      return { kind: 'external', url: rawUrl };
    }

    // 웹의 /post/{id}는 앱에서 피드 상세 화면에 대응된다
    const target = prefix === '/post/' ? '/feed-detail/' : prefix;

    // 추천인·유입 추적 파라미터가 유실되지 않도록 쿼리스트링을 그대로 넘긴다
    return { kind: 'route', url: target + id + search };
  }

  private stripLegacyPrefix(path: string): string {
    if (path === LEGACY_PREFIX) {
      return '/';
    }
    if (path.indexOf(LEGACY_PREFIX + '/') === 0) {
      return path.slice(LEGACY_PREFIX.length);
    }
    return path;
  }
}
