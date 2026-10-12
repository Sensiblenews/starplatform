<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>이중인증 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body { background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; font-family: 'Pretendard', sans-serif; }
        .login-card { background: rgba(255,255,255,0.97); padding: 40px; border-radius: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.2); width: 100%; max-width: 420px; }
        .login-title { font-weight: 800; color: #1e3c72; text-align: center; margin-bottom: 8px; }
        .form-control { height: 50px; border-radius: 8px; }
        .code-input { font-size: 26px; letter-spacing: 8px; text-align: center; font-family: ui-monospace, Menlo, monospace; }
        .btn-login { height: 50px; background-color: #1e3c72; border: none; font-weight: bold; font-size: 16px; border-radius: 8px; }
        .btn-login:hover { background-color: #162e58; }
        .hint { font-size: 0.85rem; color: #6c757d; }
    </style>
</head>
<body>
    <div class="login-card">
        <h2 class="login-title">이중인증</h2>
        <p class="text-center text-secondary mb-4 small">
            <strong><c:out value="${adminId}"/></strong> 계정의 인증 앱(Google Authenticator)에 표시된
            6자리 번호를 입력하세요.
        </p>

        <form id="otpForm" autocomplete="off">
            <div class="mb-3">
                <input type="text" class="form-control code-input" id="code" name="code" inputmode="numeric"
                       placeholder="000000" maxlength="11" autofocus required>
            </div>
            <div class="d-grid">
                <button type="submit" class="btn btn-primary btn-login" id="submitBtn">확인</button>
            </div>
        </form>

        <div id="msg" class="alert alert-danger mt-3 small d-none" role="alert"></div>

        <hr class="my-4">
        <p class="hint mb-1">휴대폰이 없다면 등록 때 받은 <strong>복구 코드</strong>(예: ABCDE-FGHJK)를 같은 칸에 입력하세요. 복구 코드는 한 번만 쓸 수 있습니다.</p>
        <p class="hint mb-1">5회 연속 실패하면 10분간 잠깁니다. 번호가 계속 틀리면 휴대폰 시간 자동 설정을 확인하세요.</p>
        <p class="hint mb-0">복구 코드까지 없으면 총운영사(최고관리자)에게 OTP 재설정을 요청하세요.</p>

        <p class="text-center mt-4 mb-0"><a href="/super/logout.do" class="small text-secondary">다른 계정으로 로그인</a></p>
    </div>

<script>
    $(function () {
        $('#otpForm').on('submit', function (e) {
            e.preventDefault();
            var code = $('#code').val().trim();
            if (!code) { return; }
            $('#submitBtn').prop('disabled', true);
            $('#msg').addClass('d-none');

            $.post('/super/mfa/verifyPrc.do', { code: code }, function (res) {
                if (res.status === 'success') {
                    if (res.usedBackupCode) {
                        alert('복구 코드로 로그인했습니다. 남은 복구 코드: ' + res.remainingBackupCodes + '개\n'
                            + '휴대폰을 교체했다면 [보안 · 이중인증] 메뉴에서 OTP 를 다시 등록하세요.');
                    }
                    location.href = res.redirectUrl || '/super/dashboard.do';
                } else if (res.status === 'expired') {
                    alert(res.msg);
                    location.href = res.redirectUrl || '/super/login.do';
                } else {
                    $('#msg').text(res.msg || '인증에 실패했습니다.').removeClass('d-none');
                    $('#code').val('').focus();
                    $('#submitBtn').prop('disabled', !!res.locked);
                }
            }).fail(function () {
                $('#msg').text('서버 통신 오류가 발생했습니다.').removeClass('d-none');
                $('#submitBtn').prop('disabled', false);
            }).always(function () {
                if (!$('#submitBtn').prop('disabled')) { return; }
                // 잠금이 아니면 버튼을 다시 활성화
                if ($('#msg').hasClass('d-none') || $('#msg').text().indexOf('잠') < 0) { $('#submitBtn').prop('disabled', false); }
            });
        });
    });
</script>
</body>
</html>
