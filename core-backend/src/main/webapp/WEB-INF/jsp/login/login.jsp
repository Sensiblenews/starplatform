<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>


<script type='text/javascript'>
var resultCode = "<c:out value='${resultCode}'/>";
var userId = "<c:out value='${userId}'/>";

$(function() {
	
	if(resultCode == 'R')
	{
		showMsgPop("사용할 수 없는 아이디입니다.<br />[탈퇴처리 된 아이디 입니다.]");	
	}
	
	if(resultCode == 'F')
	{
		showMsgPop("Login User ID 또는 Password 가 올바르지 않습니다.<br />로그인 정보를 확인해 주십시오.",null,null,{width:400});		
	}
	
	if(resultCode == 'A')
	{
		showConfirmPop("스타플랫폼에 함께해 주셔서 감사합니다. <br />보안을 위해 비밀번호를 변경해 주십시오.", setNewPasswordPop, null,{width:380});
	}
	

	var validation = $('#loginForm').sv({
		checkOnSubmit : false,
		rules : {
			PRS_ID : [ 'required' ],
			PRS_PWD : [ 'required' ]
		},
		errorCss : 'formError',
		stopOnFirstError : true
	});
	
	$("#actionForm").click(function() {
		
		if (!validation.validate()) {
			return;
		}
		
		$.blockUI();
		
		$('#loginForm').attr('action', '${ctx }/login/loginPrc.do');
		$('#loginForm').submit();
		return false;
	}); 
	
	$(':input', $('#loginForm')).keydown(function(event) {
	     if(event.keyCode == 13 ) {	     
	     	 $('#actionForm').trigger('click');
	     	 return false;
	     }
	 });
	 
	
		 
});
  
</script>


 <div class="middle-box text-center loginscreen animated fadeInDown">
        <div class="ibox-content" style="margin-top: 200px;">
           
            <form id="loginForm" name="loginForm" class="m-t" role="form" method="POST">               
                  <div class="form-group">
                      <input type="text" class="form-control" name="PRS_ID" id="PRS_ID" title="아이디" placeholder="User ID" >
                  </div>
                  <div class="form-group">
                      <input type="password" class="form-control" name="PRS_PWD"  Id="PRS_PWD" title="비밀번호" placeholder="Password" >
                  </div>
                  <button type="button" class="btn btn-primary block full-width m-b" id="actionForm">Login</button>
                 

              </form>
        </div>
    </div>


</div>
