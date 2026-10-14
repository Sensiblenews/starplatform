<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%-- 어드민 인사이트 — 스타 한 명의 그래프 5종 (2-34차 2-2, 요청서 "해당 유저 어드민 이동 → 인사이트 바로 보기").
     스타 계정으로 대리 로그인하지 않고, 스타 본인 마이페이지와 같은 그래프를 여기서 보여 준다 --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>인사이트 · <c:out value="${star.PRS_NAME}"/> - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link href="/resources/css/insight-charts.css" rel="stylesheet">

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
    <script src="/resources/js/insight-charts.js"></script>

    <style>
        body { background-color: #f0f2f5; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; }
        .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
        .star-avatar { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; background: #ddd; }

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

    <div class="main-content">
        <a href="/super/insight/list.do?metric=${fromMetric}&period=${period}" class="text-secondary small"><i class="fas fa-arrow-left me-1"></i>목록으로</a>
        <div class="d-flex align-items-center gap-3 my-3">
            <img class="star-avatar" src="<c:out value="${star.STORED_FILE_NM}"/>" alt="" onerror="this.style.visibility='hidden'">
            <div>
                <h4 class="fw-bold mb-0"><c:out value="${star.PRS_NAME}"/></h4>
                <div class="text-secondary small"><c:out value="${star.PRS_ID}"/> · <c:out value="${star.PRS_COUNTRY}"/></div>
            </div>
        </div>

        <div class="insight-period" id="periodButtons">
            <c:forEach var="opt" items="${fn:split('DAY:매일,WEEK:매주,MONTH:매달,M3:3개월,M6:6개월,Y1:1년', ',')}">
                <c:set var="p" value="${fn:substringBefore(opt, ':')}" />
                <button type="button" data-period="${p}" class="${p eq period ? 'active' : ''}"><c:out value="${fn:substringAfter(opt, ':')}"/></button>
            </c:forEach>
        </div>
        <div class="text-secondary small mb-2">오늘 수치는 실시간, 지난 날짜는 하루 한 번 집계한 값입니다. 순위 그래프는 2-34차 배포일부터 그려집니다.</div>
        <div class="insight-charts" id="insightCharts"></div>
    </div>

    <script>
        var STAR_ID = '<c:out value="${star.PRS_ID}"/>';

        function loadInsight(period) {
            $('#periodButtons button').removeClass('active').filter('[data-period="' + period + '"]').addClass('active');
            $.getJSON('/super/insight/series.json', { id: STAR_ID, period: period }, function (res) {
                if (!res || res.result !== 'OK') {
                    $('#insightCharts').html('<div class="text-danger">' + $('<div>').text((res && res.msg) || '불러오지 못했습니다.').html() + '</div>');
                    return;
                }
                InsightCharts.render(document.getElementById('insightCharts'), res);
            });
        }

        $('#periodButtons').on('click', 'button', function () { loadInsight($(this).data('period')); });
        loadInsight('${period}');
    </script>
</body>
</html>
