package com.sensible.api.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//import java.util.Base64;
import org.apache.commons.codec.binary.Base64;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.firebase.messaging.Notification;
import com.owlike.genson.convert.DefaultConverters.PrimitiveConverterFactory.booleanConverter;
import com.owlike.genson.convert.DefaultConverters.PrimitiveConverterFactory.intConverter;
import com.owlike.genson.convert.DefaultConverters.PrimitiveConverterFactory.longConverter;
import com.sensible.common.Constants;
import com.sensible.common.dao.DefaultDAO;
//import com.sensible.common.util.AppleLoginUtil;
import com.sensible.common.filter.InterceptorAPI;
import com.sensible.common.util.AWTUtil;

//import net.minidev.json.JSONObject;

@Service("memberService")
public class MemberService {

	private static final Logger logger = LoggerFactory.getLogger(MemberService.class);
//	private final String FILEPATH = "/home/hagangmin/tmp/chatfiles";
//	private final String THUMBNAILPATH = "/home/hagangmin/tmp/chatfiles/thumbnail";
	private final String FILEPATH = "/home/ubuntu/uploads";
	private final String THUMBNAILPATH = "/home/ubuntu/uploads/thumbnail";
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
	
	private final long deletion1Min = 60, deletion5Min = 300;

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	@Resource(name = "bcryptPasswordEncoder")
	private BCryptPasswordEncoder bcryptPasswordEncoder;
	
	@Resource(name = "firebaseService")
	private FirebaseService firebaseService;

	// token check
	public Map<String, Object> memberAccessTokenCheck(String token)
			throws Exception {
		return dao.selectOne("member.memberAccessTokenCheck", token);
	}

	// 가입
	// 17.05.09. 카톡 가입 추가
	// 22.09.22. 애플로그인 추가
	public Map<String, Object> user(Map<String, Object> map) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		String memId = "";
		String memType = String.valueOf(map.get("MEM_TYPE"));

		System.out.println("memType =====================" + memType);

