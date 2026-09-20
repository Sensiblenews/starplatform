<%--
  공개 웹사이트 공통 푸터.

  원칙: 여기 있는 링크는 전부 실제로 200을 돌려주는 페이지여야 한다.
  자리만 채우는 링크를 두면 크롤러가 404를 수집한다. 그래서 다음 항목은
  일부러 뺐다.
   - 공식 SNS: 운영 중인 계정을 확인받지 못했다
   - 언어 선택: 번역된 페이지가 없다
   - 공식 이메일 직접 표기: 확정된 대외 주소를 받지 못해 /contact 로 보낸다

  사업자 정보(상호·대표자·주소)는 클라이언트에게 받는 즉시 아래
  "운영 주체" 블록에 채운다. 받기 전까지는 없는 정보를 지어내지 않는다.
--%>
<footer class="site-footer">
  <div class="site-footer-inner">
    <div class="site-footer-cols">
      <div class="site-footer-col">
        <h2>Service</h2>
        <ul>
          <li><a href="${pageContext.request.contextPath}/about">About StarPlatform</a></li>
          <li><a href="${pageContext.request.contextPath}/posts">Public Posts</a></li>
          <li><a href="${pageContext.request.contextPath}/faq">FAQ</a></li>
        </ul>
      </div>

      <div class="site-footer-col">
        <h2>Support</h2>
        <ul>
          <li><a href="${pageContext.request.contextPath}/contact">Contact Us</a></li>
          <li><a href="${pageContext.request.contextPath}/faq#safety">Reporting &amp; Blocking</a></li>
          <li><a href="${pageContext.request.contextPath}/contact?type=ACCOUNT">Account Help</a></li>
          <li><a href="${pageContext.request.contextPath}/contact?type=PARTNERSHIP">Partnership</a></li>
        </ul>
      </div>

      <div class="site-footer-col">
        <h2>Policies</h2>
        <ul>
          <li><a href="${pageContext.request.contextPath}/terms">Terms of Service</a></li>
          <li><a href="${pageContext.request.contextPath}/privacy">Privacy Policy</a></li>
          <li><a href="${pageContext.request.contextPath}/faq#advertising">Cookies &amp; Advertising</a></li>
          <li><a href="${pageContext.request.contextPath}/faq#content-policy">Content Policy</a></li>
        </ul>
      </div>

      <div class="site-footer-col">
        <h2>Get the App</h2>
        <ul>
          <li><a href="#" onclick="spOpenApp(); return false;">Open in App</a></li>
          <li><a href="https://play.google.com/store/apps/details?id=kr.co.sensiblenews.witchHuntingVU2D7F2P7E">Google Play</a></li>
          <li><a href="https://apps.apple.com/app/id1188195403">App Store</a></li>
        </ul>
      </div>
    </div>

    <div class="site-footer-bottom">
      <p><strong>StarPlatform</strong> &mdash; a global creator platform operated at witch-hunting.com.</p>
      <%-- 운영 주체: 클라이언트 데이터 수령 시 여기에 상호·대표자·주소를 추가한다 --%>
      <p>Questions, reports, and partnership enquiries: <a href="${pageContext.request.contextPath}/contact">Contact Us</a></p>
      <p>&copy; 2026 StarPlatform. All rights reserved. &middot; Last updated 2026-09-16</p>
    </div>
  </div>
</footer>
