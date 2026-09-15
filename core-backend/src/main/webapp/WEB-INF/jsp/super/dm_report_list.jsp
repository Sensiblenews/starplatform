<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>채팅 신고 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        body { background-color: #f0f2f5; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .table-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        .stat-card { background: white; border-radius: 16px; padding: 18px 22px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        .stat-num { font-size: 30px; font-weight: 700; line-height: 1.1; }
        /* 증거 첨부는 원본 비율 그대로 보되 행 높이가 튀지 않게 상한을 둔다 */
        .review-thumb { width: 150px; height: 150px; object-fit: cover; border-radius: 10px;
                        background: #e9ecef; cursor: zoom-in; }
        .text-truncate-3 { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
        /* 신고 본문은 공격자가 쓴 글이다. 줄바꿈·긴 문자열이 표를 밀지 않게 가둔다 */
        .evidence-text { white-space: pre-wrap; word-break: break-all; }

        /* 부트스트랩 기본 nav-link 색이 흰 배경에서 거의 안 보여 대비를 올린다 */
        .nav-pills .nav-link {
            color: #343a40;
            font-weight: 600;
            background-color: #e9ecef;
            margin-right: 6px;
        }
        .nav-pills .nav-link:hover {
            background-color: #dee2e6;
            color: #000;
        }
        .nav-pills .nav-link.active {
            background-color: #0d6efd;
            color: #fff;
        }
        /* 탭별 건수 뱃지 */
        .nav-pills .nav-link .tab-count {
            display: inline-block;
            margin-left: 6px;
            padding: 0 7px;
            border-radius: 10px;
            background: rgba(0, 0, 0, 0.12);
            font-size: 12px;
        }
        .nav-pills .nav-link.active .tab-count {
            background: rgba(255, 255, 255, 0.28);
        }
    </style>
</head>
<body>

    <!-- 공통 사이드바 include -->
    <c:set var="activeMenu" value="dm_report" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <h2 class="fw-bold mb-4 text-danger"><i class="fas fa-comment-slash me-2"></i>채팅 신고</h2>

        <!-- 상태 요약 -->
        <div class="row g-3 mb-4">
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">미처리</div>
                    <div class="stat-num text-danger">${empty counts.OPEN_CNT ? 0 : counts.OPEN_CNT}</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">처리 완료</div>
                    <div class="stat-num text-dark">${empty counts.RESOLVED_CNT ? 0 : counts.RESOLVED_CNT}</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">기각</div>
                    <div class="stat-num text-secondary">${empty counts.DISMISSED_CNT ? 0 : counts.DISMISSED_CNT}</div>
                </div>
            </div>
        </div>

        <!-- 상태 탭 -->
        <ul class="nav nav-pills mb-3">
            <li class="nav-item">
                <a class="nav-link ${status eq 'OPEN' ? 'active' : ''}"
                   href="/super/dm-report/list.do?status=OPEN">미처리
                   <span class="tab-count">${empty counts.OPEN_CNT ? 0 : counts.OPEN_CNT}</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${status eq 'RESOLVED' ? 'active' : ''}"
                   href="/super/dm-report/list.do?status=RESOLVED">처리 완료
                   <span class="tab-count">${empty counts.RESOLVED_CNT ? 0 : counts.RESOLVED_CNT}</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${status eq 'DISMISSED' ? 'active' : ''}"
                   href="/super/dm-report/list.do?status=DISMISSED">기각
                   <span class="tab-count">${empty counts.DISMISSED_CNT ? 0 : counts.DISMISSED_CNT}</span></a>
            </li>
        </ul>

        <div class="table-card">
            <p class="text-muted small mb-3">
                채팅 메시지는 발송 5분 · 읽음 1분 뒤 자동으로 사라집니다.
                아래 내용은 신고 시점에 서버가 따로 복사해 둔 것으로, <strong>남아 있는 유일한 증거</strong>입니다.
                <br>
                영구 정지는 해당 스타 계정을 즉시 사용 중지(로그인 · 채팅 · 글쓰기 전부)합니다.
                <strong>정지 해제는 이 화면이 아니라 스타 관리 화면</strong>에서 합니다.
            </p>
            <table class="table table-hover align-middle" style="table-layout: fixed;">
                <thead class="table-light">
                    <tr>
                        <th width="11%">신고일시</th>
                        <th width="13%">신고자</th>
                        <th width="17%">피신고자</th>
                        <th width="10%">사유</th>
                        <th width="31%">신고된 메시지</th>
                        <th width="18%" class="text-center">처리</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="r" items="${list}">
                        <tr id="row-${r.RPT_ID}">
                            <td class="small text-muted"><c:out value="${r.CREATED_TEXT}" /></td>

                            <td class="small">
                                <div class="text-truncate"><c:out value="${r.REPORTER_NAME}" /></div>
                                <div class="text-muted text-truncate" style="font-size: 11px;">
                                    <c:out value="${r.REPORTER_PRS_ID}" />
                                </div>
                            </td>

                            <td class="small">
                                <div class="text-truncate fw-bold"><c:out value="${r.TARGET_NAME}" /></div>
                                <div class="text-muted text-truncate" style="font-size: 11px;">
                                    <c:out value="${r.TARGET_PRS_ID}" />
                                </div>
                                <div class="mt-1">
                                    <c:if test="${r.TARGET_REPORT_CNT > 1}">
                                        <span class="badge bg-warning text-dark">누적 ${r.TARGET_REPORT_CNT}건</span>
                                    </c:if>
                                    <c:choose>
                                        <c:when test="${r.TARGET_USE_YN eq 'N'}">
                                            <span class="badge bg-dark">정지됨</span>
                                        </c:when>
                                        <c:when test="${r.TARGET_USE_YN eq 'X'}">
                                            <span class="badge bg-secondary">계정 없음</span>
                                        </c:when>
                                    </c:choose>
                                </div>
                            </td>

                            <td><span class="badge bg-danger"><c:out value="${r.REASON}" /></span></td>

                            <td>
                                <c:choose>
                                    <%-- 텍스트는 반드시 c:out으로 내보낸다. 공격자가 쓴 2000자가 그대로 들어온다 --%>
                                    <c:when test="${r.CONTENT_TYPE eq 'TEXT'}">
                                        <div class="text-truncate-3 small evidence-text"><c:out value="${r.CONTENT_SNAPSHOT}" /></div>
                                    </c:when>
                                    <c:when test="${empty r.FILE_NM}">
                                        <span class="text-muted small">
                                            <i class="fas fa-triangle-exclamation me-1"></i>
                                            첨부가 이미 폭파돼 확보하지 못했습니다.
                                        </span>
                                    </c:when>
                                    <c:when test="${r.CONTENT_TYPE eq 'IMAGE'}">
                                        <img class="review-thumb"
                                             src="/super/dm-report/preview.do?rptId=${r.RPT_ID}&amp;kind=FILE"
                                             onclick="window.open(this.src, '_blank')"
                                             alt="신고된 사진" />
                                    </c:when>
                                    <c:otherwise>
                                        <%-- 썸네일은 발송 때 추출에 실패했을 수 있다. 없으면 poster 자체를 넣지 않는다 --%>
                                        <c:choose>
                                            <c:when test="${not empty r.THUMB_NM}">
                                                <video class="review-thumb" controls preload="metadata"
                                                       poster="/super/dm-report/preview.do?rptId=${r.RPT_ID}&amp;kind=THUMB"
                                                       src="/super/dm-report/preview.do?rptId=${r.RPT_ID}&amp;kind=FILE"></video>
                                            </c:when>
                                            <c:otherwise>
                                                <video class="review-thumb" controls preload="metadata"
                                                       src="/super/dm-report/preview.do?rptId=${r.RPT_ID}&amp;kind=FILE"></video>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:otherwise>
                                </c:choose>
                                <div class="text-muted mt-1" style="font-size: 11px;">
                                    발송 <c:out value="${r.SEND_TEXT}" /> · MSG ${r.MSG_ID}
                                </div>
                            </td>

                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${status eq 'OPEN'}">
                                        <div class="btn-group-vertical btn-group-sm w-100">
                                            <button class="btn btn-danger fw-bold"
                                                    onclick="takeAction('SUSPEND', '${r.RPT_ID}')">영구 정지</button>
                                            <button class="btn btn-outline-dark"
                                                    onclick="takeAction('RESOLVED', '${r.RPT_ID}')">조치 완료</button>
                                            <button class="btn btn-outline-secondary"
                                                    onclick="takeAction('DISMISSED', '${r.RPT_ID}')">기각</button>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="small text-muted">
                                            <div><c:out value="${r.HANDLED_TEXT}" /></div>
                                            <div><c:out value="${r.ADMIN_ID}" /></div>
                                            <c:if test="${not empty r.ADMIN_MEMO}">
                                                <div class="text-dark mt-1"><c:out value="${r.ADMIN_MEMO}" /></div>
                                            </c:if>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty list}">
                        <tr><td colspan="6" class="text-center py-5 text-muted">해당 상태의 신고가 없습니다.</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

<script>
function takeAction(action, rptId) {
    var confirmMsg;
    if (action === 'SUSPEND') {
        confirmMsg = '⚠️ 이 스타 계정을 영구 정지할까요?\n\n'
                   + '정지 즉시 로그인 · 채팅 · 글쓰기가 모두 막힙니다.\n'
                   + '해제는 스타 관리 화면에서 할 수 있습니다.';
    } else if (action === 'RESOLVED') {
        confirmMsg = '계정 정지 없이 이 신고를 처리 완료로 닫을까요?';
    } else {
        confirmMsg = '이 신고를 기각할까요?';
    }
    if (!confirm(confirmMsg)) {
        return;
    }

    var memo = prompt('처리 사유를 남겨주세요. (이력에 기록됩니다)') || '';

    $.post('/super/dm-report/action.do', {
        action: action,
        rptId: rptId,
        memo: memo
    }, function (res) {
        if (res.status === 'success') {
            if (action === 'SUSPEND') {
                // 같은 스타의 다른 신고 행도 상태 표시가 바뀌므로 전체를 다시 그린다
                location.reload();
            } else {
                // 처리된 행만 지운다. 전체 새로고침하면 검토 위치를 잃는다
                $('#row-' + rptId).fadeOut(200, function () { $(this).remove(); });
            }
        } else {
            alert(res.msg || '처리에 실패했습니다.');
        }
    }).fail(function () {
        alert('서버와 통신하지 못했습니다.');
    });
}
</script>
</body>
</html>
