<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
           ① head 광고 스크립트(여기)  ② FAQ 아래 ins 슬롯  ③ 하단 광고 게이트 스크립트
         복원 코드는 git 이력 참조 — JSP 주석이라 HTML 출력에는 노출되지 않는다.
         복원 시 클라이언트 권장안 반영: ins에 style="display:block;min-height:250px" 적용
         (레이아웃 흔들림 방지). 위치는 기존과 동일하게 FAQ 아래·하단 버튼 위. --%>

    <%-- 애드센스 소유권 확인용 메타 태그 — 광고 코드가 아니므로 심사 중에도 유지 (구글 공식 확인 수단) --%>
    <meta name="google-adsense-account" content="ca-pub-9109251900558498">

    <style>
        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            font-family: 'Pretendard', -apple-system, sans-serif;
            color: #333;
            background: #fafafa;
        }

        .top-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px 16px;
            background: #fff;
            border-bottom: 1px solid #eee;
            position: sticky;
            top: 0;
        }

        .brand {
            font-weight: 800;
            font-size: 1.05rem;
            color: #ff4b5c;
        }

        .top-open {
            font-size: 0.85rem;
            font-weight: bold;
            color: #fff;
            background: #ff4b5c;
            border: none;
            border-radius: 16px;
            padding: 8px 14px;
            cursor: pointer;
        }

        .container {
            max-width: 480px;
            margin: 0 auto;
            padding: 16px;
        }

        .card {
            background: #fff;
            border: 1px solid #eee;
            border-radius: 12px;
            overflow: hidden;
        }

        .hero {
            width: 100%;
            max-height: 300px;
            object-fit: cover;
            display: block;
        }

        .card-body {
            padding: 16px;
        }

        .title {
            font-size: 1.15rem;
            font-weight: 700;
            margin: 0 0 6px;
        }

        .meta {
            font-size: 0.85rem;
            color: #888;
            margin: 0 0 12px;
        }

        .body-text {
            font-size: 0.95rem;
            line-height: 1.6;
            white-space: pre-line;
            word-break: break-word;
            margin: 0;
        }

        /* 관련 콘텐츠 카드: 페이지당 콘텐츠량·내부 링크 확보 (AdSense Thin Content 대응) */
        .related-section {
            margin-top: 20px;
        }

        .related-heading {
            font-size: 0.95rem;
            font-weight: 700;
            color: #333;
            margin: 0 0 10px 4px;
        }

        .related-card {
            display: flex;
            align-items: center;
            gap: 12px;
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
            padding: 12px;
            margin-bottom: 10px;
            text-decoration: none;
        }

        .related-thumb {
            width: 64px;
            height: 64px;
            border-radius: 8px;
            object-fit: cover;
            flex-shrink: 0;
        }

        .related-snippet {
            font-size: 0.85rem;
            line-height: 1.5;
            color: #444;
            margin: 0;
            word-break: break-word;
        }

        /* FAQ: 페이지 고유 콘텐츠 보강 + h2/h3 계층 제공 (AdSense·SEO 대응) */
        .faq-section {
            margin-top: 24px;
            background: #fff;
            border: 1px solid #eee;
            border-radius: 12px;
            padding: 16px;
        }

        .faq-title {
            font-size: 1rem;
            font-weight: 700;
            margin: 0 0 12px;
        }

        .faq-item {
            margin-bottom: 12px;
        }

        .faq-item:last-child {
            margin-bottom: 0;
        }

        .faq-q {
            font-size: 0.9rem;
            font-weight: 600;
            color: #222;
            margin: 0 0 4px;
        }

        .faq-a {
            font-size: 0.85rem;
            line-height: 1.5;
            color: #666;
            margin: 0;
        }

        /* 광고 영역: 높이를 미리 확보해 레이아웃 밀림(CLS) 방지 */
        .ad-wrap {
            margin: 24px 0;
            min-height: 280px;
            background: #f1f1f1;
            border-radius: 8px;
            padding: 6px 8px 8px;
        }

        .ad-label {
            display: block;
            font-size: 0.7rem;
            color: #999;
            letter-spacing: 1px;
            text-transform: uppercase;
            margin-bottom: 4px;
        }

        .btn-group {
            display: flex;
            flex-direction: column;
            gap: 10px;
            align-items: center;
            margin: 24px 0 8px;
        }

        .btn {
            display: block;
            width: 100%;
            max-width: 320px;
            padding: 14px 0;
            border-radius: 8px;
            font-size: 1rem;
            font-weight: bold;
            text-align: center;
            text-decoration: none;
            cursor: pointer;
            border: none;
        }

        .btn-primary {
            background-color: #ff4b5c;
            color: white;
        }

        .btn-secondary {
            background-color: #eee;
            color: #555;
        }

        .footer {
            text-align: center;
            font-size: 0.75rem;
            color: #aaa;
            padding: 16px 0 28px;
        }

        .footer a {
            color: #aaa;
        }
    </style>
