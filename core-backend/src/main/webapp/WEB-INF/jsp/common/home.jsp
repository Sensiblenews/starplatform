<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  홈 (/): 공개 포스트 목록.

  [2-29차 후속] 예전 /posts 목록 화면을 홈으로 옮기고 클라이언트 시안대로 다시 짰다.
   - 상단 히어로 배너 ("Your Page. Your World.") — 클라이언트가 준 지구 사진(resources/img/home-hero-earth.webp)을
     배경으로 쓴다. 오른쪽에 리더보드(728×90) 광고 자리. 외부 이미지를 핫링크하지 않는다.
   - "Public posts" 제목 + 설명문 + 건수/페이지 표기 (2026-09-27 시안: 토씨 그대로)
   - 포스트 그리드 3열 (카드 마크업·스타일은 include 로 공유) + PC 우측 하프페이지(300×600) 광고 자리
   - 그리드 아래 리더보드(728×90) 광고 자리, 안내 카드 3개 (원형 아이콘 + 화살표)
   - 모바일 전용 기능 칩 4개 (가로 스크롤) 와 하단 탭바는 모바일 시안에서 가져온 것
  광고 자리(.ad-slot)는 크기만 확보한 빈 상자다. AdSense 승인 후 각 자리의 주석 위치에 <ins class="adsbygoogle"> 를 넣는다.
  헤더 돋보기 검색(?q=)이 있으면 제목 아래에 검색 결과 안내를 보이고 noindex 를 건다.
  페이지네이션 URL 은 /?page=N 이다 (PublicWebController.pageUrl).
