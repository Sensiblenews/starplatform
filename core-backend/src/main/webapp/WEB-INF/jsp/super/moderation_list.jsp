<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>이미지 검수 - Super Admin</title>
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
        /* 검수 이미지는 원본 비율 그대로 보되 행 높이가 튀지 않게 상한을 둔다 */
        .review-thumb { width: 150px; height: 150px; object-fit: cover; border-radius: 10px;
                        background: #e9ecef; cursor: zoom-in; }
        .text-truncate-3 { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
        .waiting-long { color: #d63384; font-weight: 600; }

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
    <c:set var="activeMenu" value="moderation" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <h2 class="fw-bold mb-4"><i class="fas fa-image me-2"></i>이미지 검수</h2>

        <!-- 상태 요약 -->
        <div class="row g-3 mb-4">
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">검수 대기</div>
                    <div class="stat-num text-warning">${counts.PENDING_CNT}</div>
                </div>
            </div>
            <div class="col-md-2">
                <div class="stat-card">
                    <div class="text-muted small">검수 거절</div>
                    <div class="stat-num text-danger">${counts.REJECTED_CNT}</div>
                </div>
            </div>
            <div class="col-md-2">
                <div class="stat-card">
                    <div class="text-muted small">신고 블라인드</div>
                    <div class="stat-num text-dark">${counts.HIDDEN_CNT}</div>
                </div>
            </div>
            <div class="col-md-5">
                <div class="stat-card">
                    <div class="text-muted small">가장 오래 기다린 건</div>
                    <div class="mt-1">
                        <c:choose>
                            <c:when test="${empty counts.OLDEST_MEMBER and empty counts.OLDEST_STAR}">
                                <span class="text-muted">대기 중인 건이 없습니다.</span>
                            </c:when>
                            <c:otherwise>
                                <span class="waiting-long">
                                    회원 <c:out value="${empty counts.OLDEST_MEMBER ? '-' : counts.OLDEST_MEMBER}" />
                                    &nbsp;/&nbsp;
                                    스타 <c:out value="${empty counts.OLDEST_STAR ? '-' : counts.OLDEST_STAR}" />
                                </span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>

        <!-- 상태 탭 -->
        <ul class="nav nav-pills mb-3">
            <li class="nav-item">
                <a class="nav-link ${status eq 'PENDING' ? 'active' : ''}"
                   href="/super/moderation/list.do?status=PENDING">검수 대기
                   <span class="tab-count">${counts.PENDING_CNT}</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${status eq 'REJECTED' ? 'active' : ''}"
                   href="/super/moderation/list.do?status=REJECTED">검수 거절
                   <span class="tab-count">${counts.REJECTED_CNT}</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${status eq 'HIDDEN' ? 'active' : ''}"
                   href="/super/moderation/list.do?status=HIDDEN">신고 블라인드
                   <span class="tab-count">${counts.HIDDEN_CNT}</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${status eq 'APPROVED' ? 'active' : ''}"
                   href="/super/moderation/list.do?status=APPROVED">최근 승인</a>
            </li>
        </ul>

        <div class="table-card">
            <p class="text-muted small mb-3">
                승인 전까지 게시물은 작성자 외에는 보이지 않습니다. 오래 기다린 건부터 표시합니다.
            </p>
            <table class="table table-hover align-middle" style="table-layout: fixed;">
                <thead class="table-light">
                    <tr>
                        <th width="18%">이미지</th>
                        <th width="12%">구분</th>
                        <th width="14%">작성자</th>
                        <th width="30%">본문</th>
                        <th width="12%">등록일시</th>
                        <th width="14%" class="text-center">처리</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="q" items="${queue}">
                        <tr id="row-${q.TARGET_TYPE}-${q.TARGET_ID}">
                            <td>
                                <c:choose>
                                    <c:when test="${empty q.IMAGE_URL}">
                                        <span class="text-muted small">이미지 없음</span>
                                    </c:when>
                                    <c:otherwise>
                                        <%-- 대기 중 파일은 공개 디렉터리에 없으므로 관리자 전용 경로로 불러온다.
                                             c:url + c:param으로 감싸 URL 인코딩을 맡긴다 --%>
                                        <c:url var="previewUrl" value="/super/moderation/preview.do">
                                            <c:param name="file" value="${q.IMAGE_URL}" />
                                        </c:url>
                                        <img class="review-thumb" src="${previewUrl}"
                                             onclick="window.open(this.src, '_blank')"
                                             alt="검수 대상 이미지" />
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${q.TARGET_TYPE eq 'STAR_FEED'}">
                                        <span class="badge bg-primary">스타 피드</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary">회원 글</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-truncate"><c:out value="${q.WRITER}" /></td>
                            <td class="text-truncate-3 small"><c:out value="${q.BODY}" /></td>
                            <td class="small text-muted">
                                <%-- 날짜는 SQL에서 문자열로 만들어 온다.
                                     fmt:formatDate는 값이 java.util.Date가 아니면 예외를 던지는데,
                                     이 저장소는 날짜 컬럼 타입이 화면마다 제각각이다 --%>
                                <c:out value="${q.CREATED_TEXT}" />
                            </td>
                            <td class="text-center">
                                <div class="btn-group btn-group-sm">
                                    <c:if test="${status ne 'APPROVED'}">
                                        <button class="btn btn-success"
                                                onclick="takeAction('APPROVED', '${q.TARGET_TYPE}', '${q.TARGET_ID}')">공개</button>
                                    </c:if>
                                    <c:if test="${status ne 'REJECTED'}">
                                        <button class="btn btn-outline-danger"
                                                onclick="takeAction('REJECTED', '${q.TARGET_TYPE}', '${q.TARGET_ID}')">거절</button>
                                    </c:if>
                                    <c:if test="${status ne 'HIDDEN'}">
                                        <button class="btn btn-outline-dark"
                                                onclick="takeAction('HIDDEN', '${q.TARGET_TYPE}', '${q.TARGET_ID}')">블라인드</button>
                                    </c:if>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty queue}">
                        <tr><td colspan="6" class="text-center py-5 text-muted">해당 상태의 게시물이 없습니다.</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

<script>
function takeAction(action, targetType, targetId) {
    var label = (action === 'APPROVED') ? '공개'
              : (action === 'REJECTED') ? '검수 거절' : '신고 블라인드';
    if (!confirm('이 게시물을 ' + label + ' 처리할까요?')) {
        return;
    }

    var reason = '';
    if (action !== 'APPROVED') {
        reason = prompt(label + ' 사유를 남겨주세요. (이력에 기록됩니다)') || '';
    }

    $.post('/super/moderation/action.do', {
        action: action,
        targetType: targetType,
        targetId: targetId,
        reason: reason
    }, function (res) {
        if (res.status === 'success') {
            // 처리된 행만 지운다. 전체 새로고침하면 검수 위치를 잃는다
            $('#row-' + targetType + '-' + targetId).fadeOut(200, function () { $(this).remove(); });
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
