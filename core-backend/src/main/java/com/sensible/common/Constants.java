package com.sensible.common;

public class Constants {	
	
	public static final String domain = "witch-hunting.com";
	
	/* default Url - ex>로그인 후 이동할 페이지 */
	public static final String DEFAULT_RETURN_URL	= "/main/main.do";
	
	
	/* session Key */
	public static final String _SESSION_KEY		= "witchSensible_sessionInfo";
	public static final String _APP_SESSION_KEY	 = "APP_sessionInfo";
	
	/* session Allor */
	public static final String SESSION_ALLOW_KEY_WORD	= "login,main,error,chatfile";

	
	/*권한*/
	public static final String AUTH_SM	= "SM"; //시스템 관리자
	public static final String AUTH_GE	= "GE"; //일반 관리자
	public static final String AUTH_CM	= "CM"; //업체(APP) 관리자
	
	
	/*페이징 관련*/
	public static final int PAGE_SIZE = 10;			//블럭 사이즈
	public static final int RECORD_PER_PAGE = 10;	//한페이지당 보여줄 리스트 수
	
	
	/*공통 코드*/
	public static final String _SYSTEM_USER = "SYSTEM";
	
	
	public static final String _FILE_SAVE_PATH = "/var/lib/tomcat7/webapps/img/";
//	public static final String _FILE_SAVE_PATH = "/home/hagangmin/tmp/img/";
	public static final String _VIDEO_SAVE_PATH = "/var/lib/tomcat7/webapps/video/";
//	public static final String _VIDEO_SAVE_PATH = "/home/hagangmin/tmp/video/";
	public static final String _VIDEO_THUMNAIL_SAVE_PATH = "/var/lib/tomcat7/webapps/video/thumnail/";
//	public static final String _VIDEO_THUMNAIL_SAVE_PATH = "/home/hagangmin/tmp/video/thumnail/";
	public static final String _FILE_URL = "https://witch-hunting.com/img/";
	public static final String _VIDEO_FILE_URL = "https://witch-hunting.com/video/";
	public static final String _VIDEO_THUMNAIL_FILE_URL = "https://witch-hunting.com/video/thumnail/";
	
}
