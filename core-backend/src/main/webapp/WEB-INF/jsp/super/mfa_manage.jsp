<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>보안 · 이중인증 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .main-content { margin-left: 260px; padding: 30px; }
        .card-box { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); margin-bottom: 24px; }
        .backup-codes { font-family: ui-monospace, Menlo, monospace; font-size: 1.1rem; background: #fff8e1; border: 1px dashed #f0b429; border-radius: 10px; padding: 14px; }
        @media (max-width: 768px) { .main-content { margin-left: 0; padding: 15px; } }
    </style>
</head>
<body>
    <c:set var="activeMenu" value="mfa_manage" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="mb-4">
            <h4 class="fw-bold mb-1"><i class="fas fa-shield-halved text-primary me-2"></i>보안 · 이중인증(OTP)</h4>
            <div class="text-secondary small">관리자 로그인은 비밀번호 뒤에 Google Authenticator 6자리 번호를 한 번 더 확인합니다.
                OTP 검증 전에는 관리자 권한이 발급되지 않습니다.</div>
        </div>

        <c:if test="${!mfaAvailable}">
            <div class="alert alert-danger">
                <strong>이중인증이 비활성 상태입니다</strong> (<c:out value="${mfaUnavailableReason}"/>).
                서버 설정 <code>otp.encryption.key</code> 가 없거나 <code>otp.mode=off</code> 입니다. 지금은 비밀번호만으로 로그인됩니다.
                서버 관리자가 globals.properties 에 키를 넣고 WAS 를 재기동해야 합니다.
            </div>
        </c:if>
        <c:if test="${storageError}">
            <div class="alert alert-warning">
                이중인증 테이블을 읽을 수 없습니다. <code>core-backend/db/admin-mfa-audit.sql</code> 이 운영 DB에 적용됐는지 확인하세요.
            </div>
        </c:if>

        <!-- 내 상태 -->
        <div class="card-box">
            <h5 class="fw-bold mb-3"><i class="fas fa-user-shield me-2 text-secondary"></i>내 계정 — <c:out value="${me.PRS_ID}"/> (<c:out value="${me.PRS_AUTH}"/>)</h5>
            <c:choose>
                <c:when test="${myStatus != null && myStatus.OTP_ENABLED_YN eq 'Y'}">
                    <div class="row g-3 mb-3">
                        <div class="col-md-3"><div class="text-secondary small">상태</div><span class="badge bg-success">등록됨</span></div>
                        <div class="col-md-3"><div class="text-secondary small">등록일</div><div><c:out value="${myStatus.ENROLLED_DATE}"/></div></div>
                        <div class="col-md-3"><div class="text-secondary small">마지막 인증</div><div><c:out value="${myStatus.LAST_VERIFIED_DATE}"/></div></div>
                        <div class="col-md-3"><div class="text-secondary small">남은 복구 코드</div>
                            <div class="${backupLeft le 1 ? 'text-danger fw-bold' : ''}"><c:out value="${backupLeft}"/> / 5
                                <c:if test="${backupLeft le 1}"><span class="small">— 재발급 권장</span></c:if></div></div>
                    </div>
                    <c:if test="${myStatus.LOCK_REMAIN_SEC gt 0}">
                        <div class="alert alert-warning small">연속 실패로 잠금 중 (약 <c:out value="${(myStatus.LOCK_REMAIN_SEC + 59) / 60}"/>분 남음)</div>
                    </c:if>
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-outline-primary btn-sm" data-bs-toggle="modal" data-bs-target="#regenModal"><i class="fas fa-key me-1"></i>복구 코드 재발급</button>
                        <button class="btn btn-outline-secondary btn-sm" data-bs-toggle="modal" data-bs-target="#reenrollModal"><i class="fas fa-mobile-screen me-1"></i>휴대폰 교체 · 재등록</button>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="mb-3"><span class="badge bg-danger">미등록</span>
                        <c:if test="${isSubject && mfaMode eq 'required'}"><span class="small text-secondary ms-2">의무 대상 — 다음 로그인에서 등록 화면으로 안내됩니다.</span></c:if>
                        <c:if test="${isSubject && mfaMode eq 'optional'}"><span class="small text-secondary ms-2">선택 대상 — 지금 등록할 수 있습니다.</span></c:if>
                        <c:if test="${!isSubject}"><span class="small text-secondary ms-2">이 권한은 이중인증 대상이 아닙니다.</span></c:if>
                    </div>
                    <c:if test="${isSubject}">
                        <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#reenrollModal"><i class="fas fa-qrcode me-1"></i>지금 등록</button>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- SM: 관리자 전원 현황 -->
        <c:if test="${isSM}">
            <div class="card-box">
                <h5 class="fw-bold mb-1"><i class="fas fa-users me-2 text-secondary"></i>관리자 이중인증 현황</h5>
                <div class="text-secondary small mb-3">OTP 재설정은 본인 확인을 마친 뒤에만 실행하세요. 재설정하면 대상의 비밀키·복구 코드가 모두 지워지고 다음 로그인에서 새로 등록합니다.
                    DB 를 직접 고쳐 OTP 를 우회하는 조치는 금지입니다 — 모든 재설정은 이 화면에서 하고 감사 로그에 남깁니다.</div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-light">
                            <tr><th>ID</th><th>이름</th><th>권한</th><th>국가</th><th>OTP</th><th>등록일</th><th>마지막 인증</th><th>복구 코드</th><th>상태</th><th style="width:110px">조치</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="a" items="${adminList}">
                                <tr>
                                    <td class="fw-semibold"><c:out value="${a.PRS_ID}"/></td>
                                    <td><c:out value="${a.PRS_NAME}"/></td>
                                    <td><span class="badge ${a.PRS_AUTH eq 'SM' ? 'bg-dark' : 'bg-secondary'}"><c:out value="${a.PRS_AUTH}"/></span></td>
                                    <td class="small"><c:out value="${a.PRS_COUNTRY}"/></td>
                                    <td><c:choose><c:when test="${a.OTP_ENABLED_YN eq 'Y'}"><span class="badge bg-success">등록</span></c:when><c:otherwise><span class="badge bg-danger">미등록</span></c:otherwise></c:choose></td>
                                    <td class="small text-secondary"><c:out value="${a.ENROLLED_DATE}"/></td>
                                    <td class="small text-secondary"><c:out value="${a.LAST_VERIFIED_DATE}"/></td>
                                    <td class="small"><c:out value="${a.BACKUP_LEFT}"/></td>
                                    <td class="small">
                                        <c:if test="${a.PRS_USE_YN ne 'Y'}"><span class="badge bg-light text-dark border">비활성 계정</span></c:if>
                                        <c:if test="${a.LOCK_REMAIN_SEC gt 0}"><span class="badge bg-warning text-dark">잠금</span></c:if>
                                    </td>
                                    <td>
                                        <c:if test="${a.OTP_ENABLED_YN eq 'Y'}">
                                            <button class="btn btn-sm btn-outline-danger" onclick="openReset('<c:out value="${a.PRS_ID}"/>')">OTP 재설정</button>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty adminList}"><tr><td colspan="10" class="text-center text-secondary py-4">관리자 계정이 없거나 테이블을 읽을 수 없습니다.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:if>

        <div class="card-box">
            <h6 class="fw-bold">휴대폰 분실 · 교체 시</h6>
            <ol class="small mb-0">
                <li>복구 코드가 있으면 로그인 OTP 칸에 복구 코드를 넣어 들어온 뒤, 위의 "휴대폰 교체 · 재등록"으로 새 기기를 등록합니다.</li>
                <li>복구 코드도 없으면 총운영사(SM)에게 본인 확인 후 "OTP 재설정"을 요청합니다. 재설정 사유와 승인자는 감사 로그에 남습니다.</li>
                <li>기기 분실 직후에는 해당 계정의 비밀번호도 함께 바꾸고, 감사 로그에서 이상 로그인 시도를 확인합니다.</li>
            </ol>
        </div>
    </div>

    <!-- 재발급 모달 -->
    <div class="modal fade" id="regenModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered"><div class="modal-content">
        <div class="modal-header"><h5 class="modal-title">복구 코드 재발급</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div id="regenForm">
                <p class="small text-secondary">이전 복구 코드는 모두 무효가 됩니다. 본인 확인을 위해 비밀번호와 현재 OTP 6자리를 입력하세요.</p>
                <input type="password" class="form-control mb-2" id="regenPw" placeholder="비밀번호" autocomplete="current-password">
                <input type="text" class="form-control" id="regenCode" placeholder="인증 앱 6자리" inputmode="numeric" maxlength="7" autocomplete="off">
                <div id="regenMsg" class="text-danger small mt-2"></div>
            </div>
            <div id="regenResult" class="d-none">
                <div class="alert alert-success small">새 복구 코드입니다. 지금 한 번만 보입니다 — 안전한 곳에 보관하세요.</div>
                <ul class="backup-codes list-unstyled mb-0" id="regenCodes"></ul>
            </div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" onclick="if(!$('#regenResult').hasClass('d-none')) location.reload();">닫기</button>
            <button type="button" class="btn btn-primary" id="regenBtn" onclick="regenerate()">재발급</button>
        </div>
    </div></div></div>

    <!-- 재등록 모달 -->
    <div class="modal fade" id="reenrollModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered"><div class="modal-content">
        <div class="modal-header"><h5 class="modal-title">OTP 등록 / 재등록</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <p class="small text-secondary">등록 화면으로 가기 전에 현재 비밀번호를 한 번 더 확인합니다. 재등록을 마치면 이전 비밀키와 복구 코드는 무효가 됩니다.</p>
            <input type="password" class="form-control" id="reenrollPw" placeholder="비밀번호" autocomplete="current-password">
            <div id="reenrollMsg" class="text-danger small mt-2"></div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
            <button type="button" class="btn btn-primary" onclick="reenroll()">계속</button>
        </div>
    </div></div></div>

    <!-- 재설정 모달 (SM) -->
    <div class="modal fade" id="resetModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered"><div class="modal-content">
        <div class="modal-header bg-danger text-white"><h5 class="modal-title">OTP 재설정 — <span id="resetTarget"></span></h5><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <p class="small text-secondary">대상 관리자의 본인 확인(전화·대면 등 사전에 정한 절차)을 마친 뒤 실행하세요. 사유와 실행자는 감사 로그에 기록됩니다.</p>
            <textarea class="form-control mb-2" id="resetReason" rows="2" placeholder="재설정 사유 (예: 휴대폰 분실, 2026-10-12 전화로 본인 확인)"></textarea>
            <input type="password" class="form-control" id="resetPw" placeholder="내 비밀번호 재확인" autocomplete="current-password">
            <div id="resetMsg" class="text-danger small mt-2"></div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
            <button type="button" class="btn btn-danger" onclick="doReset()">재설정 실행</button>
        </div>
    </div></div></div>

