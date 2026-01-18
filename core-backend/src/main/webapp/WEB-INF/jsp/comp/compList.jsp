<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	//조회 클릭으로 검색
    $('#btn_search').click(function() {    	
		
		
		$('#compForm').attr('action','${ctx}/adm/compList.do?PRS_TYPE='+$("#PRS_TYPE").val());		
		$('#compForm').submit();		
		return false;     
    	
    });
	
    $('#searchKeyword').keydown(function(event){
		if(event.keyCode == 13){
						
			$('#compForm').attr('action','${ctx}/adm/compList.do?PRS_TYPE='+$("#PRS_TYPE").val());		
			$('#compForm').submit();    
			return false;     
		}
	 	
	});
    
    
    $(".btn_detail").click(function(){
		
		$("#PRS_ID").val($(this).attr("paramId"));
				
    	$('#compDialog').dialog('destroy');
        $('#compDialog').remove();
        
    	var option = {
    			 url : '${ctx}/adm/compUpdateForm.do',  			    
    	            target : '#compDialog',
    	            dataType : 'html',   	            
    	            success : function(data){
    	            	var $o = $('#compDialog'); 
    	                $o.html(data);  
    	            }                   
    	        };

    	$("body").append("<div id='compDialog'></div>");
    	$('#compForm').ajaxSubmit(option);
    	
    	$('#compDialog').dialog({
    	       bgiframe: false
    	       , modal : true
    	       , resizable: true
    	       , width: "70%"
    	       , height: 520	       
    	       , title : '회원사 수정'
    	       , open: function(event, ui) { 
    	    	   overlay_resize();
    	       }
    	       , close: function(event, ui){
    	           $(this).dialog('destroy');
    	           $(this).remove();
    	       }
    	      
    	 });
    	
    	$("#compDialog").parent().addClass("search-modal");
    	return false;	
			
	});
    
    $("#btn_addComp").click(function(){
    	
			
    	$('#compDialog').dialog('destroy');
        $('#compDialog').remove();
        
    	var option = {
    			 url : '${ctx}/adm/compInsertForm.do',  			    
    	            target : '#compDialog',
    	            dataType : 'html',   	            
    	            success : function(data){
    	            	var $o = $('#compDialog'); 
    	                $o.html(data);  
    	            }                   
    	        };
    	
    	$("body").append("<div id='compDialog'></div>");
    	$('#compForm').ajaxSubmit(option);
    	
    	$('#compDialog').dialog({
    	       bgiframe: true
    	       , modal : true
    	       , resizable: true
    	       , width: "70%"
    	       , height: 470       
    	       , title : '회원사 등록'
    	       , open: function(event, ui) { 
    	    	   overlay_resize();
    	       }
    	       , close: function(event, ui){
    	           $(this).dialog('destroy');
    	           $(this).remove();
    	       }     	     
			
    	 });
    	
    	$("#compDialog").parent().addClass("search-modal");
    	return false;
			
    });	 
    
   
    
	
    var infoText = $("#<c:out value='<%=session.getAttribute("menuId")%>' />").text();  
    $("#veiwInfoTxt").text(infoText);
});

</script>


<!-- 네비게이션 -->            
<div class="row wrapper border-bottom white-bg page-heading">
    <div class="col-lg-10">
        <h2>업체관리</h2>
        <ol class="breadcrumb">
        	<li>
                <a href="<%=Constants.DEFAULT_RETURN_URL%>">Home</a>
            </li>
        	<li>
                <a href="#">업체관리</a>
            </li>
            <li class="active">
                <a href="#"><span id="veiwInfoTxt"></span></a>
            </li>          
            
        </ol> 
    </div>
</div>

<div class="wrapper wrapper-content  animated fadeInUp">
	
	<div class="row">
	    <div class="col-lg-12">
	        <div class="ibox float-e-margins">
	            
	            <div class="ibox-title">
	                <h5>업체 목록 </h5>
	                
	                <div class="ibox-tools">	
     	
	                	총 : ${list.size()} 건   
	                	   
	                    <a class="collapse-link">
	                        <i class="fa fa-chevron-up"></i>
	                    </a>
	                   
	                </div>
	            </div>
	            <!-- content start -->
	            <div class="ibox-content">
	                <div class="row">	   
	                	<form name="compForm" id="compForm" method="POST">
	                	<input type="hidden" name="PRS_ID" id="PRS_ID"  />  
						<input type="hidden" name="PRS_TYPE" id="PRS_TYPE" value="${paramVO.PRS_TYPE }" />          
							<div class="col-sm-8">    	
			                	<button type="button" class="btn  btn-outline btn-primary" id="btn_addComp" >회원사 추가</button>
			                </div>		
	                    </form>
	                </div>
	                <div class="table-responsive">
	                    <!--  <table class="table table-striped">  -->
	                    <table class="table table-hover table-striped"> 
							<thead>
								<tr>
									<th width="20%" class="text-center">회원사명</th>
									<th width="20%" class="text-center">이메일</th>
									<th width="15%" class="text-center">홈페이지</th>
									<th width="10%" class="text-center">별 수익</th>
									<th width="10%" class="text-center">하트 수익</th>
									<th width="7%" class="text-center">인증</th>
									<th width="10%" class="text-center">정상(Y) 정지(N)</th>
								</tr>
							</thead>
							<tbody>
								<c:forEach var="list" items="${list}" varStatus="status">
								<tr class="btn_detail" paramId="${list.PRS_ID}" >
									<td >${list.PRS_NAME }</td>
									<td >${list.PRS_EMAIL }</td>
									<td >${list.PRS_HOMEPAGE }</td>
									<td class="text-center">☆ ${list.PRICE } ★ ${list.PRICE_CHG }</td>
									<td class="text-center">♡ ${list.HEART } ♥ ${list.HEART_CHG }</td>
									<td class="text-center">${list.PRS_AUTH_YN}</td>
									<td class="text-center">${list.PRS_USE_YN}</td>
								</tr>
							</c:forEach>
							</tbody>
						</table>
	                </div>
	
	            </div>
	            <!-- content end -->
	           
	        
	        </div>	   
	    </div>	
	</div>
	
	
</div>


						