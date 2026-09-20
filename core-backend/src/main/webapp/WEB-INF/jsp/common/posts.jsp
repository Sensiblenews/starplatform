<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  공개 포스트 목록 (/posts).

  루트 허브가 최근 8건만 보여주던 자리를 페이지 단위로 넓힌 화면이다.
  크롤러에게는 승인된 글 전체로 가는 경로가 되고, 방문자에게는 목차가 된다.
--%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title><c:choose><c:when test="${page gt 1}">Public Posts - Page ${page} | StarPlatform</c:when><c:otherwise>Public Posts on StarPlatform</c:otherwise></c:choose></title>
  <meta name="description" content="Browse public posts published by stars, creators, brands, and organizations on StarPlatform. Open any post to read it in full, no account required." />
  <meta name="robots" content="index, follow" />
  <meta name="google-adsense-account" content="ca-pub-9109251900558498" />

  <meta property="og:title" content="Public Posts on StarPlatform" />
  <meta property="og:description" content="The latest public posts from star pages around the world." />
  <meta property="og:image" content="https://witch-hunting.com/resources/img/icon.png" />
  <meta property="og:type" content="website" />
  <meta property="og:url" content="${canonicalUrl}" />
  <meta name="twitter:card" content="summary" />

  <link rel="canonical" href="${canonicalUrl}" />
  <%-- 페이지 관계를 명시해 2페이지 이후가 고아 페이지로 남지 않게 한다 --%>
  <c:if test="${not empty prevUrl}"><link rel="prev" href="${prevUrl}" /></c:if>
  <c:if test="${not empty nextUrl}"><link rel="next" href="${nextUrl}" /></c:if>

  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
  <%@ include file="/WEB-INF/jsp/common/include/web-base-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-chrome-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-card-style.jsp"%>
  <style>
    .pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 36px; }
    .pager span { font-size: 14px; color: #64748b; }
    .list-meta { font-size: 14px; color: #64748b; margin-top: 6px; }
  </style>
</head>
<body>

<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<main class="page-wrap page-wrap-wide">
  <h1 class="page-title">Public posts</h1>
  <p class="page-lead">Posts published on star pages across StarPlatform. Anyone can read them here — no account and no app required.</p>
  <c:if test="${totalCount gt 0}">
    <p class="list-meta"><c:out value="${totalCount}"/> posts published &middot; page <c:out value="${page}"/> of <c:out value="${lastPage}"/></p>
  </c:if>

  <c:choose>
    <c:when test="${not empty postCards}">
      <%@ include file="/WEB-INF/jsp/common/include/web-post-cards.jsp"%>

      <nav class="pager" aria-label="Post pages">
        <c:if test="${not empty prevUrl}"><a class="btn btn-secondary" href="${prevUrl}" rel="prev">&larr; Newer</a></c:if>
        <span>Page <c:out value="${page}"/> of <c:out value="${lastPage}"/></span>
        <c:if test="${not empty nextUrl}"><a class="btn btn-secondary" href="${nextUrl}" rel="next">Older &rarr;</a></c:if>
      </nav>
    </c:when>
    <c:otherwise>
      <%-- 목록 조회 실패나 승인된 글이 아직 없을 때. 빈 화면 대신 갈 곳을 준다 --%>
      <div class="panel" style="margin-top: 28px;">
        <h3>No posts to show yet</h3>
        <p>Published posts appear here once they have been reviewed. In the meantime, read <a href="${pageContext.request.contextPath}/about">about StarPlatform</a> or browse the <a href="${pageContext.request.contextPath}/">home page</a>.</p>
      </div>
    </c:otherwise>
  </c:choose>
</main>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
</body>
</html>
