$.ajaxSetup({
	type:'post'
	,beforeSend : function(xhr)
	{
		xhr.setRequestHeader("AJAX",true);
		$.blockUI();
	},
	complete:function(xhr,status){
		$.unblockUI();
	},
	error: function(xhr,status,err)
	{
		if(xhr.status == 999) //세션종료
		{
			location.replace("/main/viewSessionChk.do");
		}
		else if(xhr.status == 888) //중복로그인
		{
			location.replace("/main/duplicateSessionChk.do");
		}
		else
		{
			alert("오류가 발생되었습니다. 관리자에게 문의하십시요.["+xhr.status+"]["+err+"]");
			$.unblockUI();
		}
	}
});

$(window).ready(function(){
	
	

	// Enter키 검색
	$(':input', $('#searchForm')).keydown(function(event) {
	    if(event.keyCode == 13) {
	        $('#btn_search').trigger('click');
	    }
	});
	
	
	
});


/**
 * jquery Dialog popup (경고,에러)
 * @param errMsg
 * @param obj
 * @param func
 * @param style
 */
function showMsgPop(errMsg, obj, func, style)
{
	var		objTmp, w, h;	
	if(style !=null) w = style.width;
	
	if( isNotEmpty(errMsg) && errMsg.length > 200 ) {
		h = "200";
	} else {
		h = "auto";
	}

	jQuery("body").append('<div id="valDiv" style="display:none;">'+errMsg+"<br>"+'</div>');
	jQuery('#valDiv').dialog({
		bgiframe: false
		, modal: true
		, title: '확인해 주세요.'
		, height: h
		, open: function(){
			objTmp	= obj;
			overlay_resize();
		}
		, close: function(event, ui){
			jQuery('#valDiv').remove();
			$(objTmp).focus();
			if(func !=null)
				func();
		}
		,width : w
		, buttons: {
			'닫기': function() {
				$(this).dialog('close');
				$(this).remove();
			}
		}
	});
}

function popSelfClose()
{
	self.close();
}




function actionLogout() {
	if(!confirm("로그아웃 하시겠습니까 ?")) {
		return;
	}

	var option = {
		url : contextPath+'/login/logout.do',
		dataType : 'json',
		success : sucFnc
	};

	$('#topForm').ajaxSubmit(option);
}

function sucFnc() {
	document.location.replace(contextPath+"/index.jsp");
}

String.prototype.trim = function(str) {
    str = this != window ? this : str;
    return str.replace(/^\s+/g,'').replace(/\s+$/g,'');
};

String.prototype.byteLength = function(str) {
    str = this != window ? this : str;
    var size = 0;
    for(var i=0; i < str.length; i++) {
        size++;
        if ( 44032 <= str.charCodeAt(i) && str.charCodeAt(i) <= 55203 ) {
            size++;
        }
        if ( 12593 <= str.charCodeAt(i) && str.charCodeAt(i) <= 12686 ) {
            size++;
        }
    }
    return size;
};

String.prototype.replaceAll = function(str1, str2){
    var temp_str = "";
    var temp_trim = this.replace(/(^\s*)|(\s*$)/g, "");
    if (temp_trim && str1 != str2) {
        temp_str = temp_trim;
        while (temp_str.indexOf(str1) > -1) temp_str = temp_str.replace(str1, str2);
    }
    return temp_str;
};

String.prototype.hasFinalConsonant = function(str) {
    if(str == null || str == "") return false;
    str = this != window ? this : str;
    var strTemp = str.substr(str.length-1);
    return ((strTemp.charCodeAt(0) - 16) % 28 != 0);
};



/**
 * window 팝업 
 * @param config
 * @returns
 */
function openWindowPop(config) {	
	var url = config.url ? config.url : '';
	LeftPosition = (screen.width) ? (screen.width-config.width)/2 : 0;
	TopPosition = config.top ? config.top : (screen.height) ? (screen.height-config.height)/2 : 0;
	settings = 'height='+config.height+',width='+config.width+',top='+TopPosition+',left='+LeftPosition+',scrollbars='+config.scroll+',resizable='+config.resize;

	return window.open(url,config.windowId,settings);
}

