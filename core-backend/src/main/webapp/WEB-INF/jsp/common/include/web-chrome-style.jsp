<%--
  공개 웹사이트 공통 크롬(헤더 내비게이션 + 푸터) 스타일.

  기존 landing.jsp·content_landing.jsp 는 각자 독립적인 <style> 블록을 들고 있다.
  그 규칙들과 충돌하지 않도록 여기서는 element 선택자(header·footer 등)를 쓰지 않고
  .site-nav / .site-footer 로 시작하는 클래스만 정의한다. 기존 페이지에 이 파일을
  끼워 넣어도 원래 레이아웃이 바뀌지 않는다.

  <head> 안에서 include 할 것. 마크업은 web-nav.jsp / web-footer.jsp 에 있다.
--%>
<style>
  /* ── 헤더 내비게이션 ───────────────────────────────────────── */
  .site-nav { position: sticky; top: 0; z-index: 100; background: rgba(15, 23, 42, 0.92); backdrop-filter: blur(8px); border-bottom: 1px solid #1e293b; }
  .site-nav-inner { max-width: 1100px; margin: 0 auto; padding: 12px 20px; display: flex; align-items: center; gap: 16px; }
  .site-nav-brand { font-size: 20px; font-weight: 700; color: #ffffff; text-decoration: none; margin-right: auto; }
  .site-nav-links { display: flex; align-items: center; gap: 4px; list-style: none; margin: 0; padding: 0; }
  .site-nav-links a { display: block; padding: 8px 12px; border-radius: 8px; font-size: 14px; font-weight: 600; color: #cbd5f5; text-decoration: none; }
  .site-nav-links a:hover { background: #1e293b; color: #ffffff; }
  /* 현재 페이지 표시: 색과 밑줄 둘 다 준다. 색만으로는 구분이 어려운 이용자가 있다 */
  .site-nav-links a[aria-current="page"] { color: #ffffff; background: #1e293b; box-shadow: inset 0 -2px 0 #3b82f6; }
  .site-nav-cta { padding: 8px 16px; border-radius: 8px; background: #3b82f6; color: #ffffff; font-size: 14px; font-weight: 600; text-decoration: none; border: 0; cursor: pointer; font-family: inherit; }
  .site-nav-cta:hover { background: #2563eb; }
  /* 키보드 포커스 링: 마우스 사용자에게만 숨기고 키보드 탐색에는 반드시 보이게 한다 */
  .site-nav a:focus-visible, .site-nav button:focus-visible, .site-footer a:focus-visible { outline: 2px solid #60a5fa; outline-offset: 2px; }

  /* 햄버거 버튼은 모바일에서만 노출 */
  .site-nav-toggle { display: none; background: transparent; border: 1px solid #334155; border-radius: 8px; color: #e2e8f0; font-size: 13px; font-weight: 600; padding: 8px 12px; cursor: pointer; font-family: inherit; }
  .site-nav-toggle:hover { background: #1e293b; }
  /* 햄버거 아이콘: 가로 막대 3개를 CSS 로 그린다 (문자 아이콘은 인코딩에 취약하다) */
  .site-nav-bars { display: inline-block; width: 16px; height: 2px; background: currentColor; position: relative; }
  .site-nav-bars::before, .site-nav-bars::after { content: ""; position: absolute; left: 0; width: 16px; height: 2px; background: currentColor; }
  .site-nav-bars::before { top: -5px; }
  .site-nav-bars::after { top: 5px; }

  @media (max-width: 820px) {
    .site-nav-toggle { display: inline-flex; align-items: center; gap: 6px; }
    /* 모바일: 링크 목록을 세로 패널로 펼친다. 닫힘 상태는 hidden 속성으로 제어해
       키보드·스크린리더 탐색에서도 함께 사라지게 한다 (display:none 만으로는 부족) */
    .site-nav-links { display: none; position: absolute; left: 0; right: 0; top: 100%; flex-direction: column; align-items: stretch; gap: 0; padding: 8px; background: #0f172a; border-bottom: 1px solid #1e293b; max-height: calc(100vh - 60px); overflow-y: auto; }
    .site-nav-links.is-open { display: flex; }
    .site-nav-links a { padding: 12px; font-size: 15px; }
    .site-nav-links a[aria-current="page"] { box-shadow: inset 2px 0 0 #3b82f6; }
    /* PC 전용 항목은 모바일 패널 안에서 일반 링크로 합류시킨다 */
    .site-nav-cta { display: none; }
    .site-nav-links .site-nav-mobile-only { display: block; }
  }
  /* 모바일 패널에서만 보이는 항목(약관·정책 등)은 PC에서 숨긴다 */
  .site-nav-mobile-only { display: none; }
  .site-nav-inner { position: relative; }

  /* ── 푸터 ─────────────────────────────────────────────────── */
  .site-footer { margin-top: 60px; border-top: 1px solid #1e293b; background: #0b1220; }
  .site-footer-inner { max-width: 1100px; margin: 0 auto; padding: 40px 20px 24px; }
  .site-footer-cols { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 28px; }
  .site-footer-col h2 { font-size: 13px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; color: #e2e8f0; margin: 0 0 12px; }
  .site-footer-col ul { list-style: none; margin: 0; padding: 0; }
  .site-footer-col li { margin-bottom: 8px; }
  .site-footer-col a { font-size: 14px; color: #94a3b8; text-decoration: none; }
  .site-footer-col a:hover { color: #e2e8f0; text-decoration: underline; }
  .site-footer-bottom { margin-top: 32px; padding-top: 20px; border-top: 1px solid #1e293b; font-size: 13px; color: #64748b; line-height: 1.8; }
  .site-footer-bottom strong { color: #94a3b8; font-weight: 600; }
  .site-footer-bottom a { color: #94a3b8; text-decoration: none; }
  .site-footer-bottom a:hover { text-decoration: underline; }
</style>
