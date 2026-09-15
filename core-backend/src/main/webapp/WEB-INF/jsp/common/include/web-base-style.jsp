<%--
  공개 웹 페이지(about·faq·contact·posts·404) 공통 본문 스타일.

  landing.jsp·content_landing.jsp 는 이 파일을 쓰지 않는다 — 그쪽은 이미 자체
  <style> 에 같은 역할의 규칙을 갖고 있고, 겹쳐 넣으면 기존 화면이 흔들린다.
  공통 크롬(헤더·푸터)만 web-chrome-style.jsp 로 공유한다.
--%>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
  body { background: #0f172a; color: #e2e8f0; line-height: 1.7; }
  img { max-width: 100%; }

  .page-wrap { max-width: 860px; margin: 0 auto; padding: 40px 20px 20px; }
  /* 목록형 페이지는 카드가 여러 열로 서므로 본문보다 넓게 쓴다 */
  .page-wrap-wide { max-width: 1100px; }

  .page-title { font-size: 34px; font-weight: 700; color: #ffffff; line-height: 1.3; margin-bottom: 14px; }
  .page-lead { font-size: 17px; color: #cbd5f5; margin-bottom: 8px; }

  .prose h2 { font-size: 24px; color: #ffffff; margin: 44px 0 14px; }
  .prose h3 { font-size: 17px; color: #e2e8f0; margin: 26px 0 8px; }
  .prose p { font-size: 15px; color: #94a3b8; margin-bottom: 14px; }
  .prose ul { margin: 0 0 16px 20px; }
  .prose li { font-size: 15px; color: #94a3b8; margin-bottom: 8px; }
  .prose strong { color: #e2e8f0; }
  .prose a { color: #60a5fa; }

  .panel { background: #1e293b; border-radius: 12px; padding: 20px 22px; margin-bottom: 14px; }
  .panel h3 { margin-top: 0; }
  .panel p:last-child { margin-bottom: 0; }

  .btn { display: inline-block; padding: 12px 22px; border-radius: 8px; font-size: 15px; font-weight: 600; text-decoration: none; cursor: pointer; border: 0; font-family: inherit; }
  .btn-primary { background: #3b82f6; color: #ffffff; }
  .btn-primary:hover { background: #2563eb; }
  .btn-secondary { background: #1e293b; color: #e2e8f0; border: 1px solid #334155; }
  .btn-secondary:hover { background: #263449; }

  /* FAQ 목차: 항목이 많아 위에서 건너뛸 수 있게 한다 */
  .toc { display: flex; flex-wrap: wrap; gap: 8px; margin: 24px 0 8px; }
  .toc a { font-size: 13px; font-weight: 600; color: #cbd5f5; background: #1e293b; border-radius: 999px; padding: 6px 14px; text-decoration: none; }
  .toc a:hover { background: #263449; color: #ffffff; }
  /* 목차로 점프했을 때 sticky 헤더가 제목을 가리지 않도록 여유를 준다 */
  .prose h2[id] { scroll-margin-top: 80px; }

  a:focus-visible, button:focus-visible, input:focus-visible, select:focus-visible, textarea:focus-visible { outline: 2px solid #60a5fa; outline-offset: 2px; }

  @media (max-width: 600px) {
    .page-title { font-size: 26px; }
    .prose h2 { font-size: 20px; }
  }
</style>
