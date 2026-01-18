<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>
<%@ page import="com.sensible.common.util.SessionUtil"%>

<%	
	

%>


<script type="text/javascript">

$(function()
{	

 	
	
	
});



/**
*profile jsp로 이동하기 위한 java 스크립트
*
*/
function goUrl()
{
    location.href = "${ctx}/adm/profile.do";	
}
/**
* setting 폼 이동위한 javascript
*/
function goChange()
{
   location.href = "${ctx}/adm/userPwdChgForm.do";	
}
	
</script> 

			<div class="row border-bottom">
				<nav class="navbar navbar-static-top" role="navigation" style="margin-bottom: 0">
			        <div class="navbar-header">
			            <a class="navbar-minimalize minimalize-styl-2 btn btn-primary " href="#"><i class="fa fa-bars"></i> </a>

			        </div>

		            <ul class="nav navbar-top-links navbar-right">		               
		                <li>
		                    <!-- <span class="m-r-sm text-muted welcome-message">Welcome to Witch-Hunting</span> -->
		                </li>
		                <li>
		                    <a href="#" onclick="actionLogout();">
		                        <i class="fa fa-sign-out"></i> Log out
		                    </a>
		                </li>
		                
		            </ul>
		
		        </nav>
	        </div>

<form name="topForm" id="topForm" method="post">
	<input type="hidden" id="baseMenuId" name="baseMenuId" value="<c:out value='<%=session.getAttribute("baseMenuId")%>'/>" />
	<input type="hidden" id="menuId" name="menuId" value="<c:out value='<%=session.getAttribute("menuId")%>'/>" />
 	<input type="hidden" id="link" name="link" value="" />
</form>
<div id="openDialog"></div>

