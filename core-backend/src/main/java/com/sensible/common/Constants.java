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


	/*스타 직군(카테고리) — VS 배틀필드 랭킹 분류. GENERAL은 미분류(후순위 노출)*/
	public static final java.util.List<String> STAR_CATEGORIES =
			java.util.Collections.unmodifiableList(java.util.Arrays.asList("STAR", "CELEB", "BRAND", "ORG", "UNIV", "CITY", "MEDIA"));
	public static final String STAR_CATEGORY_DEFAULT = "GENERAL";
	
	
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
	/*
	 * 이미지 검수 대기·차단 보관소 (2-26차).
	 *
	 * _FILE_SAVE_PATH는 톰캣의 별도 정적 웹앱(/img)이라 그 안에 파일을 두는 순간 웹으로 공개된다.
	 * 승인 전 이미지는 아래 경로(웹앱 밖)에 두고, 관리자가 승인할 때 _FILE_SAVE_PATH로 옮긴다.
	 * 접근 제어 코드를 따로 두지 않고 "파일이 공개 디렉터리에 없으면 404"라는 성질을 그대로 쓴다.
	 */
	public static final String _PENDING_SAVE_PATH = "/var/lib/tomcat7/moderation/pending/";
	public static final String _HIDDEN_SAVE_PATH = "/var/lib/tomcat7/moderation/hidden/";

	public static final String _FILE_URL = "https://witch-hunting.com/img/";
	public static final String _VIDEO_FILE_URL = "https://witch-hunting.com/video/";
	public static final String _VIDEO_THUMNAIL_FILE_URL = "https://witch-hunting.com/video/thumnail/";
	
}
