<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script type='text/javascript'>
$(function() 
{	
	
 	 $('#myModal').on('shown.bs.modal', function () {
	    $(this).find('.modal-dialog').css({width:'700px',
	                               height:'auto', 
	                              'max-height':'100%'});
	}); 
	$("#myModal").modal({backdrop:'static'}); 
	
	
	$('.datatoggle').bootstrapToggle({});
	
	$("#btn_member_save").click(function(){
		
		
		var option = {
				url : '${ctx}/adm/memberUpdate.do',
				dataType : 'json',				
				success : function(data){					
					
					if(data.status == 'success')
					{
						//showMsgPop(data.serviceMessage,null,goMember);
						$("#myModal").modal("hide");
						location.href="${ctx}/adm/memberList.do";
					}	
					else
					{
						showMsgPop(data.serviceMessage);
					}	
					
				}				 
		};

		$('#compDetailForm').ajaxSubmit(option);
		return false;
		
	});
});

function goMember()
{
	location.href="${ctx}/adm/memberList.do";
	$("#myModal").modal("hide");
}
</script>

<div class="modal inmodal" id=myModal tabindex="-1" role="dialog" aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content animated fadeIn">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
				<h3 class="modal-title text-left">회원 정보</h3>
			</div>

            <div class="modal-body">
				<div class="row">
					<div class="col-lg-12">
						<div class="panel panel-warning">
							<div class="panel-body">
								
								<form name="compDetailForm" id="compDetailForm" class="form-horizontal" method="post" >
								<input type="hidden" name="PRS_ID" id="PRS_ID" value="${mMap.mMap.MEM_ID }" />
									
									<div class="form-group">
										<label class="col-lg-2 control-label">가입 일자</label>
	                                    <div class="col-lg-10">	                                    	 
	                                    	<span class="form-control" readonly>
	                                    		<fmt:parseDate value="${mMap.mMap.MEM_CREATE_DATE}" var="dateFmt" pattern="yyyy-MM-dd"/>
												<fmt:formatDate value="${dateFmt}" pattern="yyyy-MM-dd"/>
	                                    	</span>                                 	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">가입 유형</label>
	                                    <div class="col-lg-10 ">
	                                    	<span class="form-control" readonly>${mMap.mMap.MEM_TYPE}</span>                               	
	                                    </div>
	                                </div>                               
									<div class="form-group">
										<label class="col-lg-2 control-label">이름</label>
	                                    <div class="col-lg-10">
	                                    	<span class="form-control" readonly>${mMap.mMap.MEM_NAME}</span>                                 	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">약관 동의</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="MEM_AGREEMENT" id="MEM_AGREEMNT" 
											<c:if test="${mMap.mMap.MEM_AGREEMENT == 1}">checked</c:if>
											data-toggle="toggle" data-on="동의" data-off="미동의" data-onstyle="primary" data-offstyle="danger" data-size="small">
                          	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">무료 별</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="number" id="MEM_STAR" name="MEM_STAR" class="form-control" value="${mMap.mMap.MEM_STAR}" />
                          	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">유료 별</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="number" id="MEM_STAR_CHG" name="MEM_STAR_CHG" class="form-control" value="${mMap.mMap.MEM_STAR_CHG}" />
                          	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">별 수익</label>
	                                    <div class="col-lg-10 ">
	                                    	<span class="form-control" readonly>☆ ${mMap.mMap.PRICE } ★ ${mMap.mMap.PRICE_CHG }</span>                               	
	                                    </div>
	                                </div>       
	                                
	                                <div class="form-group" >
										<label class="col-lg-2 control-label">사용 여부</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="MEM_USE_YN" id="MEM_USE_YN" <c:if test="${mMap.mMap.MEM_USE_YN == 1}">checked</c:if>
											 data-toggle="toggle" data-on="사용" data-off="미사용" data-onstyle="primary" data-offstyle="danger" data-size="small" />
	                                    </div>
	                                </div>
	                               
	  
								</form>
							</div>
							
							<div class="panel-footer clearfix">
								<div class="pull-right">
									<button type="button" class="btn btn-success btn-sm" id="btn_member_save">
										<i class="fa fa-save"></i> 저장
									</button>
								</div>
							</div>
						</div>
					</div>
				</div>
	
			<div class="modal-footer">
		      	<button type="button" class="btn btn-white btn-sm" class="close" data-dismiss="modal" aria-hidden="true"><i class="glyphicon glyphicon-remove"></i> <span>닫기</span></button>
		    </div>
		</div>
	</div>
</div>