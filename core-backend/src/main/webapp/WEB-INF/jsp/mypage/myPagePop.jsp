<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script type="text/javascript">
	var passwordObj = new Object();

	
	passwordObj.set_new_password = function() {
		var formObj = getFormObj("#passwordForm");
		
		if( !isNotEmpty(formObj.PRS_NAME) ) {
			$("#PRS_NAME").focus();
			showMsgPop("회원사명을 입력해주세요.");
			return;
		}
		
		
		if( formObj.PRS_PWD != '' ) {
			
			if( !isNotEmpty(formObj.oldPw) ) {
				$("#oldPw").focus();
				showMsgPop("현재 비밀번호를 입력해주세요.");
				return;
			}
			
			if( !isNotEmpty(formObj.PRS_PWD) ) {
				$("#PRS_PWD").focus();
				showMsgPop("새로운 비밀번호를 입력해주세요.");
				return;
			}
			
			if( formObj.PRS_PWD.length < 8 ) {
				$("#PRS_PWD").focus();
				showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
				return;
			}
			
			if( formObj.PRS_PWD.length > 20 ) {
				$("#PRS_PWD").focus();
				showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
				return;
			}
			
			var blank_pattern = /[\s]/g;
			
			if( blank_pattern.test(formObj.PRS_PWD) == true ) {
				$("#PRS_PWD").focus();
				showMsgPop("새로운 비밀번호는 공백을 포함할 수 없습니다.");
				return;
			}		
			
			var special_pattern = /[ \{\}\[\]\/?.,;:|\)*~`!^\-_+┼<>@\#$%&\'\"\\\(\=]/gi;
			
			if( special_pattern.test(formObj.PRS_PWD) == false ) {
				$("#PRS_PWD").focus();
				showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
				return;
			}
			
			var number_pattern = /[0-9]/g;
			
			if( number_pattern.test(formObj.PRS_PWD) == false ) {
				$("#PRS_PWD").focus();
				showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
				return;
			}
			
			var char_pattern = /[a-z]/ig;
			
			if( char_pattern.test(formObj.PRS_PWD) == false ) {
				$("#PRS_PWD").focus();
				showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
				return;
			}
			
			
			if( !isNotEmpty(formObj.currentPwRe) ) {
				$("#currentPwRe").focus();
				showMsgPop("비밀번호 확인란을 입력해주세요.");
				return;
			}
			
			if( formObj.PRS_PWD === formObj.oldPw ) {
				$("#PRS_PWD").focus();
				showMsgPop("비밀번호가 변경되지 않았습니다.");
				return;
			}			
			

			
			if( formObj.PRS_PWD !== formObj.currentPwRe ) {
				$("#currentPwRe").focus();
				showMsgPop("비밀번호가 다릅니다.<br />다시 입력하세요.");
				return;
			}
		}
		
		
		
		if(confirm("수정 하시겠습니까?"))
		{
			var option = {
	    			url: '${ctx}/adm/updateMypage.do',
					dataType : 'json',
					success : function(data){
						
						if(data.status == 'success')
						{
							showMsgPop(data.serviceMessage,null,passwordObj.page_refresh);
						}else{
							showMsgPop(data.serviceMessage);
						}
						
					}				 
			};
			
			 $('#passwordForm').ajaxSubmit(option);
			 return false; 
		}	
		return false;

		
		
	};
	
	// 비밀번호 변경 결과
	passwordObj.set_new_password_result = function(result) {
		var status = result.status;
		
		if( status == "success" ) {			
			showMsgPop( result.serviceMessage, null, passwordObj.page_refresh );
		} else {
			showMsgPop( result.serviceMessage );
		}
	}	
	
	passwordObj.page_refresh = function() {
		$("#myPagePop").dialog("close");
	};

	$(document).ready(function() {
		$("#btn_set_password").click( passwordObj.set_new_password );
	});
</script>


<div class="ibox animated fadeIn">
						<div class="panel panel-warning">
							<div class="panel-body">
								
								<form name="passwordForm" id="passwordForm" method="POST" enctype="multipart/form-data">
														
	                                <div class="row m-b-sm m-t-sm">
										<label class="col-lg-2 control-label">회원사명</label>
	                                    <div class="col-lg-10 ">
	                                    	<input class="form-control" type="text" id="PRS_NAME" name="PRS_NAME" value="${pMap.PRS_NAME }" title="회원사명"/>             	
	                                    </div>
	                                </div>                               
									<div class="row m-b-sm m-t-sm">
										<label class="col-lg-2 control-label">E-MAIL</label>
	                                    <div class="col-lg-10">
	                                    	<input class="form-control" type="text" id="PRS_EMAIL" name="PRS_EMAIL" value="${pMap.PRS_EMAIL }"title="이메일" />                         	
	                                    </div>
	                                </div>
	                               
	                                <div class="row m-b-sm m-t-sm">
										<label class="col-lg-2 control-label">홈페이지</label>
	                                    <div class="col-lg-10">
	                                    	 <input class="form-control" type="text" id="PRS_HOMEPAGE" name="PRS_HOMEPAGE" value="${pMap.PRS_HOMEPAGE }"/>                         	
	                                    </div>
	                                </div>
	                                <div class="row m-b-sm m-t-sm">
										<label class="col-lg-2 control-label">회사로고</label>
	                                    <div class="col-lg-10">
	                                    	<c:if test="${not empty pMap.STORED_FILE_NM}">
	                                    	<img src="${pMap.STORED_FILE_NM}" height="80" width="80"/>
	                                    	</c:if>
	                                    	<input type="file" id="ORIG_FILE_NM" name="ORIG_FILE_NM" title="logo 이미지"> 
	                                    </div>
	                                </div>
	                                <div class="row m-b-sm m-t-sm">
										<label class="col-lg-2 control-label">현재 비밀번호</label>
	                                    <div class="col-lg-10">
	                                    	<input type="password" name="oldPw" id="oldPw" title="현재 비밀번호" class="input-sm form-control">                         	
	                                    </div>
	                                </div>
	                                <div class="row m-b-sm m-t-sm">
										<label class="col-lg-2 control-label">새로운 비밀번호</label>
	                                    <div class="col-lg-10">
	                                    	<input type="password" name="PRS_PWD" id="PRS_PWD" title="새로운 비밀번호" class="input-sm form-control">                         	
	                                    </div>
	                                </div>
	                                <div class="row m-b-sm m-t-sm">
										<label class="col-lg-2 control-label">비밀번호 확인</label>
	                                    <div class="col-lg-10">
	                                    	<input type="password" name="currentPwRe" id="currentPwRe" title="비밀번호 확인" class="input-sm form-control">                         	
	                                    </div>
	                                </div>
	
								</form>
							</div>
							
							<div class="panel-footer clearfix">
								<div class="pull-right">
									<button type="button" class="btn btn-success btn-sm" id="btn_set_password">
										<i class="fa fa-save"></i> 저장
									</button>
									<button type="button" class="btn btn-outline btn-default btn-sm" onClick="$('#myPagePop').dialog('close');">
										<i class="fa fa-close"></i> 닫기
									</button>
								</div>
							</div>
						</div>
				
</div>

	
