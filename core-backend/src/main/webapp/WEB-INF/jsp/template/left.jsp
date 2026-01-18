<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>
<%@ page import="com.sensible.common.util.SessionUtil"%>
<%@ page import="com.sensible.admin.domain.UserVO" %>
<%
	UserVO userVO = SessionUtil.getSessionUserInfo(request);		
	pageContext.setAttribute("userInfo", userVO);
	
	String _AUTH = "";
	String _TYPE = "";
	String _S_USER_ID = "";
	String _S_USER_NM = "";
	String _S_STORED_FILE_NM = "";

	if(userVO !=null)
	{
		_AUTH					= userVO.getPRS_AUTH();
		_TYPE					= userVO.getPRS_TYPE();
		_S_USER_ID				= userVO.getPRS_ID();
		_S_USER_NM				= userVO.getPRS_NAME();
		_S_STORED_FILE_NM		= userVO.getSTORED_FILE_NM();
	}	
	SessionUtil.setSessionAttribute(request, "_AUTH", _AUTH);
	SessionUtil.setSessionAttribute(request, "_TYPE", _TYPE);
	SessionUtil.setSessionAttribute(request, "_S_USER_ID", _S_USER_ID);
	SessionUtil.setSessionAttribute(request, "_S_USER_NM", _S_USER_NM);
	SessionUtil.setSessionAttribute(request, "_S_STORED_FILE_NM", _S_STORED_FILE_NM);
%>
<c:set var="_AUTH"       			value="<%=_AUTH %>"/>
<c:set var="_TYPE"       			value="<%=_TYPE %>"/>
<c:set var="_S_USER_ID"       		value="<%=_S_USER_ID %>"/>
<c:set var="_S_USER_NM"       		value="<%=_S_USER_NM %>"/>
<c:set var="_S_STORED_FILE_NM"      value="<%=_S_STORED_FILE_NM %>"/>

