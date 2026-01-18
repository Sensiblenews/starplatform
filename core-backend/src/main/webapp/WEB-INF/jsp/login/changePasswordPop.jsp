<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>


<script type="text/javascript">
	var passwordObj = new Object();
	
	// 비밀번호 체크
	passwordObj.set_new_password = function() {
		var formObj = getFormObj("#passwordForm");
		
		if( !isNotEmpty(formObj.oldPw) ) {
			$("#oldPw").focus();
			showMsgPop("현재 비밀번호를 입력해주세요.");
			return;
		}
		
		if( !isNotEmpty(formObj.currentPw) ) {
			$("#currentPw").focus();
			showMsgPop("새로운 비밀번호를 입력해주세요.");
			return;
		}
		
		if( formObj.currentPw.length < 8 ) {
			$("#currentPw").focus();
			showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
			return;
		}
		
		if( formObj.currentPw.length > 20 ) {
			$("#currentPw").focus();
			showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
			return;
		}
		
		var blank_pattern = /[\s]/g;
		
		if( blank_pattern.test(formObj.currentPw) == true ) {
			$("#currentPw").focus();
			showMsgPop("새로운 비밀번호는 공백을 포함할 수 없습니다.");
			return;
		}		
		
		var special_pattern = /[ \{\}\[\]\/?.,;:|\)*~`!^\-_+┼<>@\#$%&\'\"\\\(\=]/gi;
		
		if( special_pattern.test(formObj.currentPw) == false ) {
			$("#currentPw").focus();
			showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
			return;
		}
		
		var number_pattern = /[0-9]/g;
		
		if( number_pattern.test(formObj.currentPw) == false ) {
			$("#currentPw").focus();
			showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
			return;
		}
		
		var char_pattern = /[a-z]/ig;
		
		if( char_pattern.test(formObj.currentPw) == false ) {
			$("#currentPw").focus();
			showMsgPop("새로운 비밀번호는 <br/>숫자와 영문, 특수문자를 포함한 8~20자로 설정해 주세요.");
			return;
		}
		
		
		if( !isNotEmpty(formObj.currentPwRe) ) {
			$("#currentPwRe").focus();
			showMsgPop("비밀번호 확인란을 입력해주세요.");
			return;
		}
		
		if( formObj.currentPw === formObj.oldPw ) {
			$("#currentPw").focus();
			showMsgPop("비밀번호가 변경되지 않았습니다.");
			return;
		}
		
		if( formObj.currentPw !== formObj.currentPwRe ) {
			$("#currentPwRe").focus();
			showMsgPop("비밀번호가 다릅니다.<br />다시 입력하세요.");
			return;
		}
		
		var reqUserId;

		try {
			reqUserId = userId;
		} catch ( e ) {
			reqUserId = "";
		} finally {
			$.ajax({
				url: "${ctx}/login/setNewPassword.do"
				, data: {
					"PRS_ID": reqUserId
					, "PRS_PWD": formObj.currentPw
					, "oldLoginPw": formObj.oldPw
					, "PRS_AUTH_YN":"Y"
				}
				, success: passwordObj.set_new_password_result
			});
		}
	};
	
	// 비밀번호 변경 결과
	passwordObj.set_new_password_result = function(result) {
		var status = result.status;
		
		if( status == "success" ) {
			passwordObj.redirectUrl = result.redirectUrl;
			passwordObj.loginId = result.loginId;
			showMsgPop( result.serviceMessage, null, passwordObj.page_refresh );
		} else {
			showMsgPop( result.serviceMessage );
		}
	}
	
	// 성공일 시 화면 Refresh
	// 로그인 한 상태에서 비밀번호를 변경 했을 시, 원래 페이지로 Redirect
	// 로그인 화면에서 비밀번호를 변경했을 시, 로그인 체크 URL호출
	passwordObj.page_refresh = function() {
		var redirectUrl = passwordObj.redirectUrl;

		if( isNotEmpty(redirectUrl) ) {
			document.location.href = redirectUrl;
		} else {
			/* $("#loginId").val( passwordObj.loginId );
			$("#loginPw").val( $("#currentPw").val() ); 
			$("#loginForm").attr('action', '${ctx}/login/loginPrc.do');
			$("#loginForm").submit(); */
		}
		
		$("#passwordPop").dialog("close");
	};

	$(document).ready(function() {
		$("#btn_set_password").click( passwordObj.set_new_password );
	});
</script>


	<div class="panel">
		<div class="panel-body">
			
			<form action="" id="passwordForm" method="POST">
				<div class="row m-b-sm m-t-sm">
				
					<div class="col-md-5">
						<span class="imp-span"><i class="fa fa-asterisk"></i></span>
						<label class="control-label">현재 비밀번호</label>
					</div>
					<div class="col-md-7">
						<div class="input-group input-size">
							<input type="password" name="oldPw" id="oldPw" title="" class="input-sm form-control">
						</div>
					</div>
				</div>
	
				<div class="row m-b-sm m-t-sm">
					<div class="col-md-5">
						<span class="imp-span"><i class="fa fa-asterisk"></i></span>
						<label class="control-label">새로운 비밀번호</label>
					</div>
					<div class="col-md-7">
						<div class="input-group input-size">
							<input type="password" name="currentPw" id="currentPw" title="" class="input-sm form-control">
						</div>
					</div>
				</div>
	
				<div class="row m-b-sm m-t-sm">
					<div class="col-md-5">
						<span class="imp-span"><i class="fa fa-asterisk"></i></span>
						<label class="control-label">비밀번호 확인</label>
					</div>
					<div class="col-md-7">
						<div class="input-group input-size">
							<input type="password" name="currentPwRe" id="currentPwRe" title="" class="input-sm form-control">
						</div>
					</div>
				</div>
			</form>
			
		</div>

		<div class="panel-footer clearfix">
           	<div class="pull-right">
		 		<button type="button" class="btn btn-success btn-sm" id="btn_set_password">
					<i class="fa fa-save"></i> 저장
				</button>
				<button type="button" class="btn btn-white" onClick="$('#passwordPop').dialog('close');">Close</button>
			</div>
		</div>
	</div>
