<%--
  공개 웹사이트 공통 헤더 내비게이션.

  [2-29차 후속] 클라이언트 PC 시안 그대로: 짙은 남색 바, 노란 별 로고, 가운데 메뉴(현재 항목 노란 밑줄),
  오른쪽 돋보기(검색). "Open In App" 버튼은 후속 요청으로 PC 에서 빼고 모바일 아이콘 아래에만 둔다.

  현재 페이지 표시는 컨트롤러가 넘기는 activeNav 값으로 한다 (home|about|posts|faq|contact).
  값이 없으면 어느 항목도 활성으로 표시하지 않는다 — 정책 페이지처럼 메뉴에 대응 항목이 없는 화면이 있다.

  검색: 돋보기를 누르면 입력칸이 펼쳐지고 GET /?q=검색어 로 홈 목록을 좁힌다 (PublicWebController.home).

  스타일은 web-chrome-style.jsp 에 있고 <head> 에서 include 해야 한다.

  include 조각에는 page 지시자가 없어 Jasper 가 기본 인코딩으로 읽는다. 출력 마크업에는
  ASCII 만 쓴다 — 아이콘은 인라인 SVG(ASCII)로, 한글은 JSP 주석 안에만.
  언어 선택 항목은 일부러 넣지 않았다. 번역된 페이지가 아직 없다.
--%>
<nav class="site-nav">
  <div class="site-nav-inner">
    <a class="site-nav-brand" href="${pageContext.request.contextPath}/">
      <svg class="site-nav-star" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="#facc15" d="M12 2.5l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.4l-6 3.4 1.3-6.7-5-4.6 6.8-.8z"/></svg>
      <span>StarPlatform</span>
    </a>

    <ul class="site-nav-links" id="siteNavLinks">
      <li><a href="${pageContext.request.contextPath}/" ${activeNav eq 'home' ? 'aria-current="page"' : ''}>Home</a></li>
      <li><a href="${pageContext.request.contextPath}/about" ${activeNav eq 'about' ? 'aria-current="page"' : ''}>About</a></li>
      <li><a href="${pageContext.request.contextPath}/posts" ${activeNav eq 'posts' ? 'aria-current="page"' : ''}>Posts</a></li>
      <li><a href="${pageContext.request.contextPath}/faq" ${activeNav eq 'faq' ? 'aria-current="page"' : ''}>FAQ</a></li>
      <li><a href="${pageContext.request.contextPath}/contact" ${activeNav eq 'contact' ? 'aria-current="page"' : ''}>Contact</a></li>
      <%-- 약관·정책은 PC에서는 푸터에만 두고, 메뉴를 펼쳐야 하는 모바일에서만 헤더에도 노출한다 --%>
      <li class="site-nav-mobile-only"><a href="${pageContext.request.contextPath}/terms">Terms of Service</a></li>
      <li class="site-nav-mobile-only"><a href="${pageContext.request.contextPath}/privacy">Privacy Policy</a></li>
    </ul>

    <div class="site-nav-tools">
      <button type="button" class="site-nav-toggle site-nav-toggle-mobile" id="siteNavToggle" aria-label="Menu"
              aria-expanded="false" aria-controls="siteNavLinks">
        <span class="site-nav-bars" aria-hidden="true"></span>
      </button>
      <form class="site-nav-search" id="siteNavSearch" action="${pageContext.request.contextPath}/" method="get" role="search">
        <input type="search" name="q" id="siteNavSearchInput" placeholder="Search posts" aria-label="Search posts" maxlength="60" value="<c:out value='${q}'/>">
      </form>
      <button type="button" class="site-nav-search-btn" id="siteNavSearchBtn" aria-label="Search" aria-expanded="false" aria-controls="siteNavSearch">
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="10.5" cy="10.5" r="6.5" fill="none" stroke="currentColor" stroke-width="2"/><path d="M15.5 15.5L21 21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      </button>
      <a class="site-nav-cta" href="#" onclick="spOpenApp(); return false;">
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="7" y="2.5" width="10" height="19" rx="2" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="18" r="1" fill="currentColor"/></svg>
        <span>Open in App</span>
      </a>
    </div>
  </div>
