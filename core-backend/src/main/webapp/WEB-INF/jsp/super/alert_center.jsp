<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>경보 센터 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .main-content { margin-left: 260px; padding: 30px; }
        .card-box { background: white; border-radius: 16px; padding: 22px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); margin-bottom: 24px; }
        .metric { border-radius: 12px; padding: 14px 16px; border: 1px solid #e9ecef; height: 100%; }
        .metric .name { font-size: 0.85rem; color: #6c757d; }
        .metric .val { font-size: 1.4rem; font-weight: 700; }
        .metric .desc { font-size: 0.78rem; color: #6c757d; }
        .sev-NORMAL { border-left: 5px solid #2ecc71; }
        .sev-WARNING { border-left: 5px solid #f1c40f; background: #fffdf3; }
        .sev-DANGER { border-left: 5px solid #e67e22; background: #fff7f0; }
        .sev-CRITICAL { border-left: 5px solid #e74c3c; background: #fff2f1; }
        .badge-NORMAL { background: #2ecc71; } .badge-WARNING { background: #f1c40f; color: #212529; } .badge-DANGER { background: #e67e22; } .badge-CRITICAL { background: #e74c3c; }
        .form-label { font-size: 0.8rem; color: #6c757d; margin-bottom: 2px; }
        .settings .form-control, .settings .form-select { font-size: 0.9rem; }
        @media (max-width: 768px) { .main-content { margin-left: 0; padding: 15px; } }
    </style>
</head>
<body>
    <c:set var="activeMenu" value="alert_center" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-4">
            <div>
                <h4 class="fw-bold mb-1"><i class="fas fa-bell text-primary me-2"></i>경보 센터 <span class="badge bg-light text-dark border ms-1">플랫폼 <c:out value="${platformId}"/></span></h4>
                <div class="text-secondary small">5분마다 디스크·RAM·CPU·JVM 힙·DB·Redis·HTTP 5xx·관리자 로그인 실패를 평가합니다.
                    같은 등급이 2회 연속(10분)이면 경보가 열리고, 위험 이상은 메일로 알립니다. 주의는 이 화면에만 표시됩니다.</div>
            </div>
            <div class="text-end small text-secondary">
                마지막 평가: <span id="lastEval"><c:choose><c:when test="${lastEvaluatedAt gt 0}"><script>document.write(new Date(${lastEvaluatedAt}).toLocaleString('ko-KR'));</script></c:when><c:otherwise>아직 없음 (기동 후 첫 주기 대기)</c:otherwise></c:choose></span><br>
                <button class="btn btn-sm btn-outline-primary mt-1" onclick="evaluateNow()"><i class="fas fa-rotate me-1"></i>지금 점검</button>
            </div>
        </div>

        <c:if test="${!monitorEnabled}"><div class="alert alert-warning small">경보가 <strong>비활성</strong> 상태입니다 (alert.enabled=N). 측정은 계속하지만 경보·메일은 내지 않습니다.</div></c:if>
        <c:if test="${!mailAvailable}"><div class="alert alert-danger small">SMTP(mailSender)가 구성되지 않아 메일을 보낼 수 없습니다. 경보는 이 화면에만 표시됩니다.</div></c:if>
        <c:if test="${mailAvailable && empty recipients}"><div class="alert alert-warning small">수신자가 비어 있습니다. 아래 설정에서 운영자 이메일을 넣고 시험 메일로 실제 수신을 확인하세요.</div></c:if>
        <c:if test="${storageError}"><div class="alert alert-warning small">경보 테이블을 읽을 수 없습니다. <code>core-backend/db/sys-alert.sql</code> 적용 여부를 확인하세요. 감시·메일은 테이블 없이도 동작하지만 이력이 남지 않습니다.</div></c:if>

        <!-- 서버 현황 -->
        <div class="card-box">
            <h5 class="fw-bold mb-3"><i class="fas fa-heart-pulse me-2 text-secondary"></i>서버 현황</h5>
            <div class="row g-3" id="readingGrid">
                <c:forEach var="r" items="${readings}">
                    <div class="col-md-6 col-xl-3">
                        <div class="metric sev-${r.open != null ? r.open : r.severity}">
                            <div class="d-flex justify-content-between">
                                <span class="name"><c:out value="${r.label}"/></span>
                                <span class="badge badge-${r.open != null ? r.open : r.severity}"><c:out value="${r.open != null ? r.openLabel : r.severityLabel}"/></span>
                            </div>
                            <div class="val"><c:out value="${r.valueText}"/></div>
                            <div class="desc"><c:out value="${r.message}"/></div>
                            <c:if test="${r.open != null}">
                                <div class="desc mt-1">경보 #<c:out value="${r.alertId}"/> 열림
                                    <c:if test="${r.acked}">· 확인됨</c:if>
                                    <c:if test="${!r.acked && r.alertId gt 0}"><a href="#" class="ms-1" onclick="ack(${r.alertId}); return false;">확인 처리</a></c:if>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </c:forEach>
                <c:if test="${empty readings}"><div class="col-12 text-secondary small">아직 측정값이 없습니다. "지금 점검"을 누르거나 첫 주기(최대 5분)를 기다리세요.</div></c:if>
            </div>
        </div>

        <!-- 열린 경보 -->
        <div class="card-box">
            <h5 class="fw-bold mb-3"><i class="fas fa-triangle-exclamation me-2 text-danger"></i>열린 경보</h5>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead class="table-light"><tr><th>#</th><th>등급</th><th>항목</th><th>설명</th><th>발생</th><th>지속</th><th>메일</th><th>확인</th><th></th></tr></thead>
                    <tbody>
                        <c:forEach var="a" items="${openAlerts}">
                            <tr>
                                <td class="small"><c:out value="${a.ALERT_ID}"/></td>
                                <td><span class="badge badge-${a.SEVERITY}"><c:out value="${a.SEVERITY}"/></span></td>
                                <td class="fw-semibold"><c:out value="${a.TITLE}"/></td>
                                <td class="small"><c:out value="${a.MESSAGE}"/></td>
                                <td class="small text-secondary text-nowrap"><c:out value="${a.OPENED_DATE}"/></td>
                                <td class="small"><c:out value="${a.DURATION_MIN}"/>분</td>
                                <td class="small"><c:choose><c:when test="${a.MAIL_SENT_YN eq 'Y'}">발송 <c:out value="${a.NOTIFY_COUNT}"/>회</c:when><c:otherwise><span class="text-secondary">미발송</span></c:otherwise></c:choose></td>
                                <td class="small"><c:choose><c:when test="${a.ACK_YN eq 'Y'}"><c:out value="${a.ACK_BY}"/><br><span class="text-secondary"><c:out value="${a.ACK_DATE}"/></span></c:when><c:otherwise><span class="text-danger">미확인</span></c:otherwise></c:choose></td>
                                <td><c:if test="${a.ACK_YN ne 'Y'}"><button class="btn btn-sm btn-outline-primary" onclick="ack(${a.ALERT_ID})">확인</button></c:if></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty openAlerts}"><tr><td colspan="9" class="text-center text-secondary py-4">열린 경보가 없습니다.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="row g-4">
            <!-- 설정 -->
            <div class="col-xl-5">
                <div class="card-box settings h-100">
                    <h5 class="fw-bold mb-1"><i class="fas fa-sliders me-2 text-secondary"></i>경보 · 메일 설정</h5>
                    <div class="text-secondary small mb-3">SMTP 서버·계정은 서버 설정 파일에만 있습니다. 여기서는 수신자와 임계값만 바꿉니다. 변경은 감사 로그에 남습니다.</div>
                    <form id="settingsForm" onsubmit="saveSettings(); return false;">
                        <div class="row g-2">
                            <div class="col-12">
                                <label class="form-label">수신자 이메일 (쉼표로 여러 명)</label>
                                <input type="text" class="form-control" name="alert.mail.to" value="<c:out value='${settings["alert.mail.to"]}'/>" placeholder="ops@example.com, cto@example.com">
                            </div>
                            <div class="col-6">
                                <label class="form-label">경보 사용</label>
                                <select class="form-select" name="alert.enabled"><option value="Y" ${settings['alert.enabled'] eq 'Y' ? 'selected' : ''}>사용</option><option value="N" ${settings['alert.enabled'] ne 'Y' ? 'selected' : ''}>중지</option></select>
                            </div>
                            <div class="col-6">
                                <label class="form-label">메일 최소 등급</label>
                                <select class="form-select" name="alert.mail.min.severity">
                                    <option value="WARNING" ${settings['alert.mail.min.severity'] eq 'WARNING' ? 'selected' : ''}>주의부터</option>
                                    <option value="DANGER" ${settings['alert.mail.min.severity'] eq 'DANGER' ? 'selected' : ''}>위험부터 (기본)</option>
                                    <option value="CRITICAL" ${settings['alert.mail.min.severity'] eq 'CRITICAL' ? 'selected' : ''}>긴급만</option>
                                </select>
                            </div>
                            <div class="col-12 mt-3 small fw-semibold text-secondary">임계값 (%)</div>
                            <div class="col-4"><label class="form-label">디스크 주의</label><input type="number" class="form-control" name="alert.disk.warn" value="<c:out value='${settings["alert.disk.warn"]}'/>"></div>
                            <div class="col-4"><label class="form-label">디스크 위험</label><input type="number" class="form-control" name="alert.disk.danger" value="<c:out value='${settings["alert.disk.danger"]}'/>"></div>
                            <div class="col-4"><label class="form-label">디스크 긴급</label><input type="number" class="form-control" name="alert.disk.critical" value="<c:out value='${settings["alert.disk.critical"]}'/>"></div>
                            <div class="col-4"><label class="form-label">RAM 주의</label><input type="number" class="form-control" name="alert.ram.warn" value="<c:out value='${settings["alert.ram.warn"]}'/>"></div>
                            <div class="col-4"><label class="form-label">RAM 위험</label><input type="number" class="form-control" name="alert.ram.danger" value="<c:out value='${settings["alert.ram.danger"]}'/>"></div>
                            <div class="col-4"><label class="form-label">RAM 긴급</label><input type="number" class="form-control" name="alert.ram.critical" value="<c:out value='${settings["alert.ram.critical"]}'/>"></div>
                            <div class="col-4"><label class="form-label">CPU 주의</label><input type="number" class="form-control" name="alert.cpu.warn" value="<c:out value='${settings["alert.cpu.warn"]}'/>"></div>
                            <div class="col-4"><label class="form-label">CPU 위험</label><input type="number" class="form-control" name="alert.cpu.danger" value="<c:out value='${settings["alert.cpu.danger"]}'/>"></div>
                            <div class="col-4"></div>
                            <div class="col-4"><label class="form-label">JVM 힙 주의</label><input type="number" class="form-control" name="alert.heap.warn" value="<c:out value='${settings["alert.heap.warn"]}'/>"></div>
                            <div class="col-4"><label class="form-label">JVM 힙 위험</label><input type="number" class="form-control" name="alert.heap.danger" value="<c:out value='${settings["alert.heap.danger"]}'/>"></div>
                            <div class="col-4"></div>
                            <div class="col-12 mt-3 small fw-semibold text-secondary">오류 · 공격 징후</div>
                            <div class="col-4"><label class="form-label">5xx 최소 건수/구간</label><input type="number" class="form-control" name="alert.http5xx.min.count" value="<c:out value='${settings["alert.http5xx.min.count"]}'/>"></div>
                            <div class="col-4"><label class="form-label">5xx 비율 %</label><input type="number" class="form-control" name="alert.http5xx.percent" value="<c:out value='${settings["alert.http5xx.percent"]}'/>"></div>
                            <div class="col-4"><label class="form-label">로그인 실패/15분</label><input type="number" class="form-control" name="alert.loginfail.min.count" value="<c:out value='${settings["alert.loginfail.min.count"]}'/>"></div>
                            <div class="col-12 mt-3 small fw-semibold text-secondary">알림 피로도</div>
                            <div class="col-3"><label class="form-label">확인 횟수</label><input type="number" class="form-control" name="alert.confirm.count" value="<c:out value='${settings["alert.confirm.count"]}'/>"></div>
                            <div class="col-3"><label class="form-label">복구 횟수</label><input type="number" class="form-control" name="alert.recover.count" value="<c:out value='${settings["alert.recover.count"]}'/>"></div>
                            <div class="col-3"><label class="form-label">재알림 분</label><input type="number" class="form-control" name="alert.remind.minutes" value="<c:out value='${settings["alert.remind.minutes"]}'/>"></div>
                            <div class="col-3"><label class="form-label">재알림 최대</label><input type="number" class="form-control" name="alert.remind.max" value="<c:out value='${settings["alert.remind.max"]}'/>"></div>
                        </div>
                        <div class="text-secondary small mt-2">확인 횟수 2 = 5분 주기 × 2회 연속(10분 지속)일 때 경보. 긴급 미확인 경보는 재알림 분마다 최대 횟수까지 다시 발송.</div>
                        <div class="d-flex gap-2 mt-3">
                            <button type="submit" class="btn btn-primary btn-sm">설정 저장</button>
                            <button type="button" class="btn btn-outline-success btn-sm" onclick="testMail()" ${mailAvailable ? '' : 'disabled'}><i class="fas fa-paper-plane me-1"></i>시험 메일 발송</button>
                        </div>
                        <div id="settingsMsg" class="small mt-2"></div>
                    </form>
                </div>
            </div>

            <!-- 이력 -->
            <div class="col-xl-7">
                <div class="card-box h-100">
                    <h5 class="fw-bold mb-3"><i class="fas fa-clock-rotate-left me-2 text-secondary"></i>최근 이력</h5>
                    <div class="table-responsive" style="max-height: 520px; overflow-y: auto;">
                        <table class="table table-sm table-hover align-middle">
                            <thead class="table-light"><tr><th>시각</th><th>이벤트</th><th>항목</th><th>등급</th><th>설명</th><th>주체</th></tr></thead>
                            <tbody>
                                <c:forEach var="e" items="${recentEvents}">
                                    <tr>
                                        <td class="small text-secondary text-nowrap"><c:out value="${e.CREATED_DATE}"/></td>
                                        <td class="small"><code><c:out value="${e.EVENT_TYPE}"/></code></td>
                                        <td class="small"><c:out value="${e.ALERT_KEY}"/><c:if test="${e.ALERT_ID != null}"> #<c:out value="${e.ALERT_ID}"/></c:if></td>
                                        <td><c:if test="${e.SEVERITY != null}"><span class="badge badge-${e.SEVERITY}"><c:out value="${e.SEVERITY}"/></span></c:if></td>
                                        <td class="small"><c:out value="${e.MESSAGE}"/></td>
                                        <td class="small text-secondary"><c:out value="${e.ACTOR}"/></td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty recentEvents}"><tr><td colspan="6" class="text-center text-secondary py-4">이력이 없습니다.</td></tr></c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!-- 최근 경보 -->
        <div class="card-box">
            <h5 class="fw-bold mb-3"><i class="fas fa-list me-2 text-secondary"></i>최근 경보 (복구 포함)</h5>
            <div class="table-responsive">
                <table class="table table-sm table-hover align-middle">
                    <thead class="table-light"><tr><th>#</th><th>상태</th><th>등급</th><th>항목</th><th>발생</th><th>복구</th><th>지속</th><th>메일</th><th>확인</th></tr></thead>
                    <tbody>
                        <c:forEach var="a" items="${recentAlerts}">
                            <tr>
                                <td class="small"><c:out value="${a.ALERT_ID}"/></td>
                                <td><c:choose><c:when test="${a.STATUS eq 'OPEN'}"><span class="badge bg-danger">열림</span></c:when><c:otherwise><span class="badge bg-secondary">복구</span></c:otherwise></c:choose></td>
                                <td><span class="badge badge-${a.SEVERITY}"><c:out value="${a.SEVERITY}"/></span></td>
                                <td class="small fw-semibold"><c:out value="${a.TITLE}"/></td>
                                <td class="small text-secondary text-nowrap"><c:out value="${a.OPENED_DATE}"/></td>
                                <td class="small text-secondary text-nowrap"><c:out value="${a.RESOLVED_DATE}"/></td>
                                <td class="small"><c:out value="${a.DURATION_MIN}"/>분</td>
                                <td class="small"><c:out value="${a.NOTIFY_COUNT}"/>회</td>
                                <td class="small"><c:out value="${a.ACK_BY}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty recentAlerts}"><tr><td colspan="9" class="text-center text-secondary py-4">경보 기록이 없습니다.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

<script>
    function ack(alertId) {
        if (!confirm('경보 #' + alertId + ' 를 확인 처리할까요? (복구와는 별개이며 긴급 재알림이 멈춥니다)')) return;
        $.post('/super/alert/ack.do', { alertId: alertId }, function (res) {
            alert(res.msg || ''); if (res.status === 'success') location.reload();
        }).fail(function () { alert('처리에 실패했습니다.'); });
    }
    function evaluateNow() {
        $.post('/super/alert/evaluate.do', {}, function (res) {
            if (res.status === 'success') { location.reload(); } else { alert(res.msg || '점검 실패'); }
        }).fail(function () { alert('점검 요청에 실패했습니다.'); });
    }
    function saveSettings() {
        $('#settingsMsg').removeClass('text-danger text-success').text('저장 중…');
        $.post('/super/alert/settings.do', $('#settingsForm').serialize(), function (res) {
            $('#settingsMsg').addClass(res.status === 'success' ? 'text-success' : 'text-danger').text(res.msg || '');
        }).fail(function () { $('#settingsMsg').addClass('text-danger').text('서버 통신 오류'); });
    }
    function testMail() {
        var to = $('input[name="alert.mail.to"]').val();
        if (!confirm('시험 메일을 보냅니다.\n수신자: ' + (to || '(저장된 설정값)') + '\n\n수신자 입력값이 저장된 값과 다르면 입력값으로 보냅니다. 계속할까요?')) return;
        $('#settingsMsg').removeClass('text-danger text-success').text('발송 중… (최대 25초)');
        $.post('/super/alert/testMail.do', { to: to }, function (res) {
            $('#settingsMsg').addClass(res.status === 'success' ? 'text-success' : 'text-danger').text(res.msg || '');
        }).fail(function () { $('#settingsMsg').addClass('text-danger').text('서버 통신 오류'); });
    }
</script>
</body>
</html>
