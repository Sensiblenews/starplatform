<%--
  공개 웹사이트 공통 크롬(헤더 내비게이션 + 푸터 + 모바일 하단 탭바) 스타일.

  posts.jsp(마케팅 허브)·content_landing.jsp 는 각자 독립적인 style 블록을 들고 있다.
  그 규칙들과 충돌하지 않도록 여기서는 element 선택자(header·footer 등)를 쓰지 않고
  .site-nav / .site-footer / .site-tabbar 로 시작하는 클래스만 정의한다. 기존 페이지에
  이 파일을 끼워 넣어도 원래 레이아웃이 바뀌지 않는다.
  유일한 예외는 모바일에서 body 에 주는 하단 여백이다 — 고정 탭바가 푸터를 덮지 않게
  하려면 문서 자체에 여백이 있어야 한다. web-tabbar.jsp 를 include 하는 페이지만 이 파일을 쓴다.

  head 안에서 include 할 것. 마크업은 web-nav.jsp / web-footer.jsp / web-tabbar.jsp 에 있다.

  [2-29차 후속] 클라이언트 PC 시안: 짙은 남색 헤더·푸터, 본문은 흰 배경. 헤더에 돋보기 검색 포함.
  주의: 이 JSP 주석 안에 style 태그 문자열을 그대로 쓰지 말 것 — 파일을 스크립트로 자를 때 헷갈린다.
