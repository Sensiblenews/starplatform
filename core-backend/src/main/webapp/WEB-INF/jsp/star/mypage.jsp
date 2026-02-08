<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>My Star Page</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body { background-color: #f8f9fa; font-family: 'Pretendard', sans-serif; }
        .profile-card { background: white; border-radius: 16px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); text-align: center; }
        .profile-img { width: 150px; height: 150px; border-radius: 50%; object-fit: cover; border: 5px solid #fff; box-shadow: 0 0 15px rgba(0,0,0,0.1); }
        .gallery-img { width: 100%; height: 200px; object-fit: cover; border-radius: 8px; }
        .gallery-item { position: relative; overflow: hidden; border-radius: 8px; transition: transform 0.2s; }
        .gallery-item:hover { transform: scale(1.02); }
        .btn-delete { position: absolute; top: 10px; right: 10px; background: rgba(255,0,0,0.7); color: white; border: none; border-radius: 50%; width: 30px; height: 30px; }
        
        /* [신규] 업로드 프리뷰 박스 스타일 */
		.upload-container {
		    display: flex;
		    flex-wrap: wrap;
		    gap: 10px;
		}
		
		/* 썸네일 아이템 */
		.preview-item {
		    position: relative;
		    width: 100px;
		    height: 100px;
		    border-radius: 8px;
		    overflow: hidden;
		    border: 1px solid #ddd;
		    background: #f8f9fa;
		}
		
		.preview-item img, .preview-item video {
		    width: 100%;
		    height: 100%;
		    object-fit: cover;
		}
		
		/* 삭제(X) 버튼 */
		.btn-remove-file {
		    position: absolute;
		    top: 5px;
		    right: 5px;
		    width: 20px;
		    height: 20px;
		    background: rgba(0,0,0,0.7);
		    color: white;
		    border-radius: 50%;
		    border: none;
		    font-size: 12px;
		    display: flex;
		    align-items: center;
		    justify-content: center;
		    cursor: pointer;
		    z-index: 10;
		}
		
		/* (+) 추가 버튼 */
		.btn-add-file {
		    width: 100px;
		    height: 100px;
		    border-radius: 8px;
		    border: 2px dashed #ccc;
		    background: white;
		    display: flex;
		    flex-direction: column;
		    align-items: center;
		    justify-content: center;
		    color: #aaa;
		    cursor: pointer;
		    transition: all 0.2s;
		}
		
		.btn-add-file:hover {
		    border-color: #666;
		    color: #666;
		    background: #f8f9fa;
		}
    </style>
</head>
<body>

<nav class="navbar navbar-dark bg-dark mb-4">
    <div class="container">
        <span class="navbar-brand mb-0 h1">🌟 Star Creator Studio</span>
        <a href="/witch/super/logout.do" class="btn btn-outline-light btn-sm">로그아웃</a>
    </div>
</nav>

<div class="container">
    <div class="row">
        <div class="col-md-4">
            <div class="profile-card mb-4">
                <form id="profileForm" enctype="multipart/form-data">
                    <div class="mb-3 position-relative">
                        <img src="${myInfo.STORED_FILE_NM != null ? myInfo.STORED_FILE_NM : '/witch/resources/img/avatar.svg'}" 
                             class="profile-img mb-3" id="previewImg" 
                             onerror="this.src='/witch/resources/img/avatar.svg">
                        <br>
                        <label class="btn btn-sm btn-outline-primary mt-2">
                            <i class="fas fa-camera"></i> 사진 변경
                            <input type="file" name="profileImage" style="display: none;" onchange="readURL(this);">
                        </label>
                    </div>
                    
                    <h4 class="fw-bold">${myInfo.PRS_ID}</h4>
                    <p class="text-muted small">${myInfo.PRS_COUNTRY} 지역 스타</p>

                    <hr>
                    <div class="mb-3 text-start">
                        <label class="form-label fw-bold">활동명 (Display Name)</label>
                        <input type="text" class="form-control" name="PRS_NAME" value="${myInfo.PRS_NAME}">
                    </div>
                    <div class="mb-3 text-start">
                        <label class="form-label fw-bold">새 비밀번호 (변경 시 입력)</label>
                        <input type="password" class="form-control" name="PRS_PWD" placeholder="변경할 경우에만 입력">
                    </div>
                    <button type="submit" class="btn btn-primary w-100 fw-bold">프로필 저장</button>
                </form>
            </div>
        </div>

        <div class="col-md-8">
		    <div class="d-flex justify-content-between align-items-center mb-3">
		        <h4 class="fw-bold"><i class="fas fa-images me-2"></i>나의 피드</h4>
		        
		        <button class="btn btn-dark fw-bold" data-bs-toggle="modal" data-bs-target="#writeModal">
		            <i class="fas fa-pen"></i> 글쓰기
		        </button>
		    </div>
		
		    <div class="row g-3">
		    	<div class="row g-3">
                <c:forEach var="item" items="${gallery}">
                    <div class="col-6 col-md-4">
                        <div class="gallery-item position-relative">
                            <img src="${item.IMG_URL}" class="gallery-img" 
							     onclick="viewFeed(${item.CON_ID})" 
							     style="cursor: pointer;" title="클릭해서 상세보기">
                            
                            <c:if test="${item.MEDIA_TYPE eq 'VIDEO'}">
                                <div class="position-absolute top-50 start-50 translate-middle">
                                    <i class="fas fa-play-circle text-white fa-2x" style="text-shadow: 0 0 5px rgba(0,0,0,0.5);"></i>
                                </div>
                            </c:if>

                            <c:if test="${item.MEDIA_COUNT > 1}">
                                <div class="position-absolute top-0 end-0 m-2">
                                    <i class="fas fa-images text-white fa-lg" style="text-shadow: 0 0 3px rgba(0,0,0,0.5);"></i>
                                </div>
                            </c:if>

                            <c:if test="${item.IS_PINNED eq 'Y'}">
                                <span class="badge bg-danger position-absolute top-0 start-0 m-2">TOP</span>
                            </c:if>

                            <button class="btn-delete" onclick="deletePhoto(${item.CON_ID})">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    </div>
                </c:forEach>
                
                <c:if test="${empty gallery}">
                    <div class="col-12 text-center py-5">
                        <i class="fas fa-camera fa-3x text-muted mb-3"></i>
                        <p class="text-muted">아직 업로드된 게시물이 없습니다.<br>첫 번째 게시물을 올려보세요!</p>
                    </div>
                </c:if>
            </div>
		    </div>
		</div>
		
		<div class="modal fade" id="writeModal" tabindex="-1" aria-hidden="true">
		  <div class="modal-dialog modal-dialog-centered">
		    <div class="modal-content">
		      
		      <div class="modal-header bg-dark text-white">
		        <h5 class="modal-title fw-bold"><i class="fas fa-edit"></i> 새 게시물 작성</h5>
		        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
		      </div>
		      
		      <div class="modal-body">
		        <form id="feedForm">
		            <div class="mb-3">
		                <label class="form-label fw-bold">내용</label>
		                <textarea class="form-control" name="feedText" rows="5" maxlength="5000" 
		                          placeholder="팬들에게 전하고 싶은 이야기를 적어주세요..."></textarea>
		                <div class="text-end text-muted small"><span id="charCount">0</span> / 5000</div>
		            </div>
		
		            <div class="mb-3">
		                <div class="mb-3">
						    <label class="form-label fw-bold">사진 / 동영상 첨부 (최대 10개)</label>
						    
						    <div class="upload-container" id="uploadBox">
						        <div class="btn-add-file" onclick="$('#hiddenFileInput').click()">
						            <i class="fas fa-plus fa-lg mb-1"></i>
						            <span class="small">추가</span>
						        </div>
						    </div>
						
						    <input type="file" id="hiddenFileInput" multiple accept="image/*, video/*" 
						           style="display: none;" onchange="handleFileSelect(this)">
						
						    <div class="form-text text-danger small mt-2">
						        * 동영상은 개당 최대 2MB까지 가능합니다.<br>
						        * 최소 1개 이상의 미디어를 선택해주세요.
						    </div>
						</div>
		            </div>
		        </form>
		      </div>
		      
		      <div class="modal-footer">
		        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
		        <button type="button" class="btn btn-primary fw-bold" onclick="uploadFeed()">
		            <i class="fas fa-paper-plane"></i> 게시하기
		        </button>
		      </div>
		
		    </div>
		  </div>
		</div>
		
		<div class="modal fade" id="viewModal" tabindex="-1" aria-hidden="true">
		  <div class="modal-dialog modal-dialog-centered"> <div class="modal-content overflow-hidden">
		      
		      <div class="modal-header border-0 p-2 position-absolute top-0 end-0" style="z-index: 1050;">
		        <button type="button" class="btn-close btn-close-white bg-white rounded-circle p-2 opacity-75" data-bs-dismiss="modal"></button>
		      </div>
		
		      <div class="modal-body p-0">
		        
		        <div id="feedCarousel" class="carousel slide" data-bs-interval="false"> <div class="carousel-indicators" id="viewIndicators">
		                </div>
		            
		            <div class="carousel-inner bg-black" id="viewMediaBox" style="min-height: 300px; max-height: 500px; display: flex; align-items: center;">
		                </div>
		
		            <button class="carousel-control-prev" type="button" data-bs-target="#feedCarousel" data-bs-slide="prev">
		                <span class="carousel-control-prev-icon" aria-hidden="true"></span>
		            </button>
		            <button class="carousel-control-next" type="button" data-bs-target="#feedCarousel" data-bs-slide="next">
		                <span class="carousel-control-next-icon" aria-hidden="true"></span>
		            </button>
		        </div>
		
		        <div class="p-3">
		            <p class="text-muted small mb-2" id="viewDate"></p>
		            <div id="viewBody" class="text-dark" style="white-space: pre-wrap; line-height: 1.5;"></div>
		        </div>
		
		      </div>
		    </div>
		  </div>
		</div>
    </div>
</div>

<script>
    // 프로필 이미지 미리보기
    function readURL(input) {
        if (input.files && input.files[0]) {
            var reader = new FileReader();
            reader.onload = function(e) { $('#previewImg').attr('src', e.target.result); }
            reader.readAsDataURL(input.files[0]);
        }
    }

    // 프로필 저장
    $('#profileForm').on('submit', function(e){
        e.preventDefault();
        var formData = new FormData(this);
        $.ajax({
            url: '/witch/star/updateProfile.do',
            type: 'POST',
            data: formData,
            contentType: false, processData: false,
            success: function(res) {
                alert(res.msg);
                if(res.status==='success') location.reload();
            }
        });
    });

    // 갤러리 업로드 (파일 선택 즉시 업로드)
    $('#galleryInput').on('change', function(){
        if(!this.files[0]) return;
        var formData = new FormData();
        formData.append('galleryImage', this.files[0]);

        $.ajax({
            url: '/witch/star/uploadGallery.do',
            type: 'POST',
            data: formData,
            contentType: false, processData: false,
            success: function(res) {
                if(res.status==='success') {
                    location.reload();
                } else {
                    alert(res.msg);
                }
            }
        });
    });

    // 갤러리 삭제
    function deletePhoto(conId) {
        if(!confirm('정말 삭제하시겠습니까?')) return;
        $.ajax({
            url: '/witch/star/deleteGallery.do',
            type: 'POST',
            data: {CON_ID: conId},
            success: function(res) {
                if(res.status==='success') location.reload();
            }
        });
    }
    
 // [기능 1] 글자수 세기
    $('textarea[name=feedText]').on('input', function() {
        $('#charCount').text(this.value.length);
    });

    // [기능 2] 파일 선택 시 유효성 검사 및 목록 표시
    $('#mediaInput').on('change', function() {
        var files = this.files;
        var $preview = $('#filePreview');
        $preview.empty(); // 초기화

        if(files.length > 10) {
            alert('최대 10개까지만 업로드 가능합니다.');
            this.value = ''; // 선택 초기화
            $preview.text('선택된 파일이 없습니다.');
            return;
        }

        if(files.length === 0) {
            $preview.text('선택된 파일이 없습니다.');
            return;
        }

        // 파일 목록 보여주기
        var listHtml = '<ul class="list-unstyled mb-0">';
        for (var i = 0; i < files.length; i++) {
            var file = files[i];
            
            // 동영상 2MB 제한 체크 (Front-end 1차 방어)
            if (file.type.startsWith('video') && file.size > 2 * 1024 * 1024) {
                alert('⚠️ [' + file.name + '] 파일이 2MB를 초과하여 제외됩니다.');
                this.value = ''; // 전체 초기화 (보안상 개별 삭제가 어려움)
                $preview.text('다시 선택해주세요.');
                return;
            }

            var icon = file.type.startsWith('video') ? '<i class="fas fa-video text-danger"></i>' : '<i class="fas fa-image text-primary"></i>';
            listHtml += '<li>' + icon + ' ' + file.name + ' <span class="text-muted">(' + (file.size/1024/1024).toFixed(1) + 'MB)</span></li>';
        }
        listHtml += '</ul>';
        $preview.html(listHtml);
    });

    // [기능 3] 업로드 실행 (AJAX)
    // [중요] 선택된 파일들을 담을 전역 배열
    var globalFiles = [];

    // 1. 파일 선택 시 처리 (Handle File Select)
    function handleFileSelect(input) {
        var files = input.files;
        var totalCount = globalFiles.length + files.length;

        // 개수 제한 체크
        if (totalCount > 10) {
            alert('최대 10개까지만 업로드 가능합니다.');
            input.value = ''; // 초기화
            return;
        }

        // 파일 유효성 검사 및 배열 추가
        for (var i = 0; i < files.length; i++) {
            var file = files[i];

            // 동영상 용량 체크 (2MB)
            if (file.type.startsWith('video') && file.size > 2 * 1024 * 1024) {
                alert('⚠️ [' + file.name + '] 파일이 2MB를 초과하여 제외됩니다.');
                continue;
            }

            // 배열에 추가
            globalFiles.push(file);
        }

        // input 초기화 (같은 파일을 다시 선택할 수도 있으므로)
        input.value = '';
        
        // 화면 다시 그리기
        renderPreviews();
    }

    // 2. 화면 렌더링 (Render Previews)
    function renderPreviews() {
        var $box = $('#uploadBox');
        
        // 기존 썸네일들만 제거 ((+) 버튼은 유지하거나 다시 그림)
        $box.find('.preview-item').remove();
        $box.find('.btn-add-file').remove(); // 순서 유지를 위해 버튼도 지웠다 맨 뒤에 붙임

        // 배열에 있는 파일들 루프
        globalFiles.forEach(function(file, index) {
            var isVideo = file.type.startsWith('video');
            var html = '';

            html += '<div class="preview-item">';
            html += '  <button type="button" class="btn-remove-file" onclick="removeFile(' + index + ')"><i class="fas fa-times"></i></button>';
            
            if (isVideo) {
                // 동영상은 비디오 태그로 미리보기 (음소거)
                var blobUrl = URL.createObjectURL(file);
                html += '  <video src="' + blobUrl + '" muted></video>';
                // 비디오 아이콘 오버레이
                html += '  <div class="position-absolute bottom-0 start-0 m-1 text-white small"><i class="fas fa-video"></i></div>';
            } else {
                // 이미지는 img 태그
                var blobUrl = URL.createObjectURL(file);
                html += '  <img src="' + blobUrl + '">';
            }
            html += '</div>';

            $box.append(html);
        });

        // 10개가 꽉 차지 않았을 때만 (+) 버튼 표시
        if (globalFiles.length < 10) {
            var addBtn = '<div class="btn-add-file" onclick="$(\'#hiddenFileInput\').click()">' +
                         '  <i class="fas fa-plus fa-lg mb-1"></i>' +
                         '  <span class="small">추가</span>' +
                         '</div>';
            $box.append(addBtn);
        }
    }

    // 3. 파일 삭제 (Remove File)
    function removeFile(index) {
        globalFiles.splice(index, 1); // 배열에서 삭제
        renderPreviews(); // 다시 그리기
    }

    // 4. 업로드 실행 (AJAX) - 수정됨
    function uploadFeed() {
        // 필수 체크
        if (globalFiles.length === 0) {
            alert('사진이나 동영상을 최소 1개 이상 첨부해주세요.');
            return;
        }

        // FormData 직접 구성 (중요!)
        var formData = new FormData();
        
        // 텍스트 추가
        formData.append('feedText', $('textarea[name=feedText]').val());

        // 파일 배열 추가
        // 주의: Controller는 'mediaFiles'라는 이름의 List<MultipartFile>을 받습니다.
        globalFiles.forEach(function(file) {
            formData.append('mediaFiles', file);
        });

        // 로딩 UI
        var $btn = $('.modal-footer .btn-primary');
        var originText = $btn.html();
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> 업로드 중...');

        $.ajax({
            url: '/witch/star/writeFeed.do',
            type: 'POST',
            data: formData,
            contentType: false, processData: false,
            success: function(res) {
                if(res.status === 'success') {
                    alert('성공적으로 게시되었습니다!');
                    location.reload();
                } else {
                    alert(res.msg);
                    $btn.prop('disabled', false).html(originText);
                }
            },
            error: function() {
                alert('서버 오류가 발생했습니다.');
                $btn.prop('disabled', false).html(originText);
            }
        });
    }

    // [참고] 모달이 닫힐 때 초기화해주면 좋음
    $('#writeModal').on('hidden.bs.modal', function () {
        globalFiles = []; // 배열 비우기
        $('textarea[name=feedText]').val(''); // 글 비우기
        $('#charCount').text('0');
        renderPreviews(); // 화면 초기화
    });
    
    function viewFeed(conId) {
        // 1. 모달 초기화 (이전 데이터 잔상 제거)
        $('#viewMediaBox').empty();
        $('#viewIndicators').empty();
        $('#viewBody').text('');
        $('#viewDate').text('');

        // 2. 데이터 요청
        $.ajax({
            url: '/witch/star/getFeedDetail.do',
            type: 'GET',
            data: { CON_ID: conId },
            success: function(res) {
                if(res.status === 'success') {
                    renderViewModal(res.master, res.medias);
                    $('#viewModal').modal('show'); // 모달 띄우기
                } else {
                    alert(res.msg);
                }
            },
            error: function() {
                alert('데이터를 불러오지 못했습니다.');
            }
        });
    }

    // [Helper] 모달 렌더링 함수
    function renderViewModal(master, medias) {
        // A. 텍스트 바인딩
        $('#viewBody').text(master.CON_BODY); // text()로 넣어야 XSS 방지 및 줄바꿈 처리됨
        $('#viewDate').text(master.CREATED_DATE);

        // B. 미디어 바인딩 (Carousel)
        var innerHtml = '';
        var indicatorsHtml = '';

        for(var i=0; i<medias.length; i++) {
            var media = medias[i];
            var activeClass = (i === 0) ? 'active' : ''; // 첫 번째 슬라이드만 active
            
            // 인디케이터 (하단 점)
            indicatorsHtml += '<button type="button" data-bs-target="#feedCarousel" data-bs-slide-to="' + i + '" class="' + activeClass + '"></button>';

            // 슬라이드 아이템
            innerHtml += '<div class="carousel-item ' + activeClass + '" style="height: 100%;">';
            
            if(media.MEDIA_TYPE === 'VIDEO') {
                // 동영상: controls 속성으로 재생바 표시
                innerHtml += '<video src="' + media.MEDIA_URL + '" class="d-block w-100" style="max-height: 500px; object-fit: contain;" controls playsinline></video>';
            } else {
                // 사진
                innerHtml += '<img src="' + media.MEDIA_URL + '" class="d-block w-100" style="max-height: 500px; object-fit: contain;">';
            }
            innerHtml += '</div>';
        }

        $('#viewMediaBox').html(innerHtml);
        
        // 미디어가 1개면 인디케이터/화살표 숨기기 (UI 디테일)
        if(medias.length > 1) {
            $('#viewIndicators').html(indicatorsHtml);
            $('.carousel-control-prev, .carousel-control-next').show();
        } else {
            $('.carousel-control-prev, .carousel-control-next').hide();
        }
    }
</script>
</body>
</html>