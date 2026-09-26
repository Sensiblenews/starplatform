<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  홈 (/): 공개 포스트 목록.

  [2-29차 후속] 예전 /posts 목록 화면을 홈으로 옮기고 클라이언트 시안대로 다시 짰다.
   - 상단 히어로 배너 ("Your Page. Your World.") — 원본 지구 사진을 받지 못해 CSS 그라디언트로 그린다.
     외부 이미지를 핫링크하지 않는다.
   - 모바일 전용 기능 칩 4개 (가로 스크롤)
   - Latest Posts 그리드 (카드 마크업·스타일은 include 로 공유) + PC 우측 사이드바 프로모
   - 하단 안내 카드 3개
   - "Public posts" 제목·설명문·건수 표기는 시안에 따라 뺐다
  광고 슬롯은 넣지 않는다 — AdSense 승인 대기 중이라 시안의 광고 자리는 빈 공간으로 둔다.
  페이지네이션 URL 은 /?page=N 이다 (PublicWebController.pageUrl).
--%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title><c:choose><c:when test="${page gt 1}">Latest Posts - Page ${page} | StarPlatform</c:when><c:otherwise>StarPlatform - Your Page. Your World.</c:otherwise></c:choose></title>
  <meta name="description" content="StarPlatform is a global creator platform. Read the latest public posts from star pages around the world, no account and no app required." />
  <meta name="robots" content="index, follow" />
  <%-- 애드센스 소유권 확인용 메타 태그 — 광고 코드가 아니므로 심사 중에도 유지 (구글 공식 확인 수단) --%>
  <meta name="google-adsense-account" content="ca-pub-9109251900558498" />

  <meta property="og:title" content="StarPlatform - Your Page. Your World." />
  <meta property="og:description" content="Connect, share and grow across the world. The latest public posts from star pages on StarPlatform." />
  <meta property="og:image" content="https://witch-hunting.com/resources/img/icon.png" />
  <meta property="og:type" content="website" />
  <meta property="og:url" content="${canonicalUrl}" />
  <meta name="twitter:card" content="summary" />

  <link rel="canonical" href="${canonicalUrl}" />
  <%-- 페이지 관계를 명시해 2페이지 이후가 고아 페이지로 남지 않게 한다 --%>
  <c:if test="${not empty prevUrl}"><link rel="prev" href="${prevUrl}" /></c:if>
  <c:if test="${not empty nextUrl}"><link rel="next" href="${nextUrl}" /></c:if>

  <!-- JSON-LD: 사이트 대표 구조화 데이터 (정적 값만 사용) -->
  <script type="application/ld+json">
  {"@context":"https://schema.org","@type":"WebSite","name":"StarPlatform","url":"https://witch-hunting.com/"}
  </script>

  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
  <%@ include file="/WEB-INF/jsp/common/include/web-base-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-chrome-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-card-style.jsp"%>
  <style>
    /* ── 히어로 배너: 짙은 남색 그라디언트 + CSS 로 그린 행성 광채 ── */
    .home-hero { position: relative; overflow: hidden; color: #ffffff; background: radial-gradient(circle at 85% 30%, rgba(96, 165, 250, 0.35) 0, transparent 40%), linear-gradient(120deg, #0b1a3a 0%, #102a5c 55%, #143a7c 100%); }
    .home-hero::before { content: ""; position: absolute; right: -140px; bottom: -300px; width: 560px; height: 560px; border-radius: 50%; background: radial-gradient(circle at 35% 30%, #60a5fa 0%, #2563eb 35%, #1e3a8a 65%, #0b1a3a 90%); box-shadow: 0 0 90px 30px rgba(59, 130, 246, 0.45); opacity: 0.95; pointer-events: none; }
    .home-hero::after { content: ""; position: absolute; left: -80px; top: -120px; width: 320px; height: 320px; border-radius: 50%; background: radial-gradient(circle, rgba(59, 130, 246, 0.35) 0, transparent 70%); pointer-events: none; }
    .home-hero-inner { position: relative; z-index: 1; max-width: 1100px; margin: 0 auto; padding: 48px 20px; min-height: 260px; display: flex; flex-direction: column; justify-content: center; align-items: flex-start; }
    .home-kicker { font-size: 12px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; color: #93c5fd; margin-bottom: 12px; }
    .home-hero h1 { font-size: 40px; font-weight: 700; line-height: 1.15; color: #ffffff; margin-bottom: 14px; }
    .home-hero h1 .hl { color: #fbbf24; }
    .home-hero-sub { font-size: 17px; color: #dbeafe; max-width: 560px; margin-bottom: 24px; }
    .btn-hero { background: #2563eb; color: #ffffff; box-shadow: 0 8px 20px rgba(37, 99, 235, 0.35); }
    .btn-hero:hover { background: #1d4ed8; }

    /* ── 모바일 기능 칩: 가로 스크롤 한 줄 ── */
    .home-chips { display: none; }
    .home-chip { flex: 0 0 auto; display: flex; align-items: center; gap: 10px; min-width: 230px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 10px 14px; }
    .home-chip-dot { position: relative; width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0; }
    .home-chip-dot::after { content: ""; position: absolute; left: 10px; top: 10px; width: 12px; height: 12px; border-radius: 50%; background: #ffffff; }
    .home-chip-dot-1 { background: #2563eb; }
    .home-chip-dot-2 { background: #16a34a; }
    .home-chip-dot-3 { background: #f59e0b; }
    .home-chip-dot-4 { background: #7c3aed; }
    .home-chip strong { display: block; font-size: 13px; color: #0f172a; line-height: 1.3; }
    .home-chip span { display: block; font-size: 11px; color: #64748b; line-height: 1.3; }

    /* ── 본문: 포스트 그리드 + PC 사이드바 ── */
    .home-body { padding-top: 28px; }
    .home-section-head { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .home-section-head h2 { font-size: 22px; font-weight: 700; color: #0f172a; }
    .home-live { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; color: #16a34a; }
    .home-live::before { content: ""; width: 8px; height: 8px; border-radius: 50%; background: #22c55e; }
    .home-main .post-grid { margin-top: 16px; }
    .home-side { display: none; }
    .home-promo { position: sticky; top: 76px; background: linear-gradient(160deg, #0b1a3a 0%, #143a7c 100%); color: #ffffff; border-radius: 14px; padding: 26px 22px; }
    .home-promo-brand { font-size: 18px; font-weight: 700; margin-bottom: 14px; }
    .home-promo-kicker { font-size: 11px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; color: #93c5fd; margin-bottom: 6px; }
    .home-promo-title { font-size: 22px; font-weight: 700; line-height: 1.25; margin-bottom: 18px; }
    .home-promo .btn { display: block; text-align: center; }

    .pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 36px; }
    .pager span { font-size: 14px; color: #64748b; }

    /* ── 하단 안내 카드 3개 ── */
    .home-info { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-top: 44px; }
    .home-info-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 22px; }
    .home-info-card h3 { font-size: 17px; font-weight: 700; color: #0f172a; margin-bottom: 8px; }
    .home-info-card p { font-size: 14px; color: #475569; margin-bottom: 14px; }
    .home-info-card a { font-size: 14px; font-weight: 600; color: #2563eb; text-decoration: none; }
    .home-info-card a:hover { text-decoration: underline; }

    @media (min-width: 1000px) {
      .home-body { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 28px; align-items: start; }
      .home-main .post-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
      .home-side { display: block; }
    }
    @media (max-width: 820px) {
      .home-hero-inner { padding: 40px 20px; min-height: 0; }
      .home-hero h1 { font-size: 30px; }
      .home-hero-sub { font-size: 15px; }
      .home-hero::before { width: 380px; height: 380px; right: -160px; bottom: -240px; }
      .home-chips { display: flex; gap: 10px; overflow-x: auto; padding: 14px 20px 0; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
      .home-chips::-webkit-scrollbar { display: none; }
      .home-body { padding-top: 20px; }
      .home-info { grid-template-columns: 1fr; margin-top: 32px; }
    }
  </style>
</head>
<body>

<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<main>
  <section class="home-hero">
    <div class="home-hero-inner">
      <p class="home-kicker">One Global Network</p>
      <h1>Your Page. <span class="hl">Your World.</span></h1>
      <p class="home-hero-sub">Connect, share and grow across the world. Your digital home, your global stage.</p>
      <a class="btn btn-hero" href="${pageContext.request.contextPath}/about">Explore StarPlatform &rarr;</a>
    </div>
  </section>

  <%-- 모바일 전용 기능 칩. 항목은 클라이언트 시안 그대로이며 별도 데이터는 없다 --%>
  <div class="home-chips" aria-label="Highlights">
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-1" aria-hidden="true"></span><span><strong>Global Ranking</strong><span>Worldwide ranking system</span></span></div>
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-2" aria-hidden="true"></span><span><strong>Link Economy</strong><span>An economy connected by links</span></span></div>
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-3" aria-hidden="true"></span><span><strong>Traffic Economy</strong><span>Opportunities created by traffic</span></span></div>
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-4" aria-hidden="true"></span><span><strong>Personal Monetization</strong><span>Your own monetization system</span></span></div>
  </div>

  <div class="page-wrap page-wrap-wide home-body">
    <div class="home-main">
      <div class="home-section-head">
        <h2>Latest Posts</h2>
        <span class="home-live">Real-time Updates</span>
      </div>

      <c:choose>
        <c:when test="${not empty postCards}">
          <%@ include file="/WEB-INF/jsp/common/include/web-post-cards.jsp"%>

          <nav class="pager" aria-label="Post pages">
            <c:if test="${not empty prevUrl}"><a class="btn btn-secondary" href="${prevUrl}" rel="prev">&larr; Newer</a></c:if>
            <span>Page <c:out value="${page}"/> of <c:out value="${lastPage}"/></span>
            <c:if test="${not empty nextUrl}"><a class="btn btn-secondary" href="${nextUrl}" rel="next">Older &rarr;</a></c:if>
          </nav>
        </c:when>
        <c:otherwise>
          <%-- 목록 조회 실패나 승인된 글이 아직 없을 때. 빈 화면 대신 갈 곳을 준다 --%>
          <div class="panel" style="margin-top: 20px;">
            <h3>No posts to show yet</h3>
            <p>Published posts appear here once they have been reviewed. In the meantime, read <a href="${pageContext.request.contextPath}/about">about StarPlatform</a> or see <a href="${pageContext.request.contextPath}/posts">how StarPlatform works</a>.</p>
          </div>
        </c:otherwise>
      </c:choose>
    </div>

    <%-- PC 사이드바. 시안의 광고 자리는 AdSense 승인 전이라 비워 둔다 (플레이스홀더도 넣지 않는다) --%>
    <aside class="home-side">
      <div class="home-promo">
        <p class="home-promo-brand">StarPlatform</p>
        <p class="home-promo-kicker">One Global Network</p>
        <p class="home-promo-title">Your Page. Your World.</p>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/about">Explore StarPlatform</a>
      </div>
    </aside>
  </div>

  <section class="page-wrap page-wrap-wide home-info" aria-label="Get started">
    <div class="home-info-card">
      <h3>Join the Global Community</h3>
      <p>Be part of a worldwide network of creators, brands and fans.</p>
      <a href="${pageContext.request.contextPath}/about">Learn more &rarr;</a>
    </div>
    <div class="home-info-card">
      <h3>Create Your Star Page</h3>
      <p>Build your own page and share your story with the world.</p>
      <a href="#" onclick="spOpenApp(); return false;">Open in App &rarr;</a>
    </div>
    <div class="home-info-card">
      <h3>Grow Together</h3>
      <p>More views, more connections, more opportunities.</p>
      <a href="${pageContext.request.contextPath}/about">Learn more &rarr;</a>
    </div>
  </section>
</main>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
<%@ include file="/WEB-INF/jsp/common/include/web-tabbar.jsp"%>
</body>
</html>
