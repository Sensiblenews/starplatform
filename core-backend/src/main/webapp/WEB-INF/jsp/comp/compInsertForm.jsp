<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script type='text/javascript'>
$(function() 
{	

	$('.datatoggle').bootstrapToggle({});
	
	
	
	var validation = $('#compDetailForm').sv({
		checkOnSubmit : false,
		rules : {
			PRS_ID : [ 'required' ],
			PRS_NAME : [ 'required' ],
			PRS_EMAIL : [  'email' ]
		},
		errorCss : 'formError',
		stopOnFirstError : true
	});
	
	
	$("#btn_comp_save").click(function(){
		
		if (!validation.validate()) {
			return;
		}
		
		if($("#CHK_PRS_USE_YN").is(":checked")){
			$("#PRS_USE_YN").val("Y");
		}else{
			$("#PRS_USE_YN").val("N");
		}
		
		var option = {
				url : '${ctx}/adm/compInsertSave.do',
				dataType : 'json',				
				success : function(data){					
					
					if(data.status == 'success')
					{
						 $('#compDialog').dialog('destroy');
						 $('#compDialog').remove();
						location.href="${ctx}/adm/compList.do?PRS_TYPE="+$("#PRS_TYPE").val();
					}	
					else
					{
						showMsgPop(data.serviceMessage);
					}	
					
				}				 
		};
	
		if (confirm("저장 하시겠습니까?")) {
			$('#compDetailForm').ajaxSubmit(option);
			return false;
		}		
		
		return false;
		
	});
});

function goMember()
{
	location.href="${ctx}/adm/memberList.do";
	$("#myModal").modal("hide");
}
</script>

<div class="ibox animated fadeIn">
			
          
			
						<div class="panel panel-warning">
							<div class="panel-body">
								
								<form name="compDetailForm" id="compDetailForm" class="form-horizontal" method="post" >
								<input type="hidden" name="PRS_USE_YN" id="PRS_USE_YN" />
								<input type="hidden" name="PRS_TYPE" id="PRS_TYPE"  value="${paramVO.PRS_TYPE}"/>
									
	                                <div class="form-group">
										<label class="col-lg-2 control-label">구분</label>
	                                    <div class="col-lg-10 ">
	                                    	<span class="form-control" readonly>
	                                    		<c:if test="${paramVO.PRS_TYPE eq '1' }">언론사</c:if>
												<c:if test="${paramVO.PRS_TYPE eq '2' }">학원/출판</c:if>
												<c:if test="${paramVO.PRS_TYPE eq '3' }">기업/단체</c:if>
	                                    	</span>                               	
	                                    </div>
	                                </div>                               
									<div class="form-group">
										<label class="col-lg-2 control-label">회원사 ID</label>
	                                    <div class="col-lg-10">
	                                    	<input type="text" class="form-control"  id="PRS_ID" name="PRS_ID" title="회원사 ID" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">회원사명</label>
	                                    <div class="col-lg-10">
	                                    	<input type="text" class="form-control"  id="PRS_NAME" name="PRS_NAME" title="회원사명" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">이메일</label>
	                                    <div class="col-lg-10">
	                                    	<input type="text" class="form-control"  id="PRS_EMAIL" name="PRS_EMAIL" title="이메일" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">홈페이지</label>
	                                    <div class="col-lg-10">
	                                    	<input type="text" class="form-control"  id="PRS_HOMEPAGE" name="PRS_HOMEPAGE" title="홈페이지" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">사용 여부</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="CHK_PRS_USE_YN" id="CHK_PRS_USE_YN" checked
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">
                          	
	                                    </div>
	                                </div>	                                
	                               
	  
								</form>
							</div>
							
							<div class="panel-footer clearfix">
								<div class="pull-right">
									<button type="button" class="btn btn-success btn-sm" id="btn_comp_save">
										<i class="fa fa-save"></i> 저장
									</button>
									<button type="button" class="btn btn-outline btn-default btn-sm" onClick="$('#compDialog').dialog('close');">
										<i class="fa fa-close"></i> 닫기
									</button>
								</div>
							</div>
						</div>
					
			
	
</div>