/**
 * 팝업화면생성
 * @param config
 *         url  : 팝업URL
 *         width : 팝업 width, default : 760
 *         height : 팝업 height, default : 760
 *         windowId : 팝업 window ID, default : popup
 *         ex>{ "url" : "/abc.do"}
 *         ex>{ "url" : "/abc.do", "height" : 200}
 *         ex>{ "url" : "/abc.do", "width" : 200, "height" : 200}
 *         ex>{ "url" : "/abc.do", "width" : 200, "height" : 200, "windowId" : "openPop"  }
 * @return
 */
function openWindow(config){
    var url = config.url;
    var width = config.width ? config.width : "790";
    var height = config.height ? config.height : "760";;
    var windowId = config.windowId ? config.windowId : "popup";
    return window.open(url, windowId, 'toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width=' + width + ',height=' + height);
}

/**
 * 팝업화면생성 (스크롤바 생기지 않는 팝업 화면 2011.11.18 BY KJLEE)
 * @param config
 *         url  : 팝업URL
 *         width : 팝업 width, default : 760
 *         height : 팝업 height, default : 760
 *         windowId : 팝업 window ID, default : popup
 *         ex>{ "url" : "/abc.do"}
 *         ex>{ "url" : "/abc.do", "height" : 200}
 *         ex>{ "url" : "/abc.do", "width" : 200, "height" : 200}
 *         ex>{ "url" : "/abc.do", "width" : 200, "height" : 200, "windowId" : "openPop"  }
 * @return
 */
function openWindowNoScroll(config){
    var url = config.url;
    var width = config.width ? config.width : "790";
    var height = config.height ? config.height : "760";;
    var windowId = config.windowId ? config.windowId : "popup";
    return window.open(url, windowId, 'toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=no,resizable=yes,width=' + width + ',height=' + height);
}

/* written by Sachin Khosla
 * http://www.digimantra.com/tutorials/sleep-or-wait-function-in-javascript/
 * */
function sleep(ms) {
    var dt = new Date();
    dt.setTime(dt.getTime() + ms);
    while (new Date().getTime() < dt.getTime());
}

// null 체크
var isNotEmpty = function( str ) {
	if( typeof str != "undefined" && str != null && str != "" && str.toString().trim().length > 0 )
		return true;
	else
		return false;
};


stringLpad = function( originStr, count, padStr ) {
	if( !isNotEmpty( originStr ) || !isNotEmpty( padStr ) || originStr.length >= count ) {
		return originStr;
	}
	
	var max = count - originStr.length;
	var idx = 0;
	
	while( idx < max ) {
		originStr = padStr + originStr;
		idx = idx + padStr.length;
	}
	
	return originStr;
};

stringRpad = function( originStr, count, padStr ) {
	if( !isNotEmpty( originStr ) || !isNotEmpty( padStr ) || originStr.length >= count ) {
		return originStr;
	}
	
	var max = count - originStr.length;
	var idx = 0;
	
	while( idx < max ) {
		originStr = originStr + padStr;
		idx = idx + padStr.length;
	}
	
	return originStr;
};


function mypage()
{
	$("#myPagePop").dialog('destroy');
    $("#myPagePop").remove();

	$.ajax({
		url: contextPath + "/adm/myPagePop.do"
		, target: "#myPagePop"
		, dataType: "html"
		, success: function( data ) {
			var $o = $("#myPagePop"); 
			$o.html( data );
		}  
	});

	$("body").append("<div id='myPagePop'></div>");
	
	$("#myPagePop").dialog({
	       bgiframe: true
	       , modal : true
	       , resizable: true
	       , width: "50%"
	       , height: 560
	       , title : "내정보 변경"
	       , open: function(event, ui) { // Dialog Open 시 z-Index 값을 2050으로 변경한다,
	    	   overlay_resize();
	       }
	       , close: function(event, ui){
	           $(this).dialog("destroy");
	           $(this).remove();	           
	       }
	 });
	
	$("#myPagePop").parent().addClass("search-modal");
}


