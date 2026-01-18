<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script type='text/javascript'>
$(function() 
{	
	
	$('.datatoggle').bootstrapToggle({});
	

	var validation = $('#evtDetailForm').sv({
		checkOnSubmit : false,
		rules : {
			EVT_BTN_TEXT : [ 'required','maxLength=20' ],
			EVT_TEXT : [ 'required' ],
			EVT_STAR : [  'required' ]
		},
		errorCss : 'formError',
		stopOnFirstError : true
	});
	
	
	$("#btn_cont_save").click(function(){
		
		if (!validation.validate()) {
			return;
		}
		
		<c:if test="${_AUTH  eq 'SM'}">		
			if ($('#PRS_ID').val() == "") {
				showMsgPop("APP을 선택하세요.");
				return false;
			}
		</c:if>
		
		
		
	
		var option = {
				url : '${ctx}/adm/eventInsertSave.do',
				dataType : 'json',				
				success : function(data){					
					
					if(data.status == 'success')
					{
						$('#contDialog').dialog('close');
			       		$('#contDialog').remove();
						location.href="${ctx}/adm/eventList";
					}	
					else
					{
						showMsgPop(data.serviceMessage);
					}	
					
				}				 
		};
	
		if (confirm("저장 하시겠습니까?")) {
			
			$('#evtDetailForm').ajaxSubmit(option);
			return false;
		}		
		
		return false;
		
	});
	
	
});


</script>

<div class="ibox animated fadeIn">
						<div class="panel panel-warning">
							<div class="panel-body">
								
								<form name="evtDetailForm" id="evtDetailForm" class="form-horizontal" method="POST" >
																						
	                                <div class="form-group">
										<label class="col-lg-2 control-label">이벤트 제목</label>
	                                    <div class="col-lg-10 ">
	                                    	<input type="text" class="form-control"  id="EVT_BTN_TEXT" name="EVT_BTN_TEXT" title="이벤트 제목" />             	
	                                    </div>
	                                </div>                               
									<div class="form-group">
										<label class="col-lg-2 control-label">이벤트 내용</label>
	                                    <div class="col-lg-10">
	                                    	<input type="text" class="form-control"  id="EVT_TEXT" name="EVT_TEXT" title="이벤트 내용" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">URL</label>
	                                    <div class="col-lg-10">
	                                    	<input type="text" class="form-control"  id="EVT_URL" name="EVT_URL" title="URL" />                         	
	                                    </div>
	                                </div>
	                                <div class="form-group">
										<label class="col-lg-2 control-label">별</label>
	                                    <div class="col-lg-10">
	                                    	 <input type="text" class="form-control"  id="EVT_STAR" name="EVT_STAR" title="별" />                   	
	                                    </div>
	                                </div>
	                                <c:if test="${_AUTH  eq 'SM'}">  
	                                <div class="form-group">
										<label class="col-lg-2 control-label">유료 Y 무료 N</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="EVT_CHG_YN" id="EVT_CHG_YN" checked
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">
                          	
	                                    </div>
	                                </div>	  
	                                </c:if> 
	                                
	                                <div class="form-group">
										<label class="col-lg-2 control-label">게시 여부</label>
	                                    <div class="col-lg-10">	                                    	
	                                    	<input type="checkbox" class="datatoggle" name="EVT_USE_YN" id="EVT_USE_YN" checked
											data-toggle="toggle" data-on="Y" data-off="N" data-onstyle="primary" data-offstyle="danger" data-size="small">                          	
	                                    </div>
	                                </div>	
	                                <c:if test="${_AUTH  eq 'SM'}">   
	                                <div class="form-group">
										<label class="col-lg-2 control-label">APP 선택</label>
	                                    <div class="col-lg-6">
	                                    	<select class="form-control m-b" name="PRS_ID" id="PRS_ID">
	                                    	<option value="">선택하세요.</option>
											<c:forEach var="prsList" items="${prsList}" varStatus="status">
												<option value="${prsList.PRS_ID }">${prsList.PRS_NAME }</option>
											</c:forEach>
	                                    	</select>                     	
	                                    </div>
	                                </div>
	                               </c:if>                              
	                               
	  
								</form>
							</div>
							
							<div class="panel-footer clearfix">
								<div class="pull-right">
									<button type="button" class="btn btn-success btn-sm" id="btn_cont_save">
										<i class="fa fa-save"></i> 저장
									</button>
									<button type="button" class="btn btn-outline btn-default btn-sm" onClick="$('#contDialog').dialog('close');">
										<i class="fa fa-close"></i> 닫기
									</button>
								</div>
							</div>
						</div>
				
</div>