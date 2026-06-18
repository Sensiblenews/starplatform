<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>스타 관리 - Super Admin</title>
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
        
        /* 커스텀 뱃지 스타일 */
        .badge-kr { background-color: #e3f2fd; color: #0d47a1; }
        .badge-us { background-color: #ffebee; color: #b71c1c; }

        /* 📱 모바일 반응형 스타일 추가 */
        @media (max-width: 768px) {
            .sidebar {
                width: 100%;
                height: auto;
                position: relative;
                padding-bottom: 10px;
            }
            .sidebar .nav {
                flex-direction: row !important;
                flex-wrap: wrap;
                justify-content: center;
                gap: 10px;
            }
            .sidebar .nav-item {
                margin-bottom: 0 !important;
            }
            .main-content {
                margin-left: 0;
                padding: 15px;
            }
            /* 모바일에서 상단 버튼들 가로 꽉 차게 변경 */
            .action-buttons {
                width: 100%;
                justify-content: stretch !important;
            }
            .action-buttons > * {
                width: 100% !important;
                max-width: 100% !important;
                margin-left: 0 !important;
                margin-bottom: 8px;
            }
        }
    </style>
</head>
<body>

    <div class="sidebar d-flex flex-column p-3">
        <h3 class="text-center mb-5 mt-3 fw-bold">🚀 Super Admin</h3>
        <ul class="nav flex-column">
            <li class="nav-item mb-2">
                <a href="/witch/super/dashboard.do" class="nav-link"><i class="fas fa-chart-pie me-2"></i> 대시보드</a>
            </li>
            <li class="nav-item mb-2">
                <a href="/witch/super/star/list.do" class="nav-link active"><i class="fas fa-users-cog me-2"></i> 스타 관리</a>
            </li>
            <li class="nav-item mb-2">
                <a href="/witch/super/star/create.do" class="nav-link"><i class="fas fa-user-plus me-2"></i> 스타 등록</a>
            </li>
            <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                <li class="nav-item mb-2">
                    <a href="/witch/super/local/create.do" class="nav-link text-warning"><i class="fas fa-user-shield me-2"></i> 지역 관리자 생성</a>
                </li>
                <li class="nav-item mb-2">
                    <a href="/witch/super/report/list.do" class="nav-link" style="color: #ff8a65;">
                        <i class="fas fa-flag me-2"></i> 신고 관리
                    </a>
                </li>
            </c:if>
            <li class="nav-item mt-auto">
                <a href="/witch/super/logout.do" class="nav-link text-danger"><i class="fas fa-sign-out-alt me-2"></i> 로그아웃</a>
            </li>
        </ul>
    </div>

    <div class="main-content">
        <div class="d-flex justify-content-end mb-4">
            <div class="d-flex flex-wrap justify-content-end action-buttons w-100">
                <input type="month" id="statMonth" class="form-control" value="2026-02" style="max-width: 180px">
            
                <button onclick="downloadStats()" class="btn btn-success text-white ms-2">
                    <i class="fas fa-file-csv me-2"></i>통계 다운로드
                </button>
                <button onclick="downloadTotalStats()" class="btn btn-dark text-white ms-2" title="누적된 모든 통계 다운로드">
                    <i class="fas fa-globe me-2"></i>전체(Total)
                </button>
                <button onclick="openOrderModal()" class="btn btn-outline-primary ms-2" style="width: 160px;">
                    <i class="fas fa-list-ol me-2"></i>스타 순서 설정
                </button>
                <button onclick="openAllFeedModal()" class="btn btn-outline-dark ms-2" style="width: 160px;">
                    <i class="fas fa-th-large me-2"></i>전체 게시물
                </button>
                <button onclick="openIpoModal()" class="btn btn-warning fw-bold ms-2 text-dark" style="width: 160px;">
                    <i class="fas fa-file-signature me-2"></i>개설 신청 관리
                </button>
                
                <a href="/witch/super/star/create.do" class="btn btn-primary ms-2" style="width: 160px;">
                    <i class="fas fa-plus me-2"></i>신규 등록
                </a>
            </div>
        </div>

        <div class="table-card p-3">
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
                            <th>등록일</th>
                            <th class="text-center">상태 (활동/정지)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="star" items="${starList}">
                            <tr>
                                <td class="fw-bold text-secondary">${star.PRS_ID}</td>
                   
                                <td onclick="openFeedModal('${star.PRS_ID}', '${star.PRS_NAME}')" 
                                    style="cursor: pointer;"
                                    class="text-primary fw-bold text-decoration-underline-hover"
                                    title="클릭하여 피드 관리">
                                    
                                    ${star.PRS_NAME} <i class="fas fa-edit small ms-1 text-muted"></i>
                                </td>
                                <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                                    <td><span class="badge rounded-pill bg-light text-dark border">${star.PRS_COUNTRY}</span></td>
                                </c:if>
                                <td>${star.AD_VIEW_CNT} 회</td>
                                <td class="text-danger fw-bold"><i class="fas fa-heart"></i> ${star.FOLLOWER_CNT}</td>
                                <td class="text-muted small">${star.CON_CREATE_DATE}</td>
                                <td class="text-center">
                                    <div class="form-check form-switch d-inline-block">
                                        <input class="form-check-input" type="checkbox" 
                                               id="switch_${star.PRS_ID}" 
                                               onchange="toggleStatus('${star.PRS_ID}', this)"
                                               ${star.PRS_USE_YN eq 'Y' ? 'checked' : ''}>
                                        <label class="form-check-label ms-2" for="switch_${star.PRS_ID}" id="label_${star.PRS_ID}">
                                            ${star.PRS_USE_YN eq 'Y' ? '<span class="text-success fw-bold">활동중</span>' : '<span class="text-danger fw-bold">정지</span>'}
                                        </label>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        
                        <c:if test="${empty starList}">
                            <tr><td colspan="7" class="text-center py-5 text-muted">등록된 스타가 없습니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    
    <div class="modal fade" id="feedListModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title"><i class="fas fa-images me-2"></i><span id="modalStarName"></span>님의 피드</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body bg-light" style="max-height: 520px; overflow-y: auto;">
                    <div class="row g-2" id="feedGrid">
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="feedActionModal" tabindex="-1" aria-hidden="true" style="z-index: 1060;"> 
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-body p-0 position-relative">
                    <button type="button" class="btn-close position-absolute top-0 end-0 m-3 bg-white" 
                            data-bs-dismiss="modal" style="z-index: 10;"></button>
                    
                    <div class="bg-black d-flex align-items-center justify-content-center" style="min-height: 300px;">
                        <img id="actionImg" class="img-fluid" style="max-height: 400px;">
                        <i id="actionVideoIcon" class="fas fa-play-circle text-white fa-3x position-absolute" style="display:none;"></i>
                    </div>
                    
                    <div class="p-3">
                        <p id="actionBody" class="text-secondary small mb-3" style="white-space: pre-wrap;"></p>
                        
                        <div class="d-flex gap-2">
                            <button id="btnPin" class="btn btn-outline-warning flex-grow-1 fw-bold" onclick="togglePin()">
                                <i class="fas fa-thumbtack me-1"></i> <span>상단 고정</span>
                            </button>
                            
                            <button class="btn btn-outline-danger flex-grow-1 fw-bold" onclick="deleteFeed()">
                                <i class="fas fa-trash-alt me-1"></i> 삭제
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="orderModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-xl modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title"><i class="fas fa-list-ol me-2"></i>메인 화면 스타 진열 순서 설정</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body bg-light">
                    <div class="row">
                        <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
                            <div class="col-md-6 border-end">
                                <h5 class="fw-bold text-warning mb-3"><i class="fas fa-star"></i> 가로 스와이프 (최대 16명)</h5>
                                <div id="popularSlots">
                                </div>
                            </div>
                            <div class="col-md-6">
                                <h5 class="fw-bold text-primary mb-3"><i class="fas fa-sort-amount-down"></i> 세로 리스트 최상단 (1~16번)</h5>
                                <div id="listTopSlots">
                                </div>
                            </div>
                        </c:if>

                        <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'LC'}">
                            <div class="col-md-12">
                                <h5 class="fw-bold text-success mb-3"><i class="fas fa-sort-numeric-down"></i> 세로 리스트 지역할당 (17~32번)</h5>
                                <p class="text-muted small">* 1~16번은 최고관리자가 글로벌 스타로 지정합니다.</p>
                                <div class="row" id="listLocalSlots">
                                </div>
                            </div>
                        </c:if>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button type="button" class="btn btn-primary fw-bold" onclick="saveStarOrder()">저장하기</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="ipoListModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header bg-warning text-dark">
                    <h5 class="modal-title fw-bold"><i class="fas fa-file-signature me-2"></i>스타페이지 개설 신청 관리</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body bg-light">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle bg-white border">
                            <thead class="table-light">
                                <tr>
                                    <th>신청일시</th>
                                    <th>이름(활동명)</th>
                                    <th>분야</th>
                                    <th>SNS</th>
                                    <th>콜라보 희망가</th>
                                    <th>파트너십 희망가</th>
                                    <th>이메일</th>
                                    <th class="text-center">상태</th>
                                    <th class="text-center">관리</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="ipoTbody">
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="pinPromotionModal" tabindex="-1" aria-hidden="true" style="z-index: 1070;">
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
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button type="button" class="btn btn-warning fw-bold" onclick="submitFeedPin()">고정하기</button>
                </div>
            </div>
        </div>
    </div>

</body>

<script>
	var allFeedCurrentPage = 1;
	var isAllFeedLoading = false;
	var hasMoreAllFeeds = true;

    // 상태 변경 (Freeze) 함수
    function toggleStatus(starId, checkbox) {
        // 변경하려는 상태값 (체크되면 Y, 해제되면 N)
        const targetStatus = checkbox.checked ? 'Y' : 'N';
        const labelSpan = targetStatus === 'Y' ? 
            '<span class="text-success fw-bold">활동중</span>' : 
            '<span class="text-danger fw-bold">정지</span>';

        if(!confirm('해당 스타의 상태를 변경하시겠습니까?\n(정지 시 앱에서 활동정지로 표시됩니다)')) {
            checkbox.checked = !checkbox.checked; // 취소 시 스위치 원복
            return;
        }

        $.ajax({
            url: '/witch/super/star/toggleStatus.do',
            type: 'POST',
            data: {
                PRS_ID: starId,
                TARGET_STATUS: targetStatus
            },
            success: function(res) {
                if(res.status === 'success') {
                    // 라벨 텍스트 즉시 변경
                    $('#label_' + starId).html(labelSpan);
                } else {
                    alert('변경 실패: ' + res.msg);
                    checkbox.checked = !checkbox.checked; // 실패 시 원복
                }
            },
            error: function(err) {
                alert('서버 통신 오류');
                checkbox.checked = !checkbox.checked; // 오류 시 원복
            }
        });
    }
    
    function togglePopular(starId, country, checkbox) {
        // 체크하면 Y, 해제하면 N
        const targetStatus = checkbox.checked ? 'Y' : 'N';
        const label = $(checkbox).siblings('label');

        $.ajax({
            url: '/witch/super/star/togglePopular.do',
            type: 'POST',
            data: {
                PRS_ID: starId,
                PRS_COUNTRY: country,
                TARGET_STATUS: targetStatus
            },
            success: function(res) {
                if(res.status === 'success') {
                    // UI 업데이트: 성공 시 별 모양 표시/제거
                    if(targetStatus === 'Y') label.text('★');
                    else label.text('');
                } else {
                    // [핵심] 6명 초과 시 에러 메시지 띄우고 스위치 원복
                    alert(res.msg); 
                    checkbox.checked = !checkbox.checked; // 체크 상태 되돌리기
                }
            },
            error: function(err) {
                alert('서버 통신 오류');
                checkbox.checked = !checkbox.checked;
            }
        });
    }
    
    let currentStarId = '';
    let currentConId = '';
    let currentIsPinned = 'N';

    // [1] 피드 목록 모달 열기
    function openFeedModal(starId, starName) {
        currentStarId = starId;
        $('#modalStarName').text(starName);
        loadFeedList();
        $('#feedListModal').modal('show');
    }

    // [2] 목록 불러오기 (AJAX)
    function loadFeedList() {
        $.ajax({
            url: '/witch/super/star/feedList.do',
            data: { starId: currentStarId },
            success: function(res) {
                renderFeedGrid(res.list, false); // false: 작성자 이름 숨김 (이미 아니까)
            }
        });
    }
    
    function renderFeedGrid(list, showWriterName, isAppend) {
        var html = '';
        if(list && list.length > 0) {
            list.forEach(function(item) {
                var isPinned = item.IS_PINNED === 'Y';
                var shouldShowPin = isPinned && !showWriterName;
                // 배지 UI
                var pinBadge = shouldShowPin ? 
                    '<span class="position-absolute top-0 start-0 m-2 badge bg-warning text-dark border border-white"><i class="fas fa-thumbtack"></i> 고정됨</span>' : '';
                
                var videoIcon = item.MEDIA_TYPE === 'VIDEO' ? 
                    '<div class="position-absolute top-50 start-50 translate-middle text-white"><i class="fas fa-play-circle fa-2x"></i></div>' : '';

                // [신규] 전체 보기일 때만 하단에 작성자 이름 표시 (누구 글인지 알아야 하니까)
                var writerBadge = showWriterName ? 
                    '<span class="position-absolute bottom-0 start-0 m-2 badge bg-dark text-white opacity-75 small">' + item.PRS_NAME + '</span>' : '';

                html += '<div class="col-4 col-md-3">';
                html += '  <div class="card h-100 border-0 shadow-sm position-relative" style="cursor: pointer;"';
                html += '       onclick="openActionModal(\'' + item.CON_ID + '\', \'' + item.image + '\', \'' + escapeHtml(item.CON_BODY) + '\', \'' + item.IS_PINNED + '\', \'' + item.MEDIA_TYPE + '\')">';
                
                // 썸네일
                html += '    <img src="' + item.image + '" class="card-img-top" style="height: 120px; object-fit: cover;" loading="lazy" decoding="async">';
                
                // 오버레이 요소들
                html +=      pinBadge;
                html +=      videoIcon;
                html +=      writerBadge; // 작성자 이름 (옵션)
                
                html += '  </div>';
                html += '</div>';
            });
        } else {
            html = '<div class="col-12 text-center py-5 text-muted">등록된 게시물이 없습니다.</div>';
        }
        

     // append 여부에 따라 처리
        if (isAppend) {
            $('#feedGrid').append(html);
        } else {
            $('#feedGrid').html(html);
        }
    }
    
 // [신규] 전체 게시물 모달 열기
    function openAllFeedModal() {
	    $('#modalStarName').text("전체 최신");
	    
	    // 초기화
	    allFeedCurrentPage = 1;
	    hasMoreAllFeeds = true;
	    $('#feedGrid').empty(); // 기존 내용 비우기
	    
	    // 1페이지 로드
	    loadAllFeedList(allFeedCurrentPage);
	    $('#feedListModal').modal('show');
	}

    // [신규] 전체 목록 불러오기 (AJAX)
    function loadAllFeedList(page) {
	    if (isAllFeedLoading || !hasMoreAllFeeds) return;
	    isAllFeedLoading = true;
	
	    $.ajax({
	        url: '/witch/super/star/allFeedList.do',
	        type: 'POST',
	        data: { page: page }, // 페이지 번호 전송
	        success: function(res) {
	            if (res.status === 'success') {
	                if (res.list && res.list.length > 0) {
	                    // 페이지가 1보다 크면 뒤에 붙이기(true), 1이면 새로 그리기(false)
	                    let isAppend = page > 1; 
	                    renderFeedGrid(res.list, true, isAppend);
	                    allFeedCurrentPage++; // 다음 로드를 위해 페이지 증가
	                } else {
	                    // 불러올 데이터가 없으면 플래그 변경
	                    hasMoreAllFeeds = false;
	                    if (page === 1) {
	                        $('#feedGrid').html('<div class="col-12 text-center py-5 text-muted">등록된 게시물이 없습니다.</div>');
	                    }
	                }
	            } else {
	                alert('데이터를 불러오지 못했습니다: ' + res.msg);
	            }
	        },
	        complete: function() {
	            isAllFeedLoading = false;
	        }
	    });
	}

    // [3] 액션(상세) 모달 열기
    function openActionModal(conId, imgUrl, body, isPinned, mediaType) {
        currentConId = conId;
        currentIsPinned = isPinned;

        // UI 세팅
        $('#actionImg').attr('src', imgUrl);
        $('#actionBody').text(body || '내용 없음'); // escapeHtml 처리된 텍스트
        
        if(mediaType === 'VIDEO') $('#actionVideoIcon').show();
        else $('#actionVideoIcon').hide();

        // 버튼 상태 (고정 vs 해제)
        updatePinButtonUI();

        $('#feedActionModal').modal('show');
    }

    function updatePinButtonUI() {
        const btn = $('#btnPin');
        const span = btn.find('span');
        if (currentIsPinned === 'Y') {
            btn.removeClass('btn-outline-warning').addClass('btn-warning');
            btn.html('<i class="fas fa-thumbtack me-1"></i> 고정 해제');
        } else {
            btn.removeClass('btn-warning').addClass('btn-outline-warning');
            btn.html('<i class="fas fa-thumbtack me-1"></i> 상단 고정');
        }
    }

    // [4] 고정 토글 실행
    // [4] 고정 토글 버튼 클릭 시 (수정됨)
    function togglePin() {
        if (currentIsPinned === 'Y') {
            // 이미 고정되어 있다면 -> 확인 후 바로 해제(N) 통신
            if(!confirm('상단 고정을 해제하시겠습니까?')) return;
            executePinUpdate('N', '', '', '', '');
        } else {
            // 고정되어 있지 않다면 -> 프로모션 입력 모달 띄우기
            $('#feedPromoText1').val('');
            $('#feedPromoUrl1').val('');
            $('#feedPromoText2').val('');
            $('#feedPromoUrl2').val('');
            $('#pinPromotionModal').modal('show');
        }
    }

    // [신규] 모달 안에서 "고정하기" 버튼 클릭 시
    function submitFeedPin() {
        const t1 = $('#feedPromoText1').val().trim();
        const u1 = $('#feedPromoUrl1').val().trim();
        const t2 = $('#feedPromoText2').val().trim();
        const u2 = $('#feedPromoUrl2').val().trim();
        
        executePinUpdate('Y', t1, u1, t2, u2);
    }

    // [신규] 실제 API 통신 로직
    function executePinUpdate(targetStatus, t1, u1, t2, u2) {
        $.post('/witch/super/star/togglePin.do', {
            conId: currentConId,
            targetStatus: targetStatus,
            pinLinkText1: t1,
            pinLinkUrl1: u1,
            pinLinkText2: t2,
            pinLinkUrl2: u2
        }, function(res) {
            if(res.status === 'success') {
                currentIsPinned = targetStatus;
                updatePinButtonUI();
                $('#pinPromotionModal').modal('hide');
                loadFeedList(); // 배경에 있는 목록 새로고침
            } else {
                alert('실패: ' + res.msg);
            }
        });
    }

    // [5] 게시물 삭제 실행
    function deleteFeed() {
        if(!confirm('정말 삭제하시겠습니까? 복구할 수 없습니다.')) return;

        $.post('/witch/super/star/deleteFeed.do', { conId: currentConId }, function(res) {
            if(res.status === 'success') {
                $('#feedActionModal').modal('hide');
                loadFeedList(); // 목록 새로고침
            } else {
                alert('삭제 실패: ' + res.msg);
            }
        });
    }

    // 문자열 이스케이프 (따옴표 깨짐 방지)
    function escapeHtml(text) {
        if (!text) return "";
        return text.replace(/'/g, "&#39;").replace(/"/g, "&quot;");
    }
    
    function downloadStats() {
        const month = document.getElementById('statMonth').value;
        if (!month) {
            alert("월을 선택해주세요.");
            return;
        }
        
        // 페이지 이동 방식(location.href)으로 다운로드 트리거
        location.href = '/witch/super/stats/download.do?targetMonth=' + month;
    }
    
    function downloadTotalStats() {
        if(!confirm("전체 기간의 통계를 다운로드 하시겠습니까?\n데이터 양에 따라 시간이 걸릴 수 있습니다.")) return;
        
        // targetMonth 파라미터를 빼고 호출 -> SQL에서 전체 조회됨
        location.href = '/witch/super/stats/download.do'; 
    }

    // (참고) 페이지 로드 시 현재 달 자동 선택
    $(document).ready(function() {
        const date = new Date();
        const monthStr = date.toISOString().slice(0, 7); // YYYY-MM 형식
        $('#statMonth').val(monthStr);
    });
    
    $(document).ready(function() {
        // 모달의 body 요소에 스크롤 이벤트 바인딩
        $('#feedListModal .modal-body').on('scroll', function() {
            // 전체 최신 보기 모달일 때만 작동하도록 조건 추가 (필요시)
            if ($('#modalStarName').text() !== "전체 최신") return;

            const scrollHeight = $(this).prop('scrollHeight');
            const scrollTop = $(this).scrollTop();
            const clientHeight = $(this).height();

            // 바닥에 닿기 50px 전일 때 다음 페이지 로드
            if (scrollHeight - scrollTop - clientHeight < 50) {
                loadAllFeedList(allFeedCurrentPage);
            }
        });
    });
    
 // [신규] 순서 설정 모달 열기
    function openOrderModal() {
        $.ajax({
            url: '/witch/super/star/getOrderSettings.do',
            type: 'GET',
            success: function(res) {
                if(res.status === 'success') {
                    renderOrderSlots(res.allStars, res.currentSettings);
                    $('#orderModal').modal('show');
                } else {
                    alert('데이터를 불러오지 못했습니다.');
                }
            }
        });
    }

 	// [수정] JSP 문법 충돌을 방지하기 위해 백틱(`) 대신 일반 문자열(+) 결합 사용
    function renderOrderSlots(stars, current) {
        var optionsHtml = '<option value="">-- 비워둠 --</option>';
        stars.forEach(function(star) {
            optionsHtml += '<option value="' + star.PRS_ID + '">' + star.PRS_NAME + ' (' + star.PRS_ID + ')</option>';
        });

        // 이 부분은 JSP 변수이므로 정상 작동합니다.
        const auth = '${sessionScope.SUPER_USER_SESSION.PRS_AUTH}';

        if (auth === 'SM') {
            // SM: 인기 1~6번
            let popHtml = '';
            for(let i=1; i<=16; i++) {
                popHtml += '<div class="mb-2 input-group"><span class="input-group-text bg-warning fw-bold">' + i + '위</span><select class="form-select pop-slot" data-slot="' + i + '">' + optionsHtml + '</select></div>';
            }
            $('#popularSlots').html(popHtml);
            
            // SM: 리스트 1~16번
            let listTopHtml = '';
            for(let i=1; i<=16; i++) {
                listTopHtml += '<div class="mb-2 input-group"><span class="input-group-text bg-primary text-white fw-bold">' + i + '위</span><select class="form-select list-slot" data-slot="' + i + '">' + optionsHtml + '</select></div>';
            }
            $('#listTopSlots').html(listTopHtml);

            // 선택값 매핑 (따옴표로 확실하게 감싸줌)
            for(let i=1; i<=16; i++) {
                $('.pop-slot[data-slot="' + i + '"]').val(current.popular[i] || '');
            }
            for(let i=1; i<=16; i++) {
                $('.list-slot[data-slot="' + i + '"]').val(current.list[i] || '');
            }

         // [기존 코드 삭제 후 아래 코드로 교체]
        } else if (auth === 'LC') {
            // LC: 리스트 17~32번 (지역 할당)
            let localHtml = '';
            for(let i=17; i<=32; i++) {
                localHtml += '<div class="col-md-6 mb-2"><div class="input-group"><span class="input-group-text bg-success text-white fw-bold">지역 ' + (i-16) + '위</span><select class="form-select list-slot" data-slot="' + i + '">' + optionsHtml + '</select></div></div>';
            }
            $('#listLocalSlots').html(localHtml);
            
            // 선택값 매핑
            for(let i=17; i<=32; i++) {
                $('.list-slot[data-slot="' + i + '"]').val(current.list[i] || '');
            }
        }
    }

    // [신규] 순서 저장
    function saveStarOrder() {
        let payload = { popular: {}, list: {} };
        const auth = '${sessionScope.SUPER_USER_SESSION.PRS_AUTH}';

        if(auth === 'SM') {
            $('.pop-slot').each(function() { if($(this).val()) payload.popular[$(this).data('slot')] = $(this).val(); });
            $('.list-slot').each(function() { if($(this).val()) payload.list[$(this).data('slot')] = $(this).val(); });
        } else {
            $('.list-slot').each(function() { if($(this).val()) payload.list[$(this).data('slot')] = $(this).val(); });
        }

        $.ajax({
            url: '/witch/super/star/saveOrderSettings.do',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function(res) {
                if(res.status === 'success') {
                    alert('순서가 저장되었습니다.');
                    $('#orderModal').modal('hide');
                    location.reload(); // 변경사항 확인을 위해 새로고침
                } else {
                    alert('저장 실패: ' + res.msg);
                }
            }
        });
    }
    
 // 🌟 [추가] 개설 신청 모달 열기 및 리스트 로드
    function openIpoModal() {
        loadIpoList();
        $('#ipoListModal').modal('show');
    }

    // 🌟 [추가] 신청 리스트 AJAX 로드
    function loadIpoList() {
        $.ajax({
            url: '/witch/super/ipo/getList.do', // 컨트롤러에 만든 주소 (경로가 맞는지 확인 필요)
            type: 'POST',
            success: function(res) {
                if(res.status === 'success') {
                    renderIpoTable(res.list);
                } else {
                    alert('목록을 불러오지 못했습니다.');
                }
            },
            error: function() {
                alert('서버 통신 오류');
            }
        });
    }

    // 🌟 [추가] 리스트 테이블 HTML 렌더링
    function renderIpoTable(list) {
        var html = '';
        if(list && list.length > 0) {
            list.forEach(function(item) {
                html += '<tr>';
                html += '  <td class="text-muted small">' + (item.created_at || '') + '</td>';
                html += '  <td class="fw-bold">' + (item.name || '') + '</td>';
                html += '  <td><span class="badge bg-secondary">' + (item.category || '') + '</span></td>';
                
             		// 1. 유효한 절대 경로 URL로 변환하는 방어 로직 추가
                var validUrl = item.sns_link || '';
                if (validUrl !== '') {
                    // 맨 앞에 http:// 또는 https:// 가 없다면 강제로 붙여줌
                    if (!/^https?:\/\//i.test(validUrl)) {
                        validUrl = 'http://' + validUrl;
                    }
                    html += '  <td><a href="' + validUrl + '" target="_blank" class="btn btn-sm btn-outline-info"><i class="fas fa-link"></i></a></td>';
                } else {
                    html += '  <td>-</td>';
                }
                
                html += '  <td class="text-primary fw-bold">' + (item.collab_price || '-') + '</td>';
                html += '  <td class="text-primary fw-bold">' + (item.partner_price || '-') + '</td>';
                html += '  <td class="small">' + (item.email || '') + '</td>';
                
                // 상태 배지
                if(item.status === 'pending') {
                    html += '  <td class="text-center"><span class="badge bg-warning text-dark">검토중</span></td>';
                } else if(item.status === 'approved') {
                    html += '  <td class="text-center"><span class="badge bg-success">승인됨</span></td>';
                } else {
                    html += '  <td class="text-center"><span class="badge bg-danger">반려됨</span></td>';
                }
                
                // 관리 버튼 (pending 상태일 때만 승인/반려 버튼 노출)
                html += '  <td class="text-center">';
                if(item.status === 'pending') {
                    html += '    <button class="btn btn-sm btn-success fw-bold me-1" onclick="processIpo(' + item.id + ', \'approved\')">승인</button>';
                    html += '    <button class="btn btn-sm btn-danger fw-bold" onclick="processIpo(' + item.id + ', \'rejected\')">반려</button>';
                } else {
                    // 🌟 [수정] 처리 완료된 항목 옆에 (X) 버튼 추가
                    html += '    <span class="text-muted small me-2">처리완료</span>';
                    html += '    <button class="btn btn-sm btn-outline-secondary" onclick="hideIpo(' + item.id + ')" title="목록에서 제외"><i class="fas fa-times"></i></button>';
                }
                html += '  </td>';
                html += '</tr>';
            });
        } else {
            html = '<tr><td colspan="9" class="text-center py-5 text-muted">들어온 신청이 없습니다.</td></tr>';
        }
        $('#ipoTbody').html(html);
    }

    // 🌟 [추가] 승인/반려 처리
    function processIpo(id, targetStatus) {
        var actionStr = targetStatus === 'approved' ? '승인' : '반려';
        var confirmMsg = targetStatus === 'approved' ? 
            '해당 스타의 개설 신청을 승인하시겠습니까?\n(승인 즉시 기업 리스트 보드에 노출됩니다.)' : 
            '해당 신청을 반려하시겠습니까?';

        if(!confirm(confirmMsg)) return;

        $.ajax({
            url: '/witch/super/ipo/process.do',
            type: 'POST',
            data: { id: id, status: targetStatus },
            success: function(res) {
                if(res.status === 'success') {
                    alert(actionStr + ' 처리되었습니다.');
                    loadIpoList(); // 리스트 새로고침
                } else {
                    alert('처리 실패: ' + res.msg);
                }
            },
            error: function() {
                alert('서버 오류 발생');
            }
        });
    }
    
    function hideIpo(id) {
        if(!confirm('정말 목록에서 제외하시겠습니까?\n(제외된 내역은 화면에 더 이상 표시되지 않습니다.)')) return;

        // 기존의 process.do API를 재활용하여 status만 'hidden'으로 업데이트
        $.ajax({
            url: '/witch/super/ipo/process.do',
            type: 'POST',
            data: { id: id, status: 'hidden' },
            success: function(res) {
                if(res.status === 'success') {
                    loadIpoList(); // 리스트 새로고침 (hidden 항목은 이제 안 가져옴)
                } else {
                    alert('처리 실패: ' + res.msg);
                }
            },
            error: function() {
                alert('서버 오류 발생');
            }
        });
    }
    
    
</script>

</html>