		if (memType.equals("KT")) {
			Map<String, Object> ktMap = (Map<String, Object>) map.get("ktUser");
			String ktKey = (String) ktMap.get("KT_KEY");
			String token = memberTokenEncrypt("KT", ktKey);
			String ktToken = (String) ktMap.get("KT_TOKEN");
			String ktTokenExpd = (String) ktMap.get("KT_TOKEN_EXPD");
			String ktPicture = (String) ktMap.get("KT_PICTURE");

			map.put("MEM_TOKEN", token);
			map.put("MEM_PICTURE", ktPicture);

			map.put("KT_KEY", ktKey);
			map.put("KT_TOKEN", ktToken);
			map.put("KT_TOKEN_EXPD", ktTokenExpd);
			map.put("KT_PICTURE", ktPicture);

			// 기 존재 token refresh. 아닌 경우 가입
			Map<String, Object> kkcMap = dao
					.selectOne("member.ktKeyCheck", map);
			if (kkcMap != null && kkcMap.get("MEM_ID") != null) {
				memId = (String) kkcMap.get("MEM_ID");
				map.put("MEM_ID", memId);
				dao.update("member.updateMember", map);
				resultMap = dao.selectOne("member.selectMember", memId);
			} else {
				memId = dao.selectOne("member.nextMemId");
				map.put("MEM_ID", memId);
				dao.insert("member.insertMember", map);
				dao.insert("member.insertKakaotalk", map);
				resultMap = dao.selectOne("member.selectMember", memId);
			}
		} else if (memType.equals("FB")) {
			Map<String, Object> fbMap = (Map<String, Object>) map.get("fbUser");
			String fbKey = (String) fbMap.get("FB_KEY");
			String token = memberTokenEncrypt("FB", fbKey);
			String fbToken = (String) fbMap.get("FB_TOKEN");
			String fbTokenExpd = (String) fbMap.get("FB_TOKEN_EXPD");
			String fbPicture = (String) fbMap.get("FB_PICTURE");

			map.put("MEM_TOKEN", token);
			map.put("FB_KEY", fbKey);
			map.put("FB_TOKEN", fbToken);
			map.put("FB_TOKEN_EXPD", fbTokenExpd);
			map.put("FB_PICTURE", fbPicture);

			// 기 존재 token refresh. 아닌 경우 가입
			Map<String, Object> fkcMap = dao
					.selectOne("member.fbKeyCheck", map);
			if (fkcMap != null && fkcMap.get("MEM_ID") != null) {
				memId = (String) fkcMap.get("MEM_ID");
				map.put("MEM_ID", memId);
				dao.insert("member.updateMember", map);
				resultMap = dao.selectOne("member.selectMember", memId);
			} else {
				memId = dao.selectOne("member.nextMemId");
				map.put("MEM_ID", memId);
				dao.insert("member.insertMember", map);
				dao.insert("member.insertFacebook", map);
				resultMap = dao.selectOne("member.selectMember", memId);
			}
		} else if (memType.equals("AP")) {

		} else if (memType.equals("GO")) {
			Map<String, Object> goMap = (Map<String, Object>) map.get("goUser");
			String goKey = (String) goMap.get("GO_KEY");
			String token = memberTokenEncrypt("GO", goKey);
			String goToken = (String) goMap.get("GO_TOKEN");
			String goTokenExpd = (String) goMap.get("GO_TOKEN_EXPD");
			String goPicture = (String) goMap.get("GO_PICTURE");

			map.put("MEM_TOKEN", token);
			map.put("MEM_PICTURE", goPicture);

			map.put("GO_KEY", goKey);
			map.put("GO_TOKEN", goToken);
			map.put("GO_TOKEN_EXPD", goTokenExpd);
			map.put("GO_PICTURE", goPicture);

			// 기 존재 token refresh. 아닌 경우 가입
			Map<String, Object> kkcMap = dao
					.selectOne("member.goKeyCheck", map);
			if (kkcMap != null && kkcMap.get("MEM_ID") != null) {
				memId = (String) kkcMap.get("MEM_ID");
				map.put("MEM_ID", memId);
				dao.update("member.updateMember", map);
				resultMap = dao.selectOne("member.selectMember", memId);
			} else {
				memId = dao.selectOne("member.nextMemId");
				map.put("MEM_ID", memId);
				dao.insert("member.insertMember", map);
				dao.insert("member.insertGoogle", map);
				resultMap = dao.selectOne("member.selectMember", memId);
			}

		}
		return resultMap;
	}

	// 17.05.09. 로그인 체크
	public Map<String, Object> userChk(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		Map<String, Object> memMap = dao.selectOne(
				"member.memberAccessTokenCheckAndUser", map);

		if (memMap != null && memMap.get("MEM_ID") != null) {
			String memId = (String) memMap.get("MEM_ID");
			map.put("MEM_ID", memId);
			resultMap.put("item", memMap);
			resultMap.put("RESULT", "OK");
		} else {
			resultMap.put("RESULT", "FAIL");
		}
		return resultMap;
	}

	// 타 유저 프로필 조회
	public Map<String, Object> profile(Map<String, Object> map)
			throws Exception {
		Map<String, Object> memMap = dao.selectOne("member.profile", map);
		if (memMap == null || memMap.get("MEM_ID") == null) {
			memMap = dao.selectOne("member.profilePress", map);
		}

		return memMap;
	}

	// 로긴 인증
	public Map<String, Object> getApiUser(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		// 동일한지 체크
		Map<String, Object> fkamcMap = dao.selectOne(
				"member.fbKeyAndMemIdCheck", map);
		if (fkamcMap != null && map.get("MEM_ID") != null) {
			String memId = (String) map.get("MEM_ID");
			Map<String, Object> userMap = dao.selectOne("member.selectMember",
					memId);
			resultMap.put("result", true);
			resultMap.put("user", userMap);
		} else {
			resultMap.put("result", false);
		}

		return resultMap;
	}

	// 약관 동의
	public Map<String, Object> putApiUser(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String memId = (String) map.get("MEM_ID");
		dao.update("member.updateMemberAgree", map);
		resultMap = dao.selectOne("member.selectMember", memId);

		return resultMap;
	}

	// 회원 탈퇴
	public Map<String, Object> deleteApiUser(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String memId = (String) map.get("MEM_ID");
		dao.update("member.deleteApiUser", map);
		// resultMap = dao.selectOne("member.selectMember", memId);
		resultMap.put("isSuccess", "1");
		return resultMap;
	}

	// 닉네임 변경
	public Map<String, Object> modifyNickname(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String memId = (String) map.get("MEM_ID");
		String nickname = (String) map.get("MEM_NAME");
		dao.update("member.modifyNickname", map); 
		resultMap.put("isSuccess", 1);
		return resultMap;
	}
	
	// 닉네임 변경
	public Map<String, Object> modifyIntroduction(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		dao.update("member.modifyIntroduction", map); 
		resultMap.put("isSuccess", 1);
		return resultMap;
	}

	// 팔로우
	public Map<String, Object> followUser(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Object> followMap = dao
				.selectOne("member.followCheck", map);
		if (followMap == null) {
			dao.insert("member.followUser", map);

			followMap = dao.selectOne("member.followCheck", map);
			if (followMap != null) {
				resultMap.put("result", true);
			} else {
				resultMap.put("result", false);
			}
		} else {
			resultMap.put("result", false);
		}

		return resultMap;
	}

	// 언팔로우
	public Map<String, Object> unFollowUser(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Object> followMap = dao
				.selectOne("member.followCheck", map);
		if (followMap != null) {
			dao.delete("member.unFollowUser", map);

			followMap = dao.selectOne("member.followCheck", map);
			if (followMap == null) {
				resultMap.put("result", true);
			} else {
				resultMap.put("result", false);
			}
		} else {
			resultMap.put("result", false);
		}

		return resultMap;
	}
	
	// 팔로잉/팔로워 카운트
	public Map<String, Object> followCount(Map<String, Object> map) throws Exception
	{
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		resultMap = dao.selectOne("member.followCount", map);
		
		return resultMap;
	}

	// 팔로우 목록
	public List<Map<String, Object>> follows(Map<String, Object> map, boolean isSearch)
			throws Exception {
		map.put("isSearch", isSearch);
		
		List<Map<String, Object>> resultMap = dao.selectList("member.follows", map);
		return resultMap;
	}

	// 팔로워 목록
	public List<Map<String, Object>> followers(Map<String, Object> map, boolean isSearch)
			throws Exception {
		map.put("isSearch", isSearch);

		List<Map<String, Object>> resultMap = dao.selectList("member.followers", map);
		return resultMap;
	}

	// 프로필 이미지 변경
	public Map<String, Object> modifyProfileImage(Map<String, Object> map)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		try {
			String memId = (String) map.get("MEM_ID");
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());

			// 서버에 이미지 업로드
			String fileNm = memId + "_" + timestamp.getTime() + ".jpg";

			logger.info("profileImgName = "+fileNm);
			
			makeImgFile((String) map.get("PROFILE_THUMNAIL"), fileNm);

			map.put("PROFILE_THUMNAIL", Constants._FILE_URL + fileNm);

			dao.update("member.updateProfileImage", map);
			resultMap = dao.selectOne("member.selectMember", memId);

			resultMap.put("RESULT", "OK");
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("RESULT", "FAIL");
		}

		return resultMap;
	}

	private void makeImgFile(String imgbase64, String savename)
			throws Exception {

		try {
			// create a buffered image
			BufferedImage image = null;

			String[] base64Arr = imgbase64.split(","); // image/png;base64, 이
			// 부분 버리기 위한 작업
			byte[] imageByte = Base64.decodeBase64(base64Arr[1]); // base64 to byte
																// array로 변경
			logger.info("imageByteLength = "+imageByte.length);

			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(bis);
			bis.close();

			// write the image to a file
			File outputfile = new File(Constants._FILE_SAVE_PATH + savename);

			logger.info(Constants._FILE_SAVE_PATH + savename);

			ImageIO.write(image, "jpg", outputfile); // 파일생성

		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * APP check
	 * 
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public String selectAppInfo(String appAuthKey) throws Exception {
		return dao.selectOne("member.selectAppInfo", appAuthKey);
	}

	// bcrypt
	public String memberTokenEncrypt(String type, String Key) {
		long now = System.currentTimeMillis();
		String str = "sEnSiBle";
		String nowStr = Long.toString(now);
		String sign = bcryptPasswordEncoder.encode(type + Key + str + nowStr);
		return sign;
	}
	
	public Map<String, Object> checkReceivedMessage(Map<String, Object> map) {
		return dao.selectOne("member.checkReceivedMessage", map);
	}

	// 쪽지 리스트 조회
	public List<Map<String, Object>> messageList(Map<String, Object> map) {
		return dao.selectList("member.messageList", map);
	}

	// 쪽지 등록
	public Map<String, Object> addMessage(Map<String, Object> map) {
		int result =  dao.insert("member.addMessage", map);
		logger.info("message added");
		final Integer messageId = Integer.valueOf(String.valueOf(map.get("messageId")));
		
		Runnable deletionTask = () -> {
			Map<String, Object> deletionMap = new HashMap<String, Object>();
			deletionMap.put("messageId", messageId);
			logger.info("deleting message " + messageId);
			dao.update("member.deleteMessage", deletionMap);
		};
		
		scheduler.schedule(deletionTask, deletion5Min, TimeUnit.SECONDS);

		if(result > 0) {
			map.put("result", true);
			sendNotificationToReceiver(map, String.valueOf(map.get("CONTENT")));
		} else {
			map.put("result", false);
		}

		return map;
	}

	// 쪽지 읽음 처리
	public Map<String, Object> readMessage(Map<String, Object> map) {
		Map<String, Object> deletionMap = new HashMap<>();
		deletionMap.put("SEND_MEM_ID", String.valueOf(map.get("SEND_MEM_ID")));
		deletionMap.put("RECEIVE_MEM_ID", String.valueOf(map.get("RECEIVE_MEM_ID")));
		List<Map<String, Object>> unreadIdMaps = dao.selectList("member.getUnreadMessage", deletionMap);
		
		Runnable deletionTask = () -> {
			if (unreadIdMaps == null || unreadIdMaps.size() == 0) {
				return;
			}
			
			unreadIdMaps.forEach(unreadIdMap -> {
				deletionMap.put("messageId", String.valueOf(unreadIdMap.get("MESSAGE_ID")));
				dao.update("member.deleteMessage", deletionMap);
			});
		};
		
		scheduler.schedule(deletionTask, deletion1Min, TimeUnit.SECONDS);
		
		int result = dao.update("member.readMessage", map);

		if(result > 0)
			map.put("result", true);
		else
			map.put("result", false);

		return map;
	}
	
	public Map<String, Object> uploadFile(Map<String, Object> map) {		

        boolean flag = false;
		try {
	        // FormData가 아닌 JSON 객체에서 데이터를 추출합니다.
	        String filename = (String) map.get("fileName");
	        String base64data = (String) map.get("base64");
	        
	        // data:image/jpeg;base64, 부분 제거
	        String base64Pure = base64data.substring(base64data.indexOf(",") + 1);
	        
	        // Base64 문자열을 byte 배열로 디코딩합니다.
	        byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Pure);
	        
	        File dir = new File(FILEPATH);
	        if (!dir.exists()) dir.mkdirs();
	        
	        File targetFile = new File(dir, filename);
	        Files.write(targetFile.toPath(), fileBytes);
	        
	        String fileExtension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
	        if (fileExtension.equals(".mov") || fileExtension.equals(".avi") || fileExtension.equals(".wmv")) {
	        	flag = true;
	            logger.info("Video conversion required for: " + filename);
	            String mp4Filename = filename.substring(0, filename.lastIndexOf('.')) + ".mp4";
	            File convertedFile = new File(dir, mp4Filename);

	            convertVideoToMp4(targetFile.getAbsolutePath(), convertedFile.getAbsolutePath());

	            logger.info("Successfully converted to: " + mp4Filename);

	            targetFile.delete();

	            filename = mp4Filename;
	            targetFile = convertedFile;
	            
	            String contentType = ((String) map.get("CONTENT")).split("::")[0];
	            map.put("CONTENT", contentType + "::" + filename);
	        }
	        
	        if (filename.endsWith(".mp4") || filename.endsWith(".avi") || filename.endsWith(".mov") || filename.endsWith(".3gp")) {
	            String thumbnailFileName = filename.substring(0, filename.lastIndexOf('.')) + ".jpg";
	            File thumbDir = new File(THUMBNAILPATH);
	            if (!thumbDir.exists()) thumbDir.mkdirs();

	            String thumbnailFullPath = THUMBNAILPATH + File.separator + thumbnailFileName;
	            generateVideoThumbnailWithJCodec(targetFile.getAbsolutePath(), thumbnailFullPath);

	            // 썸네일 경로 map에 저장
	            map.put("thumbnailPath", thumbnailFullPath);
	        }
	        
	        map.put("result", true);
	    } catch (Exception e) {
	        e.printStackTrace();
	        map.put("result", false);
	        return map; // 실패 시 바로 반환
	    }
		
		int result =  dao.insert("member.addMessage", map);
		logger.info("message added");

		if(result > 0) {
			sendNotificationToReceiver(map, flag ? "동영상을 보냈습니다." : "사진을 보냈습니다.");
			map.put("result", true);
		} else
			map.put("result", false);

		return map;
	}
	
	/**
     * FFmpeg를 사용하여 비디오를 웹 호환 MP4 형식으로 변환
     * @param inputPath 원본 비디오 파일 경로
     * @param outputPath 변환된 MP4 파일이 저장될 경로
     * @throws IOException
     * @throws InterruptedException
     */
	private void convertVideoToMp4(String inputPath, String outputPath) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-i", inputPath,      // 입력 파일
                "-vcodec", "libx264",  // 비디오 코덱: H.264 (가장 높은 호환성)
                "-acodec", "aac",       // 오디오 코덱: AAC (표준)
                "-pix_fmt", "yuv420p", // 픽셀 포맷 (구형 디바이스 호환성 향상)
                "-preset", "fast",      // 변환 속도 설정 (fast, medium, slow 등)
                "-y",                   // 출력 파일이 이미 존재하면 덮어쓰기
                outputPath
        );

        logger.info("Executing FFmpeg command: " + String.join(" ", builder.command()));

        Process process = builder.inheritIO().start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
        }
    }
	
	/** 영상 파일에서 썸네일 이미지를 저장한다. */
	private void generateVideoThumbnailWithJCodec(String videoPath, String thumbnailPath) throws IOException, JCodecException {
		File videoFile = new File(videoPath);
		FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));

		// 특정 시간으로 이동 (예: 1초)
		grab.seekToSecondPrecise(1);

		Picture picture = grab.getNativeFrame();
		if (picture == null) {
			throw new RuntimeException("프레임을 추출할 수 없습니다. 비디오 파일을 확인하세요.");
		}

		// YUV 데이터를 BufferedImage로 변환
		BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);

		// 썸네일 저장
		File outputFile = new File(thumbnailPath);
		if (!ImageIO.write(bufferedImage, "jpg", outputFile)) {
			throw new RuntimeException("이미지 파일 저장에 실패했습니다: " + thumbnailPath);
		}

		System.out.println("썸네일 생성 완료: " + thumbnailPath);

		if (!ImageIO.write(bufferedImage, "jpg", outputFile)) {
			throw new RuntimeException("이미지 파일 저장에 실패했습니다. 경로를 확인하세요: " + thumbnailPath);
		}

		ImageIO.write(bufferedImage, "jpg", outputFile); // 썸네일 저장
	}
	
	private void sendNotificationToReceiver(Map<String, Object> map, String content) {
		Map<String, Object> receiver = dao.selectOne("member.getReceiverToken", map), sender = dao.selectOne("member.getSenderName", map);
		String fcmToken = String.valueOf(receiver.get("FCM_TOKEN"));
		String senderName = String.valueOf(sender.get("MEM_NAME"));
		
		Notification notification = Notification.builder()
			    .setTitle(senderName)
			    .setBody(content)
			    .build();
		firebaseService.sendPersonalNotification(fcmToken, notification);
	}
}
