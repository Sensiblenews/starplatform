/**
 * Simple Validation
 *
 * v0.1, 2009-06-01
 * 
 * 클라이언트용 form 밸리데이션 플러그인
 */


(function($) {
	
	
	// validator Object를 Singleton 으로 생성
	$.fn.sv = function(options){
		if(!this.length){
			alert('There is no form to be processed.');
			return false;
		}

		
		var validator = $.data(this[0],'_validator_'); // 첫번째 폼에서 simplevalidator 라는 키로 validator 객체 갖고온다.		
		if(validator == undefined){ // 없다면 새로 만든다.
		 	validator = new $.fn.validator(options, this[0]);
		 	$.data(this[0], '_validator_', validator); 
		}				

		if(validator.settings.checkOnSubmit){
			$(this[0]).bind('submit',function(){
				return validator.validate();										
			});
		}
		return validator;
	};

	// validator Object
	$.fn.validator = function(options, form) {
		// 셋팅
		this.settings = $.extend( {}, $.fn.validator.defaults, options );
		// 선택된 폼
		this.currentForm = form;									
		// 에러메지시들
		this.errorMsgs = [];
	};

	// validator Object 확장
	$.extend($.fn.validator, {

		// 기본 옵션값
		defaults: {
			checkOnSubmit:		false // 서브밋시 자동으로 체크여부
			, rules: 			[] // 적용할 validation rule
			, stopOnFirstError:	true // 에러메시지 보여주기, all or first 둘중에 하나 선택가능
			, errorTitle: 		'확인해 주세요.' // 에러보여줄때 타이틀
			, errorCss: 		null // 에러난 form object 표시할 스타일명
			, errorViewHandler:	null // 에러난 내용을 표시할 콜백함수
			, onSuccessHandler: null // 밸리데이션 성공시 콜백
			, onFailHandler:	null // 밸리데이션 실패시 콜백		
			, debug: 			true
		}
		// 기본 메시지 
		,messages: {
			 notApplicable:	'적용할수 없습니다.'
			, notAmethod: 	'검증 메소드가 없습니다.'
			, required: 	'필수 입력 값입니다.'
			, reqNm: 	    '신청자를 입력하세요.'				
			, minLength:	'입력값의 최소 길이는 {0}자 입니다.'
			, maxLength:	'입력값의 최대 길이는 {0}자 입니다.'	
			, minByte:		'입력값은 적어도 {0}byte 이상 입력해야 합니다.'
			, maxByte:		'입력값은 {0}byte 를 넘을수 없습니다.'
			, number:		'숫자가 아닙니다.'
			, digit:		'숫자만 입력가능합니다.'
			, email:		'이메일 형식이 아닙니다.'
			, url:			'url 형식이 아닙니다.'
			, engNum:		'영어와 숫자만 입력 가능 합니다.'
			, method:		'메소드 검증이 실패하였습니다.'
			, equalTo:		'{0} 필드와 값이 틀립니다.'
			, minSel:		'적어도 {0}개 이상 선택해야 합니다.'
			, maxSel:		'최대 {0}개 까지 선택 가능합니다.'
			, bizNo:		'사업자번호가 유효하지 않습니다.'
		}

		/*
		 * form 에 속한 엘리먼트 name 으로 해당 validation에 필요한 elementObj를 만들어 낸다.
		 */
		,makeElementObj: function(elementName, form){			
			var obj = $(":input[name="+elementName+"]",form);
			var tagName = obj.get(0).tagName.toLowerCase();
			var tagType = obj.get(0).type;
			var tagLength = obj.length;
			var title;
			if(obj.get(0).title){
				title = obj.get(0).title;
			}else{
				title = elementName;
			}			
			var elementObj = {};
			elementObj.obj = obj;
			elementObj.tagName = tagName;
			elementObj.tagType = tagType;
			elementObj.tagLength = tagLength;
			elementObj.name = 	elementName;
			elementObj.title = 	title;
			return elementObj;					
		}
		,addMethod: function(name, method, message) {
			$.fn.validator.methods[name] = method;
			$.fn.validator.messages[name] = message != undefined ? message : $.validator.messages[name];
			if (method.length < 3) {
				//$.validator.addClassRules(name, $.validator.normalizeRule(name));
			}
		}
		
		/*
		 * 메지시를 포맷한다.
		 */
		,format: function(source, params) {
			if ( arguments.length == 1 ) 
				return function() {
					var args = $.makeArray(arguments);
					args.unshift(source);
					return $.validator.format.apply( this, args );
				};
			if ( arguments.length > 2 && params.constructor != Array  ) {
				params = $.makeArray(arguments).slice(1);
			}
			if ( params.constructor != Array ) {
				params = [ params ];
			}
			$.each(params, function(i, n) {
				source = source.replace(new RegExp("\\{" + i + "\\}", "g"), n);
			});
			return source;
		}
		/*
		 * 바이트수를 갖고온다.
		 */
		,getbyte: function(string){ 
			var l = 0; 
			for (var i=0; i<string.length; i++){
				l += (string.charCodeAt(i) > 128) ? 2 : 1;
			}  
			return l; 			
		}
		/*
		 * 기본 ErrorHandler
		 */
		,defaultErrorViewHandler: function(errTitle, errMsgs){
			if($.ui.dialog){
				
				jQuery('body').append('<div id="valDiv" style="display:none; zindex:99999;">'+errMsgs.join("<br>")+'</div>');
				jQuery('#valDiv').dialog({
					bgiframe: true
					, modal: true
					, title: errTitle
					, open: function(event, ui) {
						overlay_resize();
					}
					, close: function(event, ui){
						jQuery('#valDiv').remove();
					}
					, buttons: {
						'닫기': function() {
							$(this).dialog('close');
						}
					}			
					//, width: '600px'
				});		
				
			}else{
				alert(errTitle+"\n"+errMsgs.join("\n"));	
			}
		}
		,prototype:{
			validate: function(){	
				
				// 기존 에러메시지 클리어
				this.errorMsgs = [];
				
				var validationState = true;							
				
				for(element in this.settings.rules){
					
					// element가 존재하고 disabled가 아닐때
					if(this.currentForm[element] && !this.currentForm[element].disabled){
						
						if(this.settings.debug){
							if(window.console){
								console.error("element name is : " + element);								
							}
						}										
						
						var rules = this.settings.rules[element];
						
						
						var errObj = {}; // 에러 오브젝트를 담는 함수
						
						for ( var i in rules) {
							var param = rules[i].split(","); // 룰과 에러메시지를 자른다.
							var rule = param.shift().split("=");								
							var ruleName = rule.shift();						 												
							var ruleParam = rule;
							var errorMsg = param;
													
							var method = $.fn.validator.methods[ruleName];
							if(method){								
								// 검증위한 오브젝트를 생성한다.
								var elementObj = $.fn.validator.makeElementObj(element, this.currentForm);
								var result = method.call(this,elementObj,ruleParam);
								// 에러일 겨우 여기서 메지시 처리를 한다.(커스텀 메시지 포함)
								if(!result){
									// 커스텀 메시지가 있으면 해당 메시지를 넣는다
									var msg;
									if(!errorMsg || errorMsg == null || errorMsg.length == 0){
										msg = $.fn.validator.messages[ruleName];	
									}else{
										msg = errorMsg.toString();
									}
									
									if(ruleName == 'equalTo'){
										var tempParamObj = $.fn.validator.makeElementObj(ruleParam, this.currentForm);
										ruleParam = '['+tempParamObj.title+']';
									}
									msg = $.fn.validator.format(msg,ruleParam);
									this.errorMsgs.push('['+elementObj.title+'] : '+msg);									
									elementObj.obj.addClass(this.settings.errorCss);																		
									validationState = false;																		
									if(this.settings.stopOnFirstError){
										// 에러난 곳에서 fucus
										elementObj.obj.focus();
										break;
									}else{
										errObj[elementObj.obj] = 'error';
									}
								}else{
									// 에러 클래스 삭제
									if(!errObj[elementObj.obj]){
										elementObj.obj.removeClass(this.settings.errorCss);	
									}
									
								}
							}else{																												
								if(this.settings.debug){
									if(window.console){
										console.error("\t\tcan not apply method : " + ruleName);
									}
								}
							}
						}

					}else{
						if(this.settings.debug){
							if(window.console){
								console.error("invalid element : " + element);
							}
						}
					} 
					if(!validationState){
						if(this.settings.stopOnFirstError){
							break;
						}
					}
				}
				
				// 결과값을 체크
				if(!validationState){
					// 에러뷰 확인해서 사용자 에러뷰가 있으면 실행 없으면 기본
					if(this.settings.errorViewHandler && typeof(this.settings.errorViewHandler) == 'function'){
						this.settings.errorViewHandler(this.errorTitle, this.errorMsgs);
					}else{
						$.fn.validator.defaultErrorViewHandler(this.settings.errorTitle, this.errorMsgs);
					}
					
					if(this.settings.onFailHandler && typeof(this.settings.onFailHandler) == 'function'){
						this.settings.onFailHandler(this);
					}
				}else{
					if(this.settings.onSuccessHandler && typeof(this.settings.onSuccessHandler) == 'function'){
						this.settings.onSuccessHandler(this);
					}					
				}						
				return validationState;
			}
		}	
		
		// http://docs.jquery.com/Plugins/Validation/Validator/addMethod
		,addMethod: function(name, method, message) {
			$.fn.validator.methods[name] = method;
			$.fn.validator.messages[name] = message != undefined ? message : $.fn.validator.messages[name];
			
		}
		
		/*
		 * rule 검증 메소드
		 */
		,methods: {
			 
			/*
			 * 반드시 필요
			 */
			required: function(element){	
													
				var length; // 선택 혹은 입력 여부를 판단하기 위한 변수
				
				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						length = element.obj.filter(":checked").length;
						break;
					case 'radio':
						length = element.obj.filter(":checked").length;
						break;						
					default:						 
						length = element.obj.val().length;						
						break;
					}
					break;
				case 'select':					
					length = element.obj.find('option:selected:first').val().length;
					break;				
				case 'textarea':					
					length = element.obj.val().length;
					break;
				}															
				if(length > 0){
					return true;
				}else{
					return false;
				}				
			}
		
		    ,reqNm: function(element){	
				
				var length; // 선택 혹은 입력 여부를 판단하기 위한 변수
				
				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						length = element.obj.filter(":checked").length;
						break;
					case 'radio':
						length = element.obj.filter(":checked").length;
						break;						
					default:						 
						length = element.obj.val().length;						
						break;
					}
					break;
				case 'select':					
					length = element.obj.find('option:selected:first').val().length;
					break;				
				case 'textarea':					
					length = element.obj.val().length;
					break;
				}															
				if(length > 0){
					return true;
				}else{
					return false;
				}				
			}
		
		
			/*
			 * 문자열 최소 길이
			 */			
			, minLength: function(element, size){
				
				var result = true;
				
				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:						 
						if(element.obj.val().length >= size){
							result = true;
						}else{
							result = false;
						}
						break;
					}
					break;
				case 'select':					
					//TODO 처리기준 확정
					break;				
				case 'textarea':					
					if(element.obj.val().length >= size){
						result = true;
					}else{
						result = false;
					}
					break;
				}
				return result;				
			}
			 
			/*
			 * 문자열 최대길이
			 */			
			, maxLength: function(element, size){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:						 
						if(element.obj.val().length <= size){
							result = true
						}else{
							result = false;
						}
						break;
					}
					break;
				case 'select':					
					//TODO 처리기준 확정
					break;				
				case 'textarea':					
					if(element.obj.val().length <= size){
						result = true
					}else{
						result = false;
					}
					break;
				}
				return result;
			}
			
			/*
			 * 문자열 최소 바이트
			 */			
			, minByte: function(element, size){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:						 
						if($.fn.validator.getbyte(element.obj.val()) >= size){
							result = true;
						}else{
							result = false;
						}
						break;
					}
					break;
				case 'select':					
					//TODO 처리기준 확정
					break;				
				case 'textarea':					
					if($.fn.validator.getbyte(element.obj.val()) >= size){
						result = true;
					}else{
						result = false;
					}
					break;
				}
				return result;
			}
			
			/*
			 * 문자열 최대 바이트
			 */			
			, maxByte: function(element, size){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:						 
						if($.fn.validator.getbyte(element.obj.val()) <= size){
							result = true
						}else{
							result = false;
						}
						break;
					}
					break;
				case 'select':					
					//TODO 처리기준 확정
					break;				
				case 'textarea':					
					if($.fn.validator.getbyte(element.obj.val()) <= size){
						result = true
					}else{
						result = false;
					}
					break;
				}
				return result;
			}
			 
			/*
			 * 숫자만. 
			 */			
			, number: function(element){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:
						if(element.obj.val().length > 0){
							result = /^-?(?:\d+|\d{1,3}(?:,\d{3})+)(?:\.\d+)?$/.test(element.obj.val());
						}
						
						break;
					}
					break;
				case 'select':	
					//TODO 처리기준 확정
					break;	
				case 'textarea':
					if(element.obj.val().length > 0){
						result = /^-?(?:\d+|\d{1,3}(?:,\d{3})+)(?:\.\d+)?$/.test(element.obj.val());
					}					
					break;
				}
				return result;
			}

			/*
			 * 이메일형태
			 */		
			, email: function(element){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:
						if(element.obj.val().length > 0){
							result = /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i.test(element.obj.val());
						}
						
						break;
					}
					break;
				case 'select':	
					//TODO 처리기준 확정
					break;	
				case 'textarea':
					if(element.obj.val().length > 0){
						result = /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i.test(element.obj.val());
					}					
					break;
				}
				return result;
			}

			/*
			 * url형태
			 */		
			, url: function(element){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:
						if(element.obj.val().length > 0){
							result = /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(element.obj.val());
						}
						break;
					}
					break;
				case 'select':	
					//TODO 처리기준 확정
					break;	
				case 'textarea':
					if(element.obj.val().length > 0){
						result = /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(element.obj.val());
					}
					break;
				}
				return result;
			}
			 
			/*
			 * 영어+숫자
			 */
			, engNum: function(element){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:
						if(element.obj.val().length > 0){
							result = /(^[a-zA-Z0-9\-_]+$)/i.test(element.obj.val());
						}
						
						break;
					}
					break;
				case 'select':	
					//TODO 처리기준 확정
					break;	
				case 'textarea':
					if(element.obj.val().length > 0){
						result = /(^[a-zA-Z0-9\-_]+$)/i.test(element.obj.val());
					}					
					break;
				}
				return result;
			}
			/*
			 * 아라비아 숫자만
			 */
			, digit: function(element){
				var result = true;

				switch(element.tagName){
				case 'input':
					switch(element.tagType){
					case 'checkbox':
						//TODO 처리기준 확정
						break;
					case 'radio':
						//TODO 처리기준 확정
						break;						
					default:
						if(element.obj.val().length > 0){
							result = /(^[0-9]+$)/i.test(element.obj.val());
						}
						
						break;
					}
					break;
				case 'select':	
					//TODO 처리기준 확정
					break;	
				case 'textarea':
					if(element.obj.val().length > 0){
						result = /(^[0-9]+$)/i.test(element.obj.val());
					}					
					break;
				}
				return result;
			}			 
			, method: function(element, callback){
				var result = true;
				var method = window[callback];
				if(typeof(method) == 'function'){
					result = method.call(element);	
				}				
				return result;
			}
			
			/*
			 * 같아야됨. 
			 */
			, equalTo: function(element, srcEle){
				var srcEleObj = $.fn.validator.makeElementObj(srcEle, this.currentForm);
				return element.obj.val() == srcEleObj.obj.val();
			}
			 
			/*
			 * 최소 숫자
			 */
			/*
			minval: function(element, val){
				if(this.settings.debug){
					if(window.console){
						console.debug("\t\tmethod 'minval' called, argumets is :  " + arguments);	
					}					
				}
			},						
			*/
			/*
			 * 최대숫자
			 */
			/*
			maxval: function(element, val){
				if(this.settings.debug){
					if(window.console){
						console.debug("\t\tmethod 'maxval' called, argumets is :  " + arguments);	
					}					
				}
			},
			*/
			/*
			 * 최소 선택
			 */
			
			, minSel: function(element, val){
				var result = true;				
				switch(element.tagName){
				case 'input':					
					switch(element.tagType){
					case 'checkbox':
						var len = 0;
						$(element.obj).each(function(i){
							alert( $(this).attr('checked'));
							if( $(this).attr('checked')){
								len++;
							}							
						});
						if(len < val){
							result = false;
						}
						break;						
					default:
						break;
					}
					break;
				case 'select':						
					var len = $('option:selected',element.obj).length;
					if(len < val){
						result = false;
					}
					break;	
				case 'textarea':					
					break;
				}
				return result;
			}
			
			/*
			 * 최대선택
			 */			
			, maxSel: function(element, val){
				var result = true;				
				switch(element.tagName){
				case 'input':					
					switch(element.tagType){
					case 'checkbox':
						var len = 0;
						$(element.obj).each(function(i){
							alert( $(this).attr('checked'));
							if( $(this).attr('checked')){
								len++;
							}							
						});
						if(len > val){
							result = false;
						}
						break;						
					default:
						break;
					}
					break;
				case 'select':						
					var len = $('option:selected',element.obj).length;
					if(len > val){
						result = false;
					}
					break;	
				case 'textarea':					
					break;
				}
				return result;
			}							
			//주민번호1(필드하나가 모두)
			//주민번호2(필드 두개 합쳐서) 
			, bizNo: function(element){
				
				var result = true;
				// bizID는 숫자만 10자리로 해서 문자열로 넘긴다. 
			    var checkID = new Array(1, 3, 7, 1, 3, 7, 1, 3, 5, 1); 
			    var i, chkSum=0, c2, remander; 
			    var bizID = element.obj.val();
			    bizID = bizID.replace(/-/gi,''); 

			     for (i=0; i<=7; i++) chkSum += checkID[i] * bizID.charAt(i); 
			     c2 = "0" + (checkID[8] * bizID.charAt(8)); 
			     c2 = c2.substring(c2.length - 2, c2.length); 
			     chkSum += Math.floor(c2.charAt(0)) + Math.floor(c2.charAt(1)); 
			     remander = (10 - (chkSum % 10)) % 10 ; 

			    if (Math.floor(bizID.charAt(9)) == remander) return true ; // OK! 
			      return false; 
				
				return result;
			}
		}
		
		
	});
})(jQuery);