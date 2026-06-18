<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>My Creator Page</title>
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
        
        /* 업로드 프리뷰 박스 스타일 */
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
        <span class="navbar-brand mb-0 h1">🌟 Creator Studio</span>
        <a href="/witch/super/logout.do" class="btn btn-outline-light btn-sm">Logout</a>
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
                             onerror="this.src='/witch/resources/img/avatar.svg'">
                        <br>
                        <label class="btn btn-sm btn-outline-primary mt-2">
                            <i class="fas fa-camera"></i> Change Photo
                            <input type="file" name="profileImage" style="display: none;" onchange="readURL(this);">
                        </label>
                    </div>
                    
                    <h4 class="fw-bold">${myInfo.PRS_ID}</h4>
               
                    <p class="text-muted small">Creator from ${myInfo.PRS_COUNTRY}</p>
					<div class="mb-3">
                        <span class="badge bg-danger rounded-pill px-3 py-2 fs-6 shadow-sm">
                            <i class="fas fa-heart me-1"></i> Favorite Fans: ${myInfo.FOLLOWER_CNT != null ? myInfo.FOLLOWER_CNT : 0}
                        </span>
                        <button type="button" class="btn btn-outline-dark btn-sm rounded-pill mt-1" onclick="openStarCommentModal()">
					        <i class="fas fa-comments"></i> Profile Comments
					    </button>
                    </div>
     
                    <hr>
                    <div class="mb-3 text-start">
                        <label class="form-label fw-bold">Display Name</label>
                        <input type="text" class="form-control" name="PRS_NAME" value="${myInfo.PRS_NAME}">
                    </div>
                    <div class="mb-3 text-start">
                        <label class="form-label fw-bold">New Password (Optional)</label>
                        <input type="password" class="form-control" name="PRS_PWD" placeholder="Enter only if changing">
                    </div>
                    <button type="submit" class="btn btn-primary w-100 fw-bold">Save Profile</button>
                </form>
            </div>
        </div>

        <div class="col-md-8">
		    <div class="d-flex justify-content-between align-items-center mb-3">
		      
                <h4 class="fw-bold"><i class="fas fa-images me-2"></i>My Feed</h4>
		        
		        <button class="btn btn-dark fw-bold" data-bs-toggle="modal" data-bs-target="#writeModal">
		            <i class="fas fa-pen"></i> Write
		        </button>
		    </div>
		
		    <div class="row g-3">
		    	<div class="row g-3">
                <c:forEach var="item" items="${gallery}">
               
                    <div class="col-6 col-md-4">
                        <div class="gallery-item position-relative">
                            <img src="${item.IMG_URL}" class="gallery-img" 
							     onclick="viewFeed(${item.CON_ID})" 
							     style="cursor: pointer;" title="Click to view details">
                            
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
                        <p class="text-muted">No posts uploaded yet.<br>Upload your first post!</p>
                    </div>
                </c:if>
            </div>
		    </div>
		</div>
		
		<div class="modal fade" id="writeModal" tabindex="-1" aria-hidden="true">
		  <div class="modal-dialog modal-dialog-centered">
		    <div class="modal-content">
		      
		      <div class="modal-header bg-dark text-white">
		        <h5 class="modal-title fw-bold"><i class="fas fa-edit"></i> New Post</h5>
		        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
		      </div>
		      
		      <div class="modal-body">
		        <form id="feedForm">
		            <div class="mb-3">
		            
                        <label class="form-label fw-bold">Content</label>
		                <textarea class="form-control" name="feedText" rows="5" maxlength="5000" 
		                          placeholder="Write a message to your fans..."></textarea>
		                <div class="text-end text-muted small"><span id="charCount">0</span> / 5000</div>
		            </div>
		
		            <div class="mb-3">
		                <div class="mb-3">
						    <label class="form-label fw-bold">Attach Photo / Video (Max 10)</label>
						    
						    <div class="upload-container" id="uploadBox">
						        <div class="btn-add-file" onclick="$('#hiddenFileInput').click()">
						            <i class="fas fa-plus fa-lg mb-1"></i>
						            <span class="small">Add</span>
						        </div>
						    </div>
						
						    <input type="file" id="hiddenFileInput" multiple accept="image/*, video/*" 
						           style="display: none;" onchange="handleFileSelect(this)">
						
						    <div class="form-text text-danger small mt-2">
						        * Max 2MB per video.<br>
						        * Please select at least one media file.
                            </div>
						</div>
		            </div>
		            <div class="mb-3">
                        <label class="form-label fw-bold"><i class="fab fa-youtube text-danger"></i> YouTube Link (Optional)</label>
                        <input type="text" class="form-control" name="youtubeInput" placeholder="Paste YouTube video URL">
                    </div>
		        </form>
		      </div>
		      
		      <div class="modal-footer">
		        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
		        <button type="button" class="btn btn-primary fw-bold" onclick="uploadFeed()">
		            <i class="fas fa-paper-plane"></i> Post
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
		
		<div class="modal fade" id="starCommentModal" tabindex="-1" aria-hidden="true">
		    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
		        <div class="modal-content">
		            <div class="modal-header bg-dark text-white">
		                <h5 class="modal-title fw-bold"><i class="fas fa-comments me-2"></i>Manage Profile Comments</h5>
		                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
		            </div>
		            <div class="modal-body bg-light" id="starCommentBody">
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

    // 갤러리 삭제
    function deletePhoto(conId) {
        if(!confirm('Are you sure you want to delete?')) return;
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
            alert('You can upload up to 10 files.');
            this.value = ''; // 선택 초기화
            $preview.text('No file selected.');
            return;
        }

        if(files.length === 0) {
            $preview.text('No file selected.');
            return;
        }

        var listHtml = '<ul class="list-unstyled mb-0">';
        for (var i = 0; i < files.length; i++) {
            var file = files[i];
            
            if (file.type.startsWith('video') && file.size > 5 * 1024 * 1024) {
                alert('⚠️ [' + file.name + '] file exceeds 5MB and will be excluded.');
                this.value = ''; 
                $preview.text('Please select again.');
                return;
            }

            var icon = file.type.startsWith('video') ? '<i class="fas fa-video text-danger"></i>' : '<i class="fas fa-image text-primary"></i>';
            listHtml += '<li>' + icon + ' ' + file.name + ' <span class="text-muted">(' + (file.size/1024/1024).toFixed(1) + 'MB)</span></li>';
        }
        listHtml += '</ul>';
        $preview.html(listHtml);
    });

    // [중요] 선택된 파일들을 담을 전역 배열
    var globalFiles = [];

    // 1. 파일 선택 시 처리
    function handleFileSelect(input) {
        var files = input.files;
        var totalCount = globalFiles.length + files.length;

        if (totalCount > 10) {
            alert('You can upload up to 10 files.');
            input.value = ''; 
            return;
        }

        for (var i = 0; i < files.length; i++) {
            var file = files[i];
            if (file.type.startsWith('video') && file.size > 2 * 1024 * 1024) {
                alert('⚠️ [' + file.name + '] file exceeds 2MB and will be excluded.');
                continue;
            }
            globalFiles.push(file);
        }

        input.value = '';
        renderPreviews();
    }

    // 2. 화면 렌더링
    function renderPreviews() {
        var $box = $('#uploadBox');
        $box.find('.preview-item').remove();
        $box.find('.btn-add-file').remove();

        globalFiles.forEach(function(file, index) {
            var isVideo = file.type.startsWith('video');
            var html = '';

            html += '<div class="preview-item">';
            html += '  <button type="button" class="btn-remove-file" onclick="removeFile(' + index + ')"><i class="fas fa-times"></i></button>';
            
            if (isVideo) {
                var blobUrl = URL.createObjectURL(file);
                html += '  <video src="' + blobUrl + '" muted></video>';
                html += '  <div class="position-absolute bottom-0 start-0 m-1 text-white small"><i class="fas fa-video"></i></div>';
            } else {
                var blobUrl = URL.createObjectURL(file);
                html += '  <img src="' + blobUrl + '">';
            }
            html += '</div>';

            $box.append(html);
        });

        if (globalFiles.length < 10) {
            var addBtn = '<div class="btn-add-file" onclick="$(\'#hiddenFileInput\').click()">' +
                         '  <i class="fas fa-plus fa-lg mb-1"></i>' +
                         '  <span class="small">Add</span>' +
                         '</div>';
            $box.append(addBtn);
        }
    }

    // 3. 파일 삭제
    function removeFile(index) {
        globalFiles.splice(index, 1);
        renderPreviews();
    }

    // 4. 업로드 실행 (AJAX)
    function uploadFeed() {
        if (globalFiles.length === 0) {
            alert('Please attach at least one photo or video.');
            return;
        }

        var formData = new FormData();
        formData.append('feedText', $('textarea[name=feedText]').val());
        
        var rawYoutubeUrl = $('input[name=youtubeInput]').val().trim();
        var formattedUrl = "";
        
        if (rawYoutubeUrl !== "") {
            var regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|shorts\/|watch\?v=|\&v=)([^#\&\?]*).*/;
            var match = rawYoutubeUrl.match(regExp);
            
            if (match && match[2].length === 11) {
                formattedUrl = 'https://www.youtube.com/embed/' + match[2];
            } else {
                alert('Invalid YouTube link format.\n(e.g., https://youtu.be/...)');
                return;
            }
        }
        
        formData.append('youtubeUrl', formattedUrl);
        
        globalFiles.forEach(function(file) {
            formData.append('mediaFiles', file);
        });
        
        var $btn = $('.modal-footer .btn-primary');
        var originText = $btn.html();
        
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Uploading...');

        $.ajax({
            url: '/witch/star/writeFeed.do',
            type: 'POST',
            data: formData,
            contentType: false, processData: false,
            success: function(res) {
                if(res.status === 'success') {
                    alert('Posted successfully!');
                    location.reload();
                } else {
                    alert(res.msg);
                    $btn.prop('disabled', false).html(originText);
                }
            },
            error: function() {
                alert('Server error occurred.');
                $btn.prop('disabled', false).html(originText);
            }
        });
    }

    $('#writeModal').on('hidden.bs.modal', function () {
        globalFiles = []; 
        $('textarea[name=feedText]').val(''); 
        $('#charCount').text('0');
        renderPreviews(); 
    });
    
    function viewFeed(conId) {
        $('#viewMediaBox').empty();
        $('#viewIndicators').empty();
        $('#viewBody').text('');
        $('#viewDate').text('');

        $.ajax({
            url: '/witch/star/getFeedDetail.do',
            type: 'GET',
            data: { CON_ID: conId },
            success: function(res) {
                if(res.status === 'success') {
                    renderViewModal(res.master, res.medias);
                    $('#viewModal').modal('show'); 
                } else {
                    alert(res.msg);
                }
            },
            error: function() {
                alert('Failed to load data.');
            }
        });
    }

    function renderViewModal(master, medias) {
        $('#viewBody').text(master.CON_BODY);
        $('#viewDate').text(master.CREATED_DATE);
        
        var innerHtml = '';
        var indicatorsHtml = '';
        
        for(var i=0; i<medias.length; i++) {
            var media = medias[i];
            var activeClass = (i === 0) ? 'active' : '';
            
            indicatorsHtml += '<button type="button" data-bs-target="#feedCarousel" data-bs-slide-to="' + i + '" class="' + activeClass + '"></button>';
            innerHtml += '<div class="carousel-item ' + activeClass + '" style="height: 100%;">';
            
            if(media.MEDIA_TYPE === 'VIDEO') {
                innerHtml += '<video src="' + media.MEDIA_URL + '" class="d-block w-100" style="max-height: 500px; object-fit: contain;" controls playsinline></video>';
            } else {
                innerHtml += '<img src="' + media.MEDIA_URL + '" class="d-block w-100" style="max-height: 500px; object-fit: contain;">';
            }
            innerHtml += '</div>';
        }

        $('#viewMediaBox').html(innerHtml);
        
        if(medias.length > 1) {
            $('#viewIndicators').html(indicatorsHtml);
            $('.carousel-control-prev, .carousel-control-next').show();
        } else {
            $('.carousel-control-prev, .carousel-control-next').hide();
        }
        
        loadAdminComments(master.CON_ID);
    }
    
    function loadAdminComments(conId) {
        $('.admin-comment-section').remove();
        
        $.post('/witch/api/super/comment/list', { 
        	targetType: 'FEED', 
        	targetId: conId,
        	prsId: '${myInfo.PRS_ID}'
       	}, function(res) {
            var html = '<div class="admin-comment-section mt-4 p-3 border-top bg-white">';
            html += '<h6 class="fw-bold mb-3"><i class="fas fa-tools me-2"></i>Admin Comment Management</h6>';
            
            if(res.comments && res.comments.length > 0) {
                res.comments.forEach(function(c) {
                    html += '<div class="d-flex justify-content-between align-items-center mb-2 p-2 bg-light rounded border">';
                    html += '  <div class="small">';
                    html += '    <strong class="text-primary">' + c.NICKNAME + '</strong>: ' + c.CONTENT;
                    html += '    <div class="text-muted" style="font-size: 10px;">Device ID: ' + c.DEVICE_ID + ' | Date: ' + (c.date || c.DATE) + '</div>';
                    html += '  </div>';
                    html += '  <div class="btn-group btn-group-sm ms-2" style="flex-shrink: 0;">';
                    html += '    <button class="btn btn-outline-danger" onclick="blindComment(\'' + c.CMT_ID + '\', \'FEED\')">Delete</button>';
                    html += '    <button class="btn btn-danger" onclick="blockUser(\'' + c.DEVICE_ID + '\', \'' + c.NICKNAME + '\')">Block</button>';
                    html += '  </div>';
                    html += '</div>';
                });
            } else {
                html += '<p class="text-muted small text-center py-3">No comments registered.</p>';
            }
            html += '</div>';
            
            $('#viewModal .modal-body').append(html);
        });
    }
    
    function openStarCommentModal() {
        $('#starCommentModal').modal('show');
        loadStarComments();
    }

    function loadStarComments() {
        $('#starCommentBody').empty();

        $.post('/witch/api/super/comment/list', { 
            targetType: 'STAR', 
            targetId: '${myInfo.PRS_ID}',
            prsId: '${myInfo.PRS_ID}'
        }, function(res) {
            var html = '<div class="admin-comment-section p-2">';
            
            if(res.comments && res.comments.length > 0) {
                res.comments.forEach(function(c) {
                    html += '<div class="d-flex justify-content-between align-items-center mb-2 p-2 bg-white rounded border shadow-sm">';
                    html += '  <div class="small">';
                    html += '    <strong class="text-primary">' + c.NICKNAME + '</strong>: ' + c.CONTENT;
                    html += '    <div class="text-muted" style="font-size: 10px;">Device ID: ' + c.DEVICE_ID + ' | Date: ' + (c.date || c.DATE) + '</div>';
                    html += '  </div>';
                    html += '  <div class="btn-group btn-group-sm ms-2" style="flex-shrink: 0;">';
                    html += '    <button class="btn btn-outline-danger" onclick="blindComment(\'' + c.CMT_ID + '\', \'STAR\')">Delete</button>';
                    html += '    <button class="btn btn-danger" onclick="blockUser(\'' + c.DEVICE_ID + '\', \'' + c.NICKNAME + '\')">Block</button>';
                    html += '  </div>';
                    html += '</div>';
                });
            } else {
                html += '<p class="text-muted small text-center py-4">No profile comments registered.</p>';
            }
            html += '</div>';
            
            $('#starCommentBody').html(html);
        });
    }

    function blindComment(cmtId, type) {
	    if(!confirm('Are you sure you want to delete (blind) this comment?')) return;
	    
	    $.post('/witch/star/manageComment.do', { action: 'BLIND', cmtId: cmtId }, function(res) {
	        if(res.status === 'success') {
	            alert(res.msg);
	            
	            if (type === 'STAR') {
	                loadStarComments();
	            } else {
	                var currentConId = $('#viewModal').data('conId'); 
	                loadAdminComments(currentConId);
	            }
	        } else {
	            alert(res.msg);
	        }
	    });
	}
    
    function blockUser(deviceId, nickname) {
        if(!confirm('Are you sure you want to permanently block [' + nickname + ']?\nThis device (ID: ' + deviceId + ') will no longer be able to write comments.')) return;
        
        $.post('/witch/star/manageComment.do', { action: 'BLOCK', targetDeviceId: deviceId, reason: 'Repeated malicious comments' }, function(res) {
            alert(res.msg);
        });
    }
</script>
</body>
</html>