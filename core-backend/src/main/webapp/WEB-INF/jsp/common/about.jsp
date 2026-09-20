<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  서비스 소개 독립 페이지 (/about).

  여기 적힌 기능은 전부 현재 앱·웹에 실제로 있는 것만 쓴다. 소개 페이지의 설명과
  실제 서비스가 어긋나면 그 자체가 광고 심사 감점 요인이다. 클라이언트 요청서에
  있던 항목 중 확인되지 않은 것(기업 글로벌 판매채널, 글로벌 파트너 플랫폼 확장,
  '007 Chat' 명칭)은 의도적으로 뺐다 — 구현이 확인되면 추가한다.
--%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title>About StarPlatform - How the Global Creator Platform Works</title>
  <meta name="description" content="StarPlatform gives every creator, celebrity, brand, and organization a public page, a place in a single global ranking, and a way to earn from the traffic they attract." />
  <meta name="robots" content="index, follow" />
  <meta name="google-adsense-account" content="ca-pub-9109251900558498" />

  <meta property="og:title" content="About StarPlatform" />
  <meta property="og:description" content="One page, one global ranking, one way to earn. Here is how StarPlatform works." />
  <meta property="og:image" content="https://witch-hunting.com/resources/img/icon.png" />
  <meta property="og:type" content="website" />
  <meta property="og:url" content="${canonicalUrl}" />
  <meta name="twitter:card" content="summary" />

  <link rel="canonical" href="${canonicalUrl}" />

  <script type="application/ld+json">
  {"@context":"https://schema.org","@type":"AboutPage","name":"About StarPlatform","url":"https://witch-hunting.com/about","isPartOf":{"@type":"WebSite","name":"StarPlatform","url":"https://witch-hunting.com/"}}
  </script>

  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
  <%@ include file="/WEB-INF/jsp/common/include/web-base-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-chrome-style.jsp"%>
</head>
<body>

<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<main class="page-wrap prose">
  <h1 class="page-title">About StarPlatform</h1>
  <p class="page-lead">StarPlatform is a global creator platform. Every member gets a public page, a position in one worldwide ranking, and a share of the advertising revenue their audience generates.</p>

  <h2 id="what-it-is">What StarPlatform is</h2>
  <p>Most social networks measure you against the people you already know. StarPlatform measures every page against every other page on the service, in a single list that runs across countries and categories. A student in Seoul, a musician in Lagos, a city tourism office in Lisbon, and a news desk in Toronto all appear in the same ranking and are judged by the same numbers.</p>
  <p>The service is built around one simple unit: a page. You publish to it, people visit it, and the traffic it attracts is what moves you up the ranking and what generates your earnings. There is no algorithmic feed deciding who deserves to be seen — the ranking is public, and the inputs to it are public.</p>
  <p>StarPlatform does not ask you to leave the networks you already use. Posts carry share links that work on any platform, and every public page and post on StarPlatform is readable on the open web without an account or an app.</p>

  <h2 id="star-pages">Star pages</h2>
  <p>A page on StarPlatform is called a <strong>star page</strong>. It holds your profile, your posts, your follower count, your visitor count, and your current global rank. Each page has a permanent public address that you can share anywhere, and the page you share is the page a visitor sees — no login wall, no app install prompt in the way of the content.</p>
  <p>Pages are open to more than individuals. The service recognises distinct page types for individual stars, celebrities, brands, organizations, universities, cities, and media outlets, and shows that type on the page so visitors know who they are reading.</p>

  <h2 id="ranking">The global ranking</h2>
  <p>Every star page competes in one ranking. Position is calculated from real engagement — page visits, likes, bookmarks, and followers — and it updates continuously as those numbers move. Several views of the ranking are available in the app:</p>
  <ul>
    <li><strong>Global Ranking</strong> — the full standing, with a TOP 100 view of the highest-ranked pages on the service.</li>
    <li><strong>Daily King</strong> — the page that gathered the most engagement over the last 24 hours. It resets every day, so a page that is not yet large can still take the top spot.</li>
    <li><strong>Hall of Fame</strong> — pages that have held a leading position over time, kept as a permanent record.</li>
    <li><strong>Revenue Ranking</strong> — a standing based on advertising revenue earned rather than raw attention.</li>
  </ul>
  <p>Because the ranking is driven by visits, sharing your page off-platform counts. Traffic you bring from anywhere on the internet is traffic the ranking sees.</p>

  <h2 id="vs">VS cards</h2>
  <p>A VS card puts two star pages side by side as a head-to-head matchup. Visitors decide the outcome, and the result feeds back into both pages. It is a lightweight way for a page to get in front of another page's audience, and for visitors to discover pages they would not have searched for.</p>

  <h2 id="posts">Public posts</h2>
  <p>Posts are the content of a star page: text, photos, and the comments and likes they collect. Public posts are published to the web as well as the app, which means they can be found in search engines, shared into group chats, and read by anyone who follows the link.</p>
  <p>You can browse what has been published recently on the <a href="${pageContext.request.contextPath}/posts">public posts page</a>, and open any page or post from there.</p>

  <h2 id="messages">Direct messages</h2>
  <p>Members can message each other directly in the app. Every conversation carries reporting and blocking controls: a message can be reported to the operators with its content preserved for review, and a member can be blocked so that no further messages arrive. Reported conversations are reviewed by the operations team, and accounts that break the rules are suspended.</p>

  <h2 id="earning">How earning works</h2>
  <p>Advertising on StarPlatform is automated. Ads are placed by the platform against the traffic a page receives, and the revenue attributable to a page accrues to the person who runs it. You do not negotiate with advertisers, set rates, or manage campaigns.</p>
  <p>What this rewards is genuine attention. A page with real visitors who stay and read earns; a page with inflated numbers does not, and traffic that does not pass the platform's validity checks is not counted. Earnings and the revenue ranking are visible in the app.</p>

  <h2 id="who">Who uses StarPlatform</h2>
  <ul>
    <li><strong>Creators and individuals</strong> — a public home for your work that is not tied to one network's feed.</li>
    <li><strong>Celebrities and public figures</strong> — a verified page and a measurable standing.</li>
    <li><strong>Brands and businesses</strong> — a page that ranks on the same terms as everyone else, in front of an international audience.</li>
    <li><strong>Universities, organizations, and institutions</strong> — a channel for announcements that reaches beyond a mailing list.</li>
    <li><strong>Cities and tourism offices</strong> — a destination page that competes for global attention.</li>
    <li><strong>Media and news desks</strong> — a distribution point whose reach is publicly measurable.</li>
  </ul>

  <h2 id="start">Getting started</h2>
  <p>Reading is open to everyone: public pages and posts on this site require no account. To publish, install the StarPlatform app, create your page, and start posting. Your page is live and ranked from the first post.</p>

  <div class="panel" style="margin-top: 28px;">
    <h3>Next steps</h3>
    <p>Browse the <a href="${pageContext.request.contextPath}/posts">latest public posts</a>, read the <a href="${pageContext.request.contextPath}/faq">frequently asked questions</a>, or <a href="${pageContext.request.contextPath}/contact">get in touch</a> if you have a question we have not answered.</p>
    <p style="margin-top: 16px;"><a class="btn btn-primary" href="#" onclick="spOpenApp(); return false;">Open in App</a></p>
  </div>
</main>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
</body>
</html>
