<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>지역 관리자 생성</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style> /* 스타일 생략 (기존과 동일) */ </style>
</head>
<body>
    <div class="main-content" style="margin-left:260px; padding:30px;">
        <h2 class="fw-bold mb-4">🛡️ 지역 관리자(LC) 생성</h2>
        <div class="card p-5">
            <form id="localAdminForm">
                <div class="mb-3">
                    <label class="form-label fw-bold">관리자 ID (직접 입력)</label>
                    <input type="text" name="PRS_ID" class="form-control" placeholder="예: admin_us, manager_kr" required>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">비밀번호</label>
                    <input type="password" name="PRS_PWD" class="form-control" required>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">이름 (담당자명)</label>
                    <input type="text" name="PRS_NAME" class="form-control" required>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">관할 지역</label>
                    <select class="form-select" id="PRS_COUNTRY" name="PRS_COUNTRY">
                        <option value="KR" selected>🇰🇷 대한민국 (KR)</option>
                        <option value="US">🇺🇸 미국 (US)</option>
                        <option value="JP">🇯🇵 일본 (JP)</option>
                        <option value="GB">🇬🇧 영국 (GB)</option>
                        <option value="FR">🇫🇷 프랑스 (FR)</option>
                        <option value="DE">🇩🇪 독일 (DE)</option>
                    </select>
                </div>
                <button type="submit" class="btn btn-warning w-100 fw-bold">관리자 생성</button>
            </form>
        </div>
    </div>
    
    <script>
        $('#localAdminForm').on('submit', function(e){
            e.preventDefault();
            $.ajax({
                url: '/witch/super/local/insert.do',
                type: 'POST',
                data: $(this).serialize(),
                success: function(res) {
                    alert(res.msg);
                    if(res.status === 'success') location.href = '/witch/super/dashboard.do';
                }
            });
        });
    </script>
</body>
</html>