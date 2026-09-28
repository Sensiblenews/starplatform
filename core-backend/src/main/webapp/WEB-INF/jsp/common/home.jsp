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
  <meta name="robots" content="${(empty q and empty category) ? 'index, follow' : 'noindex, follow'}" />
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

    /* ── 광고 자리 (클라이언트 참고 메일 2026-09-27 규격). 빈 상자로 크기만 확보, 승인 후 ins 삽입.
         PC: ① 히어로 우측 728×90 ② 사이드바 300×600 sticky ③ 페이지네이션 아래 728×90
         모바일: ① 숨김, A 히어로 아래·칩 카드 위 320×100, B 피드 중간 300×250(2번째 카드 뒤, 이후 6장마다),
                 C 목록 아래·탭바 위 320×100. 클릭 요소와는 20px 이상 띄운다(오클릭 방지) ── */
    /* 승인 전에는 자리가 어디인지 보이도록 점선 테두리 + "Advertisement" 라벨. ins 삽입 시 라벨 스타일은 지운다 */
    .ad-slot { position: relative; background: rgba(241, 245, 249, 0.92); border: 1px dashed #94a3b8; border-radius: 8px; }
    .ad-slot::after { content: "Advertisement"; position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); font-size: 11px; letter-spacing: 1px; text-transform: uppercase; color: #94a3b8; }
    .ad-slot-hero { flex: 0 0 auto; width: 728px; height: 90px; position: relative; z-index: 1; background: rgba(255, 255, 255, 0.92); }
    .ad-slot-side { width: 300px; height: 600px; } /* sticky 해제 — 스크롤 따라오는 것을 클라이언트가 버그로 봄 */
    .ad-slot-bottom { width: 728px; max-width: 100%; height: 90px; margin: 24px auto 0; }
    .ad-slot-mobile { width: 320px; max-width: 100%; height: 100px; margin: 20px auto 0; }
    .ad-slot-infeed { width: 300px; max-width: 100%; height: 250px; margin: 0 auto; border-radius: 14px; }

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
    /* ── 모바일 전용 섹션 (클라이언트 모바일 시안): 가로 스크롤 큰 카드·카테고리 타일·글로벌 네트워크 배너 ── */
    .m-only { display: none; }
    .m-sec-head { display: flex; align-items: center; gap: 10px; margin: 0 0 12px; }
    .m-sec-head h2 { font-size: 22px; font-weight: 700; color: #0f172a; white-space: nowrap; }
    .m-sec-head .home-live { color: #2f7cf6; font-size: 12px; white-space: nowrap; }
    .m-sec-head .home-live::before { background: #2f7cf6; }
    .m-sec-head .m-view-all { margin-left: auto; font-size: 14px; font-weight: 600; color: #2f7cf6; text-decoration: none; background: none; border: 0; font-family: inherit; cursor: pointer; white-space: nowrap; }
    /* 세로 한 줄에 카드 하나 (클라이언트 요청). 가로 스크롤은 카드가 옆 카드 높이에 끌려 늘어나고 아래가 비었다 */
    .m-posts { display: grid; grid-template-columns: 1fr; gap: 14px; }
    .m-post { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 14px; overflow: hidden; text-decoration: none; color: inherit; display: block; }
    /* 이미지 높이는 padding-top 비율로 고정한다. aspect-ratio 만으로는 세로 사진이 상자를 밀어 올려(min-height:auto) 카드가 길쭉해진다 */
    .m-post-img { display: block; position: relative; padding-top: 62.5%; background: #f1f5f9; overflow: hidden; }
    .m-post-img img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; display: block; }
    .m-post-body { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 8px; }
    .m-post-author { display: flex; align-items: center; gap: 8px; }
    .m-post-author img, .m-post-author-empty { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; background: #e2e8f0; flex-shrink: 0; }
    .m-post-author strong { font-size: 14px; display: inline-flex; align-items: center; gap: 6px; }
    .m-post-author small { display: block; font-size: 11px; color: #64748b; line-height: 1.2; }
    .m-post-text { font-size: 15px; color: #1e293b; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; word-break: break-word; }
    .m-post-stats { display: flex; gap: 18px; font-size: 12px; color: #64748b; border-top: 1px solid #f1f5f9; padding-top: 10px; }
    .m-stars { display: flex; gap: 10px; overflow-x: auto; scroll-padding-left: 16px; padding: 2px 16px 8px; margin: 0 -16px; scrollbar-width: none; }
    .m-stars::-webkit-scrollbar { display: none; }
    .m-star { flex: 0 0 104px; display: flex; flex-direction: column; align-items: center; gap: 6px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 14px 8px 12px; text-decoration: none; color: inherit; }
    .m-star img, .m-star-empty { width: 60px; height: 60px; border-radius: 50%; object-fit: cover; background: #e2e8f0; display: block; }
    .m-star-name { font-size: 13px; font-weight: 700; max-width: 88px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .m-star-meta { font-size: 11px; color: #64748b; }
    .m-load-more { display: block; margin: 14px auto 0; width: 100%; text-align: center; padding: 13px; border-radius: 12px; background: #ffffff; border: 1px solid #cbd5e1; color: #1d4ed8; font-size: 15px; font-weight: 600; text-decoration: none; }
    .m-load-more[aria-busy="true"] { opacity: 0.6; pointer-events: none; }
    .m-network { display: flex; align-items: center; gap: 14px; background: #1e40af; background: linear-gradient(120deg, #1d4ed8, #1e3a8a); color: #ffffff; border-radius: 16px; padding: 16px 16px; text-decoration: none; margin-top: 22px; }
    .m-network-globe { width: 48px; height: 48px; border-radius: 50%; background: rgba(255, 255, 255, 0.12); display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
    .m-network-globe svg { width: 28px; height: 28px; }
    .m-network-text { flex: 1 1 auto; min-width: 0; }
    .m-network-text strong { display: block; font-size: 17px; line-height: 1.25; }
    .m-network-text strong em { font-style: normal; font-size: 22px; margin-right: 6px; }
    .m-network-text span { display: block; font-size: 12px; color: #dbeafe; margin-top: 3px; }
    .m-network-flags { font-size: 18px; letter-spacing: 2px; white-space: nowrap; }
    .m-network-arrow { width: 28px; height: 28px; border-radius: 50%; border: 1.5px solid rgba(255, 255, 255, 0.6); display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 16px; }
    .m-filter-note { margin: 0 0 12px; font-size: 14px; color: #1d4ed8; }
    .m-filter-note a { color: #1d4ed8; }

    @media (max-width: 820px) {
      .m-only { display: block; }
      /* 모바일: 건수 줄·PC 안내 카드·PC 광고 자리·PC 그리드·페이지네이션은 감추고, 세로 카드 목록 + 더보기만 쓴다 */
      .home-meta, .home-info, .ad-slot-bottom, .home-main .post-grid, .home-main .pager { display: none; }
      .home-chips { position: relative; z-index: 2; margin: 14px 16px 0; padding: 12px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12); }
      .home-chip { min-width: 210px; background: #ffffff; border: 0; padding: 4px 6px; }
      .home-chip-dot { width: 38px; height: 38px; }
      .home-chip-dot::after { left: 13px; top: 13px; }
      /* [후속] 모바일 히어로는 글자 없이 지구 사진 띠만, 높이는 예전의 1/3 (약 120px). 헤더의 Open in App 버튼이 이 띠 위에 얹힌다 */
      .home-hero-inner { padding: 0; min-height: 120px; }
      .home-hero-text { display: none; }
      .home-hero::before { background: linear-gradient(180deg, rgba(5, 14, 36, 0.35) 0%, rgba(5, 14, 36, 0) 100%); }
      .home-hero { background-position: center 40%; }
      /* 여백은 위 카드 규칙(.home-chips 12px 균등)이 정한다 — 여기서 padding 을 다시 주면 위아래가 어긋나 칩이 아래로 처진다 */
      .home-chips { display: flex; gap: 10px; overflow-x: auto; align-items: center; -webkit-overflow-scrolling: touch; scrollbar-width: none; scroll-padding-left: 12px; }
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
      </div>
      <%-- [AdSense 승인 대기] 히어로 리더보드 728×90 자리. 승인 후 이 상자 안에 <ins class="adsbygoogle"> 삽입 --%>
      <div class="ad-slot ad-slot-hero" aria-hidden="true" data-ad-slot="hero-leaderboard"></div>
    </div>
  </section>

  <%-- [AdSense 승인 대기] 모바일 A: 히어로 아래·기능 칩 카드 위 320×100 (참고 메일) --%>
  <div class="m-only"><div class="ad-slot ad-slot-mobile" aria-hidden="true" data-ad-slot="mobile-top"></div></div>

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
        <%-- [후속] "Public posts" 큰 제목과 설명문은 클라이언트 요청으로 제거. 페이지 대표 h1 은 히어로 제목 --%>
        <c:if test="${totalCount gt 0}">
          <p class="home-meta"><c:out value="${totalCount}"/> posts published &middot; page <c:out value="${page}"/> of <c:out value="${lastPage}"/></p>
        </c:if>
        <c:if test="${not empty q}">
          <p class="home-search-note">Showing results for &ldquo;<c:out value="${q}"/>&rdquo; &middot; <a href="${pageContext.request.contextPath}/">Clear search</a></p>
        </c:if>
        <c:if test="${not empty category}">
          <p class="home-search-note">Showing <c:out value="${categoryLabel}"/> posts &middot; <a href="${pageContext.request.contextPath}/">Show all categories</a></p>
        </c:if>

        <%-- ── 모바일: Latest Posts 가로 스크롤 (처음 6장) → View all 로 전체 목록 펼침 ── --%>
        <div class="m-only">
          <div class="m-sec-head">
            <h2>Latest Posts</h2>
            <span class="home-live">Real-time Updates</span>
          </div>
          <c:if test="${not empty postCards}">
          <div class="m-posts" id="mPosts">
            <c:forEach var="p" items="${postCards}" varStatus="ps">
              <a class="m-post" href="${pageContext.request.contextPath}/post/${p.conId}">
                <span class="m-post-img"><c:if test="${not empty p.image}"><img src="${p.image}" alt="${p.alt}" loading="lazy" onerror="this.style.visibility='hidden'"></c:if></span>
                <span class="m-post-body">
                  <span class="m-post-author">
                    <c:choose><c:when test="${not empty p.authorImage}"><img src="${p.authorImage}" alt="" loading="lazy"></c:when><c:otherwise><span class="m-post-author-empty"></span></c:otherwise></c:choose>
                    <span><strong>${p.author}<c:if test="${not empty p.category}"><span class="post-badge"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 2.5l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.4l-6 3.4 1.3-6.7-5-4.6 6.8-.8z"/></svg>${p.category}</span></c:if></strong><small>${p.date}</small></span>
                  </span>
                  <span class="m-post-text">${p.snippet}</span>
                  <span class="m-post-stats">
                    <span class="post-stat"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" d="M12 20.5s-7.5-4.6-9.2-9.3C1.6 8 3.6 4.5 7 4.5c2 0 3.6 1.1 5 3 1.4-1.9 3-3 5-3 3.4 0 5.4 3.5 4.2 6.7-1.7 4.7-9.2 9.3-9.2 9.3z"/></svg><c:out value="${empty p.likeCnt ? 0 : p.likeCnt}"/></span>
                    <span class="post-stat"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" d="M4 5h16v11H9l-5 4z"/></svg><c:out value="${empty p.commentCnt ? 0 : p.commentCnt}"/></span>
                    <c:if test="${p.mediaCnt gt 1}"><span class="post-stat"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="2"/><path fill="none" stroke="currentColor" stroke-width="2" d="M3 16l5-5 4 4 3-3 6 5"/></svg><c:out value="${p.mediaCnt}"/></span></c:if>
                  </span>
                </span>
              </a>
              <%-- [AdSense 승인 대기] 모바일 B: 피드 중간 300×250 — 2번째 카드 뒤, 이후 6장마다 (참고 메일) --%>
              <c:if test="${ps.index mod 6 eq 1}"><div class="ad-slot ad-slot-infeed m-ad" aria-hidden="true" data-ad-slot="mobile-infeed"></div></c:if>
            </c:forEach>
          </div>
          <c:if test="${not empty nextUrl}">
            <%-- 더보기: 다음 페이지 HTML 을 받아 카드만 이어 붙인다. JS 가 없으면 그냥 다음 페이지로 간다 --%>
            <a class="m-load-more" id="mLoadMore" href="${nextUrl}" data-next="${nextUrl}">Load more</a>
          </c:if>
          </c:if>
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

        <%-- ── 모바일: Popular Stars (후속 요청: Explore by Category 대신, /posts 허브의 인기 스타를 그대로) ── --%>
        <c:if test="${not empty topStars}">
        <div class="m-only" style="margin-top: 22px;">
          <div class="m-sec-head"><h2>Popular Stars</h2></div>
          <div class="m-stars">
            <c:forEach var="st" items="${topStars}">
              <a class="m-star" href="${pageContext.request.contextPath}/star/${st.id}">
                <c:choose><c:when test="${not empty st.image}"><img src="${st.image}" alt="" loading="lazy" onerror="this.style.visibility='hidden'"></c:when><c:otherwise><span class="m-star-empty"></span></c:otherwise></c:choose>
                <span class="m-star-name">${st.name}</span>
                <span class="m-star-meta"><c:out value="${empty st.followerCnt ? 0 : st.followerCnt}"/> followers</span>
              </a>
            </c:forEach>
          </div>
        </div>
        </c:if>

        <%-- ── 모바일: 글로벌 네트워크 배너 (시안 문구, 영어) ── --%>
        <%-- [AdSense 승인 대기] 모바일 C: 목록 아래 320×100. 고정 탭바와는 body 하단 여백 + 이 상자의 여백으로 20px 이상 띄운다 (참고 메일) --%>
        <div class="m-only"><div class="ad-slot ad-slot-mobile" aria-hidden="true" data-ad-slot="mobile-bottom" style="margin-bottom: 20px;"></div></div>

        <a class="m-network m-only" href="${pageContext.request.contextPath}/about">
          <span class="m-network-globe" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="1.8"/><path fill="none" stroke="currentColor" stroke-width="1.8" d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/></svg></span>
          <span class="m-network-text"><strong><em>203</em>countries &middot; Global Page Network</strong><span>8 Billion People &times; Digital Pages &times; Global Traffic &times; Economic Activity</span></span>
          <span class="m-network-arrow" aria-hidden="true">&rsaquo;</span>
        </a>
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

<script>
  // 모바일 "Load more": 다음 페이지 HTML 을 받아 .m-post 카드만 이어 붙이고, 그 다음 페이지 주소로 갱신한다.
  // 마지막 페이지면 버튼을 지운다. fetch 실패 시에는 링크 그대로 다음 페이지로 이동한다
  (function () {
    var btn = document.getElementById('mLoadMore');
    var list = document.getElementById('mPosts');
    if (!btn || !list || !window.fetch) return;
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      btn.setAttribute('aria-busy', 'true');
      fetch(btn.getAttribute('data-next'), { credentials: 'same-origin' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
          var doc = new DOMParser().parseFromString(html, 'text/html');
          doc.querySelectorAll('#mPosts .m-post, #mPosts .m-ad').forEach(function (card) { list.appendChild(card); });
          var next = doc.getElementById('mLoadMore');
          if (next) {
            btn.setAttribute('data-next', next.getAttribute('data-next'));
            btn.setAttribute('href', next.getAttribute('href'));
            btn.removeAttribute('aria-busy');
          } else {
            btn.remove();
          }
        })
        .catch(function () { window.location.href = btn.getAttribute('href'); });
    });
  })();
</script>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
<%@ include file="/WEB-INF/jsp/common/include/web-tabbar.jsp"%>
</body>
</html>
