<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>스타 관리 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
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
            </c:if>
            <li class="nav-item mt-auto">
                <a href="/witch/super/logout.do" class="nav-link text-danger"><i class="fas fa-sign-out-alt me-2"></i> 로그아웃</a>
            </li>
        </ul>
    </div>

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2 class="fw-bold">스타 관리 (Star List)</h2>
            <div class="d-flex">
            	<div class="input-group">
                    <input type="month" id="statMonth" class="form-control" value="2026-02">
                    <button onclick="downloadStats()" class="btn btn-success text-white">
                        <i class="fas fa-file-csv me-2"></i>통계 다운로드
                    </button>
                    <button onclick="downloadTotalStats()" class="btn btn-dark text-white" title="누적된 모든 통계 다운로드">
                        <i class="fas fa-globe me-2"></i>전체(Total)
                    </button>
                </div>

                <button onclick="openAllFeedModal()" class="btn btn-outline-dark ms-2" style="width: 200px;">
                    <i class="fas fa-th-large me-2"></i>전체 게시물
                </button>
                
                <a href="/witch/super/star/create.do" class="btn btn-primary ms-2" style="width: 200px;">
                    <i class="fas fa-plus me-2"></i>신규 등록
                </a>
            </div>
        </div>

        <div class="table-card">
            <table class="table table-hover align-middle">
                <thead class="table-light">
				    <tr>
				        <th>ID</th>
				        <th>이름</th>
				        <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
				            <th>국가</th>
				        </c:if>
				        <th class="text-center text-warning">Top 6 선정</th>
				        
				        <th>누적 광고노출</th>
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
                            <td class="text-center">
						        <div class="form-check form-switch d-flex justify-content-center">
						            <input class="form-check-input bg-warning border-warning" type="checkbox" role="switch"
						                   id="pop_${star.PRS_ID}" 
						                   onchange="togglePopular('${star.PRS_ID}', '${star.PRS_COUNTRY}', this)"
						                   ${star.IS_POPULAR eq 'Y' ? 'checked' : ''}
						                   style="cursor: pointer;">
						            <label class="form-check-label ms-2 fw-bold text-warning" for="pop_${star.PRS_ID}">
						                ${star.IS_POPULAR eq 'Y' ? '★' : ''}
						            </label>
						        </div>
						    </td>
                            <td>${star.AD_VIEW_CNT} 회</td>
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
                        <tr><td colspan="6" class="text-center py-5 text-muted">등록된 스타가 없습니다.</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
    
    <div class="modal fade" id="feedListModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header bg-dark text-white">
                <h5 class="modal-title"><i class="fas fa-images me-2"></i><span id="modalStarName"></span>님의 피드</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body bg-light">
                <div class="row g-2" id="feedGrid">
                    </div>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="feedActionModal" tabindex="-1" aria-hidden="true" style="z-index: 1060;"> <div class="modal-dialog modal-dialog-centered">
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

<script>
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
    
    function renderFeedGrid(list, showWriterName) {
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
                html += '    <img src="' + item.image + '" class="card-img-top" style="height: 120px; object-fit: cover;">';
                
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
        $('#feedGrid').html(html);
    }
    
 // [신규] 전체 게시물 모달 열기
    function openAllFeedModal() {
        // 모달 제목 변경
        $('#modalStarName').text("전체 최신"); 
        // 전체 목록 로드 호출
        loadAllFeedList();
        $('#feedListModal').modal('show');
    }

    // [신규] 전체 목록 불러오기 (AJAX)
    function loadAllFeedList() {
        $.ajax({
            url: '/witch/super/star/allFeedList.do', // 새로 만든 API
            type: 'POST',
            success: function(res) {
                renderFeedGrid(res.list, true); // true: 작성자 이름 표시 옵션
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
    function togglePin() {
        const targetStatus = currentIsPinned === 'Y' ? 'N' : 'Y';
        
        $.post('/witch/super/star/togglePin.do', {
            conId: currentConId,
            targetStatus: targetStatus
        }, function(res) {
            if(res.status === 'success') {
                currentIsPinned = targetStatus;
                updatePinButtonUI();
                loadFeedList(); // 목록 새로고침 (순서 변경 반영)
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
    
    
</script>

</body>
</html>