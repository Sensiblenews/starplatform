<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles"  prefix="t"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0" />
	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <link rel="shortcut icon" href="/resources/img/favicon.jpg" />
	<title>스타플랫폼 관리자</title>
	<t:insertAttribute name="header"/> 	
	
	<link rel="stylesheet" href="https://uicdn.toast.com/editor/latest/toastui-editor.min.css" />
	<script src="https://uicdn.toast.com/editor/latest/toastui-editor-all.min.js"></script>
	
	<link rel="stylesheet" href="https://uicdn.toast.com/grid/latest/tui-grid.css" />
	<script src="https://uicdn.toast.com/grid/latest/tui-grid.js"></script>
</head>
<body class="" >
	<div id="wrapper">
		
		<t:insertAttribute name="left"/>		
		
		<div id="page-wrapper" class="gray-bg">
			<t:insertAttribute name="top"/>

			<div class="row">
				<div class="col-lg-12">
					
					<t:insertAttribute name="content"/>						
					
					<%-- <t:insertAttribute name="footer"/> --%>
				</div>
			</div>

		</div>
		
	</div>
</body>
</html>