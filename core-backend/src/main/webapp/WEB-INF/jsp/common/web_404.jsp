<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  공개 사이트 404.

  상태 코드는 컨트롤러가 404 로 세운다 (PublicWebController.notFound).
  여기서 200 이 나가면 Soft 404 가 되어 없는 페이지가 색인된다.
  canonical 은 걸지 않는다 — 존재하지 않는 페이지를 다른 URL 의 정본으로 가리킬 수 없다.
--%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title>Page not found | StarPlatform</title>
  <meta name="description" content="The page you were looking for is not available." />
  <meta name="robots" content="noindex, follow" />

  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
  <%@ include file="/WEB-INF/jsp/common/include/web-base-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-chrome-style.jsp"%>
</head>
<body>

<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<main class="page-wrap prose">
  <h1 class="page-title">This page is not available</h1>
  <p class="page-lead">The address you followed does not point to anything on StarPlatform. It may have been removed, or the link may be incomplete.</p>

  <h2>Where to go next</h2>
  <ul>
    <li><a href="${pageContext.request.contextPath}/">Home</a> — the latest posts and popular star pages</li>
    <li><a href="${pageContext.request.contextPath}/posts">Public posts</a> — everything published so far</li>
    <li><a href="${pageContext.request.contextPath}/about">About StarPlatform</a> — what the service does</li>
    <li><a href="${pageContext.request.contextPath}/faq">FAQ</a> — answers to common questions</li>
    <li><a href="${pageContext.request.contextPath}/contact">Contact us</a> — tell us if a link on our site is broken</li>
  </ul>
</main>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
</body>
</html>
