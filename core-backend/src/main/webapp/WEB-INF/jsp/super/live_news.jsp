<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>LIVE NEWS 관리 - Super Admin</title>
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

        /* 드래그 정렬 (VS 배틀필드 화면과 동일) */
        #newsTableBody tr { cursor: grab; }
        #newsTableBody tr.dragging { opacity: 0.4; }
        .drag-handle { color: #adb5bd; cursor: grab; }
        .order-input { text-align: center; }
        /* OFF 문구는 목록에서 한눈에 구분되게 흐리게 */
        #newsTableBody tr.row-off { background: #f8f9fa; }
        #newsTableBody tr.row-off td:not(:last-child) { opacity: 0.55; }

        /* 앱 티커 미리보기 — 실제 로비 바(36px, 검정 바탕, LIVE 뱃지)와 같은 모양 */
        .ticker-preview { display: flex; align-items: center; height: 36px; background: #17181c; border-radius: 8px; overflow: hidden; max-width: 390px; }
        .ticker-preview .live { background: #d92d20; color: #fff; font-size: 11px; font-weight: 800; padding: 0 10px; height: 100%; display: flex; align-items: center; letter-spacing: .5px; }
        .ticker-preview .text { color: #f2f2f2; font-size: 12px; font-weight: 600; padding: 0 10px; white-space: nowrap; }
        .char-counter { font-size: 12px; color: #6c757d; }
        .char-counter.over { color: #dc3545; font-weight: 700; }

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
    <c:set var="activeMenu" value="live_news" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
            <div>
                <h4 class="fw-bold mb-1"><i class="fas fa-broadcast-tower text-warning me-2"></i>LIVE NEWS 관리</h4>
                <div class="text-secondary small">
                    로비 VS 카드 아래 LIVE 티커에 흘러가는 관리자 문구입니다.
                    ON 상태인 문구가 노출 순서대로 먼저 나온 뒤, 카테고리별 1위 현황과 VS 상황이 이어서 순환합니다.
                    행을 드래그하거나 <b>노출순서</b> 칸에 숫자를 입력한 뒤 <b>순서 저장</b>을 눌러주세요.
                    (문구는 최대 <b>${maxLength}자</b>, 앱에는 영어 노출이 원칙입니다)
                </div>
            </div>
            <div>
                <button onclick="saveOrder()" class="btn btn-success me-2">
                    <i class="fas fa-save me-2"></i>순서 저장
                </button>
                <button onclick="openEditor(null)" class="btn btn-warning">
                    <i class="fas fa-plus me-2"></i>문구 등록
                </button>
            </div>
        </div>

        <div class="table-card p-3">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th style="width:40px;"></th>
                            <th style="width:110px;" class="text-center">노출순서</th>
                            <th>문구</th>
                            <th style="width:260px;">랜딩 타겟</th>
                            <th class="text-center" style="width:110px;">ON / OFF</th>
                            <th class="text-center" style="width:120px;">관리</th>
                        </tr>
                    </thead>
                    <tbody id="newsTableBody">
                        <c:forEach var="n" items="${newsList}">
                            <tr draggable="true" data-news-id="${n.NEWS_ID}"
                                data-message="<c:out value='${n.MESSAGE}'/>"
                                data-target-type="${n.TARGET_TYPE}"
                                data-target-value="<c:out value='${n.TARGET_VALUE}'/>"
                                class="${n.USE_YN eq 'N' ? 'row-off' : ''}">
                                <td><i class="fas fa-grip-vertical drag-handle"></i></td>
                                <td class="text-center">
                                    <input type="number" class="form-control form-control-sm order-input"
                                           min="1" step="1" value="0">
                                </td>
                                <td>
                                    <div class="ticker-preview">
                                        <span class="live">● LIVE</span>
                                        <span class="text"><c:out value="${n.MESSAGE}"/></span>
                                    </div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${n.TARGET_TYPE eq 'STAR'}">
                                            <span class="badge bg-primary">스타</span>
                                            <c:out value="${n.TARGET_STAR_NAME}"/>
                                            <span class="text-secondary small">(<c:out value="${n.TARGET_VALUE}"/>)</span>
                                        </c:when>
                                        <c:when test="${n.TARGET_TYPE eq 'VS'}">
                                            <span class="badge bg-danger">VS</span>
                                            <c:choose>
                                                <c:when test="${n.TARGET_VS_KIND eq 'CUSTOM'}">
                                                    커스텀 <c:out value="${n.TARGET_VS_TITLE}"/>
                                                </c:when>
                                                <c:when test="${not empty n.TARGET_VS_KIND}">
                                                    <span class="vs-label" data-rank="${n.TARGET_VS_RANK_TYPE}" data-cat="${n.TARGET_VS_CATEGORY}"></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-danger small">삭제된 카드 #<c:out value="${n.TARGET_VALUE}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:when test="${n.TARGET_TYPE eq 'URL'}">
                                            <span class="badge bg-success">URL</span>
                                            <a href="<c:out value='${n.TARGET_VALUE}'/>" target="_blank" rel="noopener" class="small text-break"><c:out value="${n.TARGET_VALUE}"/></a>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary">없음</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${n.USE_YN eq 'N'}">
                                            <button class="btn btn-sm btn-secondary" onclick="toggleUse('${n.NEWS_ID}', 'Y')">
                                                <i class="fas fa-toggle-off me-1"></i>OFF
                                            </button>
                                        </c:when>
                                        <c:otherwise>
                                            <button class="btn btn-sm btn-success" onclick="toggleUse('${n.NEWS_ID}', 'N')">
                                                <i class="fas fa-toggle-on me-1"></i>ON
                                            </button>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center text-nowrap">
                                    <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditorFromRow(this)">
                                        <i class="fas fa-pen"></i>
                                    </button>
                                    <button class="btn btn-sm btn-outline-danger" onclick="deleteNews('${n.NEWS_ID}')">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty newsList}">
                            <tr><td colspan="6" class="text-center text-secondary py-4">등록된 문구가 없습니다. 티커는 카테고리별 1위·VS 상황만 순환합니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- 등록/수정 모달 -->
    <div class="modal fade" id="newsModal" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title" id="newsModalTitle">📢 LIVE NEWS 등록</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="newsId" value="">
                    <div class="mb-3">
                        <label class="form-label fw-bold d-flex justify-content-between">
                            <span>문구</span>
                            <span class="char-counter"><span id="charCount">0</span> / ${maxLength}</span>
                        </label>
                        <input type="text" id="newsMessage" class="form-control" placeholder="예: GLOBAL RANKING CHALLENGE START!" autocomplete="off">
                        <div class="mt-2 ticker-preview">
                            <span class="live">● LIVE</span>
                            <span class="text" id="previewText">&nbsp;</span>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold">랜딩 타겟 (티커 터치 시 이동)</label>
                        <select id="targetType" class="form-select" onchange="onTargetTypeChange()">
                            <option value="NONE">없음 (VS 카드로 스크롤만)</option>
                            <option value="STAR">특정 스타 페이지</option>
                            <option value="VS">VS 카드</option>
                            <option value="URL">외부 URL</option>
                        </select>
                    </div>
                    <div class="mb-3 target-box" id="targetStarBox" style="display:none;">
                        <label class="form-label fw-bold">스타 검색</label>
                        <div class="input-group mb-1">
                            <input type="text" id="starKeyword" class="form-control" placeholder="이름/ID 검색" onkeydown="if(event.key==='Enter'){event.preventDefault();searchStar();}">
                            <button class="btn btn-outline-primary" type="button" onclick="searchStar()">검색</button>
                        </div>
                        <select id="starSelect" class="form-select" size="4"></select>
                        <div class="form-text">선택된 스타 ID: <b id="starSelectedLabel">-</b></div>
                    </div>
                    <div class="mb-3 target-box" id="targetVsBox" style="display:none;">
                        <label class="form-label fw-bold">VS 카드</label>
                        <select id="vsSelect" class="form-select">
                            <c:forEach var="vs" items="${vsList}">
                                <option value="${vs.VS_ID}"
                                        data-kind="${vs.CARD_KIND}" data-rank="${vs.RANK_TYPE}" data-cat="${vs.CATEGORY}"
                                        data-title="<c:out value='${vs.TITLE}'/>" data-left="<c:out value='${vs.LEFT_NAME}'/>" data-right="<c:out value='${vs.RIGHT_NAME}'/>">
                                    #${vs.VS_ID}
                                </option>
                            </c:forEach>
                        </select>
                        <div class="form-text">비노출 상태인 카드를 고르면 앱에서는 VS 영역으로 스크롤만 됩니다.</div>
                    </div>
                    <div class="mb-3 target-box" id="targetUrlBox" style="display:none;">
                        <label class="form-label fw-bold">외부 URL</label>
                        <input type="url" id="targetUrl" class="form-control" placeholder="https://" maxlength="500">
                    </div>
                    <div class="form-check form-switch" id="useYnBox">
                        <input class="form-check-input" type="checkbox" id="useYn" checked>
                        <label class="form-check-label" for="useYn">등록 즉시 ON (앱 노출)</label>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button class="btn btn-warning" onclick="submitNews()">저장</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        var MAX_LENGTH = ${maxLength};
        // 카테고리 코드 → 한글 라벨 (JSP EL과 백틱 충돌을 피하려고 문자열 연결 사용)
        var CAT_LABELS = { GLOBAL: '🌐 전체', STAR: '⭐ 스타', CELEB: '👤 셀럽', BRAND: '🏢 기업', ORG: '🏛 단체', UNIV: '🎓 대학', CITY: '🌆 도시', MEDIA: '📰 언론' };
        var RANK_LABELS = { GLOBAL: '글로벌 랭킹', DAILY: '데일리 킹' };

        function vsLabel(kind, rank, cat, title, left, right) {
            if (kind === 'CUSTOM') {
                return '커스텀 ' + (left || '?') + ' vs ' + (right || '?') + (title ? ' (' + title + ')' : '');
            }
            return (RANK_LABELS[rank] || rank) + ' · ' + (CAT_LABELS[cat] || cat) + ' (1위 vs 2위)';
        }
        $('.vs-label').each(function() {
            $(this).text(vsLabel('AUTO', $(this).data('rank'), $(this).data('cat')));
        });
        $('#vsSelect option').each(function() {
            var $o = $(this);
            $o.text('#' + $o.val() + ' ' + vsLabel($o.data('kind'), $o.data('rank'), $o.data('cat'), $o.data('title'), $o.data('left'), $o.data('right')));
        });

        // ===== 글자 수 카운터 (서비스와 같은 기준: 코드포인트 수 — 이모지 1개 = 1자) =====
        function countChars(str) {
            return Array.from(str || '').length;
        }
        $('#newsMessage').on('input', function() {
            var v = $(this).val();
            var n = countChars(v);
            $('#charCount').text(n);
            $('.char-counter').toggleClass('over', n > MAX_LENGTH);
            $('#previewText').text(v.trim() ? v : ' ');
        });

        // ===== 노출순서 (VS 배틀필드 화면과 동일한 방식) =====
        function renumberOrderInputs() {
            $('#newsTableBody tr[data-news-id]').each(function(idx) {
                $(this).find('.order-input').val(idx + 1);
            });
        }
        renumberOrderInputs();

        $('#newsTableBody').on('change', '.order-input', function() {
            var $row = $(this).closest('tr');
            var $others = $('#newsTableBody tr[data-news-id]').not($row);
            var total = $others.length + 1;
            var target = parseInt($(this).val(), 10);
            if (isNaN(target)) target = 1;
            target = Math.max(1, Math.min(total, target));

            $row.detach();
            if (target >= total) {
                $('#newsTableBody').append($row);
            } else {
                $others.eq(target - 1).before($row);
            }
            renumberOrderInputs();
        });

        var draggingRow = null;
        $('#newsTableBody tr[data-news-id]').on('dragstart', function() {
            draggingRow = this;
            $(this).addClass('dragging');
        }).on('dragend', function() {
            $(this).removeClass('dragging');
            draggingRow = null;
            renumberOrderInputs();
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
            var newsIds = [];
            $('#newsTableBody tr[data-news-id]').each(function() {
                newsIds.push($(this).data('news-id'));
            });
            if (newsIds.length === 0) {
                alert('저장할 문구가 없습니다.');
                return;
            }
            $.ajax({
                url: '/super/live-news/order.do',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ newsIds: newsIds }),
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

        // ===== ON/OFF =====
        function toggleUse(newsId, target) {
            $.post('/super/live-news/toggle.do', { newsId: newsId, useYn: target }, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('변경 실패: ' + (res.msg || ''));
                }
            });
        }

        // ===== 등록/수정 모달 =====
        function onTargetTypeChange() {
            var type = $('#targetType').val();
            $('.target-box').hide();
            if (type === 'STAR') $('#targetStarBox').show();
            if (type === 'VS') $('#targetVsBox').show();
            if (type === 'URL') $('#targetUrlBox').show();
        }

        function openEditor(row) {
            var editing = !!row;
            $('#newsModalTitle').text(editing ? '✏️ LIVE NEWS 수정' : '📢 LIVE NEWS 등록');
            $('#newsId').val(editing ? row.newsId : '');
            $('#newsMessage').val(editing ? row.message : '').trigger('input');
            $('#targetType').val(editing ? row.targetType : 'NONE');
            $('#starKeyword').val('');
            $('#starSelect').empty();
            $('#starSelectedLabel').text(editing && row.targetType === 'STAR' ? row.targetValue : '-');
            $('#starSelect').data('selected', editing && row.targetType === 'STAR' ? row.targetValue : '');
            if (editing && row.targetType === 'VS') $('#vsSelect').val(row.targetValue);
            $('#targetUrl').val(editing && row.targetType === 'URL' ? row.targetValue : '');
            // ON/OFF는 목록의 토글 버튼으로 바꾼다. 수정 모달에서는 숨김
            $('#useYnBox').toggle(!editing);
            $('#useYn').prop('checked', true);
            onTargetTypeChange();
            new bootstrap.Modal(document.getElementById('newsModal')).show();
        }

        function openEditorFromRow(btn) {
            var $tr = $(btn).closest('tr');
            openEditor({
                newsId: $tr.data('news-id'),
                message: $tr.attr('data-message'),
                targetType: $tr.attr('data-target-type'),
                targetValue: $tr.attr('data-target-value')
            });
        }

        function searchStar() {
            var keyword = $('#starKeyword').val().trim();
            $.get('/super/vs/starSearch.do', { searchKeyword: keyword }, function(res) {
                if (res.status !== 'success') {
                    alert('검색 실패: ' + (res.msg || ''));
                    return;
                }
                var $sel = $('#starSelect').empty();
                (res.list || []).forEach(function(s) {
                    var label = s.PRS_NAME + ' (' + s.PRS_ID + ' / ' + s.PRS_COUNTRY + ')';
                    $sel.append($('<option>').val(s.PRS_ID).text(label));
                });
                if (!res.list || res.list.length === 0) {
                    $sel.append($('<option>').prop('disabled', true).text('검색 결과가 없습니다'));
                }
            });
        }
        $('#starSelect').on('change', function() {
            $(this).data('selected', $(this).val());
            $('#starSelectedLabel').text($(this).val() || '-');
        });

        function submitNews() {
            var message = $('#newsMessage').val().trim();
            if (!message) {
                alert('문구를 입력해주세요.');
                return;
            }
            if (countChars(message) > MAX_LENGTH) {
                alert('문구는 ' + MAX_LENGTH + '자를 넘을 수 없습니다.');
                return;
            }
            var type = $('#targetType').val();
            var value = '';
            if (type === 'STAR') value = $('#starSelect').data('selected') || '';
            if (type === 'VS') value = $('#vsSelect').val() || '';
            if (type === 'URL') value = $('#targetUrl').val().trim();
            if (type !== 'NONE' && !value) {
                alert('타겟 값을 선택/입력해주세요.');
                return;
            }

            var newsId = $('#newsId').val();
            var url = newsId ? '/super/live-news/update.do' : '/super/live-news/insert.do';
            var payload = { message: message, targetType: type, targetValue: value };
            if (newsId) {
                payload.newsId = newsId;
            } else {
                payload.useYn = $('#useYn').is(':checked') ? 'Y' : 'N';
            }
            $.post(url, payload, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('저장 실패: ' + (res.msg || ''));
                }
            });
        }

        function deleteNews(newsId) {
            if (!confirm('이 문구를 삭제하시겠습니까?')) return;
            $.post('/super/live-news/delete.do', { newsId: newsId }, function(res) {
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
