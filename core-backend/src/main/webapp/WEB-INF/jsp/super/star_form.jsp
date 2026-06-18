<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>스타 등록 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; font-size: 1.1rem; }
        .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
        .form-card { background: white; border-radius: 16px; padding: 40px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); max-width: 800px; margin: 0 auto; }
        .form-label { font-weight: bold; color: #495057; }
        .required::after { content: " *"; color: #dc3545; }
    </style>
</head>
<body>

    <div class="sidebar d-flex flex-column p-3">
        <h3 class="text-center mb-5 mt-3 fw-bold">🚀 Super Admin</h3>
        <ul class="nav flex-column">
            <li class="nav-item mb-2">
                <a href="/witch/super/dashboard.do" class="nav-link"><i class="fas fa-chart-pie me-2"></i> 대시보드</a>
            </li>
            <li class="nav-item mb-2">
                <a href="/witch/super/star/list.do" class="nav-link"><i class="fas fa-users-cog me-2"></i> 스타 관리</a>
            </li>
            <li class="nav-item mb-2">
                <a href="/witch/super/star/create.do" class="nav-link active"><i class="fas fa-user-plus me-2"></i> 스타 등록</a>
            </li>
            <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                <li class="nav-item mb-2">
                    <a href="/witch/super/local/create.do" class="nav-link text-warning"><i class="fas fa-user-shield me-2"></i> 지역 관리자 생성</a>
                </li>
                <li class="nav-item mb-2">
                    <a href="/witch/super/report/list.do" class="nav-link" style="color: #ff8a65;">
                        <i class="fas fa-flag me-2"></i> 신고 관리
                    </a>
                </li>
            </c:if>
            <li class="nav-item mt-auto">
                <a href="/witch/super/logout.do" class="nav-link text-danger"><i class="fas fa-sign-out-alt me-2"></i> 로그아웃</a>
            </li>
        </ul>
    </div>

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2 class="fw-bold">✨ 신규 스타 등록</h2>
        </div>

        <div class="form-card">
            <form id="createStarForm">
                <h5 class="mb-4 border-bottom pb-2">기본 정보 입력</h5>
                
                <div class="row mb-3">
                    <div class="col-md-6">
                        <label for="PRS_ID" class="form-label">아이디 (ID)</label>
                        <input type="text" class="form-control bg-light" id="PRS_ID" name="PRS_ID" 
                               placeholder="등록 시 자동 생성됩니다 (예: star_101)" readonly>
                        <div class="form-text text-primary">
                            <i class="fas fa-info-circle"></i> 아이디는 'star_숫자' 형식으로 순차 발급됩니다.
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label for="PRS_PWD" class="form-label required">비밀번호</label>
                        <input type="password" class="form-control" id="PRS_PWD" name="PRS_PWD" placeholder="초기 비밀번호" required>
                    </div>
                </div>

                <div class="mb-3">
                    <label for="PRS_NAME" class="form-label required">스타 이름 (활동명)</label>
                    <input type="text" class="form-control" id="PRS_NAME" name="PRS_NAME" placeholder="활동명 입력" required>
                </div>

                <div class="mb-4">
                    <label for="PRS_COUNTRY" class="form-label">국가 (Country Code)</label>
                    
                    
                    <c:choose>
                        <c:when test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                            <select class="form-select" id="PRS_COUNTRY" name="PRS_COUNTRY">
		                        <option value="KR" selected>🇰🇷 대한민국 (KR)</option>
		                        <option value="US">🇺🇸 미국 (US)</option>
		                        <option value="JP">🇯🇵 일본 (JP)</option>
		                        <option value="GB">🇬🇧 영국 (GB)</option>
		                        <option value="FR">🇫🇷 프랑스 (FR)</option>
		                        <option value="DE">🇩🇪 독일 (DE)</option>
		                    </select>
                        </c:when>
                        <c:otherwise>
                            <input type="text" class="form-control" value="${sessionScope.SUPER_USER_SESSION.PRS_COUNTRY}" disabled>
                            <div class="form-text text-success">
                                <i class="fas fa-check-circle"></i> 관리자님의 관할 지역으로 자동 설정됩니다.
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="d-grid gap-2 d-md-flex justify-content-md-end">
                    <button type="button" class="btn btn-secondary me-md-2" onclick="history.back()">취소</button>
                    <button type="submit" class="btn btn-primary px-5 fw-bold">등록하기</button>
                </div>
            </form>
        </div>
    </div>

<script>
    $(document).ready(function() {
        $('#createStarForm').on('submit', function(e) {
            e.preventDefault(); 

            // [수정] 아이디 유효성 검사 로직 삭제함
            // (아이디는 서버에서 자동 생성되므로 검사할 필요 없음)

            $.ajax({
                url: '/witch/super/star/insert.do',
                type: 'POST',
                data: $(this).serialize(),
                success: function(res) {
                    if (res.status === 'success') {
                        alert(res.msg);
                        location.href = '/witch/super/star/list.do'; 
                    } else {
                        alert('등록 실패: ' + res.msg);
                    }
                },
                error: function(err) {
                    console.error(err);
                    alert('서버 통신 중 오류가 발생했습니다.');
                }
            });
        });
    });
</script>

</body>
</html>