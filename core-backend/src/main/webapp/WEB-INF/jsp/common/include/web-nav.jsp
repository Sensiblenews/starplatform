<%--
  공개 웹사이트 공통 헤더 내비게이션.

  현재 페이지 표시는 컨트롤러가 넘기는 activeNav 값으로 한다 (home|about|posts|faq|contact).
  값이 없으면 어느 항목도 활성으로 표시하지 않는다 — 랜딩·정책 페이지처럼
  메뉴에 대응 항목이 없는 화면이 있다.

  스타일은 web-chrome-style.jsp 에 있고 <head> 에서 include 해야 한다.

  언어 선택 항목은 일부러 넣지 않았다. 번역된 페이지가 아직 없어서
  링크를 걸면 전부 같은 영문 페이지로 가는 빈 링크가 된다.
--%>
<nav class="site-nav">
  <div class="site-nav-inner">
    <a class="site-nav-brand" href="${pageContext.request.contextPath}/">StarPlatform</a>

    <button type="button" class="site-nav-toggle" id="siteNavToggle"
            aria-expanded="false" aria-controls="siteNavLinks">
      <%-- 아이콘은 CSS 로 그린다. include 조각에는 page 지시자가 없어 Jasper 가 기본
           인코딩으로 읽으므로, 출력 마크업에 비ASCII 문자를 두면 깨진다 --%>
      <span class="site-nav-bars" aria-hidden="true"></span> Menu
    </button>

    <ul class="site-nav-links" id="siteNavLinks">
      <li><a href="${pageContext.request.contextPath}/" ${activeNav eq 'home' ? 'aria-current="page"' : ''}>Home</a></li>
      <li><a href="${pageContext.request.contextPath}/about" ${activeNav eq 'about' ? 'aria-current="page"' : ''}>About</a></li>
      <li><a href="${pageContext.request.contextPath}/posts" ${activeNav eq 'posts' ? 'aria-current="page"' : ''}>Posts</a></li>
      <li><a href="${pageContext.request.contextPath}/faq" ${activeNav eq 'faq' ? 'aria-current="page"' : ''}>FAQ</a></li>
      <li><a href="${pageContext.request.contextPath}/contact" ${activeNav eq 'contact' ? 'aria-current="page"' : ''}>Contact</a></li>
      <%-- 약관·정책은 PC에서는 푸터에만 두고, 메뉴를 펼쳐야 하는 모바일에서만 헤더에도 노출한다 --%>
      <li class="site-nav-mobile-only"><a href="${pageContext.request.contextPath}/terms">Terms of Service</a></li>
      <li class="site-nav-mobile-only"><a href="${pageContext.request.contextPath}/privacy">Privacy Policy</a></li>
      <li class="site-nav-mobile-only"><a href="#" onclick="spOpenApp(); return false;">Open in App</a></li>
    </ul>

    <a class="site-nav-cta" href="#" onclick="spOpenApp(); return false;">Open in App</a>
  </div>
</nav>

<%--
  spOpenApp: 설치돼 있으면 앱으로, 아니면 현재 페이지에 그대로 남는다.
  스토어로 강제 이동시키지 않는다 (클라이언트 확정 사항).
  landing.jsp 의 openApp() 과 같은 동작이지만, 이 include 를 쓰는 모든 페이지가
  각자 복사본을 들지 않도록 여기에 둔다. 이름이 겹치지 않게 sp 접두사를 붙였다.

  아래 스크립트 주석을 전부 JSP 주석으로 뺀 이유: include 조각에는 page 지시자가 없어
  Jasper 가 기본 인코딩으로 읽는다. 출력에 실려 나가는 // 주석에 한글을 두면
  응답에 깨진 바이트가 섞인다.
--%>
<script>
  function spOpenApp() {
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf('android') > -1) {
      var fallback = encodeURIComponent(window.location.href);
      window.location.href = 'intent://home#Intent;scheme=witchhunting;package=kr.co.sensiblenews.witchHuntingVU2D7F2P7E;S.browser_fallback_url=' + fallback + ';end';
    } else {
      window.location.href = 'witchhunting://home';
    }
  }

  (function () {
    var toggle = document.getElementById('siteNavToggle');
    var links = document.getElementById('siteNavLinks');
    if (!toggle || !links) return;

    <%-- aria-expanded 를 같이 갱신해야 스크린리더에서 열림·닫힘이 전달된다 --%>
    function setOpen(open) {
      links.classList.toggle('is-open', open);
      toggle.setAttribute('aria-expanded', String(open));
    }

    toggle.addEventListener('click', function () {
      setOpen(toggle.getAttribute('aria-expanded') !== 'true');
    });

    <%-- Esc 로 닫고 포커스를 버튼으로 되돌린다 (키보드만 쓰는 이용자가 갇히지 않게) --%>
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
        setOpen(false);
        toggle.focus();
      }
    });

    <%-- 메뉴를 연 채 창을 넓히면 패널이 PC 레이아웃에 겹친 상태로 남는다 --%>
    window.addEventListener('resize', function () {
      if (window.innerWidth > 820) setOpen(false);
    });
  })();
</script>