<script>
    function regenerate() {
        $('#regenMsg').text('');
        $('#regenBtn').prop('disabled', true);
        $.post('/super/mfa/regenerateCodes.do', { password: $('#regenPw').val(), code: $('#regenCode').val() }, function (res) {
            if (res.status === 'success') {
                var list = $('#regenCodes').empty();
                (res.backupCodes || []).forEach(function (c) { list.append($('<li>').text(c)); });
                $('#regenForm').addClass('d-none'); $('#regenResult').removeClass('d-none'); $('#regenBtn').addClass('d-none');
            } else {
                $('#regenMsg').text(res.msg || '재발급에 실패했습니다.');
                $('#regenBtn').prop('disabled', false);
            }
        }).fail(function () { $('#regenMsg').text('서버 통신 오류'); $('#regenBtn').prop('disabled', false); });
    }

    function reenroll() {
        $('#reenrollMsg').text('');
        $.post('/super/mfa/reenroll.do', { password: $('#reenrollPw').val() }, function (res) {
            if (res.status === 'success') { location.href = res.redirectUrl; }
            else { $('#reenrollMsg').text(res.msg || '확인에 실패했습니다.'); }
        }).fail(function () { $('#reenrollMsg').text('서버 통신 오류'); });
    }

    var resetTargetId = '';
    function openReset(id) {
        resetTargetId = id;
        $('#resetTarget').text(id); $('#resetReason').val(''); $('#resetPw').val(''); $('#resetMsg').text('');
        new bootstrap.Modal(document.getElementById('resetModal')).show();
    }
    function doReset() {
        $('#resetMsg').text('');
        $.post('/super/mfa/reset.do', { targetId: resetTargetId, reason: $('#resetReason').val(), password: $('#resetPw').val() }, function (res) {
            if (res.status === 'success') { alert(res.msg); location.reload(); }
            else { $('#resetMsg').text(res.msg || '재설정에 실패했습니다.'); }
        }).fail(function () { $('#resetMsg').text('서버 통신 오류'); });
    }
</script>
</body>
</html>