</head>

<body>
    <div class="top-bar">
        <span class="brand">StarPlatform</span>
    </div>

    <div class="container">
        <div class="card">
            <!-- previewImage가 비어 있으면 히어로 이미지 생략.
                 LCP 이미지이므로 lazy 없이 fetchpriority=high, width/height로 공간을 예약해 CLS 방지 -->
            ${not empty previewImage ? '<img class="hero" src="'.concat(previewImage).concat('" alt="').concat(previewTitle).concat('" fetchpriority="high" width="480" height="300">') : ''}

            <div class="card-body">
                <h1 class="title">${previewTitle}</h1>

                <!-- 스타 랜딩: 랭크/방문자 요약, 포스트 랜딩: 본문 전문
                     (AdSense '콘텐츠 없는 화면 광고' 정책 판정에 따라 50% 컷·프리뷰 잠금 제거 — 클라이언트 확정) -->
                ${not empty previewMeta ? '<p class="meta">'.concat(previewMeta).concat('</p>') : ''}
                ${not empty previewBody ? '<p class="body-text">'.concat(previewBody).concat('</p>') : ''}
            </div>
        </div>

        <!-- 관련 콘텐츠: 이 스타의 다른 최근 게시물 (내부 링크) -->
        <c:if test="${not empty relatedPosts}">
            <div class="related-section">
                <p class="related-heading">More posts</p>
                <c:forEach var="rp" items="${relatedPosts}">
                    <a class="related-card" href="${pageContext.request.contextPath}/post/${rp.conId}">
                        <c:if test="${not empty rp.image}">
                            <img class="related-thumb" src="${rp.image}" alt="" loading="lazy"
                                onerror="this.style.display='none'">
                        </c:if>
                        <p class="related-snippet">${rp.snippet}</p>
                    </a>
                </c:forEach>
            </div>
        </c:if>

        <!-- FAQ: 광고 위·본문 아래 배치. 광고가 첫 화면에 노출되지 않게 밀어주는 역할도 겸함 -->
        <section class="faq-section">
            <h2 class="faq-title">FAQ</h2>
            <div class="faq-item">
                <h3 class="faq-q">Can I read this content without installing the app?</h3>
                <p class="faq-a">Yes. The full content of this page is available right here on the web.
                    Installing the StarPlatform app adds real-time notifications, comments, and community features.</p>
            </div>
            <div class="faq-item">
                <h3 class="faq-q">What is StarPlatform?</h3>
                <p class="faq-a">StarPlatform is a fan community platform where stars share their latest posts and
                    fans follow their favorite stars, join conversations, and support them.</p>
            </div>
            <div class="faq-item">
                <h3 class="faq-q">How can I follow this star and get updates?</h3>
                <p class="faq-a">Open this page in the StarPlatform app and tap Follow.
                    You will get a notification whenever a new post is shared.</p>
            </div>
        </section>

        <%-- [AdSense 승인 대기] ② 광고 슬롯 임시 제거 — 승인 후 복원 (표식: AdSense 승인 대기) --%>

        <div class="btn-group">
            <button class="btn btn-primary" onclick="openApp()">Open in App</button>
            <button class="btn btn-secondary" onclick="goStore()">Download App</button>
        </div>

        <div class="footer">
            <a href="${pageContext.request.contextPath}/privacy">Privacy Policy</a>
            &middot;
            <a href="${pageContext.request.contextPath}/terms">Terms of Service</a>
        </div>
    </div>

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
