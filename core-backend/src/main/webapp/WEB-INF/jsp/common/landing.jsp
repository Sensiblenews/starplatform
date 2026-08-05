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

  <meta property="og:title" content="StarPlatform" />
  <meta property="og:description" content="Create your page. Grow your audience. Earn globally." />
  <meta property="og:image" content="https://witch-hunting.com/resources/img/icon.png" />
  <meta property="og:type" content="website" />

  <link rel="canonical" href="${not empty canonicalUrl ? canonicalUrl : 'https://witch-hunting.com/'}" />

  <!-- JSON-LD: 사이트 대표 구조화 데이터 (정적 값만 사용) -->
  <script type="application/ld+json">
  {"@context":"https://schema.org","@type":"WebSite","name":"StarPlatform","url":"https://witch-hunting.com/"}
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
    .section { padding: 60px 20px; text-align: center; }
    .section h2 { font-size: 28px; margin-bottom: 20px; }
    .features { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-top: 40px; }
    .card { background: #1e293b; padding: 20px; border-radius: 12px; }

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

    <div class="buttons">
      <a onclick="openApp()" class="btn btn-primary">Open in App</a>
      <a href="https://play.google.com/store/apps/details?id=kr.co.sensiblenews.witchHuntingVU2D7F2P7E" target="_blank" rel="noopener noreferrer" class="btn btn-secondary">Get it on Google Play</a>
    </div>
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
</script>
</body>
</html>