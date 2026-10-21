<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>경보 #<c:out value="${alert.ALERT_ID}"/> - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .main-content { margin-left: 260px; padding: 30px; }
        .card-box { background: white; border-radius: 16px; padding: 22px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); margin-bottom: 20px; }
        .badge-NORMAL { background: #2ecc71; } .badge-WARNING { background: #f1c40f; color: #212529; } .badge-DANGER { background: #e67e22; } .badge-CRITICAL { background: #e74c3c; }
        .now-box { border-radius: 12px; padding: 16px; border: 1px solid #e9ecef; }
        .now-box .val { font-size: 1.8rem; font-weight: 700; }
        .steps li { margin-bottom: 6px; }
        .action-btn { min-width: 150px; }
        .timeline td { font-size: 0.85rem; }
        .chart-wrap { position: relative; height: 240px; }
        @media (max-width: 768px) { .main-content { margin-left: 0; padding: 15px; } .action-btn { width: 100%; } }
    </style>
</head>
<body>
    <c:set var="activeMenu" value="alert_center" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <c:set var="isOpen" value="${alert.STATUS eq 'OPEN'}" />
    <div class="main-content">
        <a href="/super/alert/center.do" class="small text-secondary"><i class="fas fa-arrow-left me-1"></i>경보 센터</a>

        <!-- 요약 -->
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mt-2 mb-3">
            <div>
                <h4 class="fw-bold mb-1">
                    경보 #<c:out value="${alert.ALERT_ID}"/> · <c:out value="${alert.TITLE}"/>
                    <span class="badge badge-${alert.SEVERITY} ms-1"><c:out value="${alert.SEVERITY}"/></span>
                    <c:choose>
                        <c:when test="${isOpen}"><span class="badge bg-danger">열림</span></c:when>
                        <c:otherwise><span class="badge bg-secondary">복구됨</span></c:otherwise>
                    </c:choose>
                </h4>
                <div class="text-secondary small">
                    발생 <c:out value="${alert.OPENED_DATE}"/>
                    <c:if test="${not empty alert.RESOLVED_DATE}"> · 복구 <c:out value="${alert.RESOLVED_DATE}"/></c:if>
                    · 지속 <c:out value="${alert.DURATION_MIN}"/>분
                    · 메일 <c:choose><c:when test="${alert.MAIL_SENT_YN eq 'Y'}">발송 <c:out value="${alert.NOTIFY_COUNT}"/>회</c:when><c:otherwise>미발송</c:otherwise></c:choose>
                    · 확인 <c:choose><c:when test="${alert.ACK_YN eq 'Y'}"><c:out value="${alert.ACK_BY}"/> (<c:out value="${alert.ACK_DATE}"/>)</c:when><c:otherwise><span class="text-danger">미확인</span></c:otherwise></c:choose>
                </div>
            </div>
        </div>

        <div class="row g-3">
            <!-- 지금 상태 -->
            <div class="col-lg-5">
                <div class="card-box h-100">
                    <h6 class="fw-bold mb-3"><i class="fas fa-heart-pulse me-2 text-secondary"></i>지금 상태</h6>
                    <div class="now-box" id="nowBox">
                        <c:choose>
                            <c:when test="${reading != null}">
                                <div class="d-flex justify-content-between align-items-center">
                                    <span class="val" id="nowVal"><c:out value="${reading.valueText()}"/></span>
                                    <span class="badge badge-${reading.severity} fs-6" id="nowSev"><c:out value="${reading.severity.label()}"/></span>
                                </div>
                                <div class="small text-secondary mt-1" id="nowMsg"><c:out value="${reading.message}"/></div>
                                <div class="small text-secondary mt-1" id="nowWhen">마지막 정기 측정값 (5분마다 갱신)</div>
                            </c:when>
                            <c:otherwise>
                                <div class="small text-secondary" id="nowMsg">아직 측정값이 없습니다. "다시 측정"을 눌러 보세요.</div>
                                <span class="val" id="nowVal"></span><span class="badge" id="nowSev"></span><div class="small text-secondary" id="nowWhen"></div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="small text-secondary mt-2">경보 발생 당시: <c:out value="${alert.MESSAGE}"/></div>
                </div>
            </div>

            <!-- 최근 24시간 -->
            <div class="col-lg-7">
                <div class="card-box h-100">
                    <h6 class="fw-bold mb-3"><i class="fas fa-chart-line me-2 text-secondary"></i>최근 24시간</h6>
                    <div class="chart-wrap"><canvas id="historyChart"></canvas></div>
                    <div class="small text-secondary mt-2" id="historyNote">5분마다 측정한 값입니다. 서버를 재시작하면 그 시점부터 다시 쌓입니다.</div>
                </div>
            </div>
        </div>

        <!-- 권장 조치 + 원클릭 조치 -->
        <div class="row g-3 mt-0">
            <div class="col-lg-5">
                <div class="card-box h-100">
                    <h6 class="fw-bold mb-3"><i class="fas fa-list-check me-2 text-secondary"></i>권장 조치 순서</h6>
                    <c:choose>
                        <c:when test="${not empty steps}">
                            <ol class="steps small ps-3 mb-0">
                                <c:forEach var="s" items="${steps}"><li><c:out value="${s}"/></li></c:forEach>
                            </ol>
                        </c:when>
                        <c:otherwise><div class="small text-secondary">이 항목의 권장 조치 문구가 없습니다.</div></c:otherwise>
                    </c:choose>
                    <div class="small text-secondary mt-3">서버 재시작·DB 재시작·차단 규칙 변경처럼 영향이 큰 조치는 이 화면에서 실행하지 않습니다. 서버 담당자에게 요청하고, 한 일을 아래 "조치 메모"로 남겨 주세요.</div>
                </div>
            </div>

            <div class="col-lg-7">
                <div class="card-box h-100">
                    <h6 class="fw-bold mb-3"><i class="fas fa-hand-pointer me-2 text-secondary"></i>원클릭 조치</h6>
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-outline-primary btn-sm action-btn" onclick="runAction('RECHECK')"><i class="fas fa-rotate me-1"></i>이 항목 다시 측정</button>
                        <c:if test="${isOpen && alert.ACK_YN ne 'Y'}">
                            <button class="btn btn-outline-success btn-sm action-btn" onclick="ack()"><i class="fas fa-check me-1"></i>확인 처리</button>
                        </c:if>
                        <c:if test="${alertKey eq 'heap' || alertKey eq 'ram'}">
                            <button class="btn btn-outline-warning btn-sm action-btn" onclick="runAction('GC')"><i class="fas fa-broom me-1"></i>메모리 정리 (GC)</button>
                        </c:if>
                        <button class="btn btn-outline-secondary btn-sm action-btn" onclick="testMail()" ${mailAvailable ? '' : 'disabled'}><i class="fas fa-paper-plane me-1"></i>경보 메일 시험 발송</button>
                    </div>
                    <ul class="small text-secondary mt-2 mb-3 ps-3">
                        <li><b>다시 측정</b>: 이 항목만 지금 읽어 봅니다. 경보를 열거나 닫지 않습니다(조회만).</li>
                        <li><b>확인 처리</b>: "누가 보고 있다"는 표시입니다. 긴급 경보의 재알림 메일이 멈춥니다. 경보 자체는 수치가 내려가야 닫힙니다.</li>
                        <c:if test="${alertKey eq 'heap' || alertKey eq 'ram'}"><li><b>메모리 정리</b>: JVM 가비지 컬렉션을 한 번 실행합니다. 일시적으로 응답이 아주 잠깐 느려질 수 있습니다.</li></c:if>
                        <li><b>시험 메일</b>: 수신자(<c:out value="${fn:length(recipients)}"/>명)에게 시험 메일을 보내 경보 메일이 실제로 오는지 확인합니다.</li>
                    </ul>
                    <label class="form-label small fw-semibold mb-1" for="noteText">조치 메모</label>
                    <div class="d-flex gap-2">
                        <textarea id="noteText" class="form-control form-control-sm" rows="2" maxlength="500" placeholder="예: 로그 정리 요청함 / 서버 담당자에게 재시작 요청 / 원인: 배치 작업"></textarea>
                        <button class="btn btn-primary btn-sm" style="min-width: 80px;" onclick="saveNote()">저장</button>
                    </div>
                    <div id="actionMsg" class="small mt-2"></div>
                </div>
            </div>
        </div>

        <!-- 경과 -->
        <div class="card-box mt-3">
            <h6 class="fw-bold mb-3"><i class="fas fa-clock-rotate-left me-2 text-secondary"></i>경과 (발생 → 메일 → 확인 → 조치 → 복구)</h6>
            <div class="table-responsive">
                <table class="table table-sm table-hover align-middle timeline mb-0">
                    <thead class="table-light"><tr><th>시각</th><th>이벤트</th><th>등급</th><th>내용</th><th>주체</th></tr></thead>
                    <tbody>
                        <c:forEach var="e" items="${events}">
                            <tr>
                                <td class="text-secondary text-nowrap"><c:out value="${e.CREATED_DATE}"/></td>
                                <td><code><c:out value="${e.EVENT_TYPE}"/></code></td>
                                <td><c:if test="${e.SEVERITY != null}"><span class="badge badge-${e.SEVERITY}"><c:out value="${e.SEVERITY}"/></span></c:if></td>
                                <td><c:out value="${e.MESSAGE}"/></td>
                                <td class="text-secondary"><c:out value="${e.ACTOR}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty events}"><tr><td colspan="5" class="text-center text-secondary py-4">기록이 없습니다.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

<script>
    var ALERT_ID = ${alert.ALERT_ID};
    var HISTORY = ${historyJson};
    var THRESHOLDS = {<c:forEach var="t" items="${thresholds}" varStatus="st">"${t.key}": ${t.value}<c:if test="${!st.last}">,</c:if></c:forEach>};

    // 최근 24시간 그래프 + 주의·위험·긴급 기준선
    (function () {
        var canvas = document.getElementById('historyChart');
        if (!HISTORY.length) {
            canvas.parentNode.innerHTML = '<div class="text-secondary small py-5 text-center">아직 쌓인 측정값이 없습니다 (서버 기동 후 5분마다 쌓입니다).</div>';
            return;
        }
        var labels = HISTORY.map(function (p) { var d = new Date(p[0]); return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2); });
        var datasets = [{ label: '측정값', data: HISTORY.map(function (p) { return p[1]; }), borderColor: '#3880ff', backgroundColor: 'rgba(56,128,255,0.08)', fill: true, pointRadius: 0, tension: 0.2, borderWidth: 2 }];
        var lineColors = { warn: '#f1c40f', danger: '#e67e22', critical: '#e74c3c' };
        var lineNames = { warn: '주의', danger: '위험', critical: '긴급' };
        Object.keys(THRESHOLDS).forEach(function (k) {
            if (!THRESHOLDS[k]) return;
            datasets.push({ label: lineNames[k] + ' ' + THRESHOLDS[k] + '%', data: labels.map(function () { return THRESHOLDS[k]; }), borderColor: lineColors[k], borderDash: [6, 4], borderWidth: 1, pointRadius: 0, fill: false });
        });
        new Chart(canvas.getContext('2d'), {
            type: 'line',
            data: { labels: labels, datasets: datasets },
            options: { responsive: true, maintainAspectRatio: false, animation: false,
                plugins: { legend: { display: datasets.length > 1, labels: { boxWidth: 12, font: { size: 11 } } } },
                scales: { x: { ticks: { maxTicksLimit: 8, maxRotation: 0 } }, y: { beginAtZero: true } } }
        });
    })();

    function showMsg(ok, text) {
        $('#actionMsg').removeClass('text-danger text-success').addClass(ok ? 'text-success' : 'text-danger').text(text || '');
    }

    function runAction(action) {
        if (action === 'GC' && !confirm('메모리 정리(GC)를 실행할까요? 아주 잠깐 응답이 느려질 수 있습니다.')) return;
        showMsg(true, '실행 중…');
        $.post('/super/alert/action.do', { alertId: ALERT_ID, action: action }, function (res) {
            if (res.status !== 'success') { showMsg(false, res.msg || '실패했습니다.'); return; }
            showMsg(true, res.msg);
            if (action === 'RECHECK') {
                $('#nowVal').text(res.valueText);
                $('#nowSev').attr('class', 'badge fs-6 badge-' + res.severity).text(res.severityLabel);
                $('#nowMsg').text(res.detail || '');
                $('#nowWhen').text('방금 다시 측정한 값 (' + new Date().toLocaleTimeString('ko-KR') + ')');
            }
            setTimeout(function () { location.reload(); }, action === 'RECHECK' ? 2500 : 1200);
        }).fail(function () { showMsg(false, '서버 통신 오류'); });
    }

    function saveNote() {
        var note = $.trim($('#noteText').val());
        if (!note) { showMsg(false, '조치 내용을 입력하세요.'); return; }
        $.post('/super/alert/action.do', { alertId: ALERT_ID, action: 'NOTE', note: note }, function (res) {
            showMsg(res.status === 'success', res.status === 'success' ? '조치 메모를 남겼습니다.' : (res.msg || '실패했습니다.'));
            if (res.status === 'success') setTimeout(function () { location.reload(); }, 800);
        }).fail(function () { showMsg(false, '서버 통신 오류'); });
    }

    function ack() {
        if (!confirm('이 경보를 확인 처리할까요? (복구와는 별개이며 긴급 재알림이 멈춥니다)')) return;
        $.post('/super/alert/ack.do', { alertId: ALERT_ID }, function (res) {
            showMsg(res.status === 'success', res.msg);
            if (res.status === 'success') setTimeout(function () { location.reload(); }, 800);
        }).fail(function () { showMsg(false, '처리에 실패했습니다.'); });
    }

    function testMail() {
        if (!confirm('설정된 수신자에게 시험 메일을 보냅니다. 계속할까요?')) return;
        showMsg(true, '발송 중… (최대 25초)');
        $.post('/super/alert/testMail.do', {}, function (res) {
            showMsg(res.status === 'success', res.msg);
        }).fail(function () { showMsg(false, '서버 통신 오류'); });
    }
</script>
</body>
</html>
