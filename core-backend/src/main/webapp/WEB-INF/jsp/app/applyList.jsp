<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	var validation = $('#compDetailForm').sv({
		checkOnSubmit : false,
		rules : {
			PRS_ID : [ 'required' ],			
			PRS_EMAIL : [  'required', 'email' ]
		},
		errorCss : 'formError',
		stopOnFirstError : true
	});
	
    $("#btn_addApply").click(function(){
    	
    	if (!validation.validate()) {
			return;
		}
    	
    	var option = {
    			url: '${ctx}/adm/updateAppApply.do',
				dataType : 'json',
				success : function(data){					
					
					location.href="${ctx}/adm/applyList.do";
					
				}				 
		};
		
    	if (confirm("신청하시겠습니까?")) {
			
			$('#compDetailForm').ajaxSubmit(option);
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
        <h2>APP 신청</h2>
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
	                <h5>APP 신청 </h5>	                
	                
	            </div>
	            <!-- content start -->
	            <div class="ibox animated fadeIn">
          
			
						<div class="panel panel-warning">
							
							<form name="compDetailForm" id="compDetailForm" class="form-horizontal" method="post" >
								<input type="hidden" name="PRS_ID" id="PRS_ID" value="${detail.PRS_ID}"/>				
							<c:choose>
							<c:when test="${detail.APP_APPLY_YN eq 'N' }">
							
						
							<div class="panel-body">
								
												
			
	                                <div class="form-group">
										<label class="col-lg-2 control-label">이메일</label>
	                                    <div class="col-lg-6">
	                                    	<input type="text" class="form-control"  id="PRS_EMAIL" name="PRS_EMAIL" value="${detail.PRS_EMAIL}" title="이메일" />                         	
	                                    </div>
	                                </div>
	                    
								
							</div>
							
							<div class="panel-footer clearfix">
								<div class="pull-right">
									<button type="button" class="btn btn-outline btn-success btn-sm" id="btn_addApply">
										<i class="fa fa-save"></i> 신청하기
									</button>									
								</div>
							</div>
							</c:when>
							<c:otherwise>
							<div class="panel-body">
						
	                               발급 신청 되었습니다.
	                    
								
							</div>
							
							</c:otherwise>							
							</c:choose>
							</form>
						</div>
					
			
	
				</div>
	            <!-- content end -->
	           
	        
	        </div>	   
	    </div>	
	</div>
	
	
</div>


						