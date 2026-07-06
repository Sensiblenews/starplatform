<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Super Admin Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; font-size: 1.1rem; }
        .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
        .stat-card { border: none; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); transition: transform 0.2s; }
        .stat-card:hover { transform: translateY(-5px); }
        .chart-box { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); height: 100%; }
        .chat-bubble { max-width: 75%; padding: 10px 14px; border-radius: 15px; margin-bottom: 10px; font-size: 14px; }
		.chat-user { background: #e9ecef; color: #333; align-self: flex-start; border-bottom-left-radius: 2px; }
		.chat-admin { background: #3880ff; color: #fff; align-self: flex-end; border-bottom-right-radius: 2px; }
		.chat-row { display: flex; flex-direction: column; }
		.chat-time { font-size: 11px; color: #999; margin-top: 2px; }
    </style>
</head>
<body>
    <!-- 공통 사이드바 include -->
    <c:set var="activeMenu" value="dashboard" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4">
            
            <h2 class="fw-bold">
                <c:choose>
                    <c:when test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                        🌐 총괄 현황판 (Global Dashboard)
                    </c:when>
                    <c:otherwise>
                        🏳️ 지역 현황판 (${sessionScope.SUPER_USER_SESSION.PRS_COUNTRY})
                    </c:otherwise>
                </c:choose>
            </h2>

            <span class="badge bg-dark p-2">
                접속자: 
                <c:choose>
                    <c:when test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                        총괄관리자 (SM)
                    </c:when>
                    <c:otherwise>
                        지역관리자 (LC - ${sessionScope.SUPER_USER_SESSION.PRS_COUNTRY})
                    </c:otherwise>
                </c:choose>
            </span>
        </div>

        <div class="row g-3 mb-4">
            <div class="col-md-3">
                <div class="card stat-card bg-primary text-white p-3 h-100">
                    <h6 class="opacity-75"><i class="fas fa-user-clock me-2"></i>DAU (일간 접속)</h6>
                    <h2 class="mt-2 fw-bold text-end"><fmt:formatNumber value="${stats.DAU}" pattern="#,###" /> 명</h2>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card bg-info text-white p-3 h-100">
                    <h6 class="opacity-75"><i class="fas fa-users me-2"></i>MAU (월간 접속)</h6>
                    <h2 class="mt-2 fw-bold text-end"><fmt:formatNumber value="${stats.MAU}" pattern="#,###" /> 명</h2>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card bg-success text-white p-3 h-100">
                    <h6 class="opacity-75"><i class="fas fa-user-plus me-2"></i>신규 가입 (오늘)</h6>
                    <h2 class="mt-2 fw-bold text-end"><fmt:formatNumber value="${stats.newSignups}" pattern="#,###" /> 명</h2>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card bg-warning text-dark p-3 h-100">
                    <h6 class="opacity-75"><i class="fas fa-sync-alt me-2"></i>재방문율 (Retention)</h6>
                    <h2 class="mt-2 fw-bold text-end">${stats.retentionRate}%</h2>
                </div>
            </div>
        </div>
        
        

        <!-- 누적 지표 및 🌟서버 상태 모니터링 영역 -->
        <div class="row g-3 mb-4">
            <!-- 총 등록 스타 -->
            <div class="col-md-4">
                <div class="card stat-card bg-dark text-white p-3 h-100">
                    <h6 class="opacity-75"><i class="fas fa-star me-2"></i>${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM' ? '총 등록 스타' : '지역 등록 스타'}</h6>
                    <h4 class="mt-1 fw-bold text-end"><fmt:formatNumber value="${stats.starCount}" pattern="#,###" /> 명</h4>
                </div>
            </div>
            <!-- 누적 광고 시청 수 -->
            <div class="col-md-4">
                <div class="card stat-card bg-secondary text-white p-3 h-100">
                    <h6 class="opacity-75"><i class="fas fa-eye me-2"></i>총 누적 광고 시청 수</h6>
                    <h4 class="mt-1 fw-bold text-end"><fmt:formatNumber value="${stats.totalAdViews}" pattern="#,###" /> 회</h4>
                </div>
            </div>
            
            <!-- 🌟 [신규] 서버 상태 모니터링 -->
            <div class="col-md-4">
                <div class="card stat-card bg-white border-0 p-3 h-100" style="border-left: 4px solid #36b9cc !important;">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6 class="text-muted fw-bold mb-0"><i class="fas fa-server me-2"></i>서버 상태 모니터링</h6>
                        <div>
                            <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                                <a href="/super/system/panel.do" class="btn btn-outline-info btn-sm px-2 py-0 me-2" style="font-size:11px; border-radius: 4px;"><i class="fas fa-cog me-1"></i>상세/정리</a>
                            </c:if>
                            <span class="badge bg-success"><i class="fas fa-database me-1"></i>DB: ${stats.dbStatus}</span>
                        </div>
                    </div>
                    
                    <div class="mt-2">
                        <div class="d-flex justify-content-between text-sm mb-1">
                            <span class="text-secondary small">메모리 사용량</span>
                            <span class="fw-bold small text-primary">${stats.serverUsedMem}MB / ${stats.serverMaxMem}MB</span>
                        </div>
                        <div class="progress" style="height: 8px;">
                            <div class="progress-bar ${stats.serverMemPercent >= 80 ? 'bg-danger' : (stats.serverMemPercent >= 60 ? 'bg-warning' : 'bg-info')}" 
                                 role="progressbar" 
                                 style="width: ${stats.serverMemPercent}%" 
                                 aria-valuenow="${stats.serverMemPercent}" aria-valuemin="0" aria-valuemax="100"></div>
                        </div>
                        <div class="text-end mt-1">
                            <span class="small text-muted">${stats.serverMemPercent}% 사용중</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                <div class="col-md-5">
                    <div class="chart-box">
                        <h5 class="fw-bold mb-4">🌍 국가별 스타 분포</h5>
                        <canvas id="countryChart" height="250"></canvas>
                    </div>
                </div>
            </c:if>
            
            <div class="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM' ? 'col-md-7' : 'col-md-12'}">
                <div class="chart-box">
                    <h5 class="fw-bold mb-4">📈 월별 시청 추이 (최근 6개월)</h5>
                    <canvas id="revenueChart" height="170"></canvas>
                </div>
            </div>
        </div>
    </div>


<script>
    // 1. 국가별 데이터 (SM 전용)
    const countryLabels = [];
    const countryData = [];
    
    <c:forEach items="${stats.countryStats}" var="item">
        countryLabels.push("${item.LABEL}");
        countryData.push(${item.VALUE});
    </c:forEach>
    
    // 데이터가 없으면 더미 데이터 표시
    if(countryLabels.length === 0 && "${sessionScope.SUPER_USER_SESSION.PRS_AUTH}" === "SM") {
        countryLabels.push('데이터 없음');
        countryData.push(0);
    }

    // 차트 엘리먼트가 있을 때만 렌더링 (LC일 경우 에러 방지)
    const countryChartCtx = document.getElementById('countryChart');
    if (countryChartCtx) {
        new Chart(countryChartCtx, {
            type: 'doughnut',
            data: {
                labels: countryLabels,
                datasets: [{
                    data: countryData,
                    backgroundColor: ['#4e73df', '#1cc88a', '#36b9cc', '#f6c23e'],
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { position: 'right' } }
            }
        });
    }

    // 2. 월별 시청 추이 데이터
    const monthLabels = [];
    const monthData = [];

    <c:forEach items="${stats.monthlyStats}" var="item">
        monthLabels.push("${item.LABEL}"); 
        monthData.push(${item.VALUE});
    </c:forEach>

    if (monthLabels.length === 0) {
        monthLabels.push('데이터 없음');
        monthData.push(0);
    }

    new Chart(document.getElementById('revenueChart'), {
        type: 'bar',
        data: {
            labels: monthLabels,
            datasets: [{
                label: '광고 시청 수 (단위: 회)',
                data: monthData,
                backgroundColor: '#36b9cc',
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            scales: { 
                y: { beginAtZero: true, ticks: { precision: 0 } } 
            }
        }
    });
</script>

</body>
</html>