<nav class="navbar-default navbar-static-side" role="navigation">
	<form name="leftForm" id="leftForm" method="POST">
    <div id="menuSideBar" class="sidebar-collapse">
        <ul class="nav metismenu" id="side-menu">
  
         
             <li class="nav-header">
                <div class="dropdown profile-element"> 
                	<span>
                    <img alt="image" class="img-circle" src="${_S_STORED_FILE_NM}" width="48px;" height="48px;" />
                     </span>
                    <a data-toggle="dropdown" class="dropdown-toggle" href="#">
                    <span class="clear"> <span class="block m-t-xs"> <strong class="font-bold"><c:out value="${_S_USER_NM}" /></strong>
                    
                     </span> 
                     <span class="text-muted text-xs block">
                     	<c:if test="${_AUTH eq 'SM' }">시스템 관리자</c:if> 
                     	<c:if test="${_AUTH eq 'GE' }">콘텐츠 담당자</c:if>
                     	<c:if test="${_AUTH eq 'CM' }">관리자</c:if>
                     	<b class="caret"></b></span> </span> </a>
                    <ul class="dropdown-menu animated fadeInRight m-t-xs">
                        <li><a href="javascript:;" onclick="mypage();">내정보 변경</a></li>
                        <li class="divider"></li>
                        <li><a href="javascript:;" onclick="actionLogout();">Log out</a></li>
                    </ul>
                </div>
                <div class="logo-element">
                    WH+
                </div>
            </li>
             <c:if test="${_AUTH  eq 'SM'}">
			<li id="0">
				<a href="javascript:;" onClick="fs_goMenu('0','<c:out value="${ctx}/adm/statusMain" />');">
				<i class="fa fa-bar-chart-o"></i> <span class="nav-label">Dashboard</span></a>
			</li>
			<li id="1">
				<a href="javascript:;" onClick="fs_goMenu('1','<c:out value="${ctx}/adm/memberList" />');">
				<i class="fa fa-users"></i> <span class="nav-label">회원</span></a>
			</li>
			<li id="p1">
                <a href=""><i class="fa fa-th-large"></i> <span class="nav-label">업체</span> <span class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <li id="2"><a href="javascript:;" onClick="fs_goMenu('2','<c:out value="${ctx}/adm/compList?PRS_TYPE=1" />' ,'p1');">언론사</a></li>
                    <li id="3"><a href="javascript:;" onClick="fs_goMenu('3','<c:out value="${ctx}/adm/compList?PRS_TYPE=2" />' ,'p1');">학원/출판</a></li>
                    <li id="4"><a href="javascript:;" onClick="fs_goMenu('4','<c:out value="${ctx}/adm/compList?PRS_TYPE=3" />' ,'p1');">기업/단체</a></li>                   
                </ul>
            </li>			
			
			<li id="5"><a href="javascript:;" onClick="fs_goMenu('5','<c:out value="${ctx}/adm/contentList" />');"><i class="fa fa-diamond"></i> <span class="nav-label">콘텐츠</span></a></li>
			<li id="6"><a href="javascript:;" onClick="fs_goMenu('6','<c:out value="${ctx}/adm/eventList" />');"><i class="fa fa-pie-chart"></i> <span class="nav-label">이벤트</span></a></li>

			<li id="p3">
                <a href=""><i class="fa fa-cog"></i> <span class="nav-label">App 설정</span> <span class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <li id="7"><a href="javascript:;" onClick="fs_goMenu('7','<c:out value="${ctx}/adm/settingList?CON_TYPE=4" />' ,'p3');">공지사항</a></li>
                    <li id="8"><a href="javascript:;" onClick="fs_goMenu('8','<c:out value="${ctx}/adm/settingList?CON_TYPE=5" />' ,'p3');">사용설명서</a></li>
                    <li id="9"><a href="javascript:;" onClick="fs_goMenu('9','<c:out value="${ctx}/adm/settingList?CON_TYPE=3" />' ,'p3');">홍보마당</a></li>
                    <li id="10"><a href="javascript:;" onClick="fs_goMenu('10','<c:out value="${ctx}/adm/settingList?CON_TYPE=11" />' ,'p3');">논리구구단</a></li>
                    <li id="11"><a href="javascript:;" onClick="fs_goMenu('11','<c:out value="${ctx}/adm/settingList?CON_TYPE=6" />' ,'p3');">스토리</a></li>
                    <li id="12"><a href="javascript:;" onClick="fs_goMenu('12','<c:out value="${ctx}/adm/settingList?CON_TYPE=2" />' ,'p3');">FAQ</a></li>    
                    <li id="13"><a href="javascript:;" onClick="fs_goMenu('13','<c:out value="${ctx}/adm/settingList?CON_TYPE=7" />' ,'p3');">이용약관</a></li>               
                </ul>
            </li>	
            <li id="14"><a href="javascript:;" onClick="fs_goMenu('14','<c:out value="${ctx}/adm/appList" />');"><i class="fa fa-android"></i> <span class="nav-label">App 발급</span></a></li>			
			</c:if>     
			
			<c:if test="${_AUTH eq 'GE'}">
			<li id="5"><a href="javascript:;" onClick="fs_goMenu('5','<c:out value="${ctx}/adm/contentList" />');"><i class="fa fa-diamond"></i> <span class="nav-label">콘텐츠</span></a></li>	
			<li id="15"><a href="javascript:;" onClick="fs_goMenu('15','<c:out value="${ctx}/adm/applyList" />');"><i class="fa fa-android"></i> <span class="nav-label">App 신청</span></a></li>	
			</c:if>    
			<c:if test="${_AUTH  eq 'CM'}">
			<li id="0">
				<a href="javascript:;" onClick="fs_goMenu('0','<c:out value="${ctx}/adm/statusMain" />');">
				<i class="fa fa-bar-chart-o"></i> <span class="nav-label">Dashboard</span></a>
			</li>
			<%-- <li id="1">
				<a href="javascript:;" onClick="fs_goMenu('1','<c:out value="${ctx}/adm/memberList" />');">
				<i class="fa fa-users"></i> <span class="nav-label">회원</span></a>
			</li> --%>
			
			<li id="5"><a href="javascript:;" onClick="fs_goMenu('5','<c:out value="${ctx}/adm/contentList" />');"><i class="fa fa-diamond"></i> <span class="nav-label">콘텐츠</span></a></li>
			<%-- <li id="6"><a href="javascript:;" onClick="fs_goMenu('6','<c:out value="${ctx}/adm/eventList" />');"><i class="fa fa-pie-chart"></i> <span class="nav-label">이벤트</span></a></li> --%> 

			<%-- <li id="p3">
                <a href=""><i class="fa fa-cog"></i> <span class="nav-label">App 설정</span> <span class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <li id="7"><a href="javascript:;" onClick="fs_goMenu('7','<c:out value="${ctx}/adm/settingList?CON_TYPE=4" />' ,'p3');">공지사항</a></li>
                    <li id="8"><a href="javascript:;" onClick="fs_goMenu('8','<c:out value="${ctx}/adm/settingList?CON_TYPE=5" />' ,'p3');">사용설명서</a></li>
                    <li id="9"><a href="javascript:;" onClick="fs_goMenu('9','<c:out value="${ctx}/adm/settingList?CON_TYPE=3" />' ,'p3');">홍보마당</a></li>
                    <li id="10"><a href="javascript:;" onClick="fs_goMenu('10','<c:out value="${ctx}/adm/settingList?CON_TYPE=11" />' ,'p3');">논리구구단</a></li>
                    <li id="11"><a href="javascript:;" onClick="fs_goMenu('11','<c:out value="${ctx}/adm/settingList?CON_TYPE=6" />' ,'p3');">스토리</a></li>
                    <li id="12"><a href="javascript:;" onClick="fs_goMenu('12','<c:out value="${ctx}/adm/settingList?CON_TYPE=2" />' ,'p3');">FAQ</a></li>    
                    <li id="13"><a href="javascript:;" onClick="fs_goMenu('13','<c:out value="${ctx}/adm/settingList?CON_TYPE=7" />' ,'p3');">이용약관</a></li>               
                </ul>
            </li>	 --%>
            </c:if>  
	
        </ul>

    </div>
    </form>
</nav>

<script type="text/javascript">
	
	function fs_goMenu(id, url, pid) 
	{
		$('#baseMenuId').val(pid);
		$('#menuId').val(id);
		$('#link').val("redirect:"+url);
		$('#topForm').attr('action','${ctx}/pageLink/PageLink.do');
		$('#topForm').submit();
		
		return false;
	}

		
	$('#side-menu').find( "li.active" ).remove();
	$("#<c:out value='<%=session.getAttribute("baseMenuId")%>' />").addClass('active');
	$("#<c:out value='<%=session.getAttribute("menuId")%>' />").addClass('active');
	
	if('<%=session.getAttribute("menuId")%>' == 'null') $("#0").addClass('active');
	
</script> 

