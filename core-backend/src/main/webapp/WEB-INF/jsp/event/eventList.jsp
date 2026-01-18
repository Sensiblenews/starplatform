<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	//조회 클릭으로 검색
    $('#btn_search').click(function() {    	
		
    	$("#currentPageNo").val("1");
    	
		$('#eventForm').attr('action','${ctx}/adm/eventList');		
		$('#eventForm').submit();		
		return false;     
    	
    });
	
    $('#searchKeyword').keydown(function(event){
		if(event.keyCode == 13){
			$("#currentPageNo").val("1");			
			$('#eventForm').attr('action','${ctx}/adm/eventList');
			$('#eventForm').submit();    
			return false;     
		}
	 	
	});
    
    
    $(".btn_detail").click(function(){
		var evtId = $(this).attr("paramId");	
		$("#EVT_ID").val(evtId);
		
		$('#contDialog').dialog('destroy');
        $('#contDialog').remove();
        
    	var option = {
    			 url : '${ctx}/adm/eventUpdateForm.do',  			    
    	            target : '#contDialog',
    	            dataType : 'html',   	            
    	            success : function(data){
    	            	var $o = $('#contDialog'); 
    	                $o.html(data);  
    	            }                   
    	        };
    	
    	$("body").append("<div id='contDialog'></div>");
    	$('#eventForm').ajaxSubmit(option);
    	
    	$('#contDialog').dialog({
    	       bgiframe: true
    	       , modal : true
    	       , resizable: true
    	       , width: "60%"
	    	   , height: 500        
    	       , title : '이벤트 수정'
    	       , open: function(event, ui) { 
    	    	   overlay_resize();
    	       }
    	       , close: function(event, ui){
    	           $(this).dialog('destroy');
    	           $(this).remove();
    	       }     	     
			
    	 });
    	
    	$("#contDialog").parent().addClass("search-modal");
    	return false;	
	});
    
    
    $("#btn_addContent").click(function(){
    	
			$('#contDialog').dialog('destroy');
	        $('#contDialog').remove();
	        
	    	var option = {
	    			 url : '${ctx}/adm/eventInsertForm.do',  			    
	    	            target : '#contDialog',
	    	            dataType : 'html',   	            
	    	            success : function(data){
	    	            	var $o = $('#contDialog'); 
	    	                $o.html(data);  
	    	            }                   
	    	        };
	    	
	    	$("body").append("<div id='contDialog'></div>");
	    	$('#eventForm').ajaxSubmit(option);
	    	
	    	$('#contDialog').dialog({
	    	       bgiframe: true
	    	       , modal : true
	    	       , resizable: true
	    	       , width: "60%"
	    	       , height: 530       
	    	       , title : '이벤트 등록'
	    	       , open: function(event, ui) { 
	    	    	   overlay_resize();
	    	       }
	    	       , close: function(event, ui){
	    	           $(this).dialog('destroy');
	    	           $(this).remove();
	    	       }     	     
				
	    	 });
	    	
	    	$("#contDialog").parent().addClass("search-modal");
	    	return false;	
			
    });	
    
    
  
        
});

function pageLink(currentPageNo)
{
	
	$("#currentPageNo").val(currentPageNo);
	
	$('#eventForm').attr('action','${ctx}/adm/eventList');
	$('#eventForm').submit();   
	return false;	
}


</script>


<!-- 네비게이션 -->            
<div class="row wrapper border-bottom white-bg page-heading">
    <div class="col-lg-10">
        <h2>이벤트 관리</h2>       
    </div>
</div>

<div class="wrapper wrapper-content  animated fadeInUp">
	
	
	<div class="row">
	    <div class="col-lg-12">
	        <div class="ibox float-e-margins">	           
	            <div class="ibox-title">
	                 <h5>이벤트 목록 </h5>    
	                           	
	                <div class="ibox-tools">	
	                	
	                	총 : ${paramVO.totalRecordCount} 건   
	                	   
	                    <a class="collapse-link">
	                        <i class="fa fa-chevron-up"></i>
	                    </a>
	                </div>
	            </div>
	            <!-- content start -->
	            <div class="ibox-content">
	                <div class="row">	
	                
	                    <div class="col-sm-8"> 
	                	 	<button type="button" class="btn btn-outline btn-danger" id="btn_addContent" >이벤트 추가</button>	
	                	</div>  
	                	
		                <div class="col-sm-4 pull-right"> 
	                        <form name="eventForm" id="eventForm" method="POST">
							<input type="hidden" id="EVT_ID" name="EVT_ID"> 
								<input type="hidden" name="currentPageNo" id="currentPageNo" value="${paramVO.currentPageNo }" />
								
							</form>								
	                    </div>
	                </div>
	                <div class="table-responsive">
	                     <table class="table table-striped"> 	                  
							<thead>
								<tr>
									<th width="5%" class="text-center">No</th>
									<c:if test="${_AUTH eq 'SM' }">
									<th width="10%" class="text-center">회원사</th>
									</c:if>		
									<th width="15%" class="text-center">이벤트 제목</th>
									<th width="30%" class="text-center">이벤트 내용</th>
									<th width="15%" class="text-center">URL</th>
									<th width="10%" class="text-center">별</th>
									<th width="7%" class="text-center">참여 수</th>
									<th width="8%" class="text-center">게시 여부</th>			
									
								</tr>
							</thead>
							<tbody>
								<c:set value="${paramVO.totalRecordCount}" var="count"/>
								<c:forEach var="eList" items="${eList}" varStatus="status">
									<tr class="btn_detail" paramId="${eList.EVT_ID}">										
										<th class="text-center" scope="row">${count-paramVO.currentPageNo*paramVO.recordCountPerPage+paramVO.recordCountPerPage}</th>
										<c:if test="${_AUTH eq 'SM' }">
										<td class="text-center">${eList.PRS_NAME }</td>
										</c:if>	
										<td class="text-center">${eList.EVT_BTN_TEXT }</td>
										<td class="text-center">${eList.EVT_TEXT }</td>
										<td class="text-center">${eList.EVT_URL }</td>
										<td class="text-center">
										<c:choose>
											<c:when test="${eList.EVT_CHG_YN == 0}">
												☆ ${eList.EVT_STAR } 
											</c:when>
											<c:otherwise>
												★ ${eList.EVT_STAR } 
											</c:otherwise>
										</c:choose>
										</td> 
										<td class="text-center">${eList.CNT }</td>
										<td class="text-center">${eList.EVT_USE_YN }</td>
									</tr>
								<c:set var="count" value="${count-1}"/>	
								</c:forEach>
							</tbody>
						</table>
	                </div>
	
	            </div>
	            <!-- content end -->
	            <div class="pagination-bar text-center">
				    <ul class="pagination">
				       <ui:pagination paginationInfo="${paramVO}" type="image" jsFunction="pageLink" />
				    </ul>
				</div>
	           
	        
	        </div>	   
	    </div>	
	</div>
	
	
	
</div>
