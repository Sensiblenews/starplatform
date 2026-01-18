<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	String cp = request.getContextPath();
%>
<%--ContextPath 선언 --%>
<html>
<head>
<title>스타플랫폼 스토어</title>
</head>
<script>
	function appUrl(url) {
		location.href = url;
	}
</script>
<body onLoad="appUrl('${url}');">
</body>
</html>