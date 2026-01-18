<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script type='text/javascript'>
$(function() 
{	
	
	$('.datatoggle').bootstrapToggle({});
	
	
	$("#PRS_ID").change(function(){
		
		var option = {
    			url: '${ctx}/adm/freeStarHeart.do',
				dataType : 'json',
				success : function(data){					
					
					$("#FREE_CNT").val(data.FREE_CNT);
					chekFree();
				}				 
		};
		
		 $('#contDetailForm').ajaxSubmit(option);
		 return false; 
		
 	});
	
	
	
	var validation = $('#contDetailForm').sv({
		checkOnSubmit : false,
		rules : {
			CON_TITLE : [ 'required','maxLength=30' ],
			CON_PRICE : [ 'required' ],
		},
		errorCss : 'formError',
		stopOnFirstError : true
	});
	
	
	$("#btn_cont_save").click(function(){
		
		if (!validation.validate()) {
			return;
		}
		
		<c:if test="${_AUTH  eq 'SM' and CON_TYPE != '9'}">		
			if ($('#PRS_ID').val() == "") {
				showMsgPop("회원사를 선택하세요.");
				return false;
			}
		</c:if>
		
		var thumbFile = $("#CON_THUMNAIL").val();
		var videoFile = $("#CON_VIDEO").val();
		var thumbReal = $("#CON_THUMNAIL_REAL");
		var videoReal = $("#CON_VIDEO_REAL");
		
		var flag1 = false, flag2 = false;
		
		if (!videoReal.attr('src') || $.trim(videoReal.attr('src')) === '') {
			flag1 = true;
		}
		
		if (!thumbReal.attr('src') || $.trim(thumbReal.attr('src')) === '') {
			flag2 = true;
		}
		
		
		if (flag1 && flag2 && (!thumbFile && !videoFile)) {
			showMsgPop("메인 이미지 또는 메인 동영상 중 하나는 반드시 첨부해야 합니다.");
			return false;
		}
		
		if($("#CON_PRICE").val() < 0)
		{
			if(!chekFree()) return false;
		}
		
		//제목뒤에 별 붙이기
		var s = $("#CON_TITLE").val();
		s = s+" "+"☆"+$("#CON_PRICE option:selected").val();
		
		$("#CON_TITLE").val(s);
	
		var option = {
				url : '${ctx}/adm/contentUpdateSave.do',
				dataType : 'json',				
				success : function(data){					
					
					if(data.status == 'success')
					{
						$('#contDialog').dialog('close');
			       		$('#contDialog').remove();
						location.href="${ctx}/adm/contentList?CON_TYPE="+$("#CON_TYPE").val();
					}	
					else
					{
						showMsgPop(data.serviceMessage);
					}	
					
				}				 
		};
	
		if (confirm("저장 하시겠습니까?")) {
			const content = editor.getHTML(); 
	        $("#CON_BODY").val(content);
			$('#contDetailForm').ajaxSubmit(option);
			return false;
		}		
		
		return false;
		
	});
	
	
	// Toast UI 에디터 설정
	const editor = new toastui.Editor({
	    el: document.querySelector('#editorDiv'),
	    height: '400px',
	    initialEditType: 'wysiwyg',
	    // 바로 이 부분에 서버에서 받은 데이터를 넣어줍니다.
	    initialValue: '${detail.CON_BODY}' 
	});
	
});


function chekFree()
{
	if($("#CON_PRICE").val() < 0)
	{
		if($("#FREE_CNT").val() < Math.abs($("#CON_PRICE").val()))
		{
			showMsgPop("보유중인 무료별,무료하트보다 <br/>입력값이 크므로 -값은 입력불가 합니다.","#CON_PRICE",null,{width:480});
			$("#CON_PRICE").val("0");
			return false;
		}	
	}	
	return true;
}


</script>

<style>
	.video-container {
		width: 300px;
		height: 180px;
		background-color: black;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden
	}
	.video-container video {
		width: 100%;
		height: 100%;
		object-fit: contain;
		background-color: black
	}
</style>

