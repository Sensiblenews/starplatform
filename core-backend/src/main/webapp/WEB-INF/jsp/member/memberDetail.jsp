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

		$('#memDeatailForm').ajaxSubmit(option);
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
								
								<form name="memDeatailForm" id="memDeatailForm" class="form-horizontal" method="post" >
								<input type="hidden" name="MEM_ID" id="MEM_ID" value="${mMap.mMap.MEM_ID }" />
									
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
	                                    	<input type="number" id="MEM_STAR_CHG" name="MEM_STAR_CHG" class="form-control" value="${mMap.mMap.MEM_STAR_CHG}" readonly />
                          	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">별 수익</label>
	                                    <div class="col-lg-10 ">
	                                    	<span class="form-control" readonly>☆ ${mMap.mMap.PRICE } ★ ${mMap.mMap.PRICE_CHG }</span>                               	
	                                    </div>
	                                </div>       
	                                <div class="form-group">
										<label class="col-lg-2 control-label">하트 수익</label>
	                                    <div class="col-lg-10 ">
	                                    	<span class="form-control" readonly>☆ ${mMap.mMap.HEART } ★ ${mMap.mMap.HEART_CHG }</span>                               	
	                                    </div>
	                                </div>       
	                                
	                                <div class="form-group" >
										<label class="col-lg-2 control-label">사용 여부</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="MEM_USE_YN" id="MEM_USE_YN" <c:if test="${mMap.mMap.MEM_USE_YN == 1}">checked</c:if>
											 data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small" />
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
				
				<!-- 사용내역 -->
				<div class="row">
					<div class="col-lg-12">
						<div class="panel panel-info">
							<div class="panel-body">
						
							<div class="panel-group" >
								<div class="panel panel-info">
									<div class="panel-heading">
										<h4 class="panel-title" style="text-align: center;">
											<a data-toggle="collapse" href="#collapse1">별 사용 내역</a>
										</h4>
									</div>
									<div id="collapse1" class="panel-collapse collapse">
										<table class="table table-hover table-striped">
											<c:choose>
												<c:when test="${fn:length(mMap.hList) > 0}">
													<thead>
														<tr>
															<th>사용한 별</th>
															<th>사용한 시간</th>
														</tr>
													</thead>
													<c:forEach var="hList" items="${mMap.hList}"
														varStatus="status">
														<tbody>
															<tr>
																<td>☆ ${hList.CON_PRICE } ★ ${hList.CON_PRICE_CHG }</td>
																<td>${hList.PRI_CREATE_DATE }</td>
															</tr>
														</tbody>
													</c:forEach>
												</c:when>
												<c:otherwise>
													<thead>
														<tr>
															<th colspan="2">사용 내역이 없습니다.</th>
														</tr>
													</thead>
												</c:otherwise>
											</c:choose>
										</table>
									</div>
							</div>
							<div class="panel panel-info">		
									<div class="panel-heading">
										<h4 class="panel-title" style="text-align: center;"> 
											<a data-toggle="collapse" href="#collapse2">하트 사용 내역</a>
										</h4>
									</div>
									<div id="collapse2" class="panel-collapse collapse">
										<table class="table table-hover table-striped">
											<c:choose>
												<c:when test="${fn:length(mMap.heartList) > 0}">
													<thead>
														<tr>
															<th>사용한 별</th>
															<th>사용한 시간</th>
														</tr>
													</thead>
													<c:forEach var="heartList" items="${mMap.heartList}"
														varStatus="status">
														<tbody>
															<tr>
																<td>♡ ${heartList.CON_HEART } ♥ ${heartList.CON_HEART_CHG }</td>
																<td>${heartList.HRT_CREATE_DATE }</td>
															</tr>
														</tbody>
													</c:forEach>
												</c:when>
												<c:otherwise>
													<thead>
														<tr>
															<th colspan="2">사용 내역이 없습니다.</th>
														</tr>
													</thead>
												</c:otherwise>
											</c:choose>
										</table>
									</div>
								</div>
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