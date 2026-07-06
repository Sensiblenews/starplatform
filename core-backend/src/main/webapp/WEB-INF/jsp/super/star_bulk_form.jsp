<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>스타 일괄 등록 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .main-content { margin-left: 260px; padding: 30px; }
        .form-card { background: white; border-radius: 16px; padding: 40px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); max-width: 900px; margin: 0 auto; }
        .form-label { font-weight: bold; color: #495057; }
        .required::after { content: " *"; color: #dc3545; }
    </style>
</head>
<body>

    <!-- 공통 사이드바 include -->
    <c:set var="activeMenu" value="star_bulk" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2 class="fw-bold">✨ 스타 일괄 등록 (Bulk Create)</h2>
        </div>

        <div class="form-card">
            <form id="bulkStarForm">
                <h5 class="mb-4 border-bottom pb-2">스타 대량 등록 설정</h5>
                
                <div class="row mb-3">
                    <div class="col-md-6">
                        <label for="PRS_COUNTRY" class="form-label required">기본 국가 (Country Code)</label>
                        <select class="form-select" id="PRS_COUNTRY" name="PRS_COUNTRY" required>
                            <option value="KR" selected>🇰🇷 대한민국 (KR)</option>
                            <option value="US">🇺🇸 미국 (US)</option>
                            <option value="JP">🇯🇵 일본 (JP)</option>
                            <option value="GB">🇬🇧 영국 (GB)</option>
                            <option value="FR">🇫🇷 프랑스 (FR)</option>
                            <option value="DE">🇩🇪 독일 (DE)</option>
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label for="PRS_PWD" class="form-label">초기 비밀번호 (Default Password)</label>
                        <input type="text" class="form-control" id="PRS_PWD" name="PRS_PWD" placeholder="기본값: 123" value="123">
                        <div class="form-text text-muted small">입력하지 않으면 기본 비밀번호인 '123'으로 자동 등록됩니다.</div>
                    </div>
                </div>

                <div class="mb-4">
                    <label for="bulkText" class="form-label required">스타 목록 텍스트</label>
                    <textarea class="form-control" id="bulkText" name="bulkText" rows="14" 
                              placeholder="여기에 스타 목록을 붙여넣으세요.&#10;예시:&#10;1. RM&#10;2. 진&#10;3. 슈가&#10;* '(번호). (이름)' 형태 이외의 텍스트는 자동으로 무시됩니다." required></textarea>
                </div>

                <div class="card bg-light border-0 mb-4 p-3">
                    <div class="card-body py-1">
                        <h6 class="fw-bold text-dark"><i class="fas fa-info-circle me-1"></i> 파싱 규칙 설명</h6>
                        <ul class="text-secondary small mb-0 px-3">
                            <li><strong>(번호). (이름)</strong> 패턴의 라인만 자동으로 추출하여 DB에 등록합니다.</li>
                            <li>번호 앞뒤의 공백이나 마침표 뒤의 공백은 알아서 트리밍됩니다.</li>
                            <li>카테고리 헤더(예: <code>=== 🎤 K-POP ===</code>, <code>[BTS]</code>), 빈 줄, 번호가 붙지 않은 임의 텍스트는 <strong>자동으로 스킵(무시)</strong>되므로 텍스트 전체를 통째로 복사해 붙여넣으셔도 됩니다.</li>
                        </ul>
                    </div>
                </div>

                <div class="d-flex justify-content-between align-items-center mt-4">
                    <div>
                        <button type="button" class="btn btn-warning fw-bold text-dark" id="resetPwdBtn">
                            <i class="fas fa-key me-1"></i> 미설정 스타 비번 123 초기화
                        </button>
                    </div>
                    <div class="d-grid gap-2 d-md-flex justify-content-md-end">
                        <button type="button" class="btn btn-secondary me-md-2" onclick="location.href='/super/star/list.do'">취소</button>
                        <button type="submit" class="btn btn-primary px-5 fw-bold" id="submitBtn">
                            <i class="fas fa-file-import me-1"></i> 일괄 등록 시작
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>

<script>
    $(document).ready(function() {
        $('#bulkStarForm').on('submit', function(e) {
            e.preventDefault(); 
            
            const submitBtn = $('#submitBtn');
            submitBtn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>등록 중...');

            $.ajax({
                url: '/super/star/bulk-insert.do',
                type: 'POST',
                data: $(this).serialize(),
                success: function(res) {
                    if (res.status === 'success') {
                        alert(res.msg);
                        location.href = '/super/star/list.do'; 
                    } else {
                        alert('등록 실패: ' + res.msg);
                        submitBtn.prop('disabled', false).html('<i class="fas fa-file-import me-1"></i> 일괄 등록 시작');
                    }
                },
                error: function(err) {
                    console.error(err);
                    alert('서버 통신 중 오류가 발생했습니다.');
                    submitBtn.prop('disabled', false).html('<i class="fas fa-file-import me-1"></i> 일괄 등록 시작');
                }
            });
        });

        // 🌟 미설정 스타 비밀번호 123으로 일괄 복구 버튼 핸들러
        $('#resetPwdBtn').on('click', function() {
            if(!confirm('star_ 로 시작하는 스타 중 비밀번호가 빈 값인 계정들의 비밀번호를 123으로 일괄 초기화하시겠습니까?')) {
                return;
            }
            
            const btn = $(this);
            btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>처리 중...');

            $.ajax({
                url: '/super/star/reset-empty-pwd.do',
                type: 'POST',
                success: function(res) {
                    alert(res.msg);
                    btn.prop('disabled', false).html('<i class="fas fa-key me-1"></i> 미설정 스타 비번 123 초기화');
                },
                error: function(err) {
                    console.error(err);
                    alert('서버 통신 중 오류가 발생했습니다.');
                    btn.prop('disabled', false).html('<i class="fas fa-key me-1"></i> 미설정 스타 비번 123 초기화');
                }
            });
        });
    });
</script>

</body>
</html>
