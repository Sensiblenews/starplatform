<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	    
    var infoText = $("#<c:out value='<%=session.getAttribute("menuId")%>' />").text();  
    $("#veiwInfoTxt").text(infoText);
    $("#viewTit").text(infoText);	
    
    
    $(".btn_detail").click(function(){
		var conId = $(this).attr("paramId");	
		$("#CON_ID").val(conId);
		
		$('#contDialog').dialog('destroy');
        $('#contDialog').remove();
        
    	var option = {
    			 url : '${ctx}/adm/settingUpdateForm.do',  			    
    	            target : '#contDialog',
    	            dataType : 'html',   	            
    	            success : function(data){
    	            	var $o = $('#contDialog'); 
    	                $o.html(data);  
    	            }                   
    	        };
    	
    	$("body").append("<div id='contDialog'></div>");
    	$('#contForm').ajaxSubmit(option);
    	
    	$('#contDialog').dialog({
    	       bgiframe: true
    	       , modal : true
    	       , resizable: true
    	       , width: "70%"
    	       , height: 840       
    	       , title : infoText+' 수정'
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
	    			 url : '${ctx}/adm/settingInsertForm.do',  			    
	    	            target : '#contDialog',
	    	            dataType : 'html',   	            
	    	            success : function(data){
	    	            	var $o = $('#contDialog'); 
	    	                $o.html(data);  
	    	            }                   
	    	        };
	    	
	    	$("body").append("<div id='contDialog'></div>");
	    	$('#contForm').ajaxSubmit(option);
	    	
	    	$('#contDialog').dialog({
	    	       bgiframe: true
	    	       , modal : true
	    	       , resizable: true
	    	       , width: "70%"
	    	       , height: 670       
	    	       , title : infoText+' 등록'
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
    
    
    $(".btn_comment").click(function(){
    	var conId = $(this).attr("paramId");	
		$("#CON_ID").val(conId);
		var option = {
				   url : '${ctx}/adm/commentUpdateForm.do',				
		            target : '#openDialog',
		            dataType : 'html',   	            
		            success : function(data){
		            	var $o = $('#openDialog'); 
		                $o.html(data);        	              
		             
		            }                   
		        };
			$('#contForm').ajaxSubmit(option);        		
	});

});



function pageLink(currentPageNo)
{
	
	$("#currentPageNo").val(currentPageNo);
	
	$('#contForm').attr('action','${ctx}/adm/contentList');
	$('#contForm').submit();   
	return false;	
}

function subContent(conType) {
	
	$("#CON_TYPE").val(conType);
	$('#btn_search').click();
}


function movePrevUp(now) {		
	document.contForm.type.value = "prevUp";
	document.contForm.now.value = now;
	document.contForm.action="/adm/contentSort";
	document.contForm.submit();
	//event.stopPropagation();
}

function moveUp(now, prev) {		
	document.contForm.type.value = "moveUp";
	document.contForm.now.value = now;
	document.contForm.prev.value = prev;
	document.contForm.action="/adm/contentSort";
	document.contForm.submit();
	
	//event.stopPropagation();
}

function moveDown(now, next) {
	document.contForm.type.value = "moveDown";
	document.contForm.now.value = now;
	document.contForm.next.value = next;
	document.contForm.action="/adm/contentSort";
	document.contForm.submit();
	
	//event.stopPropagation();
}

function moveNextDown(now) {
	document.contForm.type.value = "nextDown";
	document.contForm.now.value = now;
	document.contForm.action="/adm/contentSort";
	document.contForm.submit();
	
	//event.stopPropagation();
}

</script>


<!-- 네비게이션 -->            
<div class="row wrapper border-bottom white-bg page-heading">
    <div class="col-lg-10">
        <h2>APP 설정</h2>   
        <ol class="breadcrumb">
        	<li>
                <a href="<%=Constants.DEFAULT_RETURN_URL%>">Home</a>
            </li>
        	<li>
                <a href="#">APP 설정</a>
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
					<h5><span id="viewTit"></span> 목록 </h5>	                           	
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
	                	<%-- <c:if test="${CON_TYPE != 5 and CON_TYPE != 7 }"> --%>
	                    <div class="col-sm-8"> 
	                	 	<button type="button" class="btn btn-outline btn-danger" id="btn_addContent" >글 작성</button>	
	                	</div>  
	                	
		                <div class="col-sm-4 pull-right"> 
	                        <form name="contForm" id="contForm" method="POST">
								<input type="hidden" id="CON_ID" name="CON_ID"> 
								<input type="hidden" id="CON_TYPE" name="CON_TYPE" value="${paramVO.CON_TYPE }" />
								<input type="hidden" name="currentPageNo" id="currentPageNo" value="${paramVO.currentPageNo }" />

							</form>								
	                    </div>
	                </div>
	                <div class="table-responsive">
	                     <table class="table table-striped"> 	                  
							<thead>
								<tr>
									<th width="10%">No</th>
									<c:if test="${_AUTH eq 'SM' }">
									<th width="10%">회원사</th>
									</c:if>		
									<c:choose>
										<c:when test="${CON_TYPE == 3 || CON_TYPE == 6 || CON_TYPE == 2 }">
											<th width="45%">제목</th>
											<th width="15%" class="text-center">게시 여부</th>
											<th width="15%" class="text-center">조회 수</th>
											<th width="15%" class="text-center">댓글 수</th>
										</c:when>
										<c:when test="${CON_TYPE == 7 }">											
											<th width="90%">제목</th>
										</c:when>
										<c:otherwise>											
											<th width="75%">제목</th>
											<th width="15%" class="text-center">게시 여부</th>
										</c:otherwise>
									</c:choose>								
								</tr>
							</thead>
							<tbody>
								<c:set value="${paramVO.totalRecordCount}" var="count"/>
								<c:forEach var="cList" items="${cList}" varStatus="status">
									<tr class="btn_detail" paramId="${cList.CON_ID}">										
										<td scope="row">${count-paramVO.currentPageNo*paramVO.recordCountPerPage+paramVO.recordCountPerPage}</td>
										<c:if test="${_AUTH eq 'SM' }">
										<td>${cList.PRS_NAME }</td>
										</c:if>			
										<c:choose>
										<c:when test="${CON_TYPE == 3 || CON_TYPE == 6 || CON_TYPE == 2 }">
											<td>${cList.CON_TITLE }</td>
											<td class="text-center">${cList.CON_USE_YN }</td>
											<td class="text-center">${cList.CNT }</td>
											<td class="text-center">
											<a href="#" onClick="getComments('${cList.CON_ID }');">
											${cList.CMT_CNT }
											</a>
											</td>
										</c:when>
										<c:when test="${CON_TYPE == 7 }">
											<td>${cList.CON_TITLE }</td>
										</c:when>
										<c:otherwise>
											<td>${cList.CON_TITLE }</td>
											<td class="text-center">${cList.CON_USE_YN }</td>
										</c:otherwise>
									</c:choose>	
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
