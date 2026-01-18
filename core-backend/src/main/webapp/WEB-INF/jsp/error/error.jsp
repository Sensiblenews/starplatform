<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


 <div class="middle-box text-center animated fadeInDown">
     <h1>Error</h1>
     <h3 class="font-bold">Internal Server Error</h3>

     <div class="error-desc">
           죄송합니다. 요청하신 서비스에 예기치 않는 오류가 발생되었습니다. 잠시 후 다시 이용해 주시기 바랍니다.<br/>
         <br/><a href="${ctx }<%=Constants.DEFAULT_RETURN_URL%>" class="btn btn-primary m-t">처음으로</a>
     </div>
 </div>