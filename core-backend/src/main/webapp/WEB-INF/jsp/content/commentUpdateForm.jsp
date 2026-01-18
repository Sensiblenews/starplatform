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
	
	$('input[type="checkbox"]').change(function() {
		var useYn = 0;
		var comId = $(this).attr("name");
		if (!$(this).is(':checked')) {
			useYn = 1;
		}

		$.ajax({
			url : "/adm/commentUpdate",
			method : "POST",
			async: false,
			data : {
				CON_ID : $("#CON_ID").val(),
				COM_ID : comId,
				COM_USE_YN : useYn
			},
			dataType : "text",
			success : function(data) {
				alert("처리 되었습니다.");
			}
		});
	});
	
});

</script>

<div class="modal inmodal" id=myModal tabindex="-1" role="dialog" aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content animated fadeIn">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
				<h3 class="modal-title text-left">댓글 정보</h3>
			</div>

            <div class="modal-body">
								
				<!-- 사용내역 -->
				<div class="row">
					<div class="col-lg-12">
						<div class="panel panel-info">
							<div class="panel-body">						
							
								<div class="panel panel-info">
									<div class="panel-heading">
										<h4 class="panel-title" style="text-align: center;">
											<a data-toggle="collapse" href="#collapse1">댓글 목록</a>
										</h4>
									</div>
									<div id="collapse1" class="panel-collapse ">
									<form name="commentForm" id="commentForm" class="form-horizontal" method="post" >
									<input type="hidden" name="CON_ID" id="CON_ID" value="${CON_ID }">
										<table class="table table-hover table-striped">
											<c:choose>
												<c:when test="${fn:length(list) > 0}">
													<thead>
														<tr>															
															<th>작성자</th>
															<th>내용</th>
															<th>작성시간</th>
															<th>삭제유무</th>														
														</tr>
													</thead>
													<c:forEach var="list" items="${list}" varStatus="status">
														<tbody>
															<tr>
																<td>${list.MEM_NAME }</td>
																<td>${list.COM_BODY }</td>
																<td>${list.COM_CREATE_DATE }</td>
																<td>
																	<input type="checkbox" class="datatoggle" name="${list.COM_ID }" id="COM_USE_YN"
																		<c:if test="${list.COM_USE_YN == 0}">checked</c:if>
																	data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small" ></td>
															</tr>
														</tbody>
													</c:forEach>
												</c:when>
												<c:otherwise>
													<thead>
														<tr>
															<th colspan="4">댓글 내역이 없습니다.</th>
														</tr>
													</thead>
												</c:otherwise>
											</c:choose>
										</table>
										</form>
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