<div class="ibox animated fadeIn">
						<div class="panel panel-warning">
							<div class="panel-body">
								
								<form name="contDetailForm" id="contDetailForm" class="form-horizontal" method="POST" enctype="multipart/form-data">
								<input type="hidden" id="CON_ID" name="CON_ID" value="${detail.CON_ID }"> 
								<input type="hidden" id="CON_CATEGORY" name="CON_CATEGORY" value="${detail.CON_CATEGORY }"> 								
								<input type="hidden" id="CON_TYPE" name="CON_TYPE" value="${detail.CON_TYPE}">
								<input type="hidden" id="FREE_CNT" name="FREE_CNT" value="${FREE_CNT}">
								
														
	                                <div class="form-group">
										<label class="col-lg-1 control-label">제목</label>
	                                    <div class="col-lg-11 ">
	                                    	<input type="text" class="form-control"  id="CON_TITLE" name="CON_TITLE" value="${detail.CON_TITLE}" title="제목" />             	
	                                    </div>
	                                </div>                               
									<div class="form-group">
										<label class="col-lg-1 control-label">부제목</label>
	                                    <div class="col-lg-11">
	                                    	<input type="text" class="form-control"  id="CON_SUBTITLE" name="CON_SUBTITLE" value="${detail.CON_SUBTITLE}" title="부제목" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">메인 이미지</label>
	                                    <div class="col-lg-11">
	                                    	<img src="${detail.CON_THUMNAIL}" id="CON_THUMNAIL_REAL" height="200" width="200"/>
	                                    	<input type="file" id="CON_THUMNAIL" name="CON_THUMNAIL" title="메인 이미지">                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">메인 동영상</label>
	                                    <div class="col-lg-11">
	                                    	<div class="video-container">
	                                    		<video src="${detail.CON_VIDEO}" id="CON_VIDEO_REAL" style="width: 200px; height: 160px;" ></video>
	                                    	</div>
	                                    	<input type="file" id="CON_VIDEO" name="CON_VIDEO" title="메인 동영상">                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">내용</label>
	                                    <div class="col-lg-11">
	                                    	 <div id="editorDiv" style="width: 100%;"></div>
	                                    	 <input type="hidden" name="CON_BODY" id="CON_BODY" />                      	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">유튜브 URL</label>
	                                    <div class="col-lg-11">
	                                    	<input type="text" class="form-control"  id="CON_YOUTUBE_URL" name="CON_YOUTUBE_URL" value="${detail.CON_YOUTUBE_URL}" title="유튜브 URL" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">가격</label>
	                                    <div class="col-lg-4">
	                                    	<select  name="CON_PRICE" id="CON_PRICE" title="가격" class="form-control">
												<option value="5" <c:if test="${detail.CON_PRICE == '5'}"> selected </c:if> >5</option>
												<option value="4" <c:if test="${detail.CON_PRICE == '4'}"> selected </c:if> >4</option>
												<option value="3" <c:if test="${detail.CON_PRICE == '3'}"> selected </c:if> >3</option>
												<option value="2" <c:if test="${detail.CON_PRICE == '2'}"> selected </c:if> >2</option>
												<option value="1" <c:if test="${detail.CON_PRICE == '1'}"> selected </c:if> >1</option>
												<option value="0" <c:if test="${detail.CON_PRICE == '0'}"> selected </c:if> >0</option>
												<option value="-1" <c:if test="${detail.CON_PRICE == '-1'}"> selected </c:if> >-1</option>
												<option value="-2" <c:if test="${detail.CON_PRICE == '-2'}"> selected </c:if>>-2</option>
												<option value="-3" <c:if test="${detail.CON_PRICE == '-3'}"> selected </c:if> >-3</option>
												<option value="-4" <c:if test="${detail.CON_PRICE == '-4'}"> selected </c:if> >-4</option>
												<option value="-5" <c:if test="${detail.CON_PRICE == '-5'}"> selected </c:if> >-5</option>
											</select>			                                    	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">클릭/하트 수 노출</label>
	                                    <div class="col-lg-11">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="CON_META_YN" id="CON_META_YN" <c:if test="${detail.CON_META_YN == 1}"> checked </c:if>
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">
                          	
	                                    </div>
	                                </div>	    
	                                <div class="form-group">
										<label class="col-lg-1 control-label">게시 여부</label>
	                                    <div class="col-lg-11">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="CON_USE_YN" id="CON_USE_YN" <c:if test="${detail.CON_USE_YN == 1}"> checked </c:if>
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">                          	
	                                    </div>
	                                </div>	
	                                <c:if test="${_AUTH  eq 'SM'}"> 
	                                <div class="form-group">
										<label class="col-lg-1 control-label">Top 노출 여부</label>
	                                    <div class="col-lg-11">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="CON_TOP_YN" id="CON_TOP_YN" <c:if test="${detail.CON_TOP_YN == 1}"> checked </c:if>
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">                          	
	                                    </div>
	                                </div>
	                                </c:if>	
	                                <c:if test="${_AUTH  eq 'SM' and CON_TYPE != '9'}"> 
	                                <div class="form-group">
										<label class="col-lg-1 control-label">회원사 선택</label>
	                                    <div class="col-lg-11">
	                                    	<select class="form-control m-b" name="PRS_ID" id="PRS_ID">
	                                    	<option value="">선택하세요.</option>
											<c:forEach var="prsList" items="${prsList}" varStatus="status">
												<option value="${prsList.PRS_ID }" <c:if test="${detail.PRS_ID eq prsList.PRS_ID}">selected</c:if> >${prsList.PRS_NAME }</option>
											</c:forEach>
	                                    	</select>                     	
	                                    </div>
	                                </div>
	                               </c:if>  
	                             
	                               <c:if test="${_AUTH  eq 'SM' and CON_TYPE == '9'}"> 
	                               	<input type="hidden" id="PRS_ID" name="PRS_ID" value="1">
	                               </c:if>                                             
	                               
	  
								</form>
							</div>
							
							<div class="panel-footer clearfix">
								<div class="pull-right">
									<button type="button" class="btn btn-success btn-sm" id="btn_cont_save">
										<i class="fa fa-save"></i> 저장
									</button>
									<button type="button" class="btn btn-outline btn-default btn-sm" onClick="$('#contDialog').dialog('close');">
										<i class="fa fa-close"></i> 닫기
									</button>
								</div>
							</div>
						</div>
				
</div>