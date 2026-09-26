<%--
  공개 웹사이트 공통 크롬(헤더 내비게이션 + 푸터 + 모바일 하단 탭바) 스타일.

  posts.jsp(마케팅 허브)·content_landing.jsp 는 각자 독립적인 <style> 블록을 들고 있다.
  그 규칙들과 충돌하지 않도록 여기서는 element 선택자(header·footer 등)를 쓰지 않고
  .site-nav / .site-footer / .site-tabbar 로 시작하는 클래스만 정의한다. 기존 페이지에
  이 파일을 끼워 넣어도 원래 레이아웃이 바뀌지 않는다.
  유일한 예외는 모바일에서 body 에 주는 하단 여백이다 — 고정 탭바가 푸터를 덮지 않게
  하려면 문서 자체에 여백이 있어야 한다. web-tabbar.jsp 를 include 하는 페이지만 이 파일을 쓴다.

  <head> 안에서 include 할 것. 마크업은 web-nav.jsp / web-footer.jsp / web-tabbar.jsp 에 있다.

  [2-29차 후속] 화이트 모드: 흰 헤더 + 옅은 회색 푸터.
--%>
<style>
  /* ── 헤더 내비게이션 ───────────────────────────────────────── */
  .site-nav { position: sticky; top: 0; z-index: 100; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border-bottom: 1px solid #e2e8f0; }
  .site-nav-inner { max-width: 1100px; margin: 0 auto; padding: 12px 20px; display: flex; align-items: center; gap: 16px; }
  .site-nav-brand { font-size: 20px; font-weight: 700; color: #0f172a; text-decoration: none; margin-right: auto; }
  .site-nav-links { display: flex; align-items: center; gap: 4px; list-style: none; margin: 0; padding: 0; }
  .site-nav-links a { display: block; padding: 8px 12px; border-radius: 8px; font-size: 14px; font-weight: 600; color: #475569; text-decoration: none; }
  .site-nav-links a:hover { background: #f1f5f9; color: #0f172a; }
  /* 현재 페이지 표시: 색과 밑줄 둘 다 준다. 색만으로는 구분이 어려운 이용자가 있다 */
  .site-nav-links a[aria-current="page"] { color: #1d4ed8; background: #eff6ff; box-shadow: inset 0 -2px 0 #2563eb; }
  .site-nav-cta { padding: 8px 16px; border-radius: 8px; background: #2563eb; color: #ffffff; font-size: 14px; font-weight: 600; text-decoration: none; border: 0; cursor: pointer; font-family: inherit; }
  .site-nav-cta:hover { background: #1d4ed8; }
  /* 키보드 포커스 링: 마우스 사용자에게만 숨기고 키보드 탐색에는 반드시 보이게 한다 */
  .site-nav a:focus-visible, .site-nav button:focus-visible, .site-footer a:focus-visible, .site-tabbar a:focus-visible { outline: 2px solid #2563eb; outline-offset: 2px; }

  /* 햄버거 버튼은 모바일에서만 노출 */
  .site-nav-toggle { display: none; background: #ffffff; border: 1px solid #cbd5e1; border-radius: 8px; color: #0f172a; font-size: 13px; font-weight: 600; padding: 8px 12px; cursor: pointer; font-family: inherit; }
  .site-nav-toggle:hover { background: #f1f5f9; }
  /* 햄버거 아이콘: 가로 막대 3개를 CSS 로 그린다 (문자 아이콘은 인코딩에 취약하다) */
  .site-nav-bars { display: inline-block; width: 16px; height: 2px; background: currentColor; position: relative; }
  .site-nav-bars::before, .site-nav-bars::after { content: ""; position: absolute; left: 0; width: 16px; height: 2px; background: currentColor; }
  .site-nav-bars::before { top: -5px; }
  .site-nav-bars::after { top: 5px; }

  @media (max-width: 820px) {
    .site-nav-toggle { display: inline-flex; align-items: center; gap: 6px; }
    /* 모바일: 링크 목록을 세로 패널로 펼친다. 닫힘 상태는 hidden 속성으로 제어해
       키보드·스크린리더 탐색에서도 함께 사라지게 한다 (display:none 만으로는 부족) */
    .site-nav-links { display: none; position: absolute; left: 0; right: 0; top: 100%; flex-direction: column; align-items: stretch; gap: 0; padding: 8px; background: #ffffff; border-bottom: 1px solid #e2e8f0; box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08); max-height: calc(100vh - 60px); overflow-y: auto; }
    .site-nav-links.is-open { display: flex; }
    .site-nav-links a { padding: 12px; font-size: 15px; }
    .site-nav-links a[aria-current="page"] { box-shadow: inset 2px 0 0 #2563eb; }
    /* PC 전용 항목은 모바일 패널 안에서 일반 링크로 합류시킨다 */
    .site-nav-cta { display: none; }
    .site-nav-links .site-nav-mobile-only { display: block; }
  }
  /* 모바일 패널에서만 보이는 항목(약관·정책 등)은 PC에서 숨긴다 */
  .site-nav-mobile-only { display: none; }
  .site-nav-inner { position: relative; }

  /* ── 푸터 ─────────────────────────────────────────────────── */
  .site-footer { margin-top: 60px; border-top: 1px solid #e2e8f0; background: #f1f5f9; }
  .site-footer-inner { max-width: 1100px; margin: 0 auto; padding: 40px 20px 24px; }
  .site-footer-cols { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 28px; }
  .site-footer-col h2 { font-size: 13px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; color: #0f172a; margin: 0 0 12px; }
  .site-footer-col ul { list-style: none; margin: 0; padding: 0; }
  .site-footer-col li { margin-bottom: 8px; }
  .site-footer-col a { font-size: 14px; color: #475569; text-decoration: none; }
  .site-footer-col a:hover { color: #0f172a; text-decoration: underline; }
  .site-footer-bottom { margin-top: 32px; padding-top: 20px; border-top: 1px solid #e2e8f0; font-size: 13px; color: #64748b; line-height: 1.8; }
  .site-footer-bottom strong { color: #334155; font-weight: 600; }
  .site-footer-bottom a { color: #334155; text-decoration: none; }
  .site-footer-bottom a:hover { text-decoration: underline; }

  /* ── 모바일 하단 탭바 ─────────────────────────────────────── */
  /* [2-29차 후속] 클라이언트 모바일 시안의 고정 탭바. PC 에서는 헤더 메뉴가 있으므로 숨긴다.
     아이콘은 전부 CSS 로 그린다 — include 조각은 비ASCII 문자를 안전하게 내보낼 수 없다 */
  .site-tabbar { display: none; }
  @media (max-width: 820px) {
    body { padding-bottom: calc(64px + env(safe-area-inset-bottom)); }
    .site-tabbar { display: flex; position: fixed; left: 0; right: 0; bottom: 0; z-index: 100; background: #ffffff; border-top: 1px solid #e2e8f0; padding: 6px 4px calc(6px + env(safe-area-inset-bottom)); }
    .site-tabbar a { flex: 1 1 0; display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 4px 0; font-size: 11px; font-weight: 600; color: #64748b; text-decoration: none; }
    .site-tabbar a[aria-current="page"] { color: #2563eb; }
    .site-tabbar-icon { position: relative; width: 22px; height: 22px; display: block; }
    /* Home: 지붕(회전한 모서리) + 몸통 */
    .site-tabbar-icon-home::before { content: ""; position: absolute; left: 5px; top: 1px; width: 12px; height: 12px; border-left: 2px solid currentColor; border-top: 2px solid currentColor; transform: rotate(45deg); }
    .site-tabbar-icon-home::after { content: ""; position: absolute; left: 4px; bottom: 1px; width: 14px; height: 10px; border: 2px solid currentColor; border-top: 0; border-radius: 0 0 3px 3px; box-sizing: border-box; }
    /* About: 원 안의 i */
    .site-tabbar-icon-about { border: 2px solid currentColor; border-radius: 50%; box-sizing: border-box; }
    .site-tabbar-icon-about::before { content: ""; position: absolute; left: 8px; top: 4px; width: 2px; height: 2px; background: currentColor; }
    .site-tabbar-icon-about::after { content: ""; position: absolute; left: 8px; top: 8px; width: 2px; height: 7px; background: currentColor; }
    /* Posts: 가로줄 3개 */
    .site-tabbar-icon-posts::before { content: ""; position: absolute; left: 3px; right: 3px; top: 4px; height: 2px; background: currentColor; box-shadow: 0 6px 0 currentColor, 0 12px 0 currentColor; }
    /* FAQ: 원 안의 물음표 (ASCII 문자라 include 에서도 안전하다) */
    .site-tabbar-icon-faq { border: 2px solid currentColor; border-radius: 50%; box-sizing: border-box; font-size: 13px; font-weight: 700; line-height: 18px; text-align: center; }
    .site-tabbar-icon-faq::before { content: "?"; }
    /* Contact: 봉투 (사각 테두리 + 회전한 모서리로 만든 덮개) */
    .site-tabbar-icon-contact { border: 2px solid currentColor; border-radius: 4px; box-sizing: border-box; overflow: hidden; height: 18px; margin-top: 2px; }
    .site-tabbar-icon-contact::before { content: ""; position: absolute; left: 4px; top: -6px; width: 10px; height: 10px; border-right: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }
  }
</style>
