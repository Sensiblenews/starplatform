<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%-- 어드민 인사이트 — 스타 한 명의 그래프 5종 (2-34차 2-2, 요청서 "해당 유저 어드민 이동 → 인사이트 바로 보기").
     스타 계정으로 대리 로그인하지 않고, 스타 본인 마이페이지와 같은 그래프를 여기서 보여 준다.
     2-34 3차 5번: 그래프 아래에 이 스타의 게시물 전체(검수 상태 무관)를 붙였다.
     2026-10-17: 게시물마다 오른쪽에 상단 고정·프로모션 설정과 삭제 버튼 (스타 관리 > 게시물 모달과 같은 API) --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>인사이트 · <c:out value="${star.PRS_NAME}"/> - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link href="/resources/css/insight-charts.css" rel="stylesheet">

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
    <script src="/resources/js/insight-charts.js"></script>

    <style>
        body { background-color: #f0f2f5; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; }
        .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
        .star-avatar { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; background: #ddd; }
        .table-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        .post-thumb { position: relative; width: 56px; height: 56px; border-radius: 8px; overflow: hidden; background: #e9ecef;
                      display: flex; align-items: center; justify-content: center; color: #adb5bd; flex-shrink: 0; }
        .post-thumb img { width: 100%; height: 100%; object-fit: cover; }
        .post-thumb .post-video { position: absolute; color: #fff; text-shadow: 0 1px 3px rgba(0,0,0,.6); }
        .post-body { max-width: 420px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

        @media (max-width: 768px) {
            .sidebar { width: 100%; height: auto; position: relative; padding-bottom: 10px; }
            .sidebar .nav { flex-direction: row !important; flex-wrap: wrap; justify-content: center; gap: 10px; }
            .sidebar .nav-item { margin-bottom: 0 !important; }
            .main-content { margin-left: 0; padding: 15px; }
        }
    </style>
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <a href="/super/insight/list.do?metric=${fromMetric}&period=${period}" class="text-secondary small"><i class="fas fa-arrow-left me-1"></i>목록으로</a>
        <div class="d-flex align-items-center gap-3 my-3">
            <img class="star-avatar" src="<c:out value="${star.STORED_FILE_NM}"/>" alt="" onerror="this.style.visibility='hidden'">
            <div>
                <h4 class="fw-bold mb-0"><c:out value="${star.PRS_NAME}"/></h4>
                <div class="text-secondary small"><c:out value="${star.PRS_ID}"/> · <c:out value="${star.PRS_COUNTRY}"/></div>
            </div>
        </div>

        <div class="insight-period" id="periodButtons">
            <c:forEach var="opt" items="${fn:split('DAY:매일,WEEK:매주,MONTH:매달,M3:3개월,M6:6개월,Y1:1년', ',')}">
                <c:set var="p" value="${fn:substringBefore(opt, ':')}" />
                <button type="button" data-period="${p}" class="${p eq period ? 'active' : ''}"><c:out value="${fn:substringAfter(opt, ':')}"/></button>
            </c:forEach>
        </div>
        <div class="text-secondary small mb-2">오늘 수치는 실시간, 지난 날짜는 하루 한 번 집계한 값입니다. 순위 그래프는 2-34차 배포일부터 그려집니다.</div>
        <div class="insight-charts" id="insightCharts"></div>

        <!-- 게시물 전체 (2-34 3차 5번) -->
        <div class="table-card mt-4" id="posts">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h5 class="fw-bold mb-0"><i class="fas fa-images text-primary me-2"></i>게시물 전체</h5>
                <div class="text-secondary small">총 <fmt:formatNumber value="${postTotal}"/>개 · 최신순 · 검수 상태와 무관하게 모두 표시</div>
            </div>
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead>
                        <tr>
                            <th style="width:80px;">ID</th>
                            <th>게시물</th>
                            <th style="width:90px;">상태</th>
                            <th style="width:150px;">작성일</th>
                            <th class="text-end">조회</th>
                            <th class="text-end">좋아요</th>
                            <th class="text-end">댓글</th>
                            <th class="text-end">저장</th>
                            <th class="text-end">공유</th>
                            <th class="text-end" style="width:180px;">관리</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="post" items="${posts}">
                            <tr>
                                <td class="text-secondary small">${post.CON_ID}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="post-thumb">
                                            <c:choose>
                                                <c:when test="${not empty post.THUMB_URL}">
                                                    <img src="<c:out value="${post.THUMB_URL}"/>" alt="" loading="lazy" onerror="this.style.visibility='hidden'">
                                                </c:when>
                                                <c:otherwise><i class="fas fa-align-left"></i></c:otherwise>
                                            </c:choose>
                                            <c:if test="${post.MEDIA_TYPE eq 'VIDEO'}"><i class="fas fa-play post-video"></i></c:if>
                                        </div>
                                        <div style="min-width:0;">
                                            <div class="post-body" title="<c:out value="${post.CON_BODY}"/>">
                                                <c:if test="${post.IS_PINNED eq 'Y'}"><span class="badge bg-warning text-dark me-1"><i class="fas fa-thumbtack"></i></span></c:if>
                                                <c:choose>
                                                    <c:when test="${not empty post.CON_BODY}"><c:out value="${post.CON_BODY}"/></c:when>
                                                    <c:otherwise><span class="text-secondary">(본문 없음)</span></c:otherwise>
                                                </c:choose>
                                            </div>
                                            <c:if test="${post.MDR_STATUS eq 'APPROVED'}">
                                                <a class="small" href="/post/${post.CON_ID}" target="_blank" rel="noopener">웹에서 보기 <i class="fas fa-external-link-alt"></i></a>
                                            </c:if>
                                        </div>
                                    </div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${post.MDR_STATUS eq 'APPROVED'}"><span class="badge bg-success">승인</span></c:when>
                                        <c:when test="${post.MDR_STATUS eq 'PENDING'}"><span class="badge bg-secondary">검수 대기</span></c:when>
                                        <c:when test="${post.MDR_STATUS eq 'REJECTED'}"><span class="badge bg-danger">반려</span></c:when>
                                        <c:when test="${post.MDR_STATUS eq 'HIDDEN'}"><span class="badge bg-dark">숨김</span></c:when>
                                        <c:otherwise><span class="badge bg-light text-dark"><c:out value="${post.MDR_STATUS}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="small text-secondary"><c:out value="${fn:substring(post.CREATED_DATE, 0, 16)}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${post.VIEW_CNT}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${post.LIKE_CNT}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${post.COMMENT_CNT}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${post.SAVE_CNT}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${post.SHARE_CNT}"/></td>
                                <td class="text-end text-nowrap">
                                    <%-- 프로모션 링크는 data-* 로 넘겨 모달에 미리 채운다 (escapeXml 로 속성 깨짐 방지) --%>
                                    <button type="button" class="btn btn-sm ${post.IS_PINNED eq 'Y' ? 'btn-warning' : 'btn-outline-warning'} fw-bold btn-pin"
                                            data-con-id="${post.CON_ID}" data-pinned="${post.IS_PINNED eq 'Y' ? 'Y' : 'N'}"
                                            data-t1="${fn:escapeXml(post.PIN_LINK_TEXT1)}" data-u1="${fn:escapeXml(post.PIN_LINK_URL1)}"
                                            data-t2="${fn:escapeXml(post.PIN_LINK_TEXT2)}" data-u2="${fn:escapeXml(post.PIN_LINK_URL2)}">
                                        <i class="fas fa-thumbtack me-1"></i>${post.IS_PINNED eq 'Y' ? '고정·프로모션' : '상단 고정'}
                                    </button>
                                    <button type="button" class="btn btn-sm btn-outline-danger btn-delete" data-con-id="${post.CON_ID}" title="삭제">
                                        <i class="fas fa-trash-alt"></i>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty posts}">
                            <tr><td colspan="10" class="text-center text-secondary py-4">게시물이 없습니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>

            <c:if test="${postTotalPages > 1}">
                <nav class="mt-3">
                    <ul class="pagination justify-content-center flex-wrap mb-0">
                        <c:if test="${postPage > 1}">
                            <li class="page-item"><a class="page-link" href="?id=<c:out value="${star.PRS_ID}"/>&period=${period}&from=${fromMetric}&page=${postPage - 1}#posts">이전</a></li>
                        </c:if>
                        <c:forEach var="p" begin="${postPage - 4 < 1 ? 1 : postPage - 4}" end="${postPage + 4 > postTotalPages ? postTotalPages : postPage + 4}">
                            <li class="page-item ${p eq postPage ? 'active' : ''}">
                                <a class="page-link" href="?id=<c:out value="${star.PRS_ID}"/>&period=${period}&from=${fromMetric}&page=${p}#posts">${p}</a>
                            </li>
                        </c:forEach>
                        <c:if test="${postPage < postTotalPages}">
                            <li class="page-item"><a class="page-link" href="?id=<c:out value="${star.PRS_ID}"/>&period=${period}&from=${fromMetric}&page=${postPage + 1}#posts">다음</a></li>
                        </c:if>
                    </ul>
                </nav>
            </c:if>
        </div>
    </div>

    <!-- 상단 고정 및 프로모션 설정 (스타 관리 > 게시물의 pinPromotionModal 과 같은 항목·같은 API) -->
    <div class="modal fade" id="pinPromotionModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-warning text-dark">
                    <h5 class="modal-title fw-bold"><i class="fas fa-thumbtack me-2"></i>상단 고정 및 프로모션 설정</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body bg-light">
                    <p class="text-muted small mb-3">이 게시물을 해당 스타의 상단에 고정하며, 우측에 노출될 전용 홍보 링크를 설정합니다.</p>
                    <div class="mb-3">
                        <label class="form-label fw-bold text-primary">링크 1 (선택)</label>
                        <input type="text" id="feedPromoText1" class="form-control mb-2" placeholder="표시 텍스트 (예: 쿠팡)">
                        <input type="text" id="feedPromoUrl1" class="form-control" placeholder="URL (https://...)">
                    </div>
                    <div class="mb-2">
                        <label class="form-label fw-bold text-primary">링크 2 (선택)</label>
                        <input type="text" id="feedPromoText2" class="form-control mb-2" placeholder="표시 텍스트 (예: 네이버)">
                        <input type="text" id="feedPromoUrl2" class="form-control" placeholder="URL (https://...)">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary me-auto" id="btnUnpin" onclick="unpinFeed()">고정 해제</button>
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button type="button" class="btn btn-warning fw-bold" id="btnPinSubmit" onclick="submitFeedPin()">고정하기</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        // ===== 게시물 상단 고정·프로모션·삭제 (2026-10-17) =====
        var currentConId = '';

        // 바뀐 뒤에는 게시물 표 위치로 돌아와 새로 그린다 (같은 페이지·기간 유지)
        function reloadPosts() {
            location.hash = 'posts';
            location.reload();
        }

        // 고정 안 된 글 → 빈 모달로 고정, 고정된 글 → 지금 링크를 채워 수정·해제
        $(document).on('click', '.btn-pin', function () {
            var $b = $(this);
            var pinned = $b.data('pinned') === 'Y';
            currentConId = String($b.data('con-id'));
            $('#feedPromoText1').val(pinned ? ($b.attr('data-t1') || '') : '');
            $('#feedPromoUrl1').val(pinned ? ($b.attr('data-u1') || '') : '');
            $('#feedPromoText2').val(pinned ? ($b.attr('data-t2') || '') : '');
            $('#feedPromoUrl2').val(pinned ? ($b.attr('data-u2') || '') : '');
            $('#btnUnpin').toggle(pinned);
            $('#btnPinSubmit').text(pinned ? '저장' : '고정하기');
            $('#pinPromotionModal').modal('show');
        });

        function submitFeedPin() {
            executePinUpdate('Y', $('#feedPromoText1').val().trim(), $('#feedPromoUrl1').val().trim(),
                $('#feedPromoText2').val().trim(), $('#feedPromoUrl2').val().trim());
        }

        function unpinFeed() {
            if (!confirm('상단 고정을 해제하시겠습니까?')) return;
            executePinUpdate('N', '', '', '', '');
        }

        function executePinUpdate(targetStatus, t1, u1, t2, u2) {
            $.post('/super/star/togglePin.do', {
                conId: currentConId, targetStatus: targetStatus,
                pinLinkText1: t1, pinLinkUrl1: u1, pinLinkText2: t2, pinLinkUrl2: u2
            }, function (res) {
                if (res && res.status === 'success') reloadPosts();
                else alert('실패: ' + ((res && res.msg) || '로그인이 만료되었을 수 있습니다.'));
            }).fail(function () { alert('서버 오류가 발생했습니다.'); });
        }

        $(document).on('click', '.btn-delete', function () {
            if (!confirm('정말 삭제하시겠습니까? 복구할 수 없습니다.')) return;
            $.post('/super/star/deleteFeed.do', { conId: String($(this).data('con-id')) }, function (res) {
                if (res && res.status === 'success') reloadPosts();
                else alert('삭제 실패: ' + ((res && res.msg) || ''));
            }).fail(function () { alert('서버 오류가 발생했습니다.'); });
        });

        var STAR_ID = '<c:out value="${star.PRS_ID}"/>';

        function loadInsight(period) {
            $('#periodButtons button').removeClass('active').filter('[data-period="' + period + '"]').addClass('active');
            $.getJSON('/super/insight/series.json', { id: STAR_ID, period: period }, function (res) {
                if (!res || res.result !== 'OK') {
                    $('#insightCharts').html('<div class="text-danger">' + $('<div>').text((res && res.msg) || '불러오지 못했습니다.').html() + '</div>');
                    return;
                }
                InsightCharts.render(document.getElementById('insightCharts'), res);
            });
        }

        $('#periodButtons').on('click', 'button', function () { loadInsight($(this).data('period')); });
        loadInsight('${period}');
    </script>
</body>
</html>
