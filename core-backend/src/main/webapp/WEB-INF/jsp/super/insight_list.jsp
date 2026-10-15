<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%-- 어드민 인사이트 지표별 순위 목록 (2-34차 2-2). SM 전체 / LC 자국. 행을 누르면 스타별 그래프 --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>인사이트 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link href="/resources/css/insight-charts.css" rel="stylesheet">

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <style>
        body { background-color: #f0f2f5; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; }
        .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
        .table-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        .ins-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; background: #ddd; }
        .ins-row { cursor: pointer; }
        .ins-up { color: #2a9d8f; font-size: 12px; }
        .ins-down { color: #e63946; font-size: 12px; }

        @media (max-width: 768px) {
            .sidebar { width: 100%; height: auto; position: relative; padding-bottom: 10px; }
            .sidebar .nav { flex-direction: row !important; flex-wrap: wrap; justify-content: center; gap: 10px; }
            .sidebar .nav-item { margin-bottom: 0 !important; }
            .main-content { margin-left: 0; padding: 15px; }
        }
    </style>
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <c:choose>
        <c:when test="${metric eq 'VISIT'}"><c:set var="metricTitle" value="방문자수" /><c:set var="valueHead" value="기간 방문" /></c:when>
        <c:when test="${metric eq 'FOLLOW'}"><c:set var="metricTitle" value="즐겨찾기" /><c:set var="valueHead" value="기간 신규 즐겨찾기" /></c:when>
        <c:when test="${metric eq 'LIKE'}"><c:set var="metricTitle" value="좋아요" /><c:set var="valueHead" value="기간 받은 좋아요" /></c:when>
        <c:when test="${metric eq 'POST'}"><c:set var="metricTitle" value="게시물" /><c:set var="valueHead" value="기간 작성 게시물" /></c:when>
        <c:otherwise><c:set var="metricTitle" value="랭킹" /><c:set var="valueHead" value="현재 글로벌 순위" /></c:otherwise>
    </c:choose>

    <div class="main-content">
        <div class="mb-3">
            <h4 class="fw-bold mb-1"><i class="fas fa-chart-line text-primary me-2"></i>인사이트 · <c:out value="${metricTitle}"/></h4>
            <div class="text-secondary small">
                <c:choose>
                    <c:when test="${metric eq 'RANK'}">
                        앱과 같은 글로벌 순위 순서입니다. 변동은 선택한 기간의 시작 무렵 순위와 비교합니다(순위 기록은 2-34차 배포일부터 쌓입니다).
                    </c:when>
                    <c:otherwise>
                        선택한 기간의 합계가 높은 순서입니다. 하루 한 번(00:10) 집계하므로 <b>어제까지</b>의 수치입니다.
                    </c:otherwise>
                </c:choose>
                <c:if test="${isLC}"> 자국 스타만 표시됩니다.</c:if>
                행을 누르면 해당 스타의 인사이트 그래프로 이동합니다.
            </div>
        </div>

        <!-- 기간 -->
        <div class="insight-period">
            <%-- Tomcat 7(EL 2.2)이라 배열 리터럴 대신 "값:라벨" 문자열을 나눈다 --%>
            <c:forEach var="opt" items="${fn:split('DAY:매일,WEEK:매주,MONTH:매달,M3:3개월,M6:6개월,Y1:1년', ',')}">
                <c:set var="p" value="${fn:substringBefore(opt, ':')}" />
                <button type="button" class="${p eq period ? 'active' : ''}"
                        onclick="location.href='/super/insight/list.do?metric=${metric}&period=${p}'"><c:out value="${fn:substringAfter(opt, ':')}"/></button>
            </c:forEach>
        </div>

        <div class="table-card">
            <div class="d-flex justify-content-between mb-2">
                <div class="text-secondary small">총 <fmt:formatNumber value="${total}"/>명</div>
            </div>
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead>
                        <tr>
                            <th style="width:70px;">순서</th>
                            <th>스타</th>
                            <c:if test="${not isLC}"><th>국가</th></c:if>
                            <th class="text-end"><c:out value="${valueHead}"/></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${list}">
                            <tr class="ins-row" onclick="location.href='/super/insight/star.do?id=<c:out value="${row.PRS_ID}"/>&period=${period}&from=${metric}'">
                                <td class="fw-bold">${row.RANK_NO}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <img class="ins-avatar" src="<c:out value="${empty row.STORED_FILE_NM ? '/resources/img/avatar.png' : row.STORED_FILE_NM}"/>" alt="" onerror="this.style.visibility='hidden'">
                                        <div>
                                            <div class="fw-semibold"><c:out value="${row.PRS_NAME}"/></div>
                                            <div class="text-secondary small"><c:out value="${row.PRS_ID}"/></div>
                                        </div>
                                    </div>
                                </td>
                                <c:if test="${not isLC}"><td><c:out value="${row.PRS_COUNTRY}"/></td></c:if>
                                <td class="text-end">
                                    <c:choose>
                                        <c:when test="${metric eq 'RANK'}">
                                            #<fmt:formatNumber value="${row.TOTAL}"/>
                                            <c:if test="${not empty row.START_RANK}">
                                                <c:set var="diff" value="${row.START_RANK - row.TOTAL}" />
                                                <c:if test="${diff > 0}"><span class="ins-up ms-1">▲ <fmt:formatNumber value="${diff}"/></span></c:if>
                                                <c:if test="${diff < 0}"><span class="ins-down ms-1">▼ <fmt:formatNumber value="${-diff}"/></span></c:if>
                                            </c:if>
                                        </c:when>
                                        <c:otherwise><fmt:formatNumber value="${row.TOTAL}"/></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty list}">
                            <tr><td colspan="4" class="text-center text-secondary py-4">조회된 스타가 없습니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>

            <nav class="mt-3">
                <ul class="pagination justify-content-center mb-0">
                    <c:if test="${page > 1}">
                        <li class="page-item"><a class="page-link" href="?metric=${metric}&period=${period}&page=${page - 1}">이전</a></li>
                    </c:if>
                    <c:forEach var="p" begin="${startPage}" end="${endPage}">
                        <li class="page-item <c:if test="${p eq page}">active</c:if>">
                            <a class="page-link" href="?metric=${metric}&period=${period}&page=${p}">${p}</a>
                        </li>
                    </c:forEach>
                    <c:if test="${page < totalPages}">
                        <li class="page-item"><a class="page-link" href="?metric=${metric}&period=${period}&page=${page + 1}">다음</a></li>
                    </c:if>
                </ul>
            </nav>
        </div>
    </div>
</body>
</html>
