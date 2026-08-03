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

    <!-- Google AdSense (CMP 지역 팝업은 AdSense Privacy & Messaging 설정으로 자동 처리됨) -->
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-9109251900558498"
        crossorigin="anonymous"></script>

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
            <!-- previewImage가 비어 있으면 히어로 이미지 생략 -->
            ${not empty previewImage ? '<img class="hero" src="'.concat(previewImage).concat('" alt="">') : ''}

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

        <!-- 광고 (AdSense) -->
        <div class="ad-wrap" id="adWrap">
            <span class="ad-label">Advertisement</span>
            <ins class="adsbygoogle" style="display:block" data-ad-client="ca-pub-9109251900558498"
                data-ad-slot="3314400335" data-ad-format="auto" data-full-width-responsive="true"></ins>
        </div>

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

        // 광고 로드 실패(차단 포함) 시 빈 회색 박스가 남지 않도록 영역을 접음
        try {
            (adsbygoogle = window.adsbygoogle || []).push({});
        } catch (e) { }
        setTimeout(function () {
            var ins = document.querySelector('ins.adsbygoogle');
            if (!ins || ins.getAttribute('data-ad-status') !== 'filled') {
                var wrap = document.getElementById('adWrap');
                if (wrap) wrap.style.display = 'none';
            }
        }, 4000);

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

        // 🌟 설치 유저 자동 앱 실행 (1회만 시도)
        // - 인앱 브라우저(카카오톡/트위터 등)에서는 OS의 App Link / Universal Link가 발동하지 않으므로
        //   랜딩 진입 시 스크립트로 앱 실행을 시도한다.
        // - 미설치 유저는 스토어로 보내지 않고 랜딩에 남긴다 (콘텐츠 + 광고 노출이 요구사항).
        //   안드로이드: intent://의 fallback URL을 현재 랜딩으로 지정해 미설치 시 랜딩으로 복귀.
        //   iOS: 커스텀 스킴 1회 시도 후 실패해도 추가 이동 없음.
        // - load 이벤트를 기다리면 광고·이미지 로딩만큼 앱 실행이 늦어지므로 즉시 실행한다.
        (function () {
            if (isFacebookApp || isTwitterApp) return; // 페북/인스타/트위터: 자동 실행 차단·오동작 환경 (버튼 클릭으로 유도)
            if (isIOS && isKakaoTalk) return;       // iOS 카카오톡: 위의 사파리 강제 전환이 처리

            var alreadyTried = false;
            try {
                alreadyTried = sessionStorage.getItem('sp_auto_open_tried') === '1';
                sessionStorage.setItem('sp_auto_open_tried', '1');
            } catch (e) { }
            if (alreadyTried) return;               // 미설치 폴백으로 재진입한 경우 반복 시도 방지

            if (isAOS) {
                var fallback = encodeURIComponent(window.location.href);
                location.href = "intent://" + path + search
                    + "#Intent;scheme=witchhunting;package=" + aosPackage
                    + ";S.browser_fallback_url=" + fallback + ";end";
            } else if (isIOS) {
                location.href = schemeUrl;
            }
        })();

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
