<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	//조회 클릭으로 검색
    $('#btn_search').click(function() {    	
		
		
		$('#memForm').attr('action','${ctx}/adm/memberList');		
		$('#memForm').submit();		
		return false;     
    	
    });
	
    $('#searchKeyword').keydown(function(event){
		if(event.keyCode == 13){
						
			$('#memForm').attr('action','${ctx}/adm/memberList');
			$('#memForm').submit();    
			return false;     
		}
	 	
	});
    
    
    $(".btn_detail").click(function(){
		var memId = $(this).attr("paramId");	
		$("#MEM_ID").val(memId);
		var option = {
				   url : '${ctx}/adm/memberDetail.do',
					//data : {'MEM_ID':memId},
		            target : '#openDialog',
		            dataType : 'html',   	            
		            success : function(data){
		            	var $o = $('#openDialog'); 
		                $o.html(data);        	              
		             
		            }                   
		        };
		<c:if test="${_AUTH  eq 'SM'}">		
			$('#memForm').ajaxSubmit(option);   
		</c:if>
		return false;
	});
	
        
});

</script>


<!-- 네비게이션 -->            
<div class="row wrapper border-bottom white-bg page-heading">
    <div class="col-lg-10">
        <h2>회원관리</h2>       
    </div>
</div>

<div class="wrapper wrapper-content  animated fadeInUp">
	
	<div class="row">
	    <div class="col-lg-12">
	        <div class="ibox float-e-margins">
	            
	            <div class="ibox-title">
	                <h5>회원 목록 </h5>
	                <div class="ibox-tools">	                	
	                	총 : ${mList.size()} 건   
	                	   
	                    <a class="collapse-link">
	                        <i class="fa fa-chevron-up"></i>
	                    </a>
	                </div>
	            </div>
	            <!-- content start -->
	            <div class="ibox-content">
	                <div class="row">	                	
		                <div class="col-sm-4 pull-right"> 
	                        <form name="memForm" id="memForm" method="POST">
							<input type="hidden" name="MEM_ID" id="MEM_ID" />
								<div class="input-group">
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
	                   <!-- <table class="table table-hover table-striped"> -->
							<thead>
								<tr>
									<th width="9%" class="text-center">가입 날짜</th>
									<c:if test="${_AUTH eq 'SM' }">
									<th width="14%" class="text-center">회원사</th>
									</c:if>			
									<th width="5%" class="text-center">가입 유형</th>
									<th width="9%" class="text-center">이름</th>
									<th width="9%" class="text-center">약관 동의</th>
									<th width="9%" class="text-center">무료 별</th>
									<th width="9%" class="text-center">유료 별</th>
									<th width="9%" class="text-center">별 수익</th>
									<th width="9%" class="text-center">하트 수익</th>
									<th width="9%" class="text-center">포스팅 수</th>
									<th width="9%" class="text-center">사용 여부</th>
								</tr>
							</thead>
							<tbody>
								<c:forEach var="mList" items="${mList}" varStatus="status">
									<tr class="btn_detail" paramId="${mList.MEM_ID}">
										<td class="text-center">
											<span class="date"><i class=" icon-clock"> </i> 
					                    		${mList.MEM_CREATE_DATE}
					                    		<%-- <fmt:parseDate value="${mList.MEM_CREATE_DATE}" var="dateFmt" pattern="yyyy-MM-ddH"/>
												<fmt:formatDate value="${dateFmt}" pattern="yyyy-MM-dd"/>&nbsp; --%> 
					                    	</span>
										</td>
										<c:if test="${_AUTH eq 'SM' }">
										<td class="text-center">${mList.PRS_NAME }</td>
										</c:if>	
										<td class="text-center">${mList.MEM_TYPE }</td>
										<td class="text-center">${mList.MEM_NAME }</td>
										<td class="text-center">${mList.MEM_AGREEMENT }</td>
										<td class="text-center">${mList.MEM_STAR }</td>
										<td class="text-center">${mList.MEM_STAR_CHG }</td>
										<td class="text-center">☆ ${mList.PRICE } ★ ${mList.PRICE_CHG }</td>
										<td class="text-center">♡ ${mList.HEART } ♥ ${mList.HEART_CHG }</td>
										<td class="text-center">${mList.CONTENT_CNT}</td>
										<td class="text-center">${mList.MEM_USE_YN}</td>
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
