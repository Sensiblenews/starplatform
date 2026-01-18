/* Korean initialisation for the jQuery calendar extension. */
/* Written by DaeKwon Kang (ncrash.dk@gmail.com), Edited by Genie. */
jQuery(function($){
	$(".datepicker").datepicker({
		monthNamesShort: ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'],
		dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'],
		dateFormat: 'yy-mm-dd',
		prevText: '이전달',
		nextText: '다음달',
		showOn: "button",
		changeMonth: true,
		changeYear: true,
		weekHeader: 'Wk',
		firstDay: 0,
		isRTL: false,
		showMonthAfterYear: true,
		buttonImageOnly: true,
		showButtonPanel: true, // 캘린더 하단에 버튼 패널을 표시한다. 
		currentText: '오늘 날짜' , // 오늘 날짜로 이동하는 버튼 패널
		closeText: '닫기',  // 닫기 버튼 패널
		buttonImage: contextPath+'/img/btn_calendar.png'
		,beforeShow: function (cC, cD) {
            cD.input.css("zIndex", 9999);
        }
	});
	$('.ui-datepicker-trigger').attr('alt', 'Select Date').attr('title', 'Date').attr('align','absmiddle');
	$(".datepicker").mask("9999-99-99");
	
	// Date Picker 호출 시, datepicker-ym-display 클래스가 있다면, 지워준다.
	$(".datepicker").next('.ui-datepicker-trigger').click( function() {
		if( $("#ui-datepicker-div").hasClass( "datepicker-ym-display" ) ) {
			$("#ui-datepicker-div").removeClass( "datepicker-ym-display" );
		}
		
		if( $("#ui-datepicker-div").hasClass( "datepicker-y-display" ) ) {
			$("#ui-datepicker-div").removeClass( "datepicker-y-display" );
		}
	} );
	
	$(".datepickertime").datetimepicker({
		monthNamesShort: ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'],
		dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'],
		dateFormat: 'yy-mm-dd',
		prevText: '이전달',
		nextText: '다음달',
		showOn: "button",
		changeMonth: true,
		changeYear: true,
		weekHeader: 'Wk',
		firstDay: 0,
		isRTL: false,
		showMonthAfterYear: true,
		buttonImageOnly: true,
		showButtonPanel: true, // 캘린더 하단에 버튼 패널을 표시한다. 
		closeText: '닫기',  // 닫기 버튼 패널
		currentText : '현재시간',
        timeFormat: "hh:mm",
		buttonImage: contextPath+'/img/btn_calendar.png',
		beforeShow: function (cC, cD) {
            cD.input.css("zIndex", 9999);
        }
	});
	$('.ui-datepicker-trigger').attr('alt', 'Select Date').attr('title', 'Date').attr('align','absmiddle');
	$(".datepicker").mask("9999-99-99");
	
	$(".datepicker-ym").datepicker({
		monthNamesShort: ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'],
		dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'],
		dateFormat: 'yy-mm',
		prevText: '이전달',
		nextText: '다음달',
		currentText: '이번달',
		showOn: "button",
		changeMonth: true,
		changeYear: true,
		weekHeader: 'Wk',
		firstDay: 0,
		isRTL: false,
		showMonthAfterYear: true,
		buttonImageOnly: true,
		showButtonPanel: true, // 캘린더 하단에 버튼 패널을 표시한다. 
		closeText: '닫기',  // 닫기 버튼 패널
		buttonImage: contextPath+'/img/btn_calendar.png',
		onClose: function(dateText, inst) { 
            var month = $("#ui-datepicker-div .ui-datepicker-month :selected").val();
            var year = $("#ui-datepicker-div .ui-datepicker-year :selected").val();
            $(this).datepicker('setDate', new Date(year, month, 1));
        }
		,beforeShow: function (cC, cD) {
            cD.input.css("zIndex", 9999); 
        }
	});
	$('.ui-datepicker-trigger').attr('alt', 'Select Date').attr('title', 'Date').attr('align','absmiddle');
	$(".datepicker-ym").mask("9999-99");

	// Date Picker 호출 시, datepicker-ym-display 클래스를 추가한다.
	// datepicker-ym-display 클래스는 년월만 보여줄 시, 하단 달력의 Display를 none으로 바꿔 보이지 않게 한다.
	$(".datepicker-ym").next('.ui-datepicker-trigger').click( function() {
		$("#ui-datepicker-div").addClass( "datepicker-ym-display" );
		
		if( $("#ui-datepicker-div").hasClass( "datepicker-y-display" ) ) {
			$("#ui-datepicker-div").removeClass( "datepicker-y-display" );
		}
	} );
	
	$(".datepicker-y").datepicker({
		monthNamesShort: ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'],
		dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'],
		dateFormat: 'yy',
		prevText: '이전달',
		nextText: '다음달',
		currentText: '올해',
		showOn: "button",
		changeMonth: false,
		changeYear: true,
		weekHeader: 'Wk',
		firstDay: 0,
		isRTL: false,
		showMonthAfterYear: true,
		buttonImageOnly: true,
		showButtonPanel: true, // 캘린더 하단에 버튼 패널을 표시한다. 
		closeText: '닫기',  // 닫기 버튼 패널
		buttonImage: contextPath+'/img/btn_calendar.png',
		onClose: function(dateText, inst) {
            var year = $("#ui-datepicker-div .ui-datepicker-year :selected").val();
            $(this).datepicker('setDate', new Date(year, 1, 1));
        }
		,beforeShow: function (cC, cD) {
            cD.input.css("zIndex", 9999); 
        }
	});
	$('.ui-datepicker-trigger').attr('alt', 'Select Date').attr('title', 'Date').attr('align','absmiddle');
	$(".datepicker-y").mask("9999");

	// Date Picker 호출 시, datepicker-ym-display 클래스를 추가한다.
	// datepicker-ym-display 클래스는 년월만 보여줄 시, 하단 달력의 Display를 none으로 바꿔 보이지 않게 한다.
	$(".datepicker-y").next('.ui-datepicker-trigger').click( function() {
		$("#ui-datepicker-div").addClass( "datepicker-ym-display" );
		$("#ui-datepicker-div").addClass( "datepicker-y-display" );
	} );

	// datepicker 생성 시, max-width를 100px로 고정한다.
	$(".datepicker").css( "max-width", "100px" );
	$(".datepicker-ym").css( "max-width", "100px" );
	$(".datepicker-y").css( "max-width", "100px" );
});

