<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script>
	function openAppSite(){
		window.location.href = "https://witch-hunting.com/appUrl";
	}
</script>

<div class="full-box text-center animated fadeInDown">
	<div class="vcenter">
	    <h1 class="font-bold" style="font-size:40px;">스타플랫폼</h1>
	
		<img src="${pageContext.request.contextPath}/resources/img/witch_icon.png" style="margin-top: 24px;"/>
		
		<p>
		<button class="btn btn-primary btn-lg" style="margin-top: 36px;" onclick="openAppSite()">스타플랫폼 앱 다운로드</button>
		</p>
	</div>
</div>