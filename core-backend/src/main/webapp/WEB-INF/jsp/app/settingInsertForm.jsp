<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script type='text/javascript'>
$(function() 
{	
	
	$('.datatoggle').bootstrapToggle({});
	
	var validation = $('#contDetailForm').sv({
		checkOnSubmit : false,
		rules : {
			CON_TITLE : [ 'required' ]			
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
		if (!thumbFile && !videoFile) {
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
				url : '${ctx}/adm/settingInsertSave.do',
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
			oEditors.getById["CON_BODY"].exec("UPDATE_CONTENTS_FIELD", []);
			
			$('#contDetailForm').ajaxSubmit(option);
			return false;
		}		
		
		return false;
		
	});
		return false;
		
	});
	
	
	//에디터 설정
	var oEditors = [];
	nhn.husky.EZCreator.createInIFrame({
		oAppRef : oEditors,
		elPlaceHolder : "CON_BODY",
		sSkinURI : "${ctx}/resources/smarteditor2/SmartEditor2Skin.html",
		htParams : {
			// 툴바 사용 여부 (true:사용/ false:사용하지 않음)
			bUseToolbar : true,
			// 입력창 크기 조절바 사용 여부 (true:사용/ false:사용하지 않음)
			bUseVerticalResizer : true,
			bUseModeChanger : true,
			fOnBeforeUnload : function() {
			}
		},
		fCreator : "createSEditor2"
	}); 
	
});

</script>

<div class="ibox animated fadeIn">
						<div class="panel panel-warning">
							<div class="panel-body">
								
								<form name="contDetailForm" id="contDetailForm" class="form-horizontal" method="POST" enctype="multipart/form-data">
								<c:if test="${CON_TYPE != 6}">
								<input type="hidden" id="CON_CATEGORY" name="CON_CATEGORY" value="8">
								</c:if>
								<input type="hidden" id="CON_TYPE" name="CON_TYPE" value="${CON_TYPE}">								
														
	                                <div class="form-group">
										<label class="col-lg-1 control-label">제목</label>
	                                    <div class="col-lg-11 ">
	                                    	<input type="text" class="form-control"  id="CON_TITLE" name="CON_TITLE" title="제목" />             	
	                                    </div>
	                                </div>   
	                                <!-- faq , 홍보마당 , 스토리, 논리구구단-->
	                                <c:if test="${CON_TYPE == '2' || CON_TYPE == '3' || CON_TYPE == '6' || CON_TYPE == '11' }">                            
									<div class="form-group">
										<label class="col-lg-1 control-label">부제목</label>
	                                    <div class="col-lg-11">
	                                    	<input type="text" class="form-control"  id="CON_SUBTITLE" name="CON_SUBTITLE" title="부제목" />                         	
	                                    </div>
	                                </div>
	                                </c:if>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">메인 이미지</label>
	                                    <div class="col-lg-11">
	                                    	<input type="file" id="CON_THUMNAIL" name="CON_THUMNAIL" title="메인 이미지">                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">메인 동영상</label>
	                                    <div class="col-lg-11">
	                                    	<input type="file" id="CON_VIDEO" name="CON_VIDEO" title="메인 동영상">                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-1 control-label">내용</label>
	                                    <div class="col-lg-11">
	                                    	 <textarea name="CON_BODY" id="CON_BODY" style="width: 100%"></textarea>                         	
	                                    </div>
	                                </div>
	                                <!-- 홍보마당 , 스토리 ,사용설명서,이용약관 -->
	                                <c:if test="${CON_TYPE == '3' || CON_TYPE == '6'}">   
	                                <div class="form-group">
										<label class="col-lg-1 control-label">유튜브 URL</label>
	                                    <div class="col-lg-11">
	                                    	<input type="text" class="form-control"  id="CON_YOUTUBE_URL" name="CON_YOUTUBE_URL" title="유튜브 URL" />                         	
	                                    </div>
	                                </div>
	                                </c:if>
	                                <c:if test="${CON_TYPE == 6}">
									 <div class="form-group">
										<label class="col-lg-1 control-label">카테고리</label>
										<div class="col-lg-4">	       
											<select name="CON_CATEGORY">
													<option value="0">싱글남</option>
													<option value="1">싱글녀</option>
													<option value="2">전문직</option>
													<option value="3">직장인</option>
													<option value="4">고교생</option>
													<option value="5">대학생</option>
													<option value="6">공무원</option>
													<option value="7">CEO(사장님)</option>
											</select>
										</div>
	                                </div>	
									</c:if>     
	                                <div class="form-group">
										<label class="col-lg-1 control-label">게시 여부</label>
	                                    <div class="col-lg-11">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="CON_USE_YN" id="CON_USE_YN" checked
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">                          	
	                                    </div>
	                                </div>	
	                                <c:if test="${_AUTH  eq 'SM' and CON_TYPE != '9'}">   
	                                <div class="form-group">
										<label class="col-lg-1 control-label">APP 선택</label>
	                                    <div class="col-lg-11">
	                                    	<select class="form-control m-b" name="PRS_ID" id="PRS_ID">
	                                    	<option value="">선택하세요.</option>
											<c:forEach var="prsList" items="${prsList}" varStatus="status">
												<option value="${prsList.PRS_ID }">${prsList.PRS_NAME }</option>
											</c:forEach>
	                                    	</select>                     	
	                                    </div>
	                                </div>
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