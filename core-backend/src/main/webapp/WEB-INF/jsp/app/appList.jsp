<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	
    $(".btn_addApp").click(function(){
    	
    	$("#PRS_ID").val($(this).attr("paramId"));			
 
    	var option = {
    			url: '${ctx}/adm/appIssused.do',
				dataType : 'json',
				success : function(data){					
					
					location.href="${ctx}/adm/appList.do";
					
				}				 
		};
		
    	if (confirm("발급 하시겠습니까?")) {
			
			$('#compForm').ajaxSubmit(option);
			return false;
		}		
    	
		return false; 
			
    });	 
    
   
    
	
    var infoText = $("#<c:out value='<%=session.getAttribute("menuId")%>' />").text();  
    $("#veiwInfoTxt").text(infoText);
});

</script>


<!-- 네비게이션 -->            
<div class="row wrapper border-bottom white-bg page-heading">
    <div class="col-lg-10">
        <h2>APP 관리</h2>
        <ol class="breadcrumb">
        	<li>
                <a href="<%=Constants.DEFAULT_RETURN_URL%>">Home</a>
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
	                <h5>APP 발급 목록 </h5>
	                
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
	                    </form>
	                </div>
	                <div class="table-responsive">
	                    <!--  <table class="table table-striped">  -->
	                    <table class="table table-hover table-striped"> 
							<thead>
								<tr>
									<th width="10%" class="text-center">No.</th>
									<th width="30%" class="text-center">회원사명</th>
									<th width="20%" class="text-center">이메일</th>
									<th width="30%" class="text-center">발급 Key</th>
									<th width="20%" class="text-center">발급여부</th>
								</tr>
							</thead>
							<tbody>
								<c:set var="count" value="1" />
								<c:forEach var="list" items="${list}" varStatus="status">
								<tr class="btn_detail"  >
									<td class="text-center">${count}</td>
									<td class="text-center">${list.PRS_NAME }</td>
									<td class="text-center">${list.PRS_EMAIL }</td>
									<td class="text-center">${list.APP_AUTH_KEY }</td>									
									<td class="text-center" onClick="event.cancelBubble=true;">
										<c:if test="${list.APP_ISSUED_YN eq 'Y'}">
										<span class="btn btn-success">발급</span>
										</c:if>
										<c:if test="${list.APP_ISSUED_YN eq 'N'}">
										<button type="button" class="btn btn-outline  btn-primary btn_addApp" paramId="${list.PRS_ID}" " >발급하기</button>
										</c:if>									
									</td>									
								</tr>
								<c:set var="count" value="${count+1}"/>	
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


						