--%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title><c:choose><c:when test="${not empty q}">Search: <c:out value="${q}"/> | StarPlatform</c:when><c:when test="${page gt 1}">Public Posts - Page ${page} | StarPlatform</c:when><c:otherwise>StarPlatform - Your Page. Your World.</c:otherwise></c:choose></title>
  <meta name="description" content="StarPlatform is a global creator platform. Read the latest public posts from star pages around the world, no account and no app required." />
  <%-- 검색 결과 URL 은 무한히 생길 수 있어 색인하지 않는다 --%>
  <meta name="robots" content="${empty q ? 'index, follow' : 'noindex, follow'}" />
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
    /* ── 히어로: 지구 사진 배경 + 왼쪽 문구 + 오른쪽 리더보드 광고 자리 ── */
    .home-hero { position: relative; overflow: hidden; color: #ffffff; background: url('${pageContext.request.contextPath}/resources/img/home-hero-earth.webp') center right / cover no-repeat, linear-gradient(120deg, #0b1a3a 0%, #102a5c 55%, #143a7c 100%); }
    /* 왼쪽 글자 뒤만 어둡게 — 사진의 별 하늘 위에 흰 글자가 묻히지 않게 한다 */
    .home-hero::before { content: ""; position: absolute; inset: 0; background: linear-gradient(90deg, rgba(5, 14, 36, 0.78) 0%, rgba(5, 14, 36, 0.45) 40%, rgba(5, 14, 36, 0) 70%); pointer-events: none; }
    .home-hero-inner { position: relative; z-index: 1; max-width: 1400px; margin: 0 auto; padding: 34px 24px 34px; min-height: 240px; display: flex; align-items: center; justify-content: space-between; gap: 32px; }
    .home-hero-text { flex: 0 1 560px; }
    .home-kicker { font-size: 12px; font-weight: 700; letter-spacing: 2.4px; text-transform: uppercase; color: #e2e8f0; margin-bottom: 10px; }
    .home-hero h1 { font-size: 44px; font-weight: 700; line-height: 1.12; color: #ffffff; margin-bottom: 12px; }
    .home-hero h1 .hl { color: #fbbf24; }
    .home-hero-sub { font-size: 17px; line-height: 1.5; color: #e2e8f0; max-width: 520px; margin-bottom: 22px; }
    .btn-hero { display: inline-flex; align-items: center; gap: 8px; padding: 12px 20px; border-radius: 10px; background: #2f7cf6; color: #ffffff; font-size: 15px; font-weight: 600; text-decoration: none; box-shadow: 0 8px 20px rgba(37, 99, 235, 0.35); }
    .btn-hero:hover { background: #1d6ae8; }

    /* ── 광고 자리: 시안의 리더보드·하프페이지 위치에 크기만 잡아 둔 빈 상자 ── */
    .ad-slot { background: rgba(241, 245, 249, 0.92); border: 1px solid #e2e8f0; border-radius: 8px; }
    .ad-slot-hero { flex: 0 0 auto; width: 728px; height: 90px; position: relative; z-index: 1; background: rgba(255, 255, 255, 0.92); }
    .ad-slot-side { width: 300px; height: 600px; position: sticky; top: 76px; }
    .ad-slot-bottom { width: 100%; max-width: 100%; height: 90px; margin-top: 16px; }

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

    /* ── 본문: 제목 + 포스트 그리드 + PC 사이드바 광고 ── */
    .home-wrap { max-width: 1400px; margin: 0 auto; padding: 26px 24px 0; }
    .home-body { display: block; }
    .home-title { font-size: 30px; font-weight: 700; color: #0f172a; line-height: 1.2; margin-bottom: 8px; }
    .home-lead { font-size: 15px; color: #475569; margin-bottom: 6px; }
    .home-meta { font-size: 14px; color: #64748b; }
    .home-search-note { margin-top: 10px; font-size: 14px; color: #1d4ed8; }
    .home-search-note a { color: #1d4ed8; }
    .home-main .post-grid { margin-top: 20px; }
    .home-side { display: none; }

    .pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 28px; }
    .pager span { font-size: 14px; color: #64748b; }

    /* ── 하단 안내 카드 3개: 원형 아이콘 + 제목/설명 + 오른쪽 화살표 ── */
    .home-info { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-top: 18px; }
    .home-info-card { display: flex; align-items: center; gap: 16px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px 22px; text-decoration: none; color: inherit; }
    .home-info-card:hover { border-color: #93c5fd; box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06); }
    .home-info-icon { width: 52px; height: 52px; border-radius: 50%; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; color: #ffffff; }
    .home-info-icon svg { width: 26px; height: 26px; }
    .home-info-icon-blue { background: #2f7cf6; }
    .home-info-icon-purple { background: #7c5cf5; }
    .home-info-icon-green { background: #22a06b; }
    .home-info-text { flex: 1 1 auto; min-width: 0; }
    .home-info-card h3 { font-size: 15px; font-weight: 700; color: #0f172a; margin-bottom: 3px; }
    .home-info-card p { font-size: 13px; color: #475569; line-height: 1.45; margin: 0; }
    .home-info-arrow { flex-shrink: 0; color: #2f7cf6; font-size: 18px; font-weight: 700; }

    @media (min-width: 1100px) {
      .home-body { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 20px; align-items: start; }
      .home-main .post-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
      .home-side { display: block; }
    }
    @media (max-width: 1099px) {
      .ad-slot-hero { display: none; }
    }
    @media (max-width: 820px) {
      .home-hero-inner { padding: 36px 20px; min-height: 0; }
      .home-hero h1 { font-size: 32px; }
      .home-hero-sub { font-size: 15px; }
      /* 좁은 화면은 세로가 길어져 사진이 확대된다 — 지구 곡선이 보이도록 가운데 기준, 글자 뒤 어둡기는 위→아래로 */
      .home-hero { background-position: center 40%; }
      .home-hero::before { background: linear-gradient(180deg, rgba(5, 14, 36, 0.7) 0%, rgba(5, 14, 36, 0.35) 60%, rgba(5, 14, 36, 0.15) 100%); }
      .home-chips { display: flex; gap: 10px; overflow-x: auto; padding: 14px 20px 0; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
      .home-chips::-webkit-scrollbar { display: none; }
      .home-wrap { padding: 20px 16px 0; }
      .home-title { font-size: 26px; }
      .ad-slot-bottom { height: 100px; }
      .home-info { grid-template-columns: 1fr; }
    }
  </style>
</head>
<body>

<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<main>
  <section class="home-hero">
    <div class="home-hero-inner">
      <div class="home-hero-text">
        <p class="home-kicker">One Global Network</p>
        <h1>Your Page. <span class="hl">Your World.</span></h1>
        <p class="home-hero-sub">Connect, share and grow across 203 countries.<br>Your digital home, your global stage.</p>
        <a class="btn-hero" href="${pageContext.request.contextPath}/about">Explore StarPlatform <span aria-hidden="true">&rarr;</span></a>
      </div>
      <%-- [AdSense 승인 대기] 히어로 리더보드 728×90 자리. 승인 후 이 상자 안에 <ins class="adsbygoogle"> 삽입 --%>
      <div class="ad-slot ad-slot-hero" aria-hidden="true" data-ad-slot="hero-leaderboard"></div>
    </div>
  </section>

  <%-- 모바일 전용 기능 칩. 항목은 클라이언트 모바일 시안 그대로이며 별도 데이터는 없다 --%>
  <div class="home-chips" aria-label="Highlights">
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-1" aria-hidden="true"></span><span><strong>Global Ranking</strong><span>Worldwide ranking system</span></span></div>
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-2" aria-hidden="true"></span><span><strong>Link Economy</strong><span>An economy connected by links</span></span></div>
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-3" aria-hidden="true"></span><span><strong>Traffic Economy</strong><span>Opportunities created by traffic</span></span></div>
    <div class="home-chip"><span class="home-chip-dot home-chip-dot-4" aria-hidden="true"></span><span><strong>Personal Monetization</strong><span>Your own monetization system</span></span></div>
  </div>

  <div class="home-wrap">
    <div class="home-body">
      <div class="home-main">
        <h1 class="home-title">Public posts</h1>
        <p class="home-lead">Posts published on star pages across StarPlatform. Anyone can read them here &mdash; no account and no app required.</p>
        <c:if test="${totalCount gt 0}">
          <p class="home-meta"><c:out value="${totalCount}"/> posts published &middot; page <c:out value="${page}"/> of <c:out value="${lastPage}"/></p>
        </c:if>
        <c:if test="${not empty q}">
          <p class="home-search-note">Showing results for &ldquo;<c:out value="${q}"/>&rdquo; &middot; <a href="${pageContext.request.contextPath}/">Clear search</a></p>
        </c:if>

        <c:choose>
          <c:when test="${not empty postCards}">
            <%@ include file="/WEB-INF/jsp/common/include/web-post-cards.jsp"%>

            <nav class="pager" aria-label="Post pages">
              <c:if test="${not empty prevUrl}"><a class="btn btn-secondary" href="${prevUrl}" rel="prev">&larr; Newer</a></c:if>
              <span>Page <c:out value="${page}"/> of <c:out value="${lastPage}"/></span>
              <c:if test="${not empty nextUrl}"><a class="btn btn-secondary" href="${nextUrl}" rel="next">Older &rarr;</a></c:if>
            </nav>
          </c:when>
          <c:when test="${not empty q}">
            <div class="panel" style="margin-top: 20px;">
              <h3>No posts match &ldquo;<c:out value="${q}"/>&rdquo;</h3>
              <p>Try a different word, or <a href="${pageContext.request.contextPath}/">browse all public posts</a>.</p>
            </div>
          </c:when>
          <c:otherwise>
            <%-- 목록 조회 실패나 승인된 글이 아직 없을 때. 빈 화면 대신 갈 곳을 준다 --%>
            <div class="panel" style="margin-top: 20px;">
              <h3>No posts to show yet</h3>
              <p>Published posts appear here once they have been reviewed. In the meantime, read <a href="${pageContext.request.contextPath}/about">about StarPlatform</a> or see <a href="${pageContext.request.contextPath}/posts">how StarPlatform works</a>.</p>
            </div>
          </c:otherwise>
        </c:choose>

        <%-- [AdSense 승인 대기] 그리드 아래 리더보드 728×90 자리 --%>
        <div class="ad-slot ad-slot-bottom" aria-hidden="true" data-ad-slot="list-leaderboard"></div>
      </div>

      <%-- PC 사이드바: [AdSense 승인 대기] 하프페이지 300×600 자리 --%>
      <aside class="home-side">
        <div class="ad-slot ad-slot-side" aria-hidden="true" data-ad-slot="sidebar-halfpage"></div>
      </aside>
    </div>

    <section class="home-info" aria-label="Get started">
      <a class="home-info-card" href="${pageContext.request.contextPath}/about">
        <span class="home-info-icon home-info-icon-blue" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><path fill="none" stroke="currentColor" stroke-width="2" d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/></svg></span>
        <span class="home-info-text"><h3>Join the Global Community</h3><p>Be part of a worldwide network<br>of creators, brands and fans.</p></span>
        <span class="home-info-arrow" aria-hidden="true">&rarr;</span>
      </a>
      <a class="home-info-card" href="#" onclick="spOpenApp(); return false;">
        <span class="home-info-icon home-info-icon-purple" aria-hidden="true"><svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2.5l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.4l-6 3.4 1.3-6.7-5-4.6 6.8-.8z"/></svg></span>
        <span class="home-info-text"><h3>Create Your Star Page</h3><p>Build your own page and<br>share your story with the world.</p></span>
        <span class="home-info-arrow" aria-hidden="true">&rarr;</span>
      </a>
      <a class="home-info-card" href="${pageContext.request.contextPath}/about">
        <span class="home-info-icon home-info-icon-green" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="16.5" cy="9.5" r="2.5" fill="none" stroke="currentColor" stroke-width="2"/><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" d="M3.5 18c.5-3 2.7-4.5 5.5-4.5s5 1.5 5.5 4.5M14.5 17.5c.4-2 1.6-3 3.5-3 1.5 0 2.6.7 3 2"/></svg></span>
        <span class="home-info-text"><h3>Grow Together</h3><p>More views, more connections,<br>more opportunities.</p></span>
        <span class="home-info-arrow" aria-hidden="true">&rarr;</span>
      </a>
    </section>
  </div>
</main>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
<%@ include file="/WEB-INF/jsp/common/include/web-tabbar.jsp"%>
</body>
</html>
