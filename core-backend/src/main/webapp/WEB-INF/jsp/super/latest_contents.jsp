<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Latest Contents 편성 - Super Admin</title>
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

        /* 드래그 정렬 (VS 배틀필드·LIVE NEWS 화면과 동일) */
        #lcTableBody tr { cursor: grab; }
        #lcTableBody tr.dragging { opacity: 0.4; }
        .drag-handle { color: #adb5bd; cursor: grab; }
        .order-input { text-align: center; }
        #lcTableBody tr.row-off { background: #f8f9fa; }
        #lcTableBody tr.row-off td:not(:last-child) { opacity: 0.55; }

        /* 썸네일은 로비 카드와 같은 세로 비율 */
        .lc-thumb { width: 72px; height: 128px; object-fit: cover; border-radius: 8px; background: #000; display: block; }
        .lc-thumb-wrap { position: relative; width: 72px; }
        .lc-thumb-wrap .play-btn { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); color: #fff; opacity: .85; font-size: 22px; cursor: pointer; }
        .lc-avatar { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; background: #ddd; }
        .lc-body { max-width: 260px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .stat-cell { font-variant-numeric: tabular-nums; }
        .setting-card { background: white; border-radius: 16px; padding: 16px 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }

        /* 선택 모달 결과 */
        .cand-item { display: flex; gap: 12px; padding: 10px; border: 1px solid #e9ecef; border-radius: 10px; margin-bottom: 8px; align-items: center; }
        .cand-item.already { background: #f8f9fa; opacity: .7; }
        .cand-thumb { width: 54px; height: 96px; object-fit: cover; border-radius: 6px; background: #000; flex-shrink: 0; cursor: pointer; }
        .cand-text { flex: 1; min-width: 0; }
        .cand-body { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #555; font-size: 13px; }
        #previewPane { background: #000; border-radius: 10px; min-height: 300px; display: flex; align-items: center; justify-content: center; }
        #previewVideo { max-height: 420px; max-width: 100%; border-radius: 10px; }

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
    <c:set var="activeMenu" value="latest_contents" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
            <div>
                <h4 class="fw-bold mb-1"><i class="fas fa-film text-info me-2"></i>Latest Contents 편성</h4>
                <div class="text-secondary small">
                    로비 헤더와 VS 카드 사이의 숏폼 캐러셀에 나갈 영상을 편성합니다.
                    검수 승인된 <b>영상 게시물</b>만 고를 수 있고, 게시 중 + 기간 안인 항목이 노출 순서대로 앱에 나갑니다.
                    행을 드래그하거나 <b>노출순서</b> 칸에 숫자를 입력한 뒤 <b>순서 저장</b>을 눌러주세요.
                    저장 즉시 앱 캐시가 비워져 다음 로비 진입부터 반영됩니다.
                </div>
            </div>
            <div>
                <button onclick="saveOrder()" class="btn btn-success me-2">
                    <i class="fas fa-save me-2"></i>순서 저장
                </button>
                <button onclick="openPickModal()" class="btn btn-primary me-2">
                    <i class="fas fa-search me-2"></i>기존 게시물 선택
                </button>
                <button onclick="openUploadModal()" class="btn btn-info text-white">
                    <i class="fas fa-upload me-2"></i>직접 업로드
                </button>
            </div>
        </div>

        <!-- 운영 설정 -->
        <div class="setting-card mb-4">
            <div class="d-flex align-items-end flex-wrap gap-3">
                <div>
                    <label class="form-label fw-bold mb-1">로비 노출 개수</label>
                    <input type="number" id="displayCount" class="form-control" style="width:120px;"
                           min="${countMin}" max="${countMax}" value="${setting.DISPLAY_COUNT}">
                    <div class="form-text">${countMin}~${countMax}개. 편성 순서 앞에서부터 이만큼만 나갑니다.</div>
                </div>
                <div>
                    <label class="form-label fw-bold mb-1">자동 이동 간격 (초)</label>
                    <input type="number" id="intervalSec" class="form-control" style="width:120px;"
                           min="${intervalMin}" max="${intervalMax}" value="${setting.INTERVAL_SEC}">
                    <div class="form-text">${intervalMin}~${intervalMax}초. 기본 4초. 사용자가 손으로 넘기면 6초 동안 멈춥니다.</div>
                </div>
                <div class="pb-4">
                    <button onclick="saveSetting()" class="btn btn-outline-dark"><i class="fas fa-cog me-1"></i>설정 저장</button>
                </div>
            </div>
        </div>

        <div class="table-card p-3">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th style="width:40px;"></th>
                            <th style="width:90px;" class="text-center">노출순서</th>
                            <th style="width:90px;">영상</th>
                            <th>게시자 / 내용</th>
                            <th style="width:110px;" class="text-center">상태</th>
                            <th style="width:210px;">게시 기간 (KST)</th>
                            <th style="width:220px;" class="text-center">통계<br><span class="fw-normal small">노출 · 재생 · 클릭 · 프로필</span></th>
                            <th class="text-center" style="width:170px;">관리</th>
                        </tr>
                    </thead>
                    <tbody id="lcTableBody">
                        <c:forEach var="it" items="${itemList}">
                            <tr draggable="true" data-lc-id="${it.LC_ID}"
                                data-featured="${it.IS_FEATURED}"
                                data-use-yn="${it.USE_YN}"
                                data-start="${empty it.START_AT ? '' : fn:substring(it.START_AT, 0, 16)}"
                                data-end="${empty it.END_AT ? '' : fn:substring(it.END_AT, 0, 16)}"
                                data-title="<c:out value='${it.TITLE_OVERRIDE}'/>"
                                class="${it.USE_YN eq 'N' ? 'row-off' : ''}">
                                <td><i class="fas fa-grip-vertical drag-handle"></i></td>
                                <td class="text-center">
                                    <input type="number" class="form-control form-control-sm order-input" min="1" step="1" value="0">
                                </td>
                                <td>
                                    <div class="lc-thumb-wrap">
                                        <c:choose>
                                            <c:when test="${not empty it.THUMB_URL}">
                                                <img class="lc-thumb" src="<c:out value='${it.THUMB_URL}'/>" alt="">
                                                <c:if test="${not empty it.MEDIA_URL}">
                                                    <i class="fas fa-play-circle play-btn" title="미리보기"
                                                       onclick="previewVideo('<c:out value='${it.MEDIA_URL}'/>', '<c:out value='${it.THUMB_URL}'/>')"></i>
                                                </c:if>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="lc-thumb d-flex align-items-center justify-content-center text-secondary small">없음</div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty it.CON_ID or empty it.PRS_ID}">
                                            <span class="badge bg-danger">원본 삭제됨</span>
                                            <span class="text-secondary small">CON_ID <c:out value="${it.CON_ID}"/> — 앱에는 나가지 않습니다. 삭제해주세요.</span>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="d-flex align-items-center gap-2 mb-1">
                                                <img class="lc-avatar" src="<c:out value='${empty it.STORED_FILE_NM ? "" : it.STORED_FILE_NM}'/>" onerror="this.style.visibility='hidden'" alt="">
                                                <b><c:out value="${it.PRS_NAME}"/></b>
                                                <span class="text-secondary small">(<c:out value="${it.PRS_ID}"/>)</span>
                                                <c:if test="${it.IS_FEATURED eq 'Y'}"><span class="badge bg-warning text-dark">Featured</span></c:if>
                                                <c:if test="${it.MDR_STATUS ne 'APPROVED'}"><span class="badge bg-danger">검수 <c:out value="${it.MDR_STATUS}"/></span></c:if>
                                                <c:if test="${it.PRS_USE_YN ne 'Y'}"><span class="badge bg-secondary">스타 비활성</span></c:if>
                                            </div>
                                            <div class="lc-body" title="<c:out value='${it.CON_BODY}'/>">
                                                <c:choose>
                                                    <c:when test="${not empty it.TITLE_OVERRIDE}"><i class="fas fa-pen-nib text-info me-1"></i><c:out value="${it.TITLE_OVERRIDE}"/></c:when>
                                                    <c:when test="${not empty it.CON_BODY}"><c:out value="${it.CON_BODY}"/></c:when>
                                                    <c:otherwise><span class="text-secondary">(본문 없음)</span></c:otherwise>
                                                </c:choose>
                                            </div>
                                            <div class="text-secondary small">CON_ID <c:out value="${it.CON_ID}"/> · 게시 <c:out value="${fn:substring(it.CON_CREATED_DATE, 0, 10)}"/></div>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${it.STATUS eq 'LIVE'}"><span class="badge bg-success">게시 중</span></c:when>
                                        <c:when test="${it.STATUS eq 'SCHEDULED'}"><span class="badge bg-primary">예약</span></c:when>
                                        <c:when test="${it.STATUS eq 'EXPIRED'}"><span class="badge bg-dark">만료</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary">숨김</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="small">
                                    <div><span class="text-secondary">시작</span> <c:out value="${empty it.START_AT ? '즉시' : fn:substring(it.START_AT, 0, 16)}"/></div>
                                    <div><span class="text-secondary">종료</span> <c:out value="${empty it.END_AT ? '무기한' : fn:substring(it.END_AT, 0, 16)}"/></div>
                                </td>
                                <td class="text-center stat-cell">
                                    <span title="노출">${it.CNT_IMPRESSION}</span> ·
                                    <span title="재생">${it.CNT_PLAY}</span> ·
                                    <span title="클릭">${it.CNT_CLICK}</span> ·
                                    <span title="프로필 클릭">${it.CNT_PROFILE_CLICK}</span>
                                </td>
                                <td class="text-center text-nowrap">
                                    <c:choose>
                                        <c:when test="${it.USE_YN eq 'N'}">
                                            <button class="btn btn-sm btn-secondary me-1" onclick="toggleUse('${it.LC_ID}', 'Y')" title="게시로 전환">
                                                <i class="fas fa-toggle-off"></i>
                                            </button>
                                        </c:when>
                                        <c:otherwise>
                                            <button class="btn btn-sm btn-success me-1" onclick="toggleUse('${it.LC_ID}', 'N')" title="숨김으로 전환">
                                                <i class="fas fa-toggle-on"></i>
                                            </button>
                                        </c:otherwise>
                                    </c:choose>
                                    <button class="btn btn-sm btn-outline-primary me-1" onclick="openEdit(this)" title="수정">
                                        <i class="fas fa-pen"></i>
                                    </button>
                                    <button class="btn btn-sm btn-outline-danger" onclick="deleteItem('${it.LC_ID}')" title="편성에서 제거">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty itemList}">
                            <tr><td colspan="8" class="text-center text-secondary py-4">편성된 영상이 없습니다. 앱 로비에는 숏폼 영역이 나타나지 않습니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- 기존 게시물 선택 모달 -->
    <div class="modal fade" id="pickModal" tabindex="-1">
        <div class="modal-dialog modal-xl modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title">🔍 기존 게시물 선택</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="row g-3">
                        <div class="col-lg-7">
                            <div class="row g-2 mb-2">
                                <div class="col-md-5">
                                    <input type="text" id="pickKeyword" class="form-control" placeholder="스타 이름 / ID"
                                           onkeydown="if(event.key==='Enter'){event.preventDefault();searchPosts();}">
                                </div>
                                <div class="col-md-3"><input type="date" id="pickStart" class="form-control" title="게시일 시작"></div>
                                <div class="col-md-3"><input type="date" id="pickEnd" class="form-control" title="게시일 끝"></div>
                                <div class="col-md-1 d-grid"><button class="btn btn-primary" onclick="searchPosts()"><i class="fas fa-search"></i></button></div>
                            </div>
                            <div class="form-text mb-2">검수 승인된 영상 게시물, 최신순 최대 50건. 썸네일을 누르면 오른쪽에서 재생됩니다.</div>
                            <div id="candList" style="max-height: 480px; overflow-y: auto;">
                                <div class="text-secondary text-center py-4">검색어를 입력하거나 그대로 검색을 눌러주세요.</div>
                            </div>
                        </div>
                        <div class="col-lg-5">
                            <div id="previewPane">
                                <span class="text-secondary">미리보기</span>
                            </div>
                            <hr>
                            <div class="form-text mb-2">선택한 게시물: <b id="pickSelectedLabel">-</b></div>
                            <div class="form-check form-switch mb-2">
                                <input class="form-check-input" type="checkbox" id="pickFeatured">
                                <label class="form-check-label" for="pickFeatured">Featured (운영진 추천 표시)</label>
                            </div>
                            <div class="row g-2 mb-2">
                                <div class="col-6">
                                    <label class="form-label small mb-1">노출 시작 (비우면 즉시)</label>
                                    <input type="datetime-local" id="pickStartAt" class="form-control form-control-sm">
                                </div>
                                <div class="col-6">
                                    <label class="form-label small mb-1">노출 종료 (비우면 무기한)</label>
                                    <input type="datetime-local" id="pickEndAt" class="form-control form-control-sm">
                                </div>
                            </div>
                            <div class="mb-2">
                                <label class="form-label small mb-1">표시 제목 재정의 (선택, ${titleMax}자)</label>
                                <input type="text" id="pickTitle" class="form-control form-control-sm" maxlength="${titleMax}" placeholder="비우면 원본 본문을 씁니다">
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button class="btn btn-primary" id="pickSubmitBtn" onclick="submitPick()" disabled>편성에 추가</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 직접 업로드 모달 -->
    <div class="modal fade" id="uploadModal" tabindex="-1">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title">📤 영상 직접 업로드</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="alert alert-light border small mb-3">
                        지정한 스타 명의의 영상 게시물이 <b>검수 없이 바로 공개</b>되고 편성에 추가됩니다.
                        카드 하단 프로필을 누르면 그 스타의 StarPage로 이동합니다.
                        업로드 후 서버에서 웹 호환 MP4로 변환하므로 파일이 크면 1분 이상 걸릴 수 있습니다. (최대 ${uploadMaxMb}MB, 세로 9:16 권장)
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold">게시자 스타 <span class="text-danger">*</span></label>
                        <div class="input-group mb-1">
                            <input type="text" id="uploadStarKeyword" class="form-control" placeholder="이름/ID 검색"
                                   onkeydown="if(event.key==='Enter'){event.preventDefault();searchUploadStar();}">
                            <button class="btn btn-outline-primary" type="button" onclick="searchUploadStar()">검색</button>
                        </div>
                        <select id="uploadStarSelect" class="form-select" size="4"></select>
                        <div class="form-text">선택된 스타 ID: <b id="uploadStarLabel">-</b></div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold">영상 파일 <span class="text-danger">*</span></label>
                        <input type="file" id="uploadFile" class="form-control" accept="video/*">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold">본문 (게시물 내용)</label>
                        <textarea id="uploadBody" class="form-control" rows="2" placeholder="앱 게시물 본문. 비워도 됩니다"></textarea>
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label small mb-1">노출 시작 (비우면 즉시, KST)</label>
                            <input type="datetime-local" id="uploadStartAt" class="form-control form-control-sm">
                        </div>
                        <div class="col-6">
                            <label class="form-label small mb-1">노출 종료 (비우면 무기한, KST)</label>
                            <input type="datetime-local" id="uploadEndAt" class="form-control form-control-sm">
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small mb-1">표시 제목 재정의 (선택, ${titleMax}자)</label>
                        <input type="text" id="uploadTitle" class="form-control form-control-sm" maxlength="${titleMax}" placeholder="비우면 본문을 씁니다">
                    </div>
                    <div class="form-check form-switch">
                        <input class="form-check-input" type="checkbox" id="uploadFeatured">
                        <label class="form-check-label" for="uploadFeatured">Featured (운영진 추천 표시)</label>
                    </div>
                    <div class="progress mt-3" id="uploadProgressWrap" style="display:none; height: 8px;">
                        <div class="progress-bar progress-bar-striped progress-bar-animated" id="uploadProgress" style="width:0%"></div>
                    </div>
                    <div class="form-text" id="uploadStatus"></div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal" id="uploadCancelBtn">취소</button>
                    <button class="btn btn-info text-white" id="uploadSubmitBtn" onclick="submitUpload()">업로드 + 편성</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 수정 모달 -->
    <div class="modal fade" id="editModal" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title">✏️ 편성 수정</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="editLcId">
                    <div class="form-check form-switch mb-2">
                        <input class="form-check-input" type="checkbox" id="editFeatured">
                        <label class="form-check-label" for="editFeatured">Featured (운영진 추천 표시)</label>
                    </div>
                    <div class="form-check form-switch mb-3">
                        <input class="form-check-input" type="checkbox" id="editUseYn">
                        <label class="form-check-label" for="editUseYn">게시 (끄면 숨김)</label>
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label small mb-1">노출 시작 (비우면 즉시, KST)</label>
                            <input type="datetime-local" id="editStartAt" class="form-control form-control-sm">
                        </div>
                        <div class="col-6">
                            <label class="form-label small mb-1">노출 종료 (비우면 무기한, KST)</label>
                            <input type="datetime-local" id="editEndAt" class="form-control form-control-sm">
                        </div>
                    </div>
                    <div class="mb-2">
                        <label class="form-label small mb-1">표시 제목 재정의 (선택, ${titleMax}자)</label>
                        <input type="text" id="editTitle" class="form-control form-control-sm" maxlength="${titleMax}" placeholder="비우면 원본 본문을 씁니다">
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button class="btn btn-primary" onclick="submitEdit()">저장</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 영상 미리보기 모달 (목록용) -->
    <div class="modal fade" id="videoModal" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content bg-dark">
                <div class="modal-body p-2 text-center">
                    <video id="listPreviewVideo" controls playsinline style="max-height:70vh; max-width:100%;"></video>
                </div>
            </div>
        </div>
    </div>

    <script>
        // ===== 노출순서 (VS 배틀필드·LIVE NEWS 화면과 동일한 방식) =====
        function renumberOrderInputs() {
            $('#lcTableBody tr[data-lc-id]').each(function(idx) {
                $(this).find('.order-input').val(idx + 1);
            });
        }
        renumberOrderInputs();

        $('#lcTableBody').on('change', '.order-input', function() {
            var $row = $(this).closest('tr');
            var $others = $('#lcTableBody tr[data-lc-id]').not($row);
            var total = $others.length + 1;
            var target = parseInt($(this).val(), 10);
            if (isNaN(target)) target = 1;
            target = Math.max(1, Math.min(total, target));

            $row.detach();
            if (target >= total) {
                $('#lcTableBody').append($row);
            } else {
                $others.eq(target - 1).before($row);
            }
            renumberOrderInputs();
        });

        var draggingRow = null;
        $('#lcTableBody tr[data-lc-id]').on('dragstart', function() {
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
            var lcIds = [];
            $('#lcTableBody tr[data-lc-id]').each(function() {
                lcIds.push($(this).data('lc-id'));
            });
            if (lcIds.length === 0) {
                alert('저장할 편성이 없습니다.');
                return;
            }
            $.ajax({
                url: '/super/latest-contents/order.do',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ lcIds: lcIds }),
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

        // ===== 운영 설정 =====
        function saveSetting() {
            $.post('/super/latest-contents/setting.do', {
                displayCount: $('#displayCount').val(),
                intervalSec: $('#intervalSec').val()
            }, function(res) {
                if (res.status === 'success') {
                    alert('설정이 저장되었습니다.');
                    location.reload();
                } else {
                    alert('저장 실패: ' + (res.msg || ''));
                }
            });
        }

        // ===== 게시/숨김 · 삭제 =====
        function toggleUse(lcId, target) {
            $.post('/super/latest-contents/toggle.do', { lcId: lcId, useYn: target }, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('변경 실패: ' + (res.msg || ''));
                }
            });
        }

        function deleteItem(lcId) {
            if (!confirm('편성에서 제거하시겠습니까? (원본 게시물은 지우지 않습니다)')) return;
            $.post('/super/latest-contents/delete.do', { lcId: lcId }, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('삭제 실패: ' + (res.msg || ''));
                }
            });
        }

        // ===== 목록 미리보기 =====
        function previewVideo(url, poster) {
            var v = document.getElementById('listPreviewVideo');
            v.poster = poster || '';
            v.src = url;
            var modal = new bootstrap.Modal(document.getElementById('videoModal'));
            $('#videoModal').off('hidden.bs.modal').on('hidden.bs.modal', function() { v.pause(); v.removeAttribute('src'); v.load(); });
            modal.show();
        }

        // ===== 수정 =====
        // 서버 값 "yyyy-MM-dd HH:mm" → datetime-local "yyyy-MM-ddTHH:mm"
        function toLocalInput(text) {
            return text ? String(text).replace(' ', 'T') : '';
        }

        function openEdit(btn) {
            var $tr = $(btn).closest('tr');
            $('#editLcId').val($tr.data('lc-id'));
            $('#editFeatured').prop('checked', $tr.attr('data-featured') === 'Y');
            $('#editUseYn').prop('checked', $tr.attr('data-use-yn') !== 'N');
            $('#editStartAt').val(toLocalInput($tr.attr('data-start')));
            $('#editEndAt').val(toLocalInput($tr.attr('data-end')));
            $('#editTitle').val($tr.attr('data-title') || '');
            new bootstrap.Modal(document.getElementById('editModal')).show();
        }

        function submitEdit() {
            $.post('/super/latest-contents/update.do', {
                lcId: $('#editLcId').val(),
                isFeatured: $('#editFeatured').is(':checked') ? 'Y' : 'N',
                useYn: $('#editUseYn').is(':checked') ? 'Y' : 'N',
                startAt: $('#editStartAt').val(),
                endAt: $('#editEndAt').val(),
                titleOverride: $('#editTitle').val()
            }, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('저장 실패: ' + (res.msg || ''));
                }
            });
        }

        // ===== 기존 게시물 선택 =====
        var pickedConId = null;

        function openPickModal() {
            pickedConId = null;
            $('#pickSelectedLabel').text('-');
            $('#pickSubmitBtn').prop('disabled', true);
            $('#pickFeatured').prop('checked', false);
            $('#pickStartAt, #pickEndAt, #pickTitle').val('');
            $('#previewPane').html('<span class="text-secondary">미리보기</span>');
            new bootstrap.Modal(document.getElementById('pickModal')).show();
            if ($('#candList .cand-item').length === 0) searchPosts();
        }

        function searchPosts() {
            $('#candList').html('<div class="text-secondary text-center py-4">검색 중…</div>');
            $.get('/super/latest-contents/search.do', {
                keyword: $('#pickKeyword').val().trim(),
                startDate: $('#pickStart').val(),
                endDate: $('#pickEnd').val()
            }, function(res) {
                if (res.status !== 'success') {
                    alert('검색 실패: ' + (res.msg || ''));
                    return;
                }
                renderCandidates(res.list || []);
            });
        }

        function renderCandidates(list) {
            var $box = $('#candList').empty();
            if (list.length === 0) {
                $box.append('<div class="text-secondary text-center py-4">조건에 맞는 영상 게시물이 없습니다.</div>');
                return;
            }
            list.forEach(function(c) {
                var already = c.ALREADY_YN === 'Y';
                var $item = $('<div class="cand-item">').toggleClass('already', already);
                var $thumb = $('<img class="cand-thumb" alt="">').attr('src', c.THUMB_URL || '')
                    .on('click', function() { showPreview(c.MEDIA_URL, c.THUMB_URL); });
                var $text = $('<div class="cand-text">');
                $text.append($('<div>').append($('<b>').text(c.PRS_NAME || '')).append($('<span class="text-secondary small ms-1">').text('(' + c.PRS_ID + ')')));
                $text.append($('<div class="cand-body">').text(c.CON_BODY || '(본문 없음)'));
                $text.append($('<div class="text-secondary small">').text('CON_ID ' + c.CON_ID + ' · ' + String(c.CREATED_DATE || '').substring(0, 16)));
                var $btn = already
                    ? $('<span class="badge bg-secondary">이미 편성됨</span>')
                    : $('<button class="btn btn-sm btn-outline-primary">선택</button>').on('click', function() {
                        pickedConId = c.CON_ID;
                        $('#pickSelectedLabel').text('CON_ID ' + c.CON_ID + ' · ' + (c.PRS_NAME || ''));
                        $('#pickSubmitBtn').prop('disabled', false);
                        showPreview(c.MEDIA_URL, c.THUMB_URL);
                    });
                $item.append($thumb).append($text).append($btn);
                $box.append($item);
            });
        }

        function showPreview(url, poster) {
            var $pane = $('#previewPane').empty();
            if (!url) { $pane.append('<span class="text-secondary">영상 주소가 없습니다</span>'); return; }
            var v = $('<video id="previewVideo" controls playsinline muted autoplay>').attr('src', url).attr('poster', poster || '');
            $pane.append(v);
        }

        function submitPick() {
            if (!pickedConId) { alert('게시물을 선택해주세요.'); return; }
            $('#pickSubmitBtn').prop('disabled', true);
            $.post('/super/latest-contents/insert.do', {
                conId: pickedConId,
                isFeatured: $('#pickFeatured').is(':checked') ? 'Y' : 'N',
                startAt: $('#pickStartAt').val(),
                endAt: $('#pickEndAt').val(),
                titleOverride: $('#pickTitle').val()
            }, function(res) {
                if (res.status === 'success') {
                    location.reload();
                } else {
                    alert('추가 실패: ' + (res.msg || ''));
                    $('#pickSubmitBtn').prop('disabled', false);
                }
            });
        }

        // ===== 직접 업로드 =====
        function openUploadModal() {
            $('#uploadStarKeyword').val('');
            $('#uploadStarSelect').empty().data('selected', '');
            $('#uploadStarLabel').text('-');
            $('#uploadFile').val('');
            $('#uploadBody, #uploadStartAt, #uploadEndAt, #uploadTitle').val('');
            $('#uploadFeatured').prop('checked', false);
            $('#uploadProgressWrap').hide();
            $('#uploadProgress').css('width', '0%');
            $('#uploadStatus').text('');
            $('#uploadSubmitBtn, #uploadCancelBtn').prop('disabled', false);
            new bootstrap.Modal(document.getElementById('uploadModal'), { backdrop: 'static' }).show();
        }

        function searchUploadStar() {
            var keyword = $('#uploadStarKeyword').val().trim();
            $.get('/super/vs/starSearch.do', { searchKeyword: keyword }, function(res) {
                if (res.status !== 'success') {
                    alert('검색 실패: ' + (res.msg || ''));
                    return;
                }
                var $sel = $('#uploadStarSelect').empty();
                (res.list || []).forEach(function(s) {
                    var label = s.PRS_NAME + ' (' + s.PRS_ID + ' / ' + s.PRS_COUNTRY + ')';
                    $sel.append($('<option>').val(s.PRS_ID).text(label));
                });
                if (!res.list || res.list.length === 0) {
                    $sel.append($('<option>').prop('disabled', true).text('검색 결과가 없습니다'));
                }
            });
        }
        $('#uploadStarSelect').on('change', function() {
            $(this).data('selected', $(this).val());
            $('#uploadStarLabel').text($(this).val() || '-');
        });

        function submitUpload() {
            var prsId = $('#uploadStarSelect').data('selected') || '';
            var file = document.getElementById('uploadFile').files[0];
            if (!prsId) { alert('게시자 스타를 선택해주세요.'); return; }
            if (!file) { alert('영상 파일을 선택해주세요.'); return; }
            if (file.size > ${uploadMaxMb} * 1024 * 1024) { alert('영상 파일은 ${uploadMaxMb}MB 를 넘을 수 없습니다.'); return; }

            var fd = new FormData();
            fd.append('videoFile', file);
            fd.append('prsId', prsId);
            fd.append('body', $('#uploadBody').val());
            fd.append('isFeatured', $('#uploadFeatured').is(':checked') ? 'Y' : 'N');
            fd.append('startAt', $('#uploadStartAt').val());
            fd.append('endAt', $('#uploadEndAt').val());
            fd.append('titleOverride', $('#uploadTitle').val());

            $('#uploadSubmitBtn, #uploadCancelBtn').prop('disabled', true);
            $('#uploadProgressWrap').show();
            $('#uploadStatus').text('업로드 중…');
            $.ajax({
                url: '/super/latest-contents/upload.do',
                type: 'POST',
                data: fd,
                processData: false,
                contentType: false,
                xhr: function() {
                    var xhr = $.ajaxSettings.xhr();
                    xhr.upload.addEventListener('progress', function(e) {
                        if (!e.lengthComputable) return;
                        var pct = Math.round(e.loaded / e.total * 100);
                        $('#uploadProgress').css('width', pct + '%');
                        if (pct >= 100) $('#uploadStatus').text('파일 전송 완료. 서버 접수 확인 중…');
                    });
                    return xhr;
                },
                success: function(res) {
                    if (res.status === 'success' && res.jobId) {
                        // 변환·편성은 백그라운드 작업으로 넘어갔다. 결과를 폴링한다
                        pollUploadJob(res.jobId, Date.now());
                    } else {
                        alert('업로드 실패: ' + (res.msg || ''));
                        unlockUploadForm();
                    }
                },
                error: function() {
                    alert('파일을 서버에 전달하지 못했습니다. 네트워크 상태를 확인하고 다시 시도해주세요.');
                    unlockUploadForm();
                }
            });
        }

        function unlockUploadForm() {
            $('#uploadSubmitBtn, #uploadCancelBtn').prop('disabled', false);
            $('#uploadStatus').text('');
        }

        // 업로드 작업 상태 폴링. 변환은 서버에서 계속 돌므로 창을 닫아도 작업은 끝난다.
        // 서버가 재시작돼 작업을 모르면(UNKNOWN) 목록을 새로고침해 편성 여부를 확인하게 한다.
        function pollUploadJob(jobId, startedAt) {
            $.getJSON('/super/latest-contents/upload-status.json', { jobId: jobId }, function(res) {
                if (res.state === 'DONE') {
                    alert('업로드와 편성이 완료되었습니다.');
                    location.reload();
                    return;
                }
                if (res.state === 'FAILED') {
                    alert('업로드 실패: ' + (res.msg || ''));
                    unlockUploadForm();
                    return;
                }
                if (res.state === 'UNKNOWN') {
                    alert('서버가 재시작되어 변환 결과를 확인할 수 없습니다. 목록에서 편성 여부를 확인한 뒤 필요하면 다시 올려주세요.');
                    location.reload();
                    return;
                }
                var sec = Math.round((Date.now() - startedAt) / 1000);
                var label = (res.state === 'QUEUED') ? '앞선 변환 작업 대기 중…' : '서버에서 변환·썸네일 생성 중…';
                $('#uploadStatus').text(label + ' ' + sec + '초 경과. 창을 닫아도 작업은 계속됩니다.');
                setTimeout(function() { pollUploadJob(jobId, startedAt); }, 3000);
            }).fail(function() {
                // 일시적 네트워크 오류는 작업 실패가 아니다. 조금 뒤 다시 묻는다
                setTimeout(function() { pollUploadJob(jobId, startedAt); }, 5000);
            });
        }
    </script>
</body>
</html>
