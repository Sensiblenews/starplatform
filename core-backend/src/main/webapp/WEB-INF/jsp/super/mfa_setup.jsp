<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>OTP 등록 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <%-- QR 렌더링은 브라우저에서 한다. 비밀키는 이 화면 응답에만 실리고 서버 로그·URL 에 남지 않는다 --%>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <style>
        body { background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; font-family: 'Pretendard', sans-serif; padding: 20px 0; }
        .setup-card { background: rgba(255,255,255,0.97); padding: 36px; border-radius: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.2); width: 100%; max-width: 560px; }
        .title { font-weight: 800; color: #1e3c72; text-align: center; margin-bottom: 6px; }
        .step { border-left: 4px solid #1e3c72; padding-left: 14px; margin-bottom: 18px; }
        .step h6 { font-weight: 700; margin-bottom: 4px; }
        #qrcode { display: flex; justify-content: center; margin: 10px 0; min-height: 200px; }
        #qrcode img, #qrcode canvas { border: 8px solid #fff; box-shadow: 0 2px 10px rgba(0,0,0,0.15); }
        .secret { font-family: ui-monospace, Menlo, monospace; letter-spacing: 2px; background: #f1f3f5; padding: 8px 12px; border-radius: 8px; word-break: break-all; }
        .code-input { font-size: 24px; letter-spacing: 8px; text-align: center; font-family: ui-monospace, Menlo, monospace; height: 50px; }
        .btn-main { height: 48px; background-color: #1e3c72; border: none; font-weight: bold; border-radius: 8px; }
        .btn-main:hover { background-color: #162e58; }
        .backup-codes { font-family: ui-monospace, Menlo, monospace; font-size: 1.15rem; letter-spacing: 1px; background: #fff8e1; border: 1px dashed #f0b429; border-radius: 10px; padding: 16px; }
        .backup-codes li { padding: 4px 0; }
        .hint { font-size: 0.85rem; color: #6c757d; }
        @media print { body { background: #fff; } .no-print { display: none !important; } }
    </style>
</head>
<body>
    <div class="setup-card">
        <h2 class="title">Google OTP 등록</h2>
        <p class="text-center text-secondary small mb-4">
            <strong><c:out value="${adminId}"/></strong>
            <c:choose>
                <c:when test="${reenroll}">— 재등록. 완료되면 이전 비밀키와 복구 코드는 모두 무효가 됩니다.</c:when>
                <c:otherwise>— 이 계정은 이중인증 등록이 필요합니다. 등록을 마쳐야 관리자 화면에 들어갈 수 있습니다.</c:otherwise>
            </c:choose>
        </p>

        <!-- 1단계: QR -->
        <div id="phase1">
            <div class="step">
                <h6>1. 휴대폰에 Google Authenticator 설치</h6>
                <div class="hint">iPhone 은 App Store, Android 는 Play 스토어에서 "Google Authenticator" 를 설치합니다. Authy·Microsoft Authenticator 도 됩니다.</div>
            </div>
            <div class="step">
                <h6>2. 앱에서 QR 코드 스캔</h6>
                <div id="qrcode"><div class="text-secondary small align-self-center">QR 코드 생성 중…</div></div>
                <div class="hint mb-1">카메라를 쓸 수 없으면 앱에서 "설정 키 입력"을 고르고 아래 키를 직접 넣습니다 (계정: <c:out value="${issuer}"/>, 시간 기반).</div>
                <div class="secret" id="secretText">-</div>
                <div class="hint mt-1">이 화면은 <c:out value="${pendingMinutes}"/>분 동안만 유효합니다. 시간이 지나면 새로고침해 QR 을 다시 받으세요.</div>
            </div>
            <div class="step">
                <h6>3. 앱에 표시된 6자리 입력</h6>
                <form id="confirmForm" autocomplete="off">
                    <input type="text" class="form-control code-input mb-2" id="code" name="code" inputmode="numeric" placeholder="000000" maxlength="7" required>
                    <div class="d-grid">
                        <button type="submit" class="btn btn-primary btn-main" id="confirmBtn" disabled>등록 확인</button>
                    </div>
                </form>
                <div id="msg" class="alert alert-danger mt-3 small d-none" role="alert"></div>
            </div>
        </div>

        <!-- 2단계: 복구 코드 (1회 노출) -->
        <div id="phase2" class="d-none">
            <div class="alert alert-success small">OTP 가 등록되었습니다. 다음 로그인부터 비밀번호 뒤에 6자리 번호를 묻습니다.</div>
            <div class="step">
                <h6>복구 코드 5개 — 지금 한 번만 보입니다</h6>
                <div class="hint mb-2">휴대폰을 잃거나 바꿨을 때 6자리 대신 쓰는 비상 코드입니다. 각 코드는 한 번만 쓸 수 있습니다.
                    종이에 적어 금고에, 또는 비밀번호 관리자에 보관하세요. 메신저·메일·공유 문서에 두지 마세요.</div>
                <ul class="backup-codes list-unstyled mb-2" id="backupCodes"></ul>
                <div class="d-flex gap-2 no-print">
                    <button type="button" class="btn btn-outline-secondary btn-sm" onclick="copyCodes()">복사</button>
                    <button type="button" class="btn btn-outline-secondary btn-sm" onclick="window.print()">인쇄</button>
                </div>
            </div>
            <div class="form-check mb-3 no-print">
                <input class="form-check-input" type="checkbox" id="savedCheck">
                <label class="form-check-label small" for="savedCheck">복구 코드를 안전한 곳에 보관했습니다.</label>
            </div>
            <div class="d-grid no-print">
                <button type="button" class="btn btn-primary btn-main" id="continueBtn" disabled onclick="goNext()">계속</button>
            </div>
        </div>

        <p class="text-center mt-4 mb-0 no-print"><a href="/super/logout.do" class="small text-secondary">취소하고 로그아웃</a></p>
    </div>

<script>
    var redirectUrl = '/super/dashboard.do';

    function startSetup() {
        $.post('/super/mfa/setupStart.do', {}, function (res) {
            if (res.status !== 'success') {
                if (res.status === 'expired') { alert(res.msg); location.href = res.redirectUrl || '/super/login.do'; return; }
                $('#qrcode').html('<div class="text-danger small align-self-center">' + $('<div>').text(res.msg || '등록을 시작할 수 없습니다.').html() + '</div>');
                return;
            }
            $('#qrcode').empty();
            try {
                new QRCode(document.getElementById('qrcode'), { text: res.uri, width: 200, height: 200, correctLevel: QRCode.CorrectLevel.M });
            } catch (e) {
                $('#qrcode').html('<div class="text-danger small align-self-center">QR 생성 실패 — 아래 키를 직접 입력하세요.</div>');
            }
            // 4자리씩 끊어 읽기 쉽게
            $('#secretText').text((res.secret || '').replace(/(.{4})/g, '$1 ').trim());
            $('#confirmBtn').prop('disabled', false);
            $('#code').focus();
        }).fail(function () {
            $('#qrcode').html('<div class="text-danger small align-self-center">서버 통신 오류</div>');
        });
    }

    $(function () {
        startSetup();
        $('#confirmForm').on('submit', function (e) {
            e.preventDefault();
            var code = $('#code').val().trim();
            if (!code) { return; }
            $('#confirmBtn').prop('disabled', true);
            $('#msg').addClass('d-none');
            $.post('/super/mfa/setupConfirm.do', { code: code }, function (res) {
                if (res.status === 'success') {
                    redirectUrl = res.redirectUrl || redirectUrl;
                    var list = $('#backupCodes').empty();
                    (res.backupCodes || []).forEach(function (c) { list.append($('<li>').text(c)); });
                    $('#phase1').addClass('d-none');
                    $('#phase2').removeClass('d-none');
                } else if (res.status === 'expired') {
                    alert(res.msg); location.href = res.redirectUrl || '/super/login.do';
                } else {
                    $('#msg').text(res.msg || '등록 확인에 실패했습니다.').removeClass('d-none');
                    $('#code').val('').focus();
                    $('#confirmBtn').prop('disabled', false);
                    if (res.reason === 'PENDING_EXPIRED' || res.reason === 'NO_PENDING') { startSetup(); }
                }
            }).fail(function () {
                $('#msg').text('서버 통신 오류가 발생했습니다.').removeClass('d-none');
                $('#confirmBtn').prop('disabled', false);
            });
        });
        $('#savedCheck').on('change', function () { $('#continueBtn').prop('disabled', !this.checked); });
    });

    function copyCodes() {
        var text = $('#backupCodes li').map(function () { return $(this).text(); }).get().join('\n');
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(function () { alert('복사했습니다. 붙여넣은 뒤에는 클립보드를 비우세요.'); });
        } else {
            window.prompt('아래 내용을 복사하세요', text);
        }
    }

    function goNext() { location.href = redirectUrl; }
</script>
</body>
</html>