</nav>

<%--
  spOpenApp: 설치돼 있으면 앱으로, 아니면 현재 페이지에 그대로 남는다.
  스토어로 강제 이동시키지 않는다 (클라이언트 확정 사항).
  아래 스크립트 주석을 전부 JSP 주석으로 뺀 이유: include 조각은 기본 인코딩으로 읽혀
  출력에 실려 나가는 // 주석에 한글을 두면 응답에 깨진 바이트가 섞인다.
--%>
<script>
  <%-- 앱이 없으면 스토어로 보낸다 (후속 요청: 안드로이드에서 버튼이 안 먹힌다는 보고).
       안드로이드 intent:// 는 크롬 외 브라우저에서 조용히 실패하므로, 2.5초 뒤에도 화면이 그대로면 Play 로.
       iOS 는 커스텀 스킴 뒤 같은 방식으로 App Store 로 --%>
  function spOpenApp() {
    var ua = navigator.userAgent.toLowerCase();
    var isAndroid = ua.indexOf('android') > -1;
    var store = isAndroid
      ? 'https://play.google.com/store/apps/details?id=kr.co.sensiblenews.witchHuntingVU2D7F2P7E'
      : 'https://apps.apple.com/app/id1188195403';
    <%-- 크롬 계열이 아닌 안드로이드 브라우저(삼성 인터넷·인앱)는 intent:// 실패 시
         "이 동작을 수행할 수 있는 앱이 없습니다" 토스트를 띄우므로(클라이언트 보고) 스토어로 바로 보낸다 --%>
    var isChromeAndroid = isAndroid && /chrome\/\d+/.test(ua)
      && !/samsungbrowser|edga|opr\/|whale|kakaotalk|fban|fbav|instagram|naver|line\//.test(ua);
    if (isAndroid && !isChromeAndroid) {
      window.location.href = store;
      return;
    }
    var start = Date.now();
    setTimeout(function () {
      if (!document.hidden && Date.now() - start < 4000) window.location.href = store;
    }, 2500);
    if (isAndroid) {
      window.location.href = 'intent://home#Intent;scheme=witchhunting;package=kr.co.sensiblenews.witchHuntingVU2D7F2P7E;S.browser_fallback_url=' + encodeURIComponent(store) + ';end';
    } else {
      window.location.href = 'witchhunting://home';
    }
  }

  (function () {
    var toggle = document.getElementById('siteNavToggle');
    var links = document.getElementById('siteNavLinks');
    if (toggle && links) {
      <%-- aria-expanded 를 같이 갱신해야 스크린리더에서 열림·닫힘이 전달된다 --%>
      var setOpen = function (open) {
        links.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', String(open));
      };
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
    }

    <%-- 검색: 돋보기 1회 = 입력칸 펼치기, 입력칸이 열려 있고 값이 있으면 제출 --%>
    var sBtn = document.getElementById('siteNavSearchBtn');
    var sForm = document.getElementById('siteNavSearch');
    var sInput = document.getElementById('siteNavSearchInput');
    if (sBtn && sForm && sInput) {
      var searchOpen = sInput.value.length > 0;
      var setSearch = function (open) {
        searchOpen = open;
        sForm.classList.toggle('is-open', open);
        sBtn.setAttribute('aria-expanded', String(open));
        if (open) sInput.focus();
      };
      if (searchOpen) setSearch(true);
      sBtn.addEventListener('click', function () {
        if (searchOpen && sInput.value.trim().length > 0) {
          sForm.submit();
        } else {
          setSearch(!searchOpen);
        }
      });
      sInput.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') { sInput.value = ''; setSearch(false); sBtn.focus(); }
      });
    }
  })();
</script>
