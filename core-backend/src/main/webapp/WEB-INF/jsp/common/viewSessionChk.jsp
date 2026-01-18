<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>
<script>
//alert("세션 정보가 종료되었습니다. \n다시 로그인 하여 주십시요.");
if(opener)
{
	this.close();	
	opener.document.location.href="${ctx}<%=Constants.DEFAULT_RETURN_URL%>";
}
else
{
	document.location.href="${ctx}<%=Constants.DEFAULT_RETURN_URL%>";	
}	
</script>
