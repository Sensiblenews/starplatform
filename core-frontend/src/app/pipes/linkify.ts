import { Router } from '@angular/router';
import { DeepLinkService } from '../services/deep-link.service';

/**
 * 본문·소개문의 주소를 누를 수 있는 링크로 바꾼다 (2-35차 — 게시물 상세 전용이던 로직을 공용으로).
 *
 * 먼저 HTML 을 전부 escape 한 뒤 링크만 덧씌우므로 사용자 입력이 HTML 로 실행되지 않는다.
 * http(s):// 와 www. 로 시작하는 주소를 잡는다. 줄바꿈은 <br /> 로 바꾼다.
 * 결과는 bypassSecurityTrustHtml 로 넘겨 [innerHTML] 에 쓴다.
 */
export function linkifyText(text: string): string {
  const escaped = (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

  const withLinks = escaped.replace(/(https?:\/\/[^\s<]+|www\.[^\s<]+)/gi, (match: string) => {
    // 문장 끝 마침표·쉼표·닫는 괄호는 주소에서 뺀다 ("...example.com." → 링크는 example.com)
    const trail = (match.match(/[.,!?;:)\]]+$/) || [''])[0];
    const url = trail ? match.slice(0, match.length - trail.length) : match;
    const href = /^https?:\/\//i.test(url) ? url : `https://${url}`;
    return `<a class="body-link" href="${href}" rel="noopener noreferrer">${url}</a>${trail}`;
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
