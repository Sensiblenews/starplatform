<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  
  <title>${not empty postTitle ? postTitle : 'StarPlatform'}</title>
  <meta name="description" content="${not empty postDesc ? postDesc : 'StarPlatform - Build your audience and earn through global automated advertising.'}" />
  
  <meta property="og:title" content="${not empty postTitle ? postTitle : 'StarPlatform'}" />
  <meta property="og:description" content="${not empty postDesc ? postDesc : 'Create your page. Grow your audience. Earn globally.'}" />
  <meta property="og:image" content="${not empty postImage ? postImage : 'https://witch-hunting.com/assets/img/defaultImg/avatar.svg'}" />
  <meta property="og:type" content="website" />
  
  <link rel="canonical" href="https://witch-hunting.com${requestScope['javax.servlet.forward.request_uri']}" />

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
    <h1>${not empty postTitle ? postTitle : 'Create. Grow. Earn.'}</h1>
    <p>${not empty postDesc ? postDesc : 'StarPlatform is a global creator platform where anyone can launch a page, attract an audience, and generate revenue through automated advertising.'}</p>
    
    <c:if test="${empty postTitle}">
        <p>Compete in global rankings based on real engagement such as visits, likes, and bookmarks.</p>
        <p>Turn your traffic into real opportunity — and become part of a new digital economy.</p>
    </c:if>

    <div class="buttons">
      <a onclick="openApp()" class="btn btn-primary">Open in App</a>
      <a href="https://play.google.com/store/apps/details?id=kr.co.sensiblenews.witchHuntingVU2D7F2P7E" target="_blank" rel="noopener noreferrer" class="btn btn-secondary">Get it on Google Play</a>
    </div>
  </section>

  <c:if test="${empty postTitle}">
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
  </c:if>

  <footer>
    <p>© 2026 StarPlatform. All rights reserved.</p>
    <div>
      <a href="/witch/privacy">Privacy Policy</a> |
      <a href="/witch/terms">Terms of Service</a>
    </div>
  </footer>
</div>

<script>
  function openApp() {
    const ua = navigator.userAgent.toLowerCase();
    const isAndroid = ua.indexOf("android") > -1;
    const isIOS = /iphone|ipad|ipod/.test(ua);

    // 1. 앱 실행 시도 (커스텀 스킴 사용)
    // Controller에서 postId를 넘겨주면 해당 게시글로, 없으면 로비로 딥링크 조립
    const targetId = '${postId}';
    if (targetId) {
        window.location.href = "witchhunting://post/" + targetId;
    } else {
        window.location.href = "witchhunting://home";
    }

    // 2. 1.5초 후에도 브라우저에 남아있다면(앱이 설치 안됨) OS별 스토어로 강제 이동
    setTimeout(() => {
      if (isIOS) {
        window.location.href = "https://apps.apple.com/kr/app/id1188195403";
      } else if (isAndroid) {
        window.location.href = "https://play.google.com/store/apps/details?id=kr.co.sensiblenews.witchHuntingVU2D7F2P7E";
      }
    }, 1500);
  }

  // 3. 모바일 자동 리디렉션
  window.onload = function() {
    const ua = navigator.userAgent.toLowerCase();
    const isMobile = /android|iphone|ipad|ipod/.test(ua);
    const isBot = /bot|googlebot|crawler|spider|robot|crawling/i.test(ua);

    // 모바일 기기이고, 구글 봇이 아닐 때만 0.8초 후 자동으로 openApp() 실행
    // 0.8초의 딜레이는 구글 봇이 빈 화면으로 인식하는 것을 막기 위함
    if (isMobile && !isBot) {
      setTimeout(openApp, 800);
    }
  };
</script>
</body>
</html>