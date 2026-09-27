<%--
  모바일 하단 탭바 (820px 이하에서만 보인다. 스타일은 web-chrome-style.jsp).

  [2-29차 후속] 클라이언트 모바일 시안 반영. 헤더 메뉴(web-nav.jsp)와 같은 다섯 항목을
  같은 순서로 두고, 현재 페이지 표시도 같은 activeNav 값(home|about|posts|faq|contact)을 본다.

  include 조각에는 page 지시자가 없어 Jasper 가 기본 인코딩으로 읽는다.
  출력 마크업에는 ASCII 만 쓰고, 아이콘은 CSS 로 그린다.
--%>
<nav class="site-tabbar" aria-label="Site sections">
  <a href="${pageContext.request.contextPath}/" ${activeNav eq 'home' ? 'aria-current="page"' : ''}><span class="site-tabbar-icon site-tabbar-icon-home" aria-hidden="true"></span>Home</a>
  <a href="${pageContext.request.contextPath}/about" ${activeNav eq 'about' ? 'aria-current="page"' : ''}><span class="site-tabbar-icon site-tabbar-icon-about" aria-hidden="true"></span>About</a>
  <a href="${pageContext.request.contextPath}/posts" ${activeNav eq 'posts' ? 'aria-current="page"' : ''}><span class="site-tabbar-icon site-tabbar-icon-posts" aria-hidden="true"></span>Posts</a>
  <a href="${pageContext.request.contextPath}/faq" ${activeNav eq 'faq' ? 'aria-current="page"' : ''}><span class="site-tabbar-icon site-tabbar-icon-faq" aria-hidden="true"></span>FAQ</a>
  <a href="${pageContext.request.contextPath}/contact" ${activeNav eq 'contact' ? 'aria-current="page"' : ''}><span class="site-tabbar-icon site-tabbar-icon-contact" aria-hidden="true"></span>Contact</a>
</nav>
