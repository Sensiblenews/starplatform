<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<% String scheme=request.getScheme(); String serverName=request.getServerName(); int
    serverPort=request.getServerPort(); String portStr="" ; if (("http".equals(scheme) && serverPort !=80) ||
    ("https".equals(scheme) && serverPort !=443)) { portStr=":" + serverPort; } String fallbackBaseUrl="" ; if
    ("localhost".equals(serverName) || "127.0.0.1" .equals(serverName) || serverName.startsWith("192.168.")) {
    fallbackBaseUrl=scheme + "://" + serverName + portStr; } else { fallbackBaseUrl="https://witch-hunting.com" ; }
    request.setAttribute("fallbackBaseUrl", fallbackBaseUrl); %>
<!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="apple-itunes-app" content="app-id=1188195403">
    <title>${not empty ogTitle ? ogTitle : 'StarPlatform SuperApp'}</title>

    <!-- 검색 색인 허용 + 대표 URL 명시 (AdSense/SEO 대응).
         승인 게시물이 없는 스타 페이지는 빈약 콘텐츠라 색인에서 제외한다 (콘텐츠 유무 기준, UA 분기 아님) -->
    <meta name="robots" content="${robotsNoindex ? 'noindex, follow' : 'index, follow'}">
    <c:if test="${not empty canonicalUrl}">
        <link rel="canonical" href="${canonicalUrl}">
    </c:if>

    <!-- JSON-LD 구조화 데이터: 컨트롤러에서 JSON 이스케이프까지 마친 문자열을 서버 렌더링으로 출력 -->
    <c:if test="${not empty jsonLd}">
        <script type="application/ld+json">${jsonLd}</script>
    </c:if>

    <!-- Twitter Card Meta Tags (X 공유 최적화) -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:site" content="@StarPlatform">
    <meta name="twitter:domain" content="witch-hunting.com">
    <meta name="twitter:url" content="${ogUrl}">
    <meta name="twitter:title" content="${not empty ogTitle ? ogTitle : 'StarPlatform SuperApp'}">
    <meta name="twitter:description" content="${not empty ogDesc ? ogDesc : 'Everyone Can Earn'}">
    <meta name="twitter:image" content="${not empty ogImage ? ogImage : fallbackBaseUrl.concat('/resources/img/icon.png')}">
    <meta name="twitter:image:src" content="${not empty ogImage ? ogImage : fallbackBaseUrl.concat('/resources/img/icon.png')}">

    <!-- Open Graph Meta Tags (카카오톡, 페이스북 등) -->
    <meta property="og:title" content="${not empty ogTitle ? ogTitle : 'StarPlatform SuperApp | Everyone Can Earn'}">
    <meta property="og:description" content="${not empty ogDesc ? ogDesc : 'Everyone Can Earn'}">
    <meta property="og:image" content="${not empty ogImage ? ogImage : fallbackBaseUrl.concat('/resources/img/icon.png')}">
    <meta property="og:image:width" content="256">
    <meta property="og:image:height" content="256">
    <meta property="og:url" content="${ogUrl}">
    <meta property="og:type" content="website">

    <%-- [AdSense 승인 대기] 심사 신호를 깨끗하게 유지하기 위해 광고 스크립트를 임시 제거함.
         승인 메일 수신 후 아래 3곳을 함께 복원할 것 (이 파일 내 동일 표식 검색):
           ① head 광고 스크립트(여기)  ② 관련 콘텐츠 아래 ins 슬롯  ③ 하단 광고 게이트 스크립트
         복원 코드는 git 이력 참조 — JSP 주석이라 HTML 출력에는 노출되지 않는다.
         복원 시 클라이언트 권장안 반영: ins에 style="display:block;min-height:250px" 적용
         (레이아웃 흔들림 방지). 위치는 관련 콘텐츠 아래·하단 버튼 위 (FAQ 섹션은 2-29차 후속에서 제거됨). --%>

    <%-- 애드센스 소유권 확인용 메타 태그 — 광고 코드가 아니므로 심사 중에도 유지 (구글 공식 확인 수단) --%>
    <meta name="google-adsense-account" content="ca-pub-9109251900558498">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <%@ include file="/WEB-INF/jsp/common/include/web-chrome-style.jsp"%>

    <style>
        /* [2-29차 후속] 클라이언트 시안: 스타 프로필(커버·아바타·통계 바·탭·카드 그리드·사이드바),
           포스트 상세(지구 띠·본문 카드·갤러리·사이드바 작성자 카드). 헤더·푸터는 공통 include.
           웹에서 할 수 없는 것(팔로우·좋아요·즐겨찾기·신고)은 넣지 않고 앱 열기·공유만 둔다. */
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Noto Sans KR', sans-serif; background: #f6f8fb; color: #0f172a; line-height: 1.6; }
        img { max-width: 100%; }
        a { color: inherit; }

        .lp-wrap { max-width: 1400px; margin: 0 auto; padding: 24px 24px 0; }
        .lp-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 24px; align-items: start; }
        .lp-card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 14px; }
        .lp-side { display: flex; flex-direction: column; gap: 18px; }

        /* ── 배지·버튼 ── */
        .lp-badge { display: inline-flex; align-items: center; gap: 3px; font-size: 10px; font-weight: 700; letter-spacing: 0.4px; text-transform: uppercase; color: #1d4ed8; background: #dbeafe; border-radius: 999px; padding: 2px 8px; vertical-align: middle; }
        .lp-badge svg { width: 10px; height: 10px; }
        .lp-badge-dark { color: #ffffff; background: #2f7cf6; }
        .lp-btn { display: inline-flex; align-items: center; justify-content: center; gap: 8px; padding: 10px 18px; border-radius: 10px; font-size: 14px; font-weight: 600; text-decoration: none; border: 0; cursor: pointer; font-family: inherit; white-space: nowrap; }
        .lp-btn-primary { background: #2f7cf6; color: #ffffff; }
        .lp-btn-primary:hover { background: #1d6ae8; }
        .lp-btn-secondary { background: #ffffff; color: #0f172a; border: 1px solid #e2e8f0; }
        .lp-btn-secondary:hover { background: #f8fafc; }
        .lp-icon-btn { width: 40px; height: 40px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; background: #ffffff; border: 1px solid #e2e8f0; color: #0f172a; cursor: pointer; }
        .lp-icon-btn:hover { background: #f8fafc; }
        .lp-icon-btn svg { width: 18px; height: 18px; }
        .lp-stat-ico { display: inline-flex; align-items: center; gap: 5px; }
        .lp-stat-ico svg { width: 15px; height: 15px; }
        .lp-copied { position: fixed; left: 50%; bottom: 28px; transform: translateX(-50%); background: #0f172a; color: #ffffff; font-size: 13px; padding: 8px 14px; border-radius: 999px; opacity: 0; pointer-events: none; transition: opacity .2s; z-index: 200; }
        .lp-copied.is-on { opacity: 1; }

        /* ── 스타: 커버 + 아바타 ── */
        .st-cover { position: relative; height: 210px; overflow: hidden; background: #0b1730; }
        .st-cover-img { position: absolute; inset: -30px; background-position: center; background-size: cover; filter: blur(14px) saturate(1.1); transform: scale(1.1); opacity: 0.9; }
        .st-cover::after { content: ""; position: absolute; inset: 0; background: linear-gradient(180deg, rgba(5, 14, 36, 0.15) 0%, rgba(5, 14, 36, 0.55) 100%); }
        .st-cover-inner { position: relative; z-index: 1; max-width: 1400px; margin: 0 auto; padding: 0 24px; height: 100%; display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
        .st-head { display: flex; align-items: flex-end; gap: 22px; padding-bottom: 18px; min-width: 0; }
        .st-avatar { position: relative; width: 128px; height: 128px; flex-shrink: 0; }
        .st-avatar img, .st-avatar-empty { width: 128px; height: 128px; border-radius: 50%; object-fit: cover; border: 4px solid #ffffff; background: #e2e8f0; box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25); display: block; }
        .st-avatar .lp-badge { position: absolute; right: -2px; bottom: 8px; box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25); }
        .st-name-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
        .st-name { font-size: 30px; font-weight: 700; color: #ffffff; line-height: 1.15; text-shadow: 0 2px 8px rgba(0, 0, 0, 0.35); }
        .st-tagline { font-size: 15px; color: #e2e8f0; margin-top: 4px; text-shadow: 0 1px 6px rgba(0, 0, 0, 0.35); max-width: 640px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
        .st-actions { display: flex; align-items: center; gap: 10px; padding-bottom: 26px; flex-shrink: 0; }

        /* ── 스타: 통계 바 ── */
        .st-stats { display: flex; align-items: center; padding: 14px 20px; gap: 0; }
        .st-stat { flex: 1 1 0; display: flex; align-items: center; gap: 12px; padding: 4px 18px; border-left: 1px solid #e2e8f0; }
        .st-stat:first-child { border-left: 0; padding-left: 0; }
        .st-stat-ico { width: 36px; height: 36px; border-radius: 50%; background: #f1f5f9; display: inline-flex; align-items: center; justify-content: center; color: #475569; flex-shrink: 0; }
        .st-stat-ico svg { width: 18px; height: 18px; }
        .st-stat-ico-gold { color: #d97706; background: #fef3c7; }
        .st-stat-value { display: block; font-size: 20px; font-weight: 700; line-height: 1.1; }
        .st-stat-label { display: block; font-size: 12px; color: #64748b; margin-top: 2px; }
        .st-stats-actions { display: flex; gap: 8px; margin-left: 12px; }

        /* ── 스타: 탭 + 콘텐츠 ── */
        .st-tabs { display: flex; gap: 6px; border-bottom: 1px solid #e2e8f0; margin: 22px 0 16px; }
        .st-tab { background: transparent; border: 0; border-bottom: 2px solid transparent; padding: 10px 14px; font-size: 15px; font-weight: 600; color: #64748b; cursor: pointer; font-family: inherit; margin-bottom: -1px; }
        .st-tab[aria-selected="true"] { color: #1d4ed8; border-bottom-color: #2f7cf6; }
        .st-panel[hidden] { display: none; }

        .st-posts { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
        .st-post { display: flex; flex-direction: column; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; text-decoration: none; }
        .st-post:hover { border-color: #93c5fd; box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06); }
        .st-post-img { position: relative; aspect-ratio: 16 / 10; background: #f1f5f9; }
        .st-post-img img { width: 100%; height: 100%; object-fit: cover; display: block; }
        .st-post-img .lp-media-cnt { position: absolute; right: 10px; top: 10px; font-size: 11px; font-weight: 600; color: #ffffff; background: rgba(15, 23, 42, 0.7); border-radius: 999px; padding: 3px 8px; display: inline-flex; align-items: center; gap: 4px; }
        .st-post-img .lp-media-cnt svg { width: 12px; height: 12px; }
        .st-post-body { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 8px; flex: 1 1 auto; }
        .st-post-author { display: flex; align-items: center; gap: 8px; }
        .st-post-author img, .st-post-author-empty { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; background: #e2e8f0; flex-shrink: 0; }
        .st-post-author-name { font-size: 13px; font-weight: 700; }
        .st-post-date { display: block; font-size: 11px; color: #64748b; line-height: 1.2; }
        .st-post-text { font-size: 14px; color: #1e293b; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; word-break: break-word; min-height: 2.9em; }
        .st-post-stats { display: flex; gap: 16px; font-size: 12px; color: #64748b; border-top: 1px solid #f1f5f9; padding-top: 10px; margin-top: auto; }

        .st-photos { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
        .st-photo { aspect-ratio: 1; border-radius: 10px; overflow: hidden; background: #f1f5f9; display: block; }
        .st-photo img { width: 100%; height: 100%; object-fit: cover; display: block; }
        .st-about { padding: 22px 24px; }
        .st-about h2 { font-size: 18px; margin-bottom: 10px; }
        .st-about p { font-size: 15px; color: #334155; white-space: pre-line; }
        .st-about dl { display: grid; grid-template-columns: max-content 1fr; gap: 6px 18px; margin-top: 16px; font-size: 14px; }
        .st-about dt { color: #64748b; }
        .lp-empty { padding: 28px; text-align: center; color: #64748b; font-size: 14px; }

        /* ── 사이드바 ── */
        .sb-promo { position: relative; overflow: hidden; border-radius: 14px; color: #ffffff; padding: 22px; background: url('${pageContext.request.contextPath}/resources/img/home-hero-earth.webp') center right / cover no-repeat, #0b1a3a; }
        .sb-promo::before { content: ""; position: absolute; inset: 0; background: linear-gradient(90deg, rgba(5, 14, 36, 0.82) 0%, rgba(5, 14, 36, 0.35) 100%); }
        .sb-promo > * { position: relative; z-index: 1; }
        .sb-promo-brand { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; margin-bottom: 8px; }
        .sb-promo-brand svg { width: 16px; height: 16px; }
        .sb-promo-title { font-size: 20px; font-weight: 700; line-height: 1.2; }
        .sb-promo-sub { font-size: 14px; color: #dbeafe; margin: 2px 0 14px; }
        .sb-promo .lp-btn { padding: 8px 14px; font-size: 13px; }
        .sb-block { padding: 18px 18px 14px; }
        .sb-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
        .sb-head h2 { font-size: 15px; font-weight: 700; }
        .sb-head a { font-size: 12px; font-weight: 600; color: #2f7cf6; text-decoration: none; }
        .sb-list { list-style: none; }
        .sb-item { display: flex; gap: 12px; align-items: center; padding: 8px 0; border-top: 1px solid #f1f5f9; text-decoration: none; }
        .sb-item:first-child { border-top: 0; padding-top: 0; }
        .sb-item img, .sb-item-empty { width: 52px; height: 52px; border-radius: 8px; object-fit: cover; background: #f1f5f9; flex-shrink: 0; display: block; }
        .sb-item-text { min-width: 0; flex: 1 1 auto; }
        .sb-item-title { font-size: 13px; font-weight: 600; color: #0f172a; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; }
        .sb-item-meta { font-size: 11px; color: #64748b; display: flex; gap: 10px; margin-top: 2px; }
        .sb-stars { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
        .sb-star { text-align: center; text-decoration: none; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 10px 6px; }
        .sb-star img, .sb-star-empty { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; background: #e2e8f0; display: block; margin: 0 auto 6px; }
        .sb-star-name { display: block; font-size: 12px; font-weight: 700; color: #0f172a; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
        .sb-star-meta { display: block; font-size: 10px; color: #64748b; }
        .sb-join { display: flex; align-items: center; gap: 12px; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 12px; padding: 14px 16px; text-decoration: none; }
        .sb-join svg { width: 24px; height: 24px; color: #2f7cf6; flex-shrink: 0; }
        .sb-join strong { display: block; font-size: 14px; color: #1e3a8a; }
        .sb-join span { display: block; font-size: 12px; color: #1d4ed8; }
        .sb-join-arrow { margin-left: auto; color: #2f7cf6; font-size: 18px; }
        .sb-author { padding: 18px; }
        .sb-author-row { display: flex; align-items: center; gap: 12px; }
        .sb-author-row img, .sb-author-empty { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; background: #e2e8f0; flex-shrink: 0; display: block; }
        .sb-author-name { font-size: 17px; font-weight: 700; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
        .sb-author-meta { font-size: 12px; color: #64748b; }
        .sb-author-stats { display: grid; grid-template-columns: repeat(3, 1fr); background: #f8fafc; border-radius: 10px; margin-top: 14px; padding: 12px 6px; text-align: center; }
        .sb-author-stats div + div { border-left: 1px solid #e2e8f0; }
        .sb-author-stats strong { display: block; font-size: 16px; }
        .sb-author-stats span { font-size: 11px; color: #64748b; }
        .sb-author-about { margin-top: 14px; }
        .sb-author-about h3 { font-size: 14px; margin-bottom: 4px; }
        .sb-author-about p { font-size: 13px; color: #334155; white-space: pre-line; }

        /* ── 포스트: 지구 띠 + 본문 카드 ── */
        .po-band { height: 90px; background: url('${pageContext.request.contextPath}/resources/img/home-hero-earth.webp') center / cover no-repeat, #0b1a3a; }
        .po-card { padding: 22px 26px 26px; }
        .po-back { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #2f7cf6; text-decoration: none; margin-bottom: 16px; }
        .po-author { display: flex; align-items: center; gap: 12px; }
        .po-author img, .po-author-empty { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; background: #e2e8f0; flex-shrink: 0; display: block; }
        .po-author-name { font-size: 17px; font-weight: 700; display: flex; align-items: center; gap: 8px; }
        .po-author-meta { font-size: 12px; color: #64748b; }
        .po-date { margin-left: auto; font-size: 13px; color: #64748b; white-space: nowrap; }
        .po-title { font-size: 26px; font-weight: 700; line-height: 1.3; margin-top: 18px; word-break: break-word; }
        .po-stats { display: flex; gap: 20px; font-size: 14px; color: #475569; margin: 14px 0 18px; }
        .po-gallery { position: relative; border-radius: 12px; overflow: hidden; background: #0f172a; aspect-ratio: 16 / 9; }
        .po-gallery img { width: 100%; height: 100%; object-fit: contain; display: block; background: #0f172a; }
        .po-gallery-single { background: #f1f5f9; }
        .po-gallery-single img { object-fit: cover; }
        .po-gal-btn { position: absolute; top: 50%; transform: translateY(-50%); width: 40px; height: 40px; border-radius: 50%; background: rgba(15, 23, 42, 0.75); color: #ffffff; border: 0; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
        .po-gal-btn svg { width: 18px; height: 18px; }
        .po-gal-prev { left: 14px; }
        .po-gal-next { right: 14px; }
        .po-gal-count { position: absolute; right: 14px; top: 12px; font-size: 12px; font-weight: 600; color: #ffffff; background: rgba(15, 23, 42, 0.7); border-radius: 999px; padding: 4px 10px; display: inline-flex; align-items: center; gap: 5px; }
        .po-gal-count svg { width: 13px; height: 13px; }
        .po-thumbs { display: flex; gap: 8px; margin-top: 10px; overflow-x: auto; padding-bottom: 4px; }
        .po-thumb { flex: 0 0 auto; width: 96px; height: 64px; border-radius: 8px; overflow: hidden; border: 2px solid transparent; background: #f1f5f9; padding: 0; cursor: pointer; }
        .po-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
        .po-thumb[aria-current="true"] { border-color: #2f7cf6; }
        .po-actions { display: flex; gap: 10px; margin: 18px 0; padding-bottom: 18px; border-bottom: 1px solid #e2e8f0; flex-wrap: wrap; }
        .po-body { font-size: 15px; color: #1e293b; white-space: pre-line; word-break: break-word; }
        .po-admin { display: inline-block; font-size: 11px; font-weight: 700; color: #b45309; background: #fef3c7; border-radius: 999px; padding: 3px 10px; margin-bottom: 12px; }

        /* ── 하단 앱 CTA ── */
        .lp-cta { display: flex; justify-content: center; gap: 12px; margin: 28px 0 8px; flex-wrap: wrap; }

        @media (max-width: 1000px) {
            .lp-grid { grid-template-columns: 1fr; }
            .st-posts { grid-template-columns: repeat(2, minmax(0, 1fr)); }
            .st-photos { grid-template-columns: repeat(3, minmax(0, 1fr)); }
        }
        @media (max-width: 720px) {
            .lp-wrap { padding: 16px 16px 0; }
            .st-cover { height: auto; }
            .st-cover-inner { flex-direction: column; align-items: flex-start; padding: 20px 16px 18px; gap: 12px; }
            .st-head { flex-direction: column; align-items: flex-start; gap: 12px; padding-bottom: 0; }
            .st-avatar, .st-avatar img, .st-avatar-empty { width: 96px; height: 96px; }
            .st-name { font-size: 24px; }
            .st-actions { padding-bottom: 0; }
            .st-stats { flex-wrap: wrap; gap: 10px; }
            .st-stat { flex: 1 1 45%; border-left: 0; padding: 4px 0; }
            /* 모바일은 커버의 공유 버튼으로 충분 — 통계 바의 공유 버튼이 한 줄을 통째로 차지하지 않게 숨긴다 */
            .st-stats-actions { display: none; }
            .st-posts { grid-template-columns: 1fr; }
            .st-photos { grid-template-columns: repeat(2, minmax(0, 1fr)); }
            .po-card { padding: 18px 16px 20px; }
            .po-title { font-size: 21px; }
            .po-date { margin-left: 0; width: 100%; }
            .po-author { flex-wrap: wrap; }
        }
    </style>
</head>

<body>
<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<c:set var="starIcon" value="<svg viewBox='0 0 24 24' aria-hidden='true'><path fill='currentColor' d='M12 2.5l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.4l-6 3.4 1.3-6.7-5-4.6 6.8-.8z'/></svg>"/>
<c:set var="heartIcon" value="<svg viewBox='0 0 24 24' aria-hidden='true'><path fill='none' stroke='currentColor' stroke-width='2' stroke-linejoin='round' d='M12 20.5s-7.5-4.6-9.2-9.3C1.6 8 3.6 4.5 7 4.5c2 0 3.6 1.1 5 3 1.4-1.9 3-3 5-3 3.4 0 5.4 3.5 4.2 6.7-1.7 4.7-9.2 9.3-9.2 9.3z'/></svg>"/>
<c:set var="commentIcon" value="<svg viewBox='0 0 24 24' aria-hidden='true'><path fill='none' stroke='currentColor' stroke-width='2' stroke-linejoin='round' d='M4 5h16v11H9l-5 4z'/></svg>"/>
<c:set var="photoIcon" value="<svg viewBox='0 0 24 24' aria-hidden='true'><rect x='3' y='5' width='18' height='14' rx='2' fill='none' stroke='currentColor' stroke-width='2'/><path fill='none' stroke='currentColor' stroke-width='2' d='M3 16l5-5 4 4 3-3 6 5'/></svg>"/>
<c:set var="shareIcon" value="<svg viewBox='0 0 24 24' aria-hidden='true'><path fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M12 3v12M7.5 7.5L12 3l4.5 4.5M5 13v6h14v-6'/></svg>"/>
<c:set var="phoneIcon" value="<svg viewBox='0 0 24 24' aria-hidden='true'><rect x='7' y='2.5' width='10' height='19' rx='2' fill='none' stroke='currentColor' stroke-width='2'/><circle cx='12' cy='18' r='1' fill='currentColor'/></svg>"/>

<c:choose>
<%-- ════════════════════ 스타 프로필 ════════════════════ --%>
<c:when test="${landingType eq 'star'}">
    <section class="st-cover">
        <c:if test="${not empty previewImage}"><div class="st-cover-img" style="background-image: url('${previewImage}')" aria-hidden="true"></div></c:if>
        <div class="st-cover-inner">
            <div class="st-head">
                <div class="st-avatar">
                    <c:choose>
                        <c:when test="${not empty previewImage}"><img src="${previewImage}" alt="${previewTitle}" fetchpriority="high" width="128" height="128"></c:when>
                        <c:otherwise><span class="st-avatar-empty" aria-hidden="true"></span></c:otherwise>
                    </c:choose>
                    <c:if test="${not empty starCategoryLabel}"><span class="lp-badge lp-badge-dark">${starIcon}${starCategoryLabel}</span></c:if>
                </div>
                <div>
                    <div class="st-name-row">
                        <h1 class="st-name">${previewTitle}</h1>
                        <c:if test="${not empty starCategoryLabel}"><span class="lp-badge">${starIcon}${starCategoryLabel}</span></c:if>
                    </div>
                    <c:if test="${not empty starBio}"><p class="st-tagline">${starBio}</p></c:if>
                </div>
            </div>
            <div class="st-actions">
                <button type="button" class="lp-btn lp-btn-primary" onclick="openApp()">${phoneIcon}Open in App</button>
                <button type="button" class="lp-icon-btn" onclick="sharePage()" aria-label="Share this page" title="Share">${shareIcon}</button>
            </div>
        </div>
    </section>

    <div class="lp-wrap">
        <div class="lp-grid">
            <div>
                <div class="lp-card st-stats">
                    <div class="st-stat">
                        <span class="st-stat-ico st-stat-ico-gold"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M3 8l4.5 4L12 5l4.5 7L21 8l-1.5 11h-15z"/></svg></span>
                        <span><span class="st-stat-value"><c:choose><c:when test="${not empty statRank}">#<c:out value="${statRank}"/></c:when><c:otherwise>&mdash;</c:otherwise></c:choose></span>
                        <span class="st-stat-label">Global Rank<c:if test="${not empty statTotalStars}"> of <c:out value="${statTotalStars}"/></c:if></span></span>
                    </div>
                    <div class="st-stat">
                        <span class="st-stat-ico"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8" r="3" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="16.5" cy="9.5" r="2.5" fill="none" stroke="currentColor" stroke-width="2"/><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" d="M3.5 18c.5-3 2.7-4.5 5.5-4.5s5 1.5 5.5 4.5M14.5 17.5c.4-2 1.6-3 3.5-3 1.5 0 2.6.7 3 2"/></svg></span>
                        <span><span class="st-stat-value"><c:out value="${empty statFollowers ? 0 : statFollowers}"/></span><span class="st-stat-label">Followers</span></span>
                    </div>
                    <div class="st-stat">
                        <span class="st-stat-ico"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z"/><circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" stroke-width="2"/></svg></span>
                        <span><span class="st-stat-value"><c:out value="${empty statViews ? 0 : statViews}"/></span><span class="st-stat-label">Visitors</span></span>
                    </div>
                    <div class="st-stats-actions">
                        <button type="button" class="lp-icon-btn" onclick="sharePage()" aria-label="Share this page" title="Share">${shareIcon}</button>
                    </div>
                </div>

                <div class="st-tabs" role="tablist" aria-label="Star page sections">
                    <button type="button" class="st-tab" role="tab" aria-selected="true" aria-controls="tabPosts" id="tabBtnPosts">Posts</button>
                    <button type="button" class="st-tab" role="tab" aria-selected="false" aria-controls="tabPhotos" id="tabBtnPhotos">Photos</button>
                    <button type="button" class="st-tab" role="tab" aria-selected="false" aria-controls="tabAbout" id="tabBtnAbout">About</button>
                </div>

                <div class="st-panel" id="tabPosts" role="tabpanel" aria-labelledby="tabBtnPosts">
                    <c:choose>
                        <c:when test="${not empty relatedPosts}">
                            <div class="st-posts">
                                <c:forEach var="rp" items="${relatedPosts}">
                                    <a class="st-post" href="${pageContext.request.contextPath}/post/${rp.conId}">
                                        <span class="st-post-img">
                                            <c:if test="${not empty rp.image}"><img src="${rp.image}" alt="" loading="lazy" onerror="this.style.visibility='hidden'"></c:if>
                                            <c:if test="${rp.mediaCnt gt 1}"><span class="lp-media-cnt">${photoIcon}<c:out value="${rp.mediaCnt}"/></span></c:if>
                                        </span>
                                        <span class="st-post-body">
                                            <span class="st-post-author">
                                                <c:choose><c:when test="${not empty previewImage}"><img src="${previewImage}" alt="" loading="lazy"></c:when><c:otherwise><span class="st-post-author-empty"></span></c:otherwise></c:choose>
                                                <span><span class="st-post-author-name">${previewTitle} <c:if test="${not empty starCategoryLabel}"><span class="lp-badge">${starIcon}${starCategoryLabel}</span></c:if></span>
                                                <c:if test="${not empty rp.date}"><span class="st-post-date">${rp.date}</span></c:if></span>
                                            </span>
                                            <span class="st-post-text">${rp.snippet}</span>
                                            <span class="st-post-stats">
                                                <span class="lp-stat-ico">${heartIcon}<c:out value="${empty rp.likeCnt ? 0 : rp.likeCnt}"/></span>
                                                <span class="lp-stat-ico">${commentIcon}<c:out value="${empty rp.commentCnt ? 0 : rp.commentCnt}"/></span>
                                            </span>
                                        </span>
                                    </a>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise><div class="lp-card lp-empty">No public posts yet.</div></c:otherwise>
                    </c:choose>
                </div>

                <div class="st-panel" id="tabPhotos" role="tabpanel" aria-labelledby="tabBtnPhotos" hidden>
                    <c:set var="photoCount" value="0"/>
                    <div class="st-photos">
                        <c:forEach var="rp" items="${relatedPosts}">
                            <c:if test="${not empty rp.image}">
                                <c:set var="photoCount" value="${photoCount + 1}"/>
                                <a class="st-photo" href="${pageContext.request.contextPath}/post/${rp.conId}"><img src="${rp.image}" alt="" loading="lazy" onerror="this.style.visibility='hidden'"></a>
                            </c:if>
                        </c:forEach>
                    </div>
                    <c:if test="${photoCount eq 0}"><div class="lp-card lp-empty">No photos yet.</div></c:if>
                </div>

                <div class="st-panel lp-card st-about" id="tabAbout" role="tabpanel" aria-labelledby="tabBtnAbout" hidden>
                    <h2>About ${previewTitle}</h2>
                    <c:choose>
                        <c:when test="${not empty starBio}"><p>${starBio}</p></c:when>
                        <c:otherwise><p>This star has not written an introduction yet.</p></c:otherwise>
                    </c:choose>
                    <dl>
                        <c:if test="${not empty starCategoryLabel}"><dt>Category</dt><dd>${starCategoryLabel}</dd></c:if>
                        <c:if test="${not empty statRank}"><dt>Global Rank</dt><dd>#<c:out value="${statRank}"/><c:if test="${not empty statTotalStars}"> of <c:out value="${statTotalStars}"/></c:if></dd></c:if>
                        <dt>Followers</dt><dd><c:out value="${empty statFollowers ? 0 : statFollowers}"/></dd>
                        <dt>Visitors</dt><dd><c:out value="${empty statViews ? 0 : statViews}"/></dd>
                    </dl>
                </div>
            </div>

            <aside class="lp-side">
                <div class="sb-promo">
                    <p class="sb-promo-brand"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="#facc15" d="M12 2.5l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.4l-6 3.4 1.3-6.7-5-4.6 6.8-.8z"/></svg>StarPlatform</p>
                    <p class="sb-promo-title">One Global Network</p>
                    <p class="sb-promo-sub">Your Page. Your World.</p>
                    <a class="lp-btn lp-btn-primary" href="${pageContext.request.contextPath}/about">Explore StarPlatform &rarr;</a>
                </div>

                <c:if test="${not empty relatedPosts}">
                <div class="lp-card sb-block">
                    <div class="sb-head"><h2>More posts</h2></div>
                    <ul class="sb-list">
                        <c:forEach var="rp" items="${relatedPosts}" end="4">
                            <li><a class="sb-item" href="${pageContext.request.contextPath}/post/${rp.conId}">
                                <c:choose><c:when test="${not empty rp.image}"><img src="${rp.image}" alt="" loading="lazy" onerror="this.style.visibility='hidden'"></c:when><c:otherwise><span class="sb-item-empty"></span></c:otherwise></c:choose>
                                <span class="sb-item-text"><span class="sb-item-title">${rp.snippet}</span>
                                <span class="sb-item-meta"><c:if test="${not empty rp.date}"><span>${rp.date}</span></c:if><span class="lp-stat-ico">${heartIcon}<c:out value="${empty rp.likeCnt ? 0 : rp.likeCnt}"/></span></span></span>
                            </a></li>
                        </c:forEach>
                    </ul>
                </div>
                </c:if>

                <c:if test="${not empty relatedStars}">
                <div class="lp-card sb-block">
                    <div class="sb-head"><h2>Related stars</h2></div>
                    <div class="sb-stars">
                        <c:forEach var="st" items="${relatedStars}" end="5">
                            <a class="sb-star" href="${pageContext.request.contextPath}/star/${st.id}">
                                <c:choose><c:when test="${not empty st.image}"><img src="${st.image}" alt="" loading="lazy" onerror="this.style.visibility='hidden'"></c:when><c:otherwise><span class="sb-star-empty"></span></c:otherwise></c:choose>
                                <span class="sb-star-name">${st.name}</span>
                                <span class="sb-star-meta">Followers <c:out value="${empty st.followerCnt ? 0 : st.followerCnt}"/></span>
                            </a>
                        </c:forEach>
                    </div>
                </div>
                </c:if>

                <a class="sb-join" href="#" onclick="openApp(); return false;">
                    ${starIcon}
                    <span><strong>Join StarPlatform</strong><span>Create your own star page today.</span></span>
                    <span class="sb-join-arrow" aria-hidden="true">&rsaquo;</span>
                </a>
            </aside>
        </div>

        <div class="lp-cta">
            <button type="button" class="lp-btn lp-btn-primary" onclick="openApp()">Open in App</button>
            <button type="button" class="lp-btn lp-btn-secondary" onclick="goStore()">Download App</button>
        </div>
    </div>
</c:when>

<%-- ════════════════════ 포스트 상세 ════════════════════ --%>
<c:otherwise>
    <div class="po-band" aria-hidden="true"></div>

    <div class="lp-wrap">
        <div class="lp-grid">
            <article class="lp-card po-card">
                <a class="po-back" href="${pageContext.request.contextPath}/">&larr; Back to posts</a>

                <c:if test="${isAdminPost}"><span class="po-admin">Official Announcement</span></c:if>
                <div class="po-author">
                    <c:if test="${not empty authorId}">
                        <c:choose><c:when test="${not empty authorImage}"><img src="${authorImage}" alt="" onerror="this.style.visibility='hidden'"></c:when><c:otherwise><span class="po-author-empty"></span></c:otherwise></c:choose>
                        <span>
                            <span class="po-author-name"><a href="${pageContext.request.contextPath}/star/${authorId}" style="text-decoration:none">${authorName}</a><c:if test="${not empty authorCategoryLabel}"><span class="lp-badge">${starIcon}${authorCategoryLabel}</span></c:if></span>
                            <span class="po-author-meta">Followers <c:out value="${empty authorFollowers ? 0 : authorFollowers}"/></span>
                        </span>
                    </c:if>
                    <c:if test="${not empty previewDate}"><span class="po-date">${previewDate}</span></c:if>
                </div>

                <h1 class="po-title"><c:choose><c:when test="${not empty postHeadline}">${postHeadline}</c:when><c:otherwise>${previewTitle}</c:otherwise></c:choose></h1>

                <div class="po-stats">
                    <span class="lp-stat-ico">${heartIcon}<c:out value="${empty postLikes ? 0 : postLikes}"/></span>
                    <span class="lp-stat-ico">${commentIcon}<c:out value="${empty postComments ? 0 : postComments}"/></span>
                    <c:if test="${fn:length(mediaUrls) gt 1}"><span class="lp-stat-ico">${photoIcon}<c:out value="${fn:length(mediaUrls)}"/> photos</span></c:if>
                </div>

                <c:choose>
                    <c:when test="${fn:length(mediaUrls) gt 1}">
                        <div class="po-gallery" id="poGallery">
                            <img id="poGalleryImg" src="${mediaUrls[0]}" alt="${previewTitle}" fetchpriority="high">
                            <button type="button" class="po-gal-btn po-gal-prev" id="poGalPrev" aria-label="Previous photo"><svg viewBox="0 0 24 24"><path fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" d="M15 5l-7 7 7 7"/></svg></button>
                            <button type="button" class="po-gal-btn po-gal-next" id="poGalNext" aria-label="Next photo"><svg viewBox="0 0 24 24"><path fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7"/></svg></button>
                            <span class="po-gal-count">${photoIcon}<span id="poGalIndex">1</span> / <c:out value="${fn:length(mediaUrls)}"/></span>
                        </div>
                        <div class="po-thumbs" id="poThumbs">
                            <c:forEach var="t" items="${mediaThumbs}" varStatus="s">
                                <button type="button" class="po-thumb" data-index="${s.index}" ${s.index eq 0 ? 'aria-current="true"' : ''} aria-label="Photo ${s.index + 1}"><img src="${t}" alt="" loading="lazy"></button>
                            </c:forEach>
                        </div>
                        <script>window.__poMedia = [<c:forEach var="u" items="${mediaUrls}" varStatus="s">'<c:out value="${u}"/>'<c:if test="${not s.last}">,</c:if></c:forEach>];</script>
                    </c:when>
                    <c:when test="${not empty previewImage}">
                        <div class="po-gallery po-gallery-single"><img src="${previewImage}" alt="${previewTitle}" fetchpriority="high"></div>
                    </c:when>
                </c:choose>

                <div class="po-actions">
                    <button type="button" class="lp-btn lp-btn-secondary" onclick="sharePage()">${shareIcon}Share</button>
                    <button type="button" class="lp-btn lp-btn-primary" onclick="openApp()">${phoneIcon}Open in App</button>
                </div>

                <c:choose>
                    <c:when test="${not empty postBodyRest}"><p class="po-body">${postBodyRest}</p></c:when>
                    <c:when test="${empty postHeadline and not empty previewBody}"><p class="po-body">${previewBody}</p></c:when>
                </c:choose>
            </article>

            <aside class="lp-side">
                <c:if test="${not empty authorId}">
                <div class="lp-card sb-author">
                    <div class="sb-author-row">
                        <c:choose><c:when test="${not empty authorImage}"><img src="${authorImage}" alt="" loading="lazy" onerror="this.style.visibility='hidden'"></c:when><c:otherwise><span class="sb-author-empty"></span></c:otherwise></c:choose>
                        <span style="min-width:0">
                            <span class="sb-author-name"><a href="${pageContext.request.contextPath}/star/${authorId}" style="text-decoration:none">${authorName}</a><c:if test="${not empty authorCategoryLabel}"><span class="lp-badge">${starIcon}${authorCategoryLabel}</span></c:if></span>
                            <span class="sb-author-meta">Followers <c:out value="${empty authorFollowers ? 0 : authorFollowers}"/></span>
                        </span>
                    </div>
                    <div class="sb-author-stats">
                        <div><strong><c:choose><c:when test="${not empty authorRank}">#<c:out value="${authorRank}"/></c:when><c:otherwise>&mdash;</c:otherwise></c:choose></strong><span>Global Rank</span></div>
                        <div><strong><c:out value="${empty authorFollowers ? 0 : authorFollowers}"/></strong><span>Followers</span></div>
                        <div><strong><c:out value="${empty authorViews ? 0 : authorViews}"/></strong><span>Visitors</span></div>
                    </div>
                    <c:if test="${not empty authorBio}">
                    <div class="sb-author-about"><h3>About ${authorName}</h3><p>${authorBio}</p></div>
                    </c:if>
                    <a class="lp-btn lp-btn-primary" href="${pageContext.request.contextPath}/star/${authorId}" style="display:flex;margin-top:14px">View star page &rarr;</a>
                </div>
                </c:if>

                <c:if test="${not empty relatedPosts}">
                <div class="lp-card sb-block">
                    <div class="sb-head"><h2>More posts</h2><c:if test="${not empty authorId}"><a href="${pageContext.request.contextPath}/star/${authorId}">View all &rarr;</a></c:if></div>
                    <ul class="sb-list">
                        <c:forEach var="rp" items="${relatedPosts}" end="4">
                            <li><a class="sb-item" href="${pageContext.request.contextPath}/post/${rp.conId}">
                                <c:choose><c:when test="${not empty rp.image}"><img src="${rp.image}" alt="" loading="lazy" onerror="this.style.visibility='hidden'"></c:when><c:otherwise><span class="sb-item-empty"></span></c:otherwise></c:choose>
                                <span class="sb-item-text"><span class="sb-item-title">${rp.snippet}</span>
                                <span class="sb-item-meta"><c:if test="${not empty rp.date}"><span>${rp.date}</span></c:if><span class="lp-stat-ico">${heartIcon}<c:out value="${empty rp.likeCnt ? 0 : rp.likeCnt}"/></span><span class="lp-stat-ico">${commentIcon}<c:out value="${empty rp.commentCnt ? 0 : rp.commentCnt}"/></span></span></span>
                            </a></li>
                        </c:forEach>
                    </ul>
                </div>
                </c:if>

                <a class="sb-join" href="#" onclick="openApp(); return false;">
                    ${starIcon}
                    <span><strong>Join StarPlatform</strong><span>Create your own star page today.</span></span>
                    <span class="sb-join-arrow" aria-hidden="true">&rsaquo;</span>
                </a>
            </aside>
        </div>

        <div class="lp-cta">
            <button type="button" class="lp-btn lp-btn-primary" onclick="openApp()">Open in App</button>
            <button type="button" class="lp-btn lp-btn-secondary" onclick="goStore()">Download App</button>
        </div>
    </div>
</c:otherwise>
</c:choose>

<div class="lp-copied" id="lpCopied" role="status" aria-live="polite">Link copied</div>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>

    <script>
        // 공유: Web Share API 가 있으면 시스템 공유, 없으면 링크 복사
        function sharePage() {
            var url = window.location.href.split('#')[0];
            if (navigator.share) {
                navigator.share({ title: document.title, url: url }).catch(function () { });
                return;
            }
            var done = function () {
                var el = document.getElementById('lpCopied');
                if (!el) return;
                el.classList.add('is-on');
                setTimeout(function () { el.classList.remove('is-on'); }, 1600);
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(url).then(done, function () { window.prompt('Copy this link', url); });
            } else {
                window.prompt('Copy this link', url);
            }
        }

        // 스타 페이지 탭 (Posts / Photos / About) — 같은 데이터를 다른 모양으로 보이는 것이라 서버 왕복 없음
        (function () {
            var tabs = document.querySelectorAll('.st-tab');
            if (!tabs.length) return;
            tabs.forEach(function (tab) {
                tab.addEventListener('click', function () {
                    tabs.forEach(function (t) {
                        var on = t === tab;
                        t.setAttribute('aria-selected', String(on));
                        var panel = document.getElementById(t.getAttribute('aria-controls'));
                        if (panel) panel.hidden = !on;
                    });
                });
            });
        })();

        // 포스트 갤러리: 이전/다음·썸네일·키보드 좌우
        (function () {
            var media = window.__poMedia;
            var img = document.getElementById('poGalleryImg');
            if (!media || !img) return;
            var idx = 0;
            var thumbs = document.querySelectorAll('.po-thumb');
            function show(i) {
                idx = (i + media.length) % media.length;
                img.src = media[idx];
                document.getElementById('poGalIndex').textContent = String(idx + 1);
                thumbs.forEach(function (t, k) {
                    if (k === idx) { t.setAttribute('aria-current', 'true'); t.scrollIntoView({ block: 'nearest', inline: 'nearest' }); }
                    else t.removeAttribute('aria-current');
                });
            }
            document.getElementById('poGalPrev').addEventListener('click', function () { show(idx - 1); });
            document.getElementById('poGalNext').addEventListener('click', function () { show(idx + 1); });
            thumbs.forEach(function (t) { t.addEventListener('click', function () { show(parseInt(t.getAttribute('data-index'), 10)); }); });
            document.addEventListener('keydown', function (e) {
                if (e.key === 'ArrowLeft') show(idx - 1);
                if (e.key === 'ArrowRight') show(idx + 1);
            });
        })();
    </script>
    <script>
        var ua = navigator.userAgent.toLowerCase();
        var isKakaoTalk = ua.indexOf('kakaotalk') > -1;
        // 페이스북/인스타그램 인앱 브라우저: 자동 앱 실행이 차단되므로 버튼 클릭 유도만 함
        var isFacebookApp = ua.indexOf('fban') > -1 || ua.indexOf('fbav') > -1 || ua.indexOf('instagram') > -1;
        // 트위터(X) 인앱 브라우저: 자동 스킴 실행이 미설치 iOS에서 "Cannot open page" 얼럿을 띄우므로 제외.
        // 신버전 iOS 트위터는 UA를 노출하지 않아 감지 불가하지만, 버튼 클릭(사용자 제스처) 경로는 정상 동작한다.
        var isTwitterApp = ua.indexOf('twitterandroid') > -1 || ua.indexOf('twitter for iphone') > -1;

        // 모던 iOS/iPadOS 사파리 데스크톱 모드 대응을 포함한 iOS 판정식 (기존 트램폴린과 동일)
        var isIOS = /iphone|ipad|ipod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
        var isAOS = ua.indexOf('android') > -1;

        // iOS 카카오톡: 사파리로 강제 전환해야 Universal Link가 정상 동작 (기존 로직 유지)
        if (isIOS && isKakaoTalk) {
            setTimeout(function () {
                location.href = 'kakaotalk://web/openExternal?url=' + encodeURIComponent(window.location.href);
            }, 300);
        }

        <%-- [AdSense 승인 대기] ③ 광고 게이트 스크립트(콘텐츠 높이 600px 미만 미노출 + no-fill 접힘) 임시 제거.
             승인 후 복원 시 게이트 로직도 함께 되살릴 것 — 짧은 페이지 광고 과다는 재차 정책 위반이 된다. --%>

        var path = window.location.pathname.replace(/^\//, '');
        var search = window.location.search;
        var aosPackage = "kr.co.sensiblenews.witchHuntingVU2D7F2P7E";

        var schemeUrl = "witchhunting://" + path + search;
        // 🌟 디퍼드 딥링크: 스토어 설치 경로에 현재 페이지 경로를 리퍼러로 실어 보냄
        // (앱 설치 후 첫 실행 시 Play Install Referrer로 읽어 원래 페이지로 이동)
        var referrerParam = encodeURIComponent('target_route=/' + path);
        // 안드로이드 intent://: 앱이 없으면 크롬이 알아서 플레이스토어로 보냄 (market_referrer 동반 전달)
        var androidIntent = "intent://" + path + search + "#Intent;scheme=witchhunting;package=" + aosPackage
            + ";S.market_referrer=" + referrerParam + ";end";

        function openApp() {
            if (isAOS) {
                location.href = androidIntent;
            } else {
                // iOS: 커스텀 스킴 시도 후, 화면 전환이 없으면(앱 미설치) 스토어로 이동
                location.href = schemeUrl;
                setTimeout(function () {
                    if (!document.hidden) {
                        goStore();
                    }
                }, 2500);
            }
        }

        function goStore() {
            if (isAOS) {
                location.href = "https://play.google.com/store/apps/details?id=" + aosPackage
                    + "&referrer=" + referrerParam;
            } else {
                location.href = "https://apps.apple.com/kr/app/id1188195403";
            }
        }

        // [AdSense 심사 대응] 진입 즉시 앱을 자동 실행하던 로직 제거 —
        // "이동용 중간 페이지"가 아닌 완결된 콘텐츠 페이지임을 강조하기 위해
        // 앱/스토어 이동은 최하단 버튼 클릭(사용자 제스처)으로만 발생한다.

        // 🌟 방문 카운트: 서버 발급 토큰 + 2.5초 실체류 검증
        // - 토큰은 서버가 렌더링 시 발급(크롤러에겐 미발급)하며, 서버가 발급 경과시간으로 체류 하한을 재검증한다.
        // - 백그라운드 탭은 다시 보일 때 1회만 재시도. localStorage 불가(시크릿 모드 등)면 카운트하지 않는다.
        (function () {
            var token = '${visitToken}';
            if (!token) return;

            var visitorId;
            try {
                visitorId = localStorage.getItem('sp_visitor_id');
                if (!visitorId) {
                    visitorId = (window.crypto && crypto.randomUUID)
                        ? crypto.randomUUID()
                        : 'a' + Date.now().toString(16) + '-' + Math.random().toString(16).slice(2, 10);
                    localStorage.setItem('sp_visitor_id', visitorId);
                }
            } catch (e) { return; }

            var sent = false;
            function send() {
                if (sent) return;
                if (document.hidden) {
                    document.addEventListener('visibilitychange', function h() {
                        document.removeEventListener('visibilitychange', h);
                        setTimeout(send, 300);
                    });
                    return;
                }
                sent = true;
                fetch('${pageContext.request.contextPath}/api/super/landing/visit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ token: token, visitorId: visitorId }),
                    keepalive: true
                }).catch(function () { });
            }
            // 서버 하한 2500ms + 여유 100ms
            setTimeout(send, 2600);
        })();
    </script>
</body>

</html>