--%>
<style>
  /* ── 헤더 내비게이션 (클라이언트 PC 시안: 짙은 남색 바) ─────────────── */
  .site-nav { position: sticky; top: 0; z-index: 100; background: #0b1730; border-bottom: 1px solid rgba(255, 255, 255, 0.06); }
  .site-nav-inner { position: relative; max-width: 1400px; margin: 0 auto; padding: 12px 24px; display: flex; align-items: center; gap: 16px; }
  .site-nav-brand { display: inline-flex; align-items: center; gap: 8px; font-size: 21px; font-weight: 700; color: #ffffff; text-decoration: none; margin-right: auto; letter-spacing: -0.2px; }
  .site-nav-star { width: 26px; height: 26px; flex-shrink: 0; }
  /* 메뉴는 바 전체 기준 가운데. 좌우 요소 폭이 달라도 흔들리지 않게 절대 배치한다 */
  .site-nav-links { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); display: flex; align-items: center; gap: 8px; list-style: none; margin: 0; padding: 0; }
  .site-nav-links a { display: block; padding: 8px 10px; font-size: 15px; font-weight: 600; color: #e2e8f0; text-decoration: none; border-bottom: 2px solid transparent; }
  .site-nav-links a:hover { color: #ffffff; }
  /* 현재 페이지: 흰 글자 + 노란 밑줄 (시안). 색만으로는 구분이 어려운 이용자를 위해 밑줄을 함께 준다 */
  .site-nav-links a[aria-current="page"] { color: #ffffff; border-bottom-color: #facc15; }
  .site-nav-tools { display: flex; align-items: center; gap: 10px; }
  .site-nav-search { display: none; }
  .site-nav-search.is-open { display: block; }
  .site-nav-search input { width: 200px; height: 36px; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.25); background: rgba(255, 255, 255, 0.08); color: #ffffff; padding: 0 12px; font-size: 14px; font-family: inherit; outline: none; }
  .site-nav-search input::placeholder { color: #94a3b8; }
  .site-nav-search input:focus { border-color: #60a5fa; background: rgba(255, 255, 255, 0.12); }
  .site-nav-search-btn { width: 36px; height: 36px; display: inline-flex; align-items: center; justify-content: center; background: transparent; border: 0; border-radius: 8px; color: #ffffff; cursor: pointer; }
  .site-nav-search-btn svg { width: 22px; height: 22px; }
  .site-nav-search-btn:hover { background: rgba(255, 255, 255, 0.1); }
  .site-nav-cta { display: inline-flex; align-items: center; gap: 6px; padding: 9px 18px; border-radius: 10px; background: #2f7cf6; color: #ffffff; font-size: 14px; font-weight: 600; text-decoration: none; border: 0; cursor: pointer; font-family: inherit; white-space: nowrap; }
  .site-nav-cta svg { width: 16px; height: 16px; }
  .site-nav-cta:hover { background: #1d6ae8; }
  /* 키보드 포커스 링: 마우스 사용자에게만 숨기고 키보드 탐색에는 반드시 보이게 한다 */
  .site-nav a:focus-visible, .site-nav button:focus-visible, .site-footer a:focus-visible, .site-tabbar a:focus-visible { outline: 2px solid #60a5fa; outline-offset: 2px; }

  /* 햄버거 버튼은 모바일에서만 노출 */
  .site-nav-toggle { display: none; background: transparent; border: 1px solid rgba(255, 255, 255, 0.35); border-radius: 8px; color: #ffffff; font-size: 13px; font-weight: 600; padding: 8px 12px; cursor: pointer; font-family: inherit; }
  .site-nav-toggle:hover { background: rgba(255, 255, 255, 0.1); }
  /* 햄버거 아이콘: 가로 막대 3개를 CSS 로 그린다 (문자 아이콘은 인코딩에 취약하다) */
  .site-nav-bars { display: inline-block; width: 16px; height: 2px; background: currentColor; position: relative; }
  .site-nav-bars::before, .site-nav-bars::after { content: ""; position: absolute; left: 0; width: 16px; height: 2px; background: currentColor; }
  .site-nav-bars::before { top: -5px; }
  .site-nav-bars::after { top: 5px; }

  @media (max-width: 1000px) {
    .site-nav-links { position: static; transform: none; }
  }
  @media (max-width: 820px) {
    .site-nav-inner { padding: 10px 16px; }
    /* 모바일 시안: 햄버거는 아이콘만 (글자 없이). aria-label 로 이름은 유지 */
    .site-nav-toggle { display: inline-flex; align-items: center; justify-content: center; width: 40px; height: 40px; padding: 0; border: 0; font-size: 0; }
    .site-nav-toggle .site-nav-bars, .site-nav-toggle .site-nav-bars::before, .site-nav-toggle .site-nav-bars::after { width: 22px; height: 2.5px; border-radius: 2px; }
    .site-nav-toggle .site-nav-bars::before { top: -7px; }
    .site-nav-toggle .site-nav-bars::after { top: 7px; }
    .site-nav-tools { gap: 2px; }
    .site-nav-toggle-mobile { order: 10; }
    /* 모바일: 링크 목록을 세로 패널로 펼친다 */
    .site-nav-links { display: none; position: absolute; left: 0; right: 0; top: 100%; flex-direction: column; align-items: stretch; gap: 0; padding: 8px; background: #0b1730; border-bottom: 1px solid rgba(255, 255, 255, 0.08); box-shadow: 0 12px 24px rgba(0, 0, 0, 0.25); max-height: calc(100vh - 60px); overflow-y: auto; }
    .site-nav-links.is-open { display: flex; }
    .site-nav-links a { padding: 12px; font-size: 15px; border-bottom: 0; }
    .site-nav-links a[aria-current="page"] { box-shadow: inset 3px 0 0 #facc15; border-radius: 6px; }
    /* PC 전용 항목은 모바일 패널 안에서 일반 링크로 합류시킨다 */
    .site-nav-cta { display: none; }
    .site-nav-links .site-nav-mobile-only { display: block; }
    .site-nav-search input { width: 140px; }
  }
  /* 모바일 패널에서만 보이는 항목(약관·정책 등)은 PC에서 숨긴다 */
  .site-nav-mobile-only { display: none; }

  /* ── 푸터 (시안: 짙은 남색 한 줄 — 로고 / 메뉴 / SNS 아이콘) ─────────── */
  .site-footer { margin-top: 48px; background: #0b1730; color: #cbd5e1; }
  .site-footer-inner { max-width: 1400px; margin: 0 auto; padding: 22px 24px 18px; }
  .site-footer-row { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
  .site-footer-brand { display: inline-flex; align-items: center; gap: 8px; font-size: 18px; font-weight: 700; color: #ffffff; text-decoration: none; margin-right: auto; }
  .site-footer-links { display: flex; align-items: center; gap: 28px; list-style: none; margin: 0 auto; padding: 0; }
  .site-footer-links a { font-size: 14px; color: #cbd5e1; text-decoration: none; }
  .site-footer-links a:hover { color: #ffffff; }
  .site-footer-social { display: flex; align-items: center; gap: 18px; list-style: none; margin: 0 0 0 auto; padding: 0; }
  .site-footer-social a { display: inline-flex; color: #e2e8f0; }
  .site-footer-social a:hover { color: #ffffff; }
  .site-footer-social svg { width: 18px; height: 18px; }
  .site-footer-bottom { margin-top: 12px; font-size: 12px; color: #94a3b8; text-align: right; }
  .site-footer-bottom p { margin: 0; }
  .site-footer-legal { margin-left: 14px; }
  .site-footer-legal a { color: #94a3b8; text-decoration: none; }
  .site-footer-legal a:hover { color: #ffffff; text-decoration: underline; }
  @media (max-width: 820px) {
    .site-footer-row { flex-direction: column; align-items: flex-start; gap: 14px; }
    .site-footer-brand, .site-footer-social { margin: 0; }
    .site-footer-links { margin: 0; flex-wrap: wrap; gap: 14px 20px; }
    .site-footer-bottom { text-align: left; }
    .site-footer-legal { display: block; margin: 4px 0 0; }
  }

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
