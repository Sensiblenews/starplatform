<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>스타 직군 분류 - Super Admin</title>
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
        .badge-general { background-color: #eceff1; color: #546e7a; }

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
    <c:set var="activeMenu" value="star_category" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="mb-4">
            <h4 class="fw-bold mb-1"><i class="fas fa-tags text-primary me-2"></i>스타 직군 분류</h4>
            <div class="text-secondary small">
                VS 배틀필드 랭킹에 사용되는 직군을 지정합니다. 드롭다운 변경 시 즉시 저장됩니다.
                미분류(GENERAL) 스타는 랭킹에서 후순위로 노출되며 직군 탭에는 나타나지 않습니다.
            </div>
        </div>

        <!-- 🔍 검색 + 직군 필터 -->
        <div class="card border-0 shadow-sm p-3 mb-3">
            <form action="/super/star/category.do" method="GET" class="row g-2 align-items-center">
                <div class="col-md-4 col-sm-8">
                    <div class="input-group">
                        <span class="input-group-text bg-light border-end-0 text-secondary"><i class="fas fa-search"></i></span>
                        <input type="text" name="searchKeyword" class="form-control border-start-0 ps-0" placeholder="스타 이름 또는 ID로 검색..." value="<c:out value="${searchKeyword}"/>">
                    </div>
                </div>
                <div class="col-md-3 col-sm-6">
                    <select name="filterCategory" class="form-select">
                        <option value="">전체 직군</option>
                        <option value="GENERAL" <c:if test="${filterCategory eq 'GENERAL'}">selected</c:if>>미분류 (GENERAL)</option>
                        <option value="STAR" <c:if test="${filterCategory eq 'STAR'}">selected</c:if>>⭐ 스타</option>
                        <option value="CELEB" <c:if test="${filterCategory eq 'CELEB'}">selected</c:if>>👤 셀럽</option>
                        <option value="BRAND" <c:if test="${filterCategory eq 'BRAND'}">selected</c:if>>🏢 기업</option>
                        <option value="UNIV" <c:if test="${filterCategory eq 'UNIV'}">selected</c:if>>🎓 대학</option>
                        <option value="CITY" <c:if test="${filterCategory eq 'CITY'}">selected</c:if>>🌆 도시</option>
                        <option value="MEDIA" <c:if test="${filterCategory eq 'MEDIA'}">selected</c:if>>📰 언론</option>
                    </select>
                </div>
                <div class="col-md-2 col-sm-4">
                    <button type="submit" class="btn btn-primary w-100"><i class="fas fa-search me-1"></i>검색</button>
                </div>
                <c:if test="${not empty searchKeyword or not empty filterCategory}">
                    <div class="col-md-2 col-sm-4">
                        <a href="/super/star/category.do" class="btn btn-outline-secondary w-100">초기화</a>
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
                            <th>누적 광고노출</th>
                            <th>즐겨찾기 수</th>
                            <th style="width:200px;">직군</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="star" items="${starList}">
                            <tr>
                                <td class="text-secondary small">${star.PRS_ID}</td>
                                <td class="fw-bold"><c:out value="${star.PRS_NAME}"/></td>
                                <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                                    <td>${star.PRS_COUNTRY}</td>
                                </c:if>
                                <td>${star.AD_VIEW_CNT}</td>
                                <td>${star.FOLLOWER_CNT}</td>
                                <td>
                                    <select class="form-select form-select-sm category-select" data-prs-id="${star.PRS_ID}" data-original="${star.STAR_CATEGORY}">
                                        <option value="GENERAL" <c:if test="${star.STAR_CATEGORY eq 'GENERAL'}">selected</c:if>>미분류</option>
                                        <option value="STAR" <c:if test="${star.STAR_CATEGORY eq 'STAR'}">selected</c:if>>⭐ 스타</option>
                                        <option value="CELEB" <c:if test="${star.STAR_CATEGORY eq 'CELEB'}">selected</c:if>>👤 셀럽</option>
                                        <option value="BRAND" <c:if test="${star.STAR_CATEGORY eq 'BRAND'}">selected</c:if>>🏢 기업</option>
                                        <option value="UNIV" <c:if test="${star.STAR_CATEGORY eq 'UNIV'}">selected</c:if>>🎓 대학</option>
                                        <option value="CITY" <c:if test="${star.STAR_CATEGORY eq 'CITY'}">selected</c:if>>🌆 도시</option>
                                        <option value="MEDIA" <c:if test="${star.STAR_CATEGORY eq 'MEDIA'}">selected</c:if>>📰 언론</option>
                                    </select>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty starList}">
                            <tr><td colspan="6" class="text-center text-secondary py-4">조회된 스타가 없습니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>

            <!-- 페이징 (star_list와 동일한 슬라이딩 윈도우) -->
            <nav class="mt-3">
                <ul class="pagination justify-content-center mb-0">
                    <c:if test="${currentPage > 1}">
                        <li class="page-item">
                            <a class="page-link" href="?page=${currentPage - 1}&searchKeyword=<c:out value="${searchKeyword}"/>&filterCategory=${filterCategory}">이전</a>
                        </li>
                    </c:if>
                    <c:forEach var="p" begin="${startPage}" end="${endPage}">
                        <li class="page-item <c:if test="${p eq currentPage}">active</c:if>">
                            <a class="page-link" href="?page=${p}&searchKeyword=<c:out value="${searchKeyword}"/>&filterCategory=${filterCategory}">${p}</a>
                        </li>
                    </c:forEach>
                    <c:if test="${currentPage < totalPages}">
                        <li class="page-item">
                            <a class="page-link" href="?page=${currentPage + 1}&searchKeyword=<c:out value="${searchKeyword}"/>&filterCategory=${filterCategory}">다음</a>
                        </li>
                    </c:if>
                </ul>
            </nav>
        </div>
    </div>

    <script>
        // 드롭다운 변경 즉시 저장. 실패 시 원래 값으로 롤백한다
        $('.category-select').on('change', function() {
            var $sel = $(this);
            var prsId = $sel.data('prs-id');
            var category = $sel.val();
            var original = $sel.data('original');

            $sel.prop('disabled', true);
            $.post('/super/star/updateCategory.do', { prsId: prsId, category: category }, function(res) {
                if (res.status === 'success') {
                    $sel.data('original', category);
                } else {
                    alert('저장 실패: ' + (res.msg || ''));
                    $sel.val(original);
                }
            }).fail(function() {
                alert('서버 오류가 발생했습니다.');
                $sel.val(original);
            }).always(function() {
                $sel.prop('disabled', false);
            });
        });
    </script>
</body>
</html>
