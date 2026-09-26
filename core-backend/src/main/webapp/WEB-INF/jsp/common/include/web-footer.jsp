<%--
  공개 웹사이트 공통 푸터.

  [2-29차 후속] 클라이언트 PC 시안 그대로: 짙은 남색 한 줄 — 왼쪽 로고, 가운데 메뉴 5개,
  오른쪽 SNS 아이콘 5개, 그 아래 저작권 표기.
  약관·개인정보 링크는 시안에 없지만 법적 고지 경로라 저작권 줄 옆에 작게 유지한다 (AdSense 심사 요건).

  SNS 링크 주소는 확정된 공식 계정을 아직 받지 못했다. 받는 즉시 아래 data-social 항목의 href 를 채운다.
  받기 전에는 '#' 로 두고 클릭 시 아무 동작도 하지 않는다 — 지어낸 계정으로 보내지 않는다.

  include 조각은 기본 인코딩으로 읽히므로 출력 마크업은 ASCII 만 (아이콘은 인라인 SVG, 기호는 엔티티).
--%>
<footer class="site-footer">
  <div class="site-footer-inner">
    <div class="site-footer-row">
      <a class="site-footer-brand" href="${pageContext.request.contextPath}/">
        <svg class="site-nav-star" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="#facc15" d="M12 2.5l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.4l-6 3.4 1.3-6.7-5-4.6 6.8-.8z"/></svg>
        <span>StarPlatform</span>
      </a>

      <ul class="site-footer-links">
        <li><a href="${pageContext.request.contextPath}/">Home</a></li>
        <li><a href="${pageContext.request.contextPath}/about">About</a></li>
        <li><a href="${pageContext.request.contextPath}/posts">Posts</a></li>
        <li><a href="${pageContext.request.contextPath}/faq">FAQ</a></li>
        <li><a href="${pageContext.request.contextPath}/contact">Contact</a></li>
      </ul>

      <ul class="site-footer-social" aria-label="Social media">
        <li><a href="#" data-social="facebook" aria-label="Facebook" onclick="return false;"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M13.5 22v-8h2.7l.4-3.2h-3.1V8.8c0-.9.3-1.6 1.6-1.6h1.7V4.4c-.3 0-1.3-.1-2.5-.1-2.5 0-4.1 1.5-4.1 4.2v2.3H7.4V14h2.8v8h3.3z"/></svg></a></li>
        <li><a href="#" data-social="x" aria-label="X" onclick="return false;"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M17.8 3h3l-6.7 7.7L22 21h-6.2l-4.8-6.3L5.4 21h-3l7.2-8.2L2 3h6.3l4.4 5.8L17.8 3zm-1.1 16.2h1.7L7.4 4.7H5.6l11.1 14.5z"/></svg></a></li>
        <li><a href="#" data-social="instagram" aria-label="Instagram" onclick="return false;"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="5" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="4" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="17.3" cy="6.7" r="1.2" fill="currentColor"/></svg></a></li>
        <li><a href="#" data-social="youtube" aria-label="YouTube" onclick="return false;"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M22.5 7.2c-.3-1-1-1.8-2-2C18.8 4.8 12 4.8 12 4.8s-6.8 0-8.5.4c-1 .2-1.7 1-2 2C1 8.9 1 12 1 12s0 3.1.5 4.8c.3 1 1 1.8 2 2 1.7.4 8.5.4 8.5.4s6.8 0 8.5-.4c1-.2 1.7-1 2-2 .5-1.7.5-4.8.5-4.8s0-3.1-.5-4.8zM9.8 15.1V8.9l5.7 3.1-5.7 3.1z"/></svg></a></li>
        <li><a href="#" data-social="tiktok" aria-label="TikTok" onclick="return false;"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M16.6 2h-3.3v13.4a2.9 2.9 0 1 1-2.9-2.9c.3 0 .6 0 .9.1V9.3a6.2 6.2 0 1 0 5.3 6.1V8.7c1.2.9 2.7 1.4 4.3 1.4V6.8c-2.4 0-4.3-2.1-4.3-4.8z"/></svg></a></li>
      </ul>
    </div>

    <div class="site-footer-bottom">
      <p>&copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> StarPlatform. All rights reserved.
        <span class="site-footer-legal"><a href="${pageContext.request.contextPath}/terms">Terms of Service</a> &middot; <a href="${pageContext.request.contextPath}/privacy">Privacy Policy</a></span></p>
    </div>
  </div>
</footer>
