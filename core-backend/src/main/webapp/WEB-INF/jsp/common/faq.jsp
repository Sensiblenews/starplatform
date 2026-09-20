<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  FAQ 독립 페이지 (/faq).

  문항은 PublicWebFaq 한 곳에만 있고, 화면과 FAQPage JSON-LD를 모두 거기서 만든다.
  이 JSP 에는 문구를 직접 적지 말 것 — 적는 순간 구조화 데이터와 어긋난다.
--%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title>StarPlatform FAQ - Pages, Ranking, Messages, and Earning</title>
  <meta name="description" content="Answers to common questions about StarPlatform: star pages, the global ranking, Daily King, direct messages, advertising revenue, reporting, and using the service without the app." />
  <meta name="robots" content="index, follow" />
  <meta name="google-adsense-account" content="ca-pub-9109251900558498" />

  <meta property="og:title" content="StarPlatform FAQ" />
  <meta property="og:description" content="Star pages, global ranking, messages, advertising, and reporting — answered." />
  <meta property="og:image" content="https://witch-hunting.com/resources/img/icon.png" />
  <meta property="og:type" content="website" />
  <meta property="og:url" content="${canonicalUrl}" />
  <meta name="twitter:card" content="summary" />

  <link rel="canonical" href="${canonicalUrl}" />

  <%-- 화면 본문과 같은 데이터로 만들어진 FAQPage 구조화 데이터 (PublicWebFaq.toJsonLd) --%>
  <script type="application/ld+json">${faqJsonLd}</script>

  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
  <%@ include file="/WEB-INF/jsp/common/include/web-base-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-chrome-style.jsp"%>
</head>
<body>

<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<main class="page-wrap prose">
  <h1 class="page-title">Frequently asked questions</h1>
  <p class="page-lead">How star pages, the global ranking, messages, and advertising revenue work — and what to do when something goes wrong.</p>

  <nav class="toc" aria-label="FAQ sections">
    <c:forEach var="section" items="${faqSections}">
      <a href="#${section.id}"><c:out value="${section.title}"/></a>
    </c:forEach>
  </nav>

  <c:forEach var="section" items="${faqSections}">
    <h2 id="${section.id}"><c:out value="${section.title}"/></h2>
    <c:forEach var="item" items="${section.items}">
      <div class="panel"<c:if test="${not empty item.id}"> id="${item.id}"</c:if>>
        <h3><c:out value="${item.question}"/></h3>
        <p><c:out value="${item.answer}"/></p>
      </div>
    </c:forEach>
  </c:forEach>

  <div class="panel" style="margin-top: 36px;">
    <h3>Still need help?</h3>
    <p>If your question is not answered here, <a href="${pageContext.request.contextPath}/contact">send us a message</a>. For background on how the service works, see <a href="${pageContext.request.contextPath}/about">About StarPlatform</a>.</p>
  </div>
</main>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
</body>
</html>