function datepicker_onload(){
	$(".datepicker").datepicker({
		monthNamesShort: ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'],
		dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'],
		dateFormat: 'yy-mm-dd',
		prevText: '이전달',
		nextText: '다음달',
		showOn: "button",
		changeMonth: true,
		changeYear: true,
		weekHeader: 'Wk',
		firstDay: 0,
		isRTL: false,
		showMonthAfterYear: true,
		buttonImageOnly: true,
		showButtonPanel: true, // 캘린더 하단에 버튼 패널을 표시한다. 
		currentText: '오늘 날짜' , // 오늘 날짜로 이동하는 버튼 패널
		closeText: '닫기',  // 닫기 버튼 패널
		buttonImage: contextPath+'/img/btn_calendar.png'
		,beforeShow: function (cC, cD) {
            cD.input.css("zIndex", 9999);
        }
	});
	$('.ui-datepicker-trigger').attr('alt', 'Select Date').attr('title', 'Date').attr('align','absmiddle');
	$(".datepicker").mask("9999-99-99");
	
	// Date Picker 호출 시, datepicker-ym-display 클래스가 있다면, 지워준다.
	$(".datepicker").next('.ui-datepicker-trigger').click( function() {
		if( $("#ui-datepicker-div").hasClass( "datepicker-ym-display" ) ) {
			$("#ui-datepicker-div").removeClass( "datepicker-ym-display" );
		}
	} );

	// datepicker 생성 시, max-width를 100px로 고정한다.
	$(".datepicker").css( "max-width", "100px" );
}