<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>

<script>
	var validation = $('#categoryUpdateForm').sv({
		checkOnSubmit : false,
		rules : {
			CATEGORY_0 : [ 'required','maxLength=20' ],
			CATEGORY_1 : [ 'required','maxLength=20' ],
			CATEGORY_8 : [ 'required','maxLength=20' ],
			CATEGORY_9 : [ 'required','maxLength=20' ],
		},
		errorCss : 'formError',
		stopOnFirstError : true
	});
	
$("#btn_cont_save").click(function(){
		
		if (!validation.validate()) {
			return;
		}
	
		var option = {
				url : '${ctx}/adm/categoryUpdateSave.do',
				dataType : 'json',				
				success : function(data){					
					
					if(data.status == 'success')
					{
						$('#contDialog').dialog('close');
			       		$('#contDialog').remove();
						location.href="${ctx}/adm/contentList?CON_TYPE="+$("#CON_TYPE").val();
					}	
					else
					{
						showMsgPop(data.serviceMessage);
					}	
					
				}				 
		};
	
		if (confirm("저장 하시겠습니까?")) {
			$('#categoryUpdateForm').ajaxSubmit(option);
			return false;
		}		
		
		return false;
		
	});
</script>

<div class="ibox animated fadeIn">
	<div class="panel panel-warning">
		<div class="panel-body">
			<form name="categoryUpdateForm" id="categoryUpdateForm" class="form-horizontal" method="POST">
				<input type="hidden" id="CON_TYPE" name="CON_TYPE" value="${CON_TYPE}">
				<c:forEach var="categoryItem" items="${categoryList}" varStatus="status">
					<div class="form-group">
						<label class="col-lg-1 control-label">${status.count}</label>
						<div class="col-lg-11 ">
							<input type="text" class="form-control"  
								   id="CATEGORY_${categoryItem.type}" 
								   name="CATEGORY_${categoryItem.type}" 
								   title="${categoryItem.category}" 
								   value="${categoryItem.category}"/>
						</div>
					</div>
				</c:forEach>
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