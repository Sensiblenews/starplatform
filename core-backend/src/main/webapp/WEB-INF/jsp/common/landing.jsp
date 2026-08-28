<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  
  <title>StarPlatform - Follow Stars, Join the Community</title>
  <meta name="description" content="StarPlatform - Follow your favorite stars, read their latest posts, and earn through global automated advertising." />
  <meta name="robots" content="index, follow" />

  <%-- 애드센스 소유권 확인용 메타 태그 — 광고 코드가 아니므로 심사 중에도 유지 (구글 공식 확인 수단) --%>
  <meta name="google-adsense-account" content="ca-pub-9109251900558498" />

  <meta property="og:title" content="StarPlatform" />
  <meta property="og:description" content="Create your page. Grow your audience. Earn globally." />
  <meta property="og:image" content="https://witch-hunting.com/resources/img/icon.png" />
  <meta property="og:type" content="website" />

  <link rel="canonical" href="${not empty canonicalUrl ? canonicalUrl : 'https://witch-hunting.com/'}" />

  <!-- JSON-LD: 사이트 대표 구조화 데이터 (정적 값만 사용) -->
  <script type="application/ld+json">
  {"@context":"https://schema.org","@type":"WebSite","name":"StarPlatform","url":"https://witch-hunting.com/"}
  </script>

  <%-- [2-27차] FAQPage JSON-LD.
       주의: 아래 문항·답변은 본문 FAQ 섹션의 화면 텍스트와 "완전히 동일"해야 한다 (불일치는 구조화 데이터 스팸 판정 소지).
       FAQ 문구를 수정할 때는 반드시 이 블록과 본문 섹션을 함께 고칠 것. --%>
  <script type="application/ld+json">
  {"@context":"https://schema.org","@type":"FAQPage","mainEntity":[
    {"@type":"Question","name":"Can I use StarPlatform without the app?","acceptedAnswer":{"@type":"Answer","text":"Yes. Public star pages and posts on StarPlatform can be read in full right here on the web. The mobile app adds extra features such as real-time notifications, comments, and community participation."}},
    {"@type":"Question","name":"What is StarPlatform?","acceptedAnswer":{"@type":"Answer","text":"StarPlatform is a global creator platform where stars, creators, and brands run public pages, share posts, and grow an audience worldwide."}},
    {"@type":"Question","name":"How does the global ranking work?","acceptedAnswer":{"@type":"Answer","text":"Every star page competes in a single global ranking. The rank is calculated from real engagement such as page visits, likes, and followers, and is updated continuously."}},
    {"@type":"Question","name":"How do creators earn on StarPlatform?","acceptedAnswer":{"@type":"Answer","text":"Creators earn through automated advertising based on the real traffic and engagement their pages generate."}},
    {"@type":"Question","name":"Is StarPlatform free?","acceptedAnswer":{"@type":"Answer","text":"Yes. Creating a page, following stars, and reading posts are all free."}}
  ]}
  </script>

  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
    body { background: #0f172a; color: #ffffff; line-height: 1.6; }
    .container { max-width: 1100px; margin: 0 auto; padding: 20px; }
    header { display: flex; justify-content: space-between; align-items: center; padding: 20px 0; }
    .logo { font-size: 24px; font-weight: 700; cursor: pointer; }
    .hero { text-align: center; padding: 80px 20px; }
    .hero h1 { font-size: 42px; margin-bottom: 20px; }
    .hero p { font-size: 18px; color: #cbd5f5; margin-bottom: 15px; }
    .buttons { margin-top: 30px; }
    .btn { display: inline-block; padding: 12px 24px; margin: 10px; border-radius: 8px; text-decoration: none; font-weight: 600; cursor: pointer; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn-secondary { background: #1e293b; color: white; border: 1px solid #334155; }
    /* [2-27차] 다운로드는 보조 동선 — 대형 버튼 대신 낮은 시각 비중의 텍스트 링크 */
    .link-muted { display: inline-block; margin-top: 20px; font-size: 14px; color: #94a3b8; text-decoration: underline; cursor: pointer; }
    .link-muted:hover { color: #cbd5f5; }
    .section { padding: 60px 20px; text-align: center; }
    .section h2 { font-size: 28px; margin-bottom: 20px; }
    .features { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-top: 40px; }
    .card { background: #1e293b; padding: 20px; border-radius: 12px; }

    /* [2-27차] How StarPlatform Works: 서비스 이용 흐름 5단계 */
    .steps { list-style: none; display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 16px; margin-top: 40px; text-align: left; }
    .step-card { background: #1e293b; border-radius: 12px; padding: 20px; }
    .step-num { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 50%; background: #3b82f6; color: #fff; font-size: 14px; font-weight: 700; margin-bottom: 12px; }
    .step-card h3 { font-size: 16px; margin-bottom: 6px; }
    .step-card p { font-size: 14px; color: #94a3b8; }

    /* [2-27차] 메인 FAQ */
    .faq-list { margin-top: 40px; text-align: left; max-width: 760px; margin-left: auto; margin-right: auto; }
    .faq-item { background: #1e293b; border-radius: 12px; padding: 18px 20px; margin-bottom: 12px; }
    .faq-item h3 { font-size: 16px; margin-bottom: 6px; }
    .faq-item p { font-size: 14px; color: #94a3b8; }

    /* 최근 포스트·인기 스타 카드: 크롤러의 콘텐츠 발견 경로이자 실제 방문자용 목차 */
    .post-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 16px; margin-top: 30px; text-align: left; }
    .post-card { display: flex; gap: 12px; align-items: center; background: #1e293b; border-radius: 12px; padding: 14px; text-decoration: none; color: inherit; }
    .post-card:hover { background: #263449; }
    .post-thumb { width: 56px; height: 56px; border-radius: 8px; object-fit: cover; flex-shrink: 0; background: #0f172a; }
    .post-info { min-width: 0; }
    .post-author { font-size: 14px; font-weight: 600; color: #e2e8f0; margin-bottom: 2px; }
    .post-snippet { font-size: 13px; color: #94a3b8; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; word-break: break-word; }
    .star-grid { display: flex; flex-wrap: wrap; justify-content: center; gap: 14px; margin-top: 30px; }
    .star-card { display: flex; flex-direction: column; align-items: center; gap: 6px; width: 92px; text-decoration: none; color: inherit; }
    .star-avatar { width: 64px; height: 64px; border-radius: 50%; object-fit: cover; background: #1e293b; }
    .star-name { font-size: 13px; font-weight: 600; color: #e2e8f0; max-width: 92px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .star-followers { font-size: 11px; color: #94a3b8; }
    /* 공식 스토어 뱃지: 구글 뱃지는 원본 PNG에 자체 여백이 있어 애플보다 크게 잡아 시각 크기를 맞춘다 */
    .store-section { padding: 30px 20px 0; text-align: center; }
    .store-caption { font-size: 13px; letter-spacing: 2px; text-transform: uppercase; color: #94a3b8; margin-bottom: 10px; }
    .store-badges { display: flex; justify-content: center; align-items: center; flex-wrap: wrap; }
    .store-badges img { display: block; }
    .badge-google { height: 78px; }
    .badge-apple { height: 54px; margin: 0 12px; }
    footer { margin-top: 60px; padding: 30px 0; border-top: 1px solid #334155; text-align: center; font-size: 14px; color: #94a3b8; }
    footer a { color: #94a3b8; margin: 0 10px; text-decoration: none; }
    footer a:hover { text-decoration: underline; }
    @media (max-width: 600px) { .hero h1 { font-size: 30px; } .hero p { font-size: 16px; } }
  </style>
</head>
<body>
<div class="container">
  <header>
    <div class="logo" onclick="window.location.href='/'">StarPlatform</div>
  </header>
  
  <section class="hero">
    <h1>Create. Grow. Earn.</h1>
    <p>StarPlatform is a global creator platform where anyone can launch a page, attract an audience, and generate revenue through automated advertising.</p>
    <p>Compete in global rankings based on real engagement such as visits, likes, and bookmarks.</p>
    <p>Turn your traffic into real opportunity — and become part of a new digital economy.</p>

    <!-- [2-27차] Hero의 Open in App 대형 버튼을 FAQ 아래 CTA 섹션으로 이동 (웹 콘텐츠를 먼저 읽게 하는 구조).
         다운로드는 소형 텍스트 링크로 다운그레이드 — OS별 스토어 분기(goStore)는 유지 -->
    <a onclick="goStore()" class="link-muted">Download the StarPlatform App</a>
  </section>

  <!-- 최근 포스트: 크롤러가 /post/*(광고·본문 랜딩)를 발견하는 내부 링크 -->
  <c:if test="${not empty recentPosts}">
  <section class="section">
    <h2>Latest Posts</h2>
    <div class="post-grid">
      <c:forEach var="p" items="${recentPosts}">
        <a class="post-card" href="${pageContext.request.contextPath}/post/${p.conId}">
          <c:if test="${not empty p.image}">
            <img class="post-thumb" src="${p.image}" alt="" loading="lazy" onerror="this.style.display='none'">
          </c:if>
          <span class="post-info">
            <span class="post-author">${p.author}</span>
            <span class="post-snippet">${p.snippet}</span>
          </span>
        </a>
      </c:forEach>
    </div>
  </section>
  </c:if>

  <!-- 인기 스타: /star/* 내부 링크 -->
  <c:if test="${not empty topStars}">
  <section class="section">
    <h2>Popular Stars</h2>
    <div class="star-grid">
      <c:forEach var="s" items="${topStars}">
        <a class="star-card" href="${pageContext.request.contextPath}/star/${s.id}">
          <img class="star-avatar" src="${s.image}" alt="${s.name}" loading="lazy" onerror="this.style.visibility='hidden'">
          <span class="star-name">${s.name}</span>
          <span class="star-followers">${s.followerCnt} followers</span>
        </a>
      </c:forEach>
    </div>
  </section>
  </c:if>

  <section class="section">
    <h2>Why StarPlatform?</h2>
    <div class="features">
      <div class="card">
        <h3>🌍 Global Reach</h3>
        <p>Connect with users worldwide and grow without limits.</p>
      </div>
      <div class="card">
        <h3>💰 Monetization</h3>
        <p>Earn through automated advertising based on real engagement.</p>
      </div>
      <div class="card">
        <h3>🏆 Ranking System</h3>
        <p>Compete globally with transparent ranking metrics.</p>
      </div>
    </div>
  </section>

  <!-- [2-27차] How StarPlatform Works: 서비스 이용 흐름을 텍스트로 설명 (웹 자체 완결성 보강) -->
  <section class="section">
    <h2>How StarPlatform Works</h2>
    <ol class="steps">
      <li class="step-card">
        <span class="step-num">1</span>
        <h3>Create a Page</h3>
        <p>Launch your own public star page in minutes and introduce yourself to the world.</p>
      </li>
      <li class="step-card">
        <span class="step-num">2</span>
        <h3>Publish Content</h3>
        <p>Share posts, photos, and updates that your audience can read on the web or in the app.</p>
      </li>
      <li class="step-card">
        <span class="step-num">3</span>
        <h3>Build an Audience</h3>
        <p>Attract followers and visitors from around the world as your page gets discovered.</p>
      </li>
      <li class="step-card">
        <span class="step-num">4</span>
        <h3>Climb the Global Ranking</h3>
        <p>Real engagement such as visits, likes, and bookmarks moves your page up the global ranking.</p>
      </li>
      <li class="step-card">
        <span class="step-num">5</span>
        <h3>Grow and Earn</h3>
        <p>Turn your audience into revenue through automated advertising on your pages.</p>
      </li>
    </ol>
  </section>

  <!-- [2-27차] 메인 FAQ.
       주의: 문항·답변은 head의 FAQPage JSON-LD와 "완전히 동일"해야 한다 — 수정 시 두 곳을 함께 고칠 것 -->
  <section class="section">
    <h2>FAQ</h2>
    <div class="faq-list">
      <div class="faq-item">
        <h3>Can I use StarPlatform without the app?</h3>
        <p>Yes. Public star pages and posts on StarPlatform can be read in full right here on the web.
          The mobile app adds extra features such as real-time notifications, comments, and community participation.</p>
      </div>
      <div class="faq-item">
        <h3>What is StarPlatform?</h3>
        <p>StarPlatform is a global creator platform where stars, creators, and brands run public pages,
          share posts, and grow an audience worldwide.</p>
      </div>
      <div class="faq-item">
        <h3>How does the global ranking work?</h3>
        <p>Every star page competes in a single global ranking. The rank is calculated from real engagement
          such as page visits, likes, and followers, and is updated continuously.</p>
      </div>
      <div class="faq-item">
        <h3>How do creators earn on StarPlatform?</h3>
        <p>Creators earn through automated advertising based on the real traffic and engagement their pages generate.</p>
      </div>
      <div class="faq-item">
        <h3>Is StarPlatform free?</h3>
        <p>Yes. Creating a page, following stars, and reading posts are all free.</p>
      </div>
    </div>
  </section>

  <!-- [2-27차] 앱 CTA: 콘텐츠·FAQ를 모두 지난 최하단 배치 (Hero에서 이동) -->
  <section class="section">
    <h2>Already have the app?</h2>
    <p>Open StarPlatform to follow stars, join conversations, and get real-time updates.</p>
    <div class="buttons">
      <a onclick="openApp()" class="btn btn-primary">Open in StarPlatform App</a>
    </div>
  </section>

  <!-- 공식 스토어 뱃지: 구글·애플 브랜드 가이드에 따라 공식 아트워크(공식 URL)를 그대로 사용한다 -->
  <section class="store-section">
    <p class="store-caption">Don't have the app?</p>
    <div class="store-badges">
      <a href="https://play.google.com/store/apps/details?id=kr.co.sensiblenews.witchHuntingVU2D7F2P7E" target="_blank" rel="noopener noreferrer">
        <img class="badge-google" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" loading="lazy">
      </a>
      <a href="https://apps.apple.com/app/id1188195403" target="_blank" rel="noopener noreferrer">
        <img class="badge-apple" src="https://tools.applemediaservices.com/api/badges/download-on-the-app-store/black/en-us" alt="Download on the App Store" loading="lazy">
      </a>
    </div>
  </section>

  <footer>
    <p>© 2026 StarPlatform. All rights reserved.</p>
    <div>
      <a href="${pageContext.request.contextPath}/privacy">Privacy Policy</a> |
      <a href="${pageContext.request.contextPath}/terms">Terms of Service</a>
    </div>
  </footer>
</div>

<script>
  // 루트 허브는 자동 앱 실행·스토어 강제 이동을 하지 않는다 (클라이언트 확정).
  // 앱 전환은 사용자가 Open in App 버튼을 눌렀을 때만 시도하고,
  // 미설치라면 스토어로 보내지 않고 허브에 남긴다 (스토어는 Google Play 버튼으로 직접 선택).
  function openApp() {
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("android") > -1) {
      // intent://: 앱이 있으면 실행, 없으면 fallback URL(현재 허브)로 복귀
      var fallback = encodeURIComponent(window.location.href);
      window.location.href = "intent://home#Intent;scheme=witchhunting;package=kr.co.sensiblenews.witchHuntingVU2D7F2P7E;S.browser_fallback_url=" + fallback + ";end";
    } else {
      // iOS: 커스텀 스킴 1회 시도, 실패해도 추가 이동 없음
      window.location.href = "witchhunting://home";
    }
  }

  // Download App 버튼: OS에 맞는 스토어로 이동. 데스크톱 등 판별 불가 환경은 하단 뱃지 섹션으로 스크롤
  function goStore() {
    var ua = navigator.userAgent.toLowerCase();
    var isIOS = /iphone|ipad|ipod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    if (isIOS) {
      window.location.href = "https://apps.apple.com/app/id1188195403";
    } else if (ua.indexOf("android") > -1) {
      window.location.href = "https://play.google.com/store/apps/details?id=kr.co.sensiblenews.witchHuntingVU2D7F2P7E";
    } else {
      var badges = document.querySelector(".store-badges");
      if (badges) badges.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }
</script>
</body>
</html>