setNewPasswordPop = function() {
	$("#passwordPop").dialog('destroy');
    $("#passwordPop").remove();

	$.ajax({
		url: contextPath + "/login/changePasswordPop.do"
		, target: "#passwordPop"
		, dataType: "html"
		, success: function( data ) {
			var $o = $("#passwordPop"); 
			$o.html( data );
		}  
	});

	$("body").append("<div id='passwordPop'></div>");
	
	$("#passwordPop").dialog({
	       bgiframe: true
	       , modal : true
	       , resizable: true
	       , width: "40%"
	       , height: 290
	       , title : "비밀번호 변경"
	       , open: function(event, ui) { // Dialog Open 시 z-Index 값을 2050으로 변경한다,
	    	   overlay_resize();
	       }
	       , close: function(event, ui){
	           $(this).dialog("destroy");
	           $(this).remove();
	       }
	 });
	
	$("#passwordPop").parent().addClass("search-modal");
}

overlay_resize = function() {
	var navbarHeigh = $('nav.navbar-default').height();
    var wrapperHeigh = $('#page-wrapper').height();

    if (navbarHeigh > wrapperHeigh) {
    	$(".ui-widget-overlay.ui-front").css( "min-height", navbarHeigh + 60 + "px" );
    }

    if (navbarHeigh < wrapperHeigh) {
    	$(".ui-widget-overlay.ui-front").css("min-height", wrapperHeigh + 60 + "px");
    }
    $(".ui-widget-overlay.ui-front").css("z-index", "2050px");
};


//해당 오브젝트 하위에 ID를 가지고 있는 엘리먼트 리스트를 가져와서 Object로 변환
var getFormObj = function( formObj, miniumCount ) {
	var rstObj = new Object();
	var count = 0;
	var searchCheck = false;
	
	var list = $( formObj ).find( "input[type='hidden'], input[type='text'], input[type='password'], select" );
	var listLength = list.length;
	
	for(var idx = 0; idx < listLength; idx++) {
		var obj = list[idx];
		
		rstObj[obj.id] = obj.value;
		
		if( isNotEmpty(obj.value) ) {
			count++;
			searchCheck = true;
		}
	}

	if( miniumCount >= count ) {
		rstObj[ "status" ] = false;
	} else {
		rstObj[ "status" ] = true;
	}
	
	rstObj[ "searchCheck" ] = searchCheck;
	
	return rstObj;
};




/**
 * 공통 confirm dialog
 * @param msg : 메세지 내용
 * @param okFunc : callback ok 함수명
 * @param noFunc : callback no 함수명
 * @param style : w:width값임.
 */
function showConfirmPop(msg, okFunc, noFunc, style)
{
	var	w;
	var h;
	if(style !=null){ w = style.width; h = style.height;}
	
	var d = '<div id="valDivCnfm2" class="pop_type4" style="display:none;">';
	    d += '<span class="NoticIco1"><img src="/images/popup/Ico_Notice_2.gif" width="35" height="35" /></span>';
	    d += '<span class="NoticTit">알림</span>';
	    d += '<div class="contents">';
	    d +=  msg;
	    d += '</div></div>';	
	    
	jQuery('#valDivCnfm').remove();
	//jQuery('body').append(d);
	jQuery("body").append('<div id="valDivCnfm" style="display:none;">'+msg+"<br>"+'</div>');
	jQuery('#valDivCnfm').dialog({
		bgiframe: false
		, modal: true		
		, close: function(event, ui){
			jQuery('#valDivCnfm').remove();			
		}
		, title: '알림'
		,width : w
		,height : h
		, buttons: {
			'확인': function() {				
				$(this).dialog('close');
				$(this).remove();
				okFunc();
			}
			,'취소':function(){
				
				$(this).dialog('close');
				$(this).remove();
				if(noFunc !=null)
					noFunc();
			}			
		}
	});
}


