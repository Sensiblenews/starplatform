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
    </style>
</head>
<body>

    <div class="sidebar d-flex flex-column p-3">
        <h3 class="text-center mb-5 mt-3 fw-bold">🚀 Super Admin</h3>
        <ul class="nav flex-column">
            <li class="nav-item mb-2">
                <a href="/witch/super/dashboard.do" class="nav-link active"><i class="fas fa-chart-pie me-2"></i> 대시보드</a>
            </li>
            <li class="nav-item mb-2">
                <a href="/witch/super/star/list.do" class="nav-link"><i class="fas fa-users-cog me-2"></i> 스타 관리</a>
            </li>
            <li class="nav-item mb-2">
                <a href="/witch/super/star/create.do" class="nav-link"><i class="fas fa-user-plus me-2"></i> 스타 등록</a>
            </li>
            
            <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                <li class="nav-item mb-2">
                    <a href="/witch/super/local/create.do" class="nav-link text-warning"><i class="fas fa-user-shield me-2"></i> 지역 관리자 생성</a>
                </li>
            </c:if>
            
            <li class="nav-item mt-auto">
                <a href="/witch/super/logout.do" class="nav-link text-danger"><i class="fas fa-sign-out-alt me-2"></i> 로그아웃</a>
            </li>
        </ul>
    </div>

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

        <div class="row g-4 mb-5">
            <div class="col-md-4">
                <div class="card stat-card bg-primary text-white p-4">
                    <h5><i class="fas fa-users me-2"></i>
                        ${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM' ? '총 등록 스타' : '지역 등록 스타'}
                    </h5>
                    <h2 class="mt-2 fw-bold display-6 text-end">${stats.starCount} 명</h2>
                </div>
            </div>
            
            <div class="col-md-4">
                <div class="card stat-card bg-success text-white p-4">
                    <h5><i class="fas fa-eye me-2"></i>총 광고 시청 수</h5>
                    <h2 class="mt-2 fw-bold display-6 text-end">
                        <fmt:formatNumber value="${stats.totalAdViews}" pattern="#,###" /> 회
                    </h2>
                </div>
            </div>
            
            <div class="col-md-4">
                <div class="card stat-card bg-info text-white p-4">
                    <h5><i class="fas fa-mobile-alt me-2"></i>월간 활성 유저(MAU)</h5>
                    <h2 class="mt-2 fw-bold display-6 text-end">
                        <fmt:formatNumber value="${stats.activeUserCount}" pattern="#,###" /> 명
                    </h2>
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