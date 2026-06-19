<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String scheme = request.getScheme();
    String serverName = request.getServerName();
    int serverPort = request.getServerPort();
    String portStr = "";
    if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
        portStr = ":" + serverPort;
    }
    String fallbackBaseUrl = "";
    if ("localhost".equals(serverName) || "127.0.0.1".equals(serverName) || serverName.startsWith("192.168.")) {
        fallbackBaseUrl = scheme + "://" + serverName + portStr;
    } else {
        fallbackBaseUrl = "https://witch-hunting.com";
    }
    request.setAttribute("fallbackBaseUrl", fallbackBaseUrl);
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="apple-itunes-app" content="app-id=1188195403">
    <title>${not empty ogTitle ? ogTitle : 'StarPlatform SuperApp'}</title>
    
    <meta property="og:title" content="${not empty ogTitle ? ogTitle : 'StarPlatform SuperApp'}">
    <meta property="og:description" content="${not empty ogDesc ? ogDesc : '대한민국에 이제 백수는 없다. 사진 1장부터 수익창출 시작'}">
    <meta property="og:image" content="${not empty ogImage ? ogImage : fallbackBaseUrl.concat('/resources/img/icon.png')}">
    <meta property="og:url" content="${ogUrl}">
    <meta property="og:type" content="website">

    <!-- Twitter Card Meta Tags -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:title" content="${not empty ogTitle ? ogTitle : 'StarPlatform SuperApp'}">
    <meta name="twitter:description" content="${not empty ogDesc ? ogDesc : '대한민국에 이제 백수는 없다. 사진 1장부터 수익창출 시작'}">
    <meta name="twitter:image" content="${not empty ogImage ? ogImage : fallbackBaseUrl.concat('/resources/img/icon.png')}">

    <style>
        body { text-align: center; padding-top: 50px; font-family: 'Pretendard', sans-serif; color: #333; }
        h3 { font-size: 1.2rem; margin-bottom: 10px; }
        p.desc { font-size: 0.9rem; color: #666; margin-bottom: 30px; }
        
        .spinner { margin: 20px auto; width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #ff4b5c; border-radius: 50%; animation: spin 1s linear infinite; }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
        
        /* 추가된 버튼 스타일 */
        .btn-group { display: flex; flex-direction: column; gap: 10px; align-items: center; margin-top: 20px; }
        .btn { display: block; width: 200px; padding: 14px 0; border-radius: 8px; font-size: 1rem; font-weight: bold; text-decoration: none; cursor: pointer; border: none; }
        .btn-primary { background-color: #ff4b5c; color: white; }
        .btn-secondary { background-color: #eee; color: #555; }
    </style>
</head>
<body>
    <h3>Redirecting to the app...</h3>
    <p class="desc">If nothing happens, please click the button below.</p>
    
    <div class="spinner"></div>

    <div class="btn-group">
        <button class="btn btn-primary" onclick="openApp()">Open in App</button>
        <button class="btn btn-secondary" onclick="goStore()">Download App</button>
    </div>

    <script>
        var ua = navigator.userAgent.toLowerCase();
        var path = window.location.pathname.replace(/^\//, '');
        var targetUrl = '${targetAppUrl}';
        var search = window.location.search;

        // 🌟 1. 유저님이 확인해주신 정확한 패키지명으로 원상 복구!
        var aosPackage = "kr.co.sensiblenews.witchHuntingVU2D7F2P7E";

        // 2. 인텐트 및 스킴 주소 세팅
        var schemeUrl = targetUrl ? targetUrl : "witchhunting://" + path + search;
        var intentPath = targetUrl ? targetUrl.replace("witchhunting://", "") : path + search;
        
        // 🌟 3. 안드로이드 크롬의 마법: intent:// (앱 없으면 크롬이 알아서 스토어로 보냄)
        var androidIntent = "intent://" + intentPath + "#Intent;scheme=witchhunting;package=" + aosPackage + ";end";

        // 앱 열기 수동 버튼용
        function openApp() {
            if (ua.indexOf('android') > -1) {
                location.href = androidIntent;
            } else {
                location.href = schemeUrl;
            }
        }

        // 스토어 이동 함수 (market:// 대신 가장 확실한 https:// 웹 주소 사용)
        function goStore() {
            if (ua.indexOf('android') > -1) {
                location.href = "https://play.google.com/store/apps/details?id=" + aosPackage;
            } else {
                location.href = "https://apps.apple.com/kr/app/id1188195403"; 
            }
        }

        // 🚀 4. 자동 실행 로직
        window.onload = function() {
            var isIOS = ua.indexOf('iphone') > -1 || ua.indexOf('ipad') > -1 || ua.indexOf('ipod') > -1;
            var isAOS = ua.indexOf('android') > -1;

            if (isAOS) {
                // 🤖 안드로이드 (카카오톡, 크롬 등)
                // intent://를 실행하면, 크롬 브라우저가 자체적으로 앱 설치 여부를 판단해서
                // 앱이 없으면 자바스크립트 타이머 없이도 알아서 플레이스토어로 꽂아줍니다!
                location.href = androidIntent;

                // (혹시 모를 삼성 인터넷 등 구형 브라우저를 위한 2초 뒤 스토어 이동 1차 방어선)
                setTimeout(function() {
                    if (!document.hidden) {
                        goStore();
                    }
                }, 2000);
            } 
            else if (isIOS) {
                // 🍎 iOS (이 화면을 본다는 건 앱이 안 깔려 있다는 뜻이므로 조용히 스토어로)
                setTimeout(function() {
                    goStore();
                }, 1500);
            }
        };
    </script>
</body>
</html>