<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>스타 소개(Bio) 관리 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <style>
        body { background-color: #f0f2f5; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; }
        .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
        .table-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        .bio-preview { max-width: 320px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

        @media (max-width: 768px) {
            .sidebar { width: 100%; height: auto; position: relative; padding-bottom: 10px; }
            .sidebar .nav { flex-direction: row !important; flex-wrap: wrap; justify-content: center; gap: 10px; }
            .sidebar .nav-item { margin-bottom: 0 !important; }
            .main-content { margin-left: 0; padding: 15px; }
        }
    </style>
</head>
<body>
    <!-- 공통 사이드바 include -->
    <c:set var="activeMenu" value="star_bio" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="mb-4">
            <h4 class="fw-bold mb-1"><i class="fas fa-id-card text-primary me-2"></i>스타 소개(Bio) 관리</h4>
            <div class="text-secondary small">
                웹 랜딩 페이지(/star/…)의 About 섹션에 노출되는 소개문입니다. 스타별 고유 내용으로 작성해 주세요.
                "웹 노출" 표시가 있는 스타(승인 게시물 보유)가 실제 검색 노출 대상이므로 우선 작성 대상입니다.
            </div>
        </div>

        <!-- 🔍 검색 + 작성 여부 필터 -->
        <div class="card border-0 shadow-sm p-3 mb-3">
            <form action="/super/star/bio.do" method="GET" class="row g-2 align-items-center">
                <div class="col-md-4 col-sm-8">
                    <div class="input-group">
                        <span class="input-group-text bg-light border-end-0 text-secondary"><i class="fas fa-search"></i></span>
                        <input type="text" name="searchKeyword" class="form-control border-start-0 ps-0" placeholder="스타 이름 또는 ID로 검색..." value="<c:out value="${searchKeyword}"/>">
                    </div>
                </div>
                <div class="col-md-3 col-sm-6">
                    <select name="filterBio" class="form-select">
                        <option value="">전체</option>
                        <option value="EMPTY" <c:if test="${filterBio eq 'EMPTY'}">selected</c:if>>미작성</option>
                        <option value="FILLED" <c:if test="${filterBio eq 'FILLED'}">selected</c:if>>작성 완료</option>
                    </select>
                </div>
                <div class="col-md-2 col-sm-4">
                    <button type="submit" class="btn btn-primary w-100"><i class="fas fa-search me-1"></i>검색</button>
                </div>
                <c:if test="${not empty searchKeyword or not empty filterBio}">
                    <div class="col-md-2 col-sm-4">
                        <a href="/super/star/bio.do" class="btn btn-outline-secondary w-100">초기화</a>
                    </div>
                </c:if>
            </form>
        </div>

        <div class="table-card p-3">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-secondary small">총 <b>${totalCount}</b>명</span>
            </div>
            <div class="table-responsive">
                <table class="table table-hover align-middle text-nowrap mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>이름</th>
                            <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                                <th>국가</th>
                            </c:if>
                            <th>즐겨찾기 수</th>
                            <th>웹 노출</th>
                            <th>소개문</th>
                            <th style="width:90px;"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="star" items="${starList}">
                            <tr id="bio-row-${star.PRS_ID}">
                                <td class="text-secondary small">${star.PRS_ID}</td>
                                <td class="fw-bold"><c:out value="${star.PRS_NAME}"/></td>
                                <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                                    <td>${star.PRS_COUNTRY}</td>
                                </c:if>
                                <td>${star.FOLLOWER_CNT}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${star.HAS_CONTENT eq 1}"><span class="badge bg-success-subtle text-success">웹 노출</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary-subtle text-secondary">비노출</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty star.PRS_BIO}">
                                            <div class="bio-preview text-secondary small" data-role="preview"><c:out value="${fn:length(star.PRS_BIO) > 80 ? fn:substring(star.PRS_BIO, 0, 80) : star.PRS_BIO}"/><c:if test="${fn:length(star.PRS_BIO) > 80}">…</c:if></div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="bio-preview text-danger small" data-role="preview">미작성</div>
                                        </c:otherwise>
                                    </c:choose>
                                    <%-- 원문 보관용 (개행·따옴표가 깨지지 않도록 data-속성 대신 hidden textarea 사용) --%>
                                    <textarea class="d-none" data-role="raw" readonly><c:out value="${star.PRS_BIO}"/></textarea>
                                </td>
                                <td>
                                    <button type="button" class="btn btn-sm btn-outline-primary bio-edit-btn"
                                            data-prs-id="${star.PRS_ID}">
                                        <i class="fas fa-pen me-1"></i>편집
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty starList}">
                            <tr><td colspan="7" class="text-center text-secondary py-4">조회된 스타가 없습니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>

            <!-- 페이징 (star_category와 동일한 슬라이딩 윈도우) -->
            <nav class="mt-3">
                <ul class="pagination justify-content-center mb-0">
                    <c:if test="${currentPage > 1}">
                        <li class="page-item">
                            <a class="page-link" href="?page=${currentPage - 1}&searchKeyword=<c:out value="${searchKeyword}"/>&filterBio=${filterBio}">이전</a>
                        </li>
                    </c:if>
                    <c:forEach var="p" begin="${startPage}" end="${endPage}">
                        <li class="page-item <c:if test="${p eq currentPage}">active</c:if>">
                            <a class="page-link" href="?page=${p}&searchKeyword=<c:out value="${searchKeyword}"/>&filterBio=${filterBio}">${p}</a>
                        </li>
                    </c:forEach>
                    <c:if test="${currentPage < totalPages}">
                        <li class="page-item">
                            <a class="page-link" href="?page=${currentPage + 1}&searchKeyword=<c:out value="${searchKeyword}"/>&filterBio=${filterBio}">다음</a>
                        </li>
                    </c:if>
                </ul>
            </nav>
        </div>
    </div>

    <!-- ✏️ 소개문 편집 모달 -->
    <div class="modal fade" id="bioModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fas fa-pen me-2"></i>소개문 편집 — <span id="bioModalStarName"></span></h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                </div>
                <div class="modal-body">
                    <textarea id="bioModalText" class="form-control" rows="10" maxlength="${bioMaxLength}"
                              placeholder="이 스타만의 소개문을 작성하세요. (활동 분야, 주요 이력, 콘텐츠 특징 등)"></textarea>
                    <div class="text-end text-secondary small mt-1"><span id="bioCharCount">0</span> / ${bioMaxLength}</div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button type="button" class="btn btn-primary" id="bioSaveBtn"><i class="fas fa-save me-1"></i>저장</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        var bioModal = new bootstrap.Modal(document.getElementById('bioModal'));
        var currentPrsId = null;
        var BIO_MAX = ${bioMaxLength};

        // 편집 버튼 → 행의 hidden textarea 원문을 모달로 옮긴다
        $('.bio-edit-btn').on('click', function() {
            var $row = $(this).closest('tr');
            currentPrsId = $(this).data('prs-id');
            $('#bioModalStarName').text($row.find('td.fw-bold').text());
            $('#bioModalText').val($row.find('[data-role="raw"]').val());
            $('#bioCharCount').text($('#bioModalText').val().length);
            bioModal.show();
        });

        $('#bioModalText').on('input', function() {
            $('#bioCharCount').text($(this).val().length);
        });

        // 저장. 성공 시 행의 원문·미리보기를 갱신하고 모달을 닫는다
        $('#bioSaveBtn').on('click', function() {
            if (!currentPrsId) return;
            var bio = $('#bioModalText').val();
            if (bio.length > BIO_MAX) {
                alert('소개문은 ' + BIO_MAX + '자를 넘을 수 없습니다.');
                return;
            }
            var $btn = $(this);
            $btn.prop('disabled', true);
            $.post('/super/star/updateBio.do', { prsId: currentPrsId, bio: bio }, function(res) {
                if (res.status === 'success') {
                    var $row = $('#bio-row-' + $.escapeSelector(String(currentPrsId)));
                    $row.find('[data-role="raw"]').val(bio);
                    var trimmed = bio.trim();
                    var $preview = $row.find('[data-role="preview"]');
                    if (trimmed.length > 0) {
                        $preview.removeClass('text-danger').addClass('text-secondary')
                            .text(trimmed.length > 80 ? trimmed.substring(0, 80) + '…' : trimmed);
                    } else {
                        $preview.removeClass('text-secondary').addClass('text-danger').text('미작성');
                    }
                    bioModal.hide();
                } else {
                    alert('저장 실패: ' + (res.msg || ''));
                }
            }).fail(function() {
                alert('서버 오류가 발생했습니다.');
            }).always(function() {
                $btn.prop('disabled', false);
            });
        });
    </script>
</body>
</html>
