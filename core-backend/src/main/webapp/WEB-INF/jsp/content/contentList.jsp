<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	//조회 클릭으로 검색
    $('#btn_search').click(function() {    	
		
    	$("#currentPageNo").val("1");
    	
		$('#contForm').attr('action','${ctx}/adm/contentList');		
		$('#contForm').submit();		
		return false;     
    	
    });
	
    $('#searchKeyword').keydown(function(event){
		if(event.keyCode == 13){
			$("#currentPageNo").val("1");			
			$('#contForm').attr('action','${ctx}/adm/contentList');
			$('#contForm').submit();    
			return false;     
		}
	 	
	});
    
    
    $(".btn_detail").click(function(){
		var conId = $(this).attr("paramId");	
		$("#CON_ID").val(conId);
		
		$('#contDialog').dialog('destroy');
        $('#contDialog').remove();
        
    	var option = {
    			 url : '${ctx}/adm/contentUpdateForm.do',  			    
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
    	       , width: "80%"
    	       , height: 830       
    	       , title : '콘텐츠 수정'
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
	    			 url : '${ctx}/adm/contentInsertForm.do',  			    
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
	    	       , width: "80%"
	    	       , height: 830       
	    	       , title : '콘텐츠 등록'
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
    
    $("#btn_updateCategory").click(function() {
    	$('#contDialog').dialog('destroy');
        $('#contDialog').remove();
        
    	var option = {
 			 url : '${ctx}/adm/categoryUpdateForm.do',  			    
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
    	       , width: "80%"
    	       , height: 370       
    	       , title : '버튼 태그 변경'
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
        <h2>콘텐츠</h2>       
    </div>
</div>

<div class="wrapper wrapper-content  animated fadeInUp">
	
	
	<div class="row">
	    <div class="col-lg-12">
	        <div class="ibox float-e-margins">	           
	            <div class="ibox-title">
	                            			
						<c:if test="${_AUTH  ne 'GE'}">
							<c:forEach var="categoryItem" items="${categoryList}">
								<c:choose>
									<c:when test="${categoryItem.type eq 0}">
										<c:set var="btnClass" value="btn-primary" />
									</c:when>
									<c:when test="${categoryItem.type eq 1}">
										<c:set var="btnClass" value="btn-success" />
									</c:when>
									<c:when test="${categoryItem.type eq 8}">
										<c:set var="btnClass" value="btn-info" />
									</c:when>
									<c:when test="${categoryItem.type eq 9}">
										<c:set var="btnClass" value="btn-warning" />
									</c:when>
									<c:otherwise>
										<c:set var="btnClass" value="btn-default" />
									</c:otherwise>
								</c:choose>
							<button type="button" class="btn btn-sm ${btnClass} <c:if test="${CON_TYPE != categoryItem.type}">btn-outline</c:if>" 
							onClick="subContent('${categoryItem.type}')">${categoryItem.category}</button>
							</c:forEach>
						</c:if>
	                    <div class="ms-4"></div>
	                   	<button id="btn_updateCategory" type="button" class="btn btn-sm btn-secondary" style="margin-top: 5px;">버튼 태그 변경</button>
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
	                
	                    <div class="col-sm-6"> 
	                	 	<button type="button" class="btn btn-outline btn-danger" id="btn_addContent" >글 작성</button>	
	                	</div>  
	                	
		                <div class="col-sm-6 pull-right "> 
	                        <form name="contForm" id="contForm" method="POST" >
							<input type="hidden" id="CON_ID" name="CON_ID"> 
							<input type="hidden" id="CON_TYPE" name="CON_TYPE" value="${paramVO.CON_TYPE }" />
							<input type="hidden" name="currentPageNo" id="currentPageNo" value="${paramVO.currentPageNo }" />
							<input type="hidden" id="type" name="type">
							<input type="hidden" id="now" name="now">
							<input type="hidden" id="prev" name="prev">
							<input type="hidden" id="next" name="next">
								
								<div class="col-sm-3">	
									<select  name="searchCondition" id="searchCondition" class="form-control">
										<option value="1" <c:if test="${paramVO.searchCondition == '1' }">selected </c:if> >제목</option>
										<option value="2" <c:if test="${paramVO.searchCondition == '2' }">selected </c:if> >제목+내용</option>
										<option value="3" <c:if test="${paramVO.searchCondition == '3' }">selected </c:if> >회원사</option>
										<option value="4" <c:if test="${paramVO.searchCondition == '4' }">selected </c:if> >작성자</option>
									</select>								
								</div>						
								<div class="pull-right col-sm-9 input-group">
									<input type="text" name="searchKeyword" id="searchKeyword" value="${paramVO.searchKeyword }" class=" form-control " placeholder="검색어를 입력하세요." />
									<span class="input-group-btn">
										<button type="button" class="btn btn-primary" id="btn_search"><i class="fa fa-search"></i></button>		
									</span>
								</div>														
							</form>								
	                    </div>
	                </div>
	                <div class="table-responsive">
	                     <table class="table table-striped"> 	                  
							<thead>
								<tr>
									<th width="1%"></th>
									<th width="5%" class="text-center">No.</th>
									<th width="9%" class="text-center">게시일자</th>							
									<th width="18%" class="text-center">기사</th>
									<c:if test="${_AUTH eq 'SM' }">
									<th width="8%" class="text-center">회원사</th>
									</c:if>									
									<th width="7%" class="text-center">작성자</th>									
									<th width="6%" class="text-center">별 가격</th>
									<th width="7%" class="text-center">별 수익</th>
									<th width="7%" class="text-center">하트 수익</th>
									<th width="8%" class="text-center">클릭/하트 수 노출</th>
									<th width="7%" class="text-center">게시 여부</th>
									<th width="6%" class="text-center">조회 수</th>
									<th width="6%" class="text-center">댓글</th>
									<th width="8%" class="text-center">TOP 노출</th>
								</tr>
							</thead>
							<tbody>
								<c:set value="${paramVO.totalRecordCount}" var="count"/>
								<c:forEach var="cListTmp" items="${cList}" varStatus="status">
									<tr class="btn_detail" paramId="${cListTmp.CON_ID}">
										<td class="text-center" onClick="event.cancelBubble=true;">
										
											<c:choose>
												<c:when test="${status.index != 0}">
													<a href="#" onClick="moveUp('${cList[status.index].CON_PRIOT}', '${cList[status.index - 1].CON_PRIOT}');">▲</a>
												</c:when>
												<c:otherwise>
													<a href="#" onClick="movePrevUp('${cList[status.index].CON_PRIOT}');">▲</a>
												</c:otherwise>
											</c:choose><br>
											<c:choose>
												<c:when test="${fn:length(cList) != status.index + 1}">
													<a href="#" onClick="moveDown('${cList[status.index].CON_PRIOT}', '${cList[status.index + 1].CON_PRIOT}');">▼</a>
												</c:when>
												<c:otherwise>
													<a href="#" onClick="moveNextDown('${cList[status.index].CON_PRIOT}');">▼</a>
												</c:otherwise>
											</c:choose>
										
										</td>
										<th class="text-center" scope="row">${count-paramVO.currentPageNo*paramVO.recordCountPerPage+paramVO.recordCountPerPage}</th>
										<td class="text-center">
											<span class="date"><i class=" icon-clock"> </i> 
												${cListTmp.CON_CREATE_DATE}
					                    		<%-- <fmt:parseDate value="${cListTmp.CON_CREATE_DATE}" var="dateFmt" pattern="yyyy-MM-dd"/>
												<fmt:formatDate value="${dateFmt}" pattern="yyyy-MM-dd"/>&nbsp; --%> 
					                    	</span>
										</td>
										<td class="text-center" style="text-align:left;">${cListTmp.CON_TITLE }</td>
										<c:if test="${_AUTH eq 'SM' }">
										<td class="text-center">${cListTmp.PRS_NAME }</td>
										</c:if>				
										<td class="text-center">${cListTmp.REG_NAME }</td>
										<td class="text-center">${cListTmp.CON_PRICE }</td>
										<td class="text-center">☆ ${cListTmp.PRICE } ★ ${cListTmp.PRICE_CHG }</td>
										<td class="text-center">♡ ${cListTmp.HEART } ♥ ${cListTmp.HEART_CHG }</td>
										<td class="text-center">${cListTmp.CON_META_YN }</td>
										<td class="text-center">${cListTmp.CON_USE_YN }</td>
										<td class="text-center">${cListTmp.CNT }</td>
										<td class="text-center" onClick="event.cancelBubble=true;">
										<a href="#" class="btn_comment" paramId="${cListTmp.CON_ID}">
										${cListTmp.CMT_CNT }
										</a>
										</td>
										<td class="text-center">${cListTmp.CON_TOP_YN }</td>
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
