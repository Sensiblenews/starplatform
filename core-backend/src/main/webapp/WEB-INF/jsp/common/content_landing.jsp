<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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

        /* 미리보기 하단 페이드아웃: 본문이 이어진다는 시각적 신호 */
        .fade {
            height: 56px;
            margin-top: -56px;
            position: relative;
            background: linear-gradient(rgba(255, 255, 255, 0), #fff);
        }

        .lock-card {
            text-align: center;
            padding: 20px 16px;
            border-top: 1px dashed #ddd;
            background: #fffdf5;
        }

        .lock-card p {
            margin: 4px 0;
            font-size: 0.9rem;
            color: #666;
        }

        .lock-card .lock-title {
            font-size: 1rem;
            font-weight: 700;
            color: #333;
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

                <!-- 스타 랜딩: 랭크/방문자 요약, 포스트 랜딩: 서버에서 절반만 잘라 온 본문 -->
                ${not empty previewMeta ? '<p class="meta">'.concat(previewMeta).concat('</p>') : ''}
                ${not empty previewBody ? '<p class="body-text">'.concat(previewBody).concat('</p><div class="fade"></div>') : ''}
            </div>

            <div class="lock-card">
                <p class="lock-title">&#128274; This is a preview</p>
                <p>
                    ${landingType == 'post'
                        ? 'Continue in the app to read the full post.'
                        : 'Open the app to see all updates and follow this star.'}
                </p>
            </div>
        </div>

        <!-- 광고 (AdSense) -->
        <div class="ad-wrap" id="adWrap">
            <span class="ad-label">Advertisement</span>
            <ins class="adsbygoogle" style="display:block" data-ad-client="ca-pub-9109251900558498"
                data-ad-slot="3314400335" data-ad-format="auto" data-full-width-responsive="true"></ins>
        </div>

        <div class="btn-group">
            <button class="btn btn-primary" onclick="goStore()">Download App</button>
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

        function goStore() {
            if (isAOS) {
                location.href = "https://play.google.com/store/apps/details?id=" + aosPackage;
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
            if (isFacebookApp) return;              // 페북/인스타: 자동 실행 차단 환경
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
    </script>
</body>

</html>
