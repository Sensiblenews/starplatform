<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script type='text/javascript'>
$(function() 
{	
	
 	
	$('.datatoggle').bootstrapToggle({});
	
	
	
	$("#RESET_PWD").click(function(){
		
		if($("#CHK_PRS_USE_YN").is(":checked")){
			$("#PRS_USE_YN").val("Y");
		}else{
			$("#PRS_USE_YN").val("N");
		}
		
		if(confirm("비밀번호를 초기화 하시겠습니까?"))
		{
			var option = {
	    			url: '${ctx}/adm/pressResetPwd.do',
					dataType : 'json',
					success : function(data){
						
						showMsgPop(data.serviceMessage,null,goComp);						
					}				 
			};
			
			 $('#compDetailForm').ajaxSubmit(option);
			 return false; 
		}	
		return false;
		
	});
	
	
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
				url : '${ctx}/adm/compUpdateSave.do',
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
	
		if (confirm("수정 하시겠습니까?")) {
			$('#compDetailForm').ajaxSubmit(option);
			return false;
		}		
		
		return false;
		
	});
});

function goComp()
{
	//$("#myModal").modal("hide");
	location.href="${ctx}/adm/compList?PRS_TYPE="+$("#PRS_TYPE").val();
}
</script>

<div class="ibox animated fadeIn">
        
						<div class="panel panel-warning">
							<div class="panel-body">
								
								<form name="compDetailForm" id="compDetailForm" class="form-horizontal" method="post" >
								<input type="hidden" name="PRS_USE_YN" id="PRS_USE_YN" />
								<input type="hidden" name="PRS_TYPE" id="PRS_TYPE"  value="${paramVO.PRS_TYPE}"/>								
								<input type="hidden" name="PRS_ID" id="PRS_ID" value="${detail.PRS_ID }" />
						
									
	                                <div class="form-group">
										<label class="col-lg-3 control-label">구분</label>
	                                    <div class="col-lg-9 ">
	                                    	<span class="form-control" readonly>
	                                    		${detail.PRS_TYPE_NM}
	                                    	</span>                               	
	                                    </div>
	                                </div>                               
									<div class="form-group">
										<label class="col-lg-3 control-label">회원사 ID</label>
	                                    <div class="col-lg-9">
	                                    	<span class="form-control" readonly>
	                                    		${detail.PRS_ID}
	                                    	</span>                            	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-3 control-label">회원사명</label>
	                                    <div class="col-lg-9">
	                                    	<input type="text" class="form-control"  id="PRS_NAME" name="PRS_NAME" value="${detail.PRS_NAME}" title="회원사명" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-3 control-label">이메일</label>
	                                    <div class="col-lg-9">
	                                    	<input type="text" class="form-control"  id="PRS_EMAIL" name="PRS_EMAIL" value="${detail.PRS_EMAIL}" title="이메일" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-3 control-label">홈페이지</label>
	                                    <div class="col-lg-9">
	                                    	<input type="text" class="form-control"  id="PRS_HOMEPAGE" name="PRS_HOMEPAGE" value="${detail.PRS_HOMEPAGE}" title="홈페이지" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-3 control-label">사용 여부</label>
	                                    <div class="col-lg-9">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="CHK_PRS_USE_YN" id="CHK_PRS_USE_YN" <c:if test="${detail.PRS_USE_YN eq 'Y'}">checked</c:if>
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">
                          	
	                                    </div>
	                                </div>	 
	                                <div class="form-group">
										<label class="col-lg-3 control-label">비밀번호 초기화</label>
	                                    <div class="col-lg-9">	                                    	
	                                    	<input type="button" name="RESET_PWD" id="RESET_PWD" class="btn btn-primary btn-sm" value="비밀번호 초기화"/>
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
				</div>
		
</div>