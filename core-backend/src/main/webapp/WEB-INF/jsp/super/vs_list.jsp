<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>VS 배틀필드 관리 - Super Admin</title>
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

        /* 드래그 정렬 */
        #vsTableBody tr { cursor: grab; }
        #vsTableBody tr.dragging { opacity: 0.4; }
        .drag-handle { color: #adb5bd; cursor: grab; }

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
    <c:set var="activeMenu" value="vs_list" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
            <div>
                <h4 class="fw-bold mb-1"><i class="fas fa-bolt text-danger me-2"></i>VS 배틀필드 관리</h4>
                <div class="text-secondary small">
                    로비 VS 카드의 노출 순서·고정·특별전을 관리합니다.
                    행을 드래그해 순서를 바꾼 뒤 <b>순서 저장</b>을 눌러주세요. (고정 카드가 항상 먼저 노출됩니다)
                </div>
            </div>
            <div>
                <button onclick="saveOrder()" class="btn btn-success me-2">
                    <i class="fas fa-save me-2"></i>순서 저장
                </button>
                <button onclick="openCustomModal()" class="btn btn-danger">
                    <i class="fas fa-plus me-2"></i>커스텀 VS 등록
                </button>
            </div>
        </div>

        <div class="table-card p-3">
            <div class="table-responsive">
                <table class="table table-hover align-middle text-nowrap mb-0">
                    <thead class="table-light">
                        <tr>
                            <th style="width:40px;"></th>
                            <th>종류</th>
                            <th>매치업</th>
                            <th class="text-center">고정 (Pin)</th>
                            <th class="text-center">삭제</th>
                        </tr>
                    </thead>
                    <tbody id="vsTableBody">
                        <c:forEach var="vs" items="${vsList}">
                            <tr draggable="true" data-vs-id="${vs.VS_ID}">
                                <td><i class="fas fa-grip-vertical drag-handle"></i></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${vs.CARD_KIND eq 'CUSTOM'}">
                                            <span class="badge bg-danger">커스텀</span>
                                        </c:when>
                                        <c:when test="${vs.RANK_TYPE eq 'DAILY'}">
                                            <span class="badge bg-warning text-dark">데일리 킹</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-primary">글로벌 랭킹</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${vs.CARD_KIND eq 'CUSTOM'}">
                                            <b><c:out value="${vs.LEFT_NAME}"/></b>
                                            <span class="text-danger fw-bold mx-1">VS</span>
                                            <b><c:out value="${vs.RIGHT_NAME}"/></b>
                                            <c:if test="${not empty vs.TITLE}">
                                                <span class="text-secondary small ms-2">(<c:out value="${vs.TITLE}"/>)</span>
                                            </c:if>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="cat-label" data-cat="${vs.CATEGORY}"></span>
                                            <span class="text-secondary small ms-1">1위 vs 2위 자동 매치업</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${vs.IS_PINNED eq 'Y'}">
                                            <button class="btn btn-sm btn-warning" onclick="togglePin('${vs.VS_ID}', 'N')">
                                                <i class="fas fa-thumbtack me-1"></i>고정됨
                                            </button>
                                        </c:when>
                                        <c:otherwise>
                                            <button class="btn btn-sm btn-outline-secondary" onclick="togglePin('${vs.VS_ID}', 'Y')">
                                                <i class="fas fa-thumbtack me-1"></i>고정
                                            </button>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center">
                                    <c:if test="${vs.CARD_KIND eq 'CUSTOM'}">
                                        <button class="btn btn-sm btn-outline-danger" onclick="deleteCustom('${vs.VS_ID}')">
                                            <i class="fas fa-trash"></i>
                                        </button>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- 커스텀 VS 등록 모달 -->
    <div class="modal fade" id="customVsModal" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title">⚡ 커스텀 VS (특별전) 등록</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-bold">좌측 스타</label>
                        <div class="input-group mb-1">
                            <input type="text" id="leftKeyword" class="form-control" placeholder="이름/ID 검색">
                            <button class="btn btn-outline-primary" onclick="searchStar('left')">검색</button>
                        </div>
                        <select id="leftSelect" class="form-select" size="4"></select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold">우측 스타</label>
                        <div class="input-group mb-1">
                            <input type="text" id="rightKeyword" class="form-control" placeholder="이름/ID 검색">
                            <button class="btn btn-outline-danger" onclick="searchStar('right')">검색</button>
                        </div>
                        <select id="rightSelect" class="form-select" size="4"></select>
                    </div>
                    <div class="mb-2">
                        <label class="form-label fw-bold">표시명 (선택)</label>
                        <input type="text" id="customTitle" class="form-control" maxlength="100" placeholder="예: Special Match">
                    </div>
                    <div class="text-secondary small">등록된 특별전은 고정(Pin) 상태로 카드 맨 앞에 노출됩니다.</div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button class="btn btn-danger" onclick="submitCustom()">등록</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        // 카테고리 코드 → 한글 라벨 (JSP EL과 백틱 충돌을 피하려고 문자열 연결 사용)
        var CAT_LABELS = { GLOBAL: '🌐 전체', STAR: '⭐ 스타', CELEB: '👤 셀럽', BRAND: '🏢 기업', UNIV: '🎓 대학', CITY: '🌆 도시', MEDIA: '📰 언론' };
        $('.cat-label').each(function() {
            var cat = $(this).data('cat');
            $(this).text(CAT_LABELS[cat] || cat);
        });

        // ===== 드래그 정렬 =====
        var draggingRow = null;
        $('#vsTableBody tr').on('dragstart', function() {
            draggingRow = this;
            $(this).addClass('dragging');
        }).on('dragend', function() {
            $(this).removeClass('dragging');
            draggingRow = null;
        }).on('dragover', function(e) {
            e.preventDefault();
            if (!draggingRow || draggingRow === this) return;
            var rect = this.getBoundingClientRect();
            var after = (e.originalEvent.clientY - rect.top) > rect.height / 2;
            if (after) {
                $(this).after(draggingRow);
            } else {
                $(this).before(draggingRow);
            }
        });

        function saveOrder() {
            var vsIds = [];
            $('#vsTableBody tr').each(function() {
                vsIds.push($(this).data('vs-id'));
            });
            $.ajax({
                url: '/super/vs/order.do',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ vsIds: vsIds }),
                success: function(res) {
                    if (res.status === 'success') {
                        alert('순서가 저장되었습니다.');
                        location.reload();
                    } else {
                        alert('저장 실패: ' + (res.msg || ''));
                    }
                },
                error: function() { alert('서버 오류가 발생했습니다.'); }
            });
        }

        // ===== 고정 토글 =====
        function togglePin(vsId, target) {
            $.post('/super/vs/pin.do', { vsId: vsId, isPinned: target }, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('변경 실패: ' + (res.msg || ''));
                }
            });
        }

        // ===== 커스텀 VS =====
        function openCustomModal() {
            $('#leftKeyword, #rightKeyword, #customTitle').val('');
            $('#leftSelect, #rightSelect').empty();
            new bootstrap.Modal(document.getElementById('customVsModal')).show();
        }

        function searchStar(side) {
            var keyword = $('#' + side + 'Keyword').val().trim();
            $.get('/super/vs/starSearch.do', { searchKeyword: keyword }, function(res) {
                if (res.status !== 'success') {
                    alert('검색 실패: ' + (res.msg || ''));
                    return;
                }
                var $sel = $('#' + side + 'Select').empty();
                (res.list || []).forEach(function(s) {
                    var label = s.PRS_NAME + ' (' + s.PRS_ID + ' / ' + s.PRS_COUNTRY + ')';
                    $sel.append($('<option>').val(s.PRS_ID).text(label));
                });
                if (!res.list || res.list.length === 0) {
                    $sel.append($('<option>').prop('disabled', true).text('검색 결과가 없습니다'));
                }
            });
        }

        function submitCustom() {
            var left = $('#leftSelect').val();
            var right = $('#rightSelect').val();
            if (!left || !right) {
                alert('좌/우 스타를 모두 선택해주세요.');
                return;
            }
            if (left === right) {
                alert('같은 스타끼리는 대결을 만들 수 없습니다.');
                return;
            }
            $.post('/super/vs/custom/insert.do', {
                leftPrsId: left,
                rightPrsId: right,
                title: $('#customTitle').val().trim()
            }, function(res) {
                if (res.status === 'success') {
                    alert('커스텀 VS가 등록되었습니다.');
                    location.reload();
                } else {
                    alert('등록 실패: ' + (res.msg || ''));
                }
            });
        }

        function deleteCustom(vsId) {
            if (!confirm('이 커스텀 VS를 삭제하시겠습니까?')) return;
            $.post('/super/vs/custom/delete.do', { vsId: vsId }, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('삭제 실패: ' + (res.msg || ''));
                }
            });
        }
    </script>
</body>
</html>
