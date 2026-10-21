import { Router } from '@angular/router';
import { DeepLinkService } from '../services/deep-link.service';

/**
 * 링크로 인정하는 최상위 도메인 (2-35차 — https:// 없이 example.com, naver.co.kr 처럼 써도 링크가 되게).
 * 아무 "단어.단어"나 잡으면 파일명(photo.jpg)·버전(v2.0)·약어(e.g.)까지 링크가 되므로 자주 쓰는 것만 둔다.
 * 2단계 국가 도메인(co.kr, co.jp, co.uk …)은 앞 라벨 반복으로 잡히므로 국가 코드(kr, jp, uk …)만 있으면 된다.
 * 한글 라벨은 받지 않는다 — "사이트는naver.com"처럼 붙여 쓴 한글까지 주소로 잡힌다.
 * 새 도메인 접미사가 필요하면 여기에 더한다.
 */
export const LINKIFY_TLDS = [
  // 일반
  'com', 'net', 'org', 'edu', 'gov', 'mil', 'int', 'info', 'biz', 'name', 'pro', 'mobi',
  'io', 'co', 'me', 'tv', 'ai', 'app', 'dev', 'page', 'xyz', 'site', 'online', 'store', 'shop',
  'blog', 'news', 'live', 'link', 'club', 'tech', 'media', 'world', 'today', 'space', 'website',
  'ly', 'gl', 'gg', 'to', 'fm', 'cc', 'ws',
  // 국가
  'kr', 'jp', 'cn', 'tw', 'hk', 'sg', 'vn', 'th', 'ph', 'id', 'my', 'in', 'au', 'nz',
  'us', 'ca', 'mx', 'br', 'ar', 'cl', 'uk', 'ie', 'de', 'fr', 'it', 'es', 'pt', 'nl', 'be',
  'ch', 'at', 'se', 'no', 'dk', 'fi', 'pl', 'cz', 'ru', 'ua', 'tr', 'gr', 'il', 'ae', 'sa', 'za', 'eu',
];

// 1) http(s):// 로 시작하는 주소  2) www. 로 시작하는 주소
// 3) 앞이 영문자·숫자·@·.·/·- 가 아닌 자리에서 시작하는 "라벨.라벨.최상위도메인[:포트][/경로]" (이메일·경로 중간은 제외)
// lookbehind 는 iOS 16.4 미만 웹뷰에서 문법 오류라 쓰지 않고, 앞 글자를 그룹으로 잡아 그대로 돌려준다
const LINK_RE = new RegExp(
  '(https?:\\/\\/[^\\s<]+)'
  + '|(^|[^\\w@.\\/-])('
  + 'www\\.[^\\s<]+'
  + '|(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:' + LINKIFY_TLDS.join('|') + ')(?::\\d{1,5})?(?:[\\/?#][^\\s<]*)?(?![\\w-])'
  + ')',
  'gi'
);

/**
 * 본문·소개문의 주소를 누를 수 있는 링크로 바꾼다 (2-35차 — 게시물 상세 전용이던 로직을 공용으로).
 *
 * 먼저 HTML 을 전부 escape 한 뒤 링크만 덧씌우므로 사용자 입력이 HTML 로 실행되지 않는다.
 * http(s)://·www. 로 시작하는 주소와, 앞에 아무것도 없어도 LINKIFY_TLDS 로 끝나는 도메인(naver.com, abc.co.kr/path)을 잡는다.
 * 줄바꿈은 <br /> 로 바꾼다. 결과는 bypassSecurityTrustHtml 로 넘겨 [innerHTML] 에 쓴다.
 */
export function linkifyText(text: string): string {
  const escaped = (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

  const withLinks = escaped.replace(LINK_RE, (whole: string, httpUrl: string, lead: string, bare: string) => {
    const raw = httpUrl || bare;
    const prefix = httpUrl ? '' : (lead || '');
    // 주소 뒤에 붙은 따옴표·괄호(escape 된 엔티티)는 주소에서 뺀다 ("naver.com" → 링크는 naver.com)
    const entityAt = raw.search(/&(quot|#39|lt|gt);/);
    let url = entityAt >= 0 ? raw.slice(0, entityAt) : raw;
    let rest = entityAt >= 0 ? raw.slice(entityAt) : '';
    // 문장 끝 마침표·쉼표·닫는 괄호도 뺀다 ("...example.com." → 링크는 example.com)
    const trail = (url.match(/[.,!?;:)\]]+$/) || [''])[0];
    if (trail) {
      url = url.slice(0, url.length - trail.length);
      rest = trail + rest;
    }
    if (!url || url.toLowerCase() === 'www.') return whole;
    const href = /^https?:\/\//i.test(url) ? url : `https://${url}`;
    return `${prefix}<a class="body-link" href="${href}" rel="noopener noreferrer">${url}</a>${rest}`;
  });

  return withLinks.replace(/\r?\n/g, '<br />');
}

/**
 * linkifyText 로 만든 링크를 눌렀을 때 — 앱 웹뷰 안에서 페이지가 바뀌지 않게 가로챈다.
 * 우리 서비스 주소(/star/, /post/ ...)는 앱 안에서 열고, 나머지는 인앱 브라우저로 연다.
 * 링크를 누른 게 아니면 false 를 돌려준다 (카드 탭 등 원래 동작을 그대로 두기 위해)
 */
export function handleLinkClick(event: Event, deepLink: DeepLinkService, router: Router): boolean {
  const target = event.target as HTMLElement | null;
  const anchor = target && target.closest ? (target.closest('a.body-link') as HTMLAnchorElement | null) : null;
  if (!anchor) return false;

  event.preventDefault();
  event.stopPropagation();
  const href = anchor.getAttribute('href') || '';
  const decision = deepLink.resolve(href);
  if (decision.kind === 'route') {
    router.navigateByUrl(decision.url);
  } else {
    deepLink.openExternal(href);
  }
  return true;
}
