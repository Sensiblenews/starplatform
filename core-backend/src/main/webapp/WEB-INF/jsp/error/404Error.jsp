<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<div class="middle-box text-center animated fadeInDown">
	<h3 class="font-bold">요청하신 서비스를 이용하실 수 없습니다.</h3>
	<h3 class="font-bold">잠시 후 다시 이용해 주시기 바랍니다.</h3>
		
	<div>
		<a href="${ctx }<%=Constants.DEFAULT_RETURN_URL%>" class="btn btn-primary m-t">Main</a>
	</div>
</div>