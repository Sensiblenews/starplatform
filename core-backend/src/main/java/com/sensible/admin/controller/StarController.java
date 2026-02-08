package com.sensible.admin.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.sensible.admin.domain.UserVO;
import com.sensible.common.Constants;
import com.sensible.common.dao.DefaultDAO;
import com.sensible.common.util.FileUtil;

@Controller
public class StarController {

    @Resource(name="DefaultDAO")
    private DefaultDAO dao;

    // 세션 체크 헬퍼
    private UserVO getLoginStar(HttpServletRequest request) {
        UserVO user = (UserVO) request.getSession().getAttribute("SUPER_USER_SESSION");
        if (user != null && "ST".equals(user.getPRS_AUTH())) {
            return user;
        }
        return null;
    }

    /**
     * [화면] 스타 마이페이지 (프로필 + 갤러리)
     */
    @RequestMapping(value = "/star/mypage.do")
    public String myPage(HttpServletRequest request, Model model) throws Exception {
        UserVO user = getLoginStar(request);
        if (user == null) return "redirect:/super/login.do";

        // 내 정보 갱신 (DB에서 최신 정보 가져오기)
        Map<String, Object> myInfo = dao.selectOne("star.selectMyInfo", user.getPRS_ID());
        // 갤러리 목록
        List<Map<String, Object>> gallery = dao.selectList("star.selectMyGallery", user.getPRS_ID());

        model.addAttribute("myInfo", myInfo);
        model.addAttribute("gallery", gallery);
        
        return "star/mypage"; // JSP 경로
    }

    /**
     * [기능] 프로필 수정 (사진 포함)
     */
    @RequestMapping(value = "/star/updateProfile.do")
    @ResponseBody
    public Map<String, Object> updateProfile(
            HttpServletRequest request, 
            @RequestParam Map<String, Object> params,
            @RequestParam(value="profileImage", required=false) MultipartFile profileImage) {
        
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginStar(request);
        if (user == null) {
            result.put("status", "fail"); result.put("msg", "로그인이 필요합니다."); return result;
        }

        try {
            params.put("PRS_ID", user.getPRS_ID());

            // 1. 프로필 이미지 업로드 처리
            if (profileImage != null && !profileImage.isEmpty()) {
                String fullUrl = FileUtil.uploadFile(profileImage);
                params.put("STORED_FILE_NM", fullUrl); // DB 컬럼 매핑
            }

            // 2. DB 업데이트
            dao.update("star.updateProfile", params);
            
            result.put("status", "success");
            result.put("msg", "프로필이 수정되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            result.put("msg", "수정 실패: " + e.getMessage());
        }
        return result;
    }

    /**
     * [기능] 갤러리 사진 업로드
     */
    @RequestMapping(value = "/star/uploadGallery.do")
    @ResponseBody
    public Map<String, Object> uploadGallery(
            HttpServletRequest request,
            @RequestParam(value="galleryImage", required=true) MultipartFile galleryImage) {
        
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginStar(request);
        
        try {
            if (galleryImage == null || galleryImage.isEmpty()) throw new Exception("파일이 없습니다.");

            // 1. 파일 저장
            String fullUrl = FileUtil.uploadFile(galleryImage);

            // 2. DB 저장
            Map<String, Object> params = new HashMap<>();
            params.put("PRS_ID", user.getPRS_ID());
            params.put("IMG_URL", fullUrl);
            
            dao.insert("star.insertGalleryPhoto", params);

            result.put("status", "success");
            result.put("msg", "사진이 업로드되었습니다.");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "업로드 실패: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * [기능] 갤러리 사진 삭제
     */
    @RequestMapping(value = "/star/deleteGallery.do")
    @ResponseBody
    public Map<String, Object> deleteGallery(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginStar(request);
        
        try {
            params.put("PRS_ID", user.getPRS_ID()); // 본인 것만 삭제 가능
            dao.delete("star.deleteGalleryPhoto", params);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
        }
        return result;
    }
    
    /**
     * [기능] 피드 업로드 (1:N 구조 지원)
     */
    @RequestMapping(value = "/star/writeFeed.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> writeFeed(
            HttpServletRequest request,
            @RequestParam("feedText") String feedText,
            @RequestParam("mediaFiles") List<MultipartFile> mediaFiles) {
        
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginStar(request);

        // 1. 규칙 검사
        if (user == null) return errorMap("로그인이 필요합니다.");
        if (mediaFiles == null || mediaFiles.isEmpty()) return errorMap("최소 1개의 미디어를 업로드해야 합니다.");
        if (mediaFiles.size() > 10) return errorMap("미디어는 최대 10개까지만 업로드 가능합니다."); // [규칙]
        
        try {
            // 2. 게시글 마스터(WH_STAR_CONTENT) 저장
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("PRS_ID", user.getPRS_ID());
            contentMap.put("CON_BODY", feedText); // 글은 없어도 됨 (Null 허용)
            
            // 이 쿼리가 실행되면 contentMap에 'CON_ID'가 담겨와야 함 (useGeneratedKeys="true")
            dao.insert("star.insertContentMaster", contentMap);
            int conId = Integer.parseInt(contentMap.get("CON_ID").toString());

            // 3. 미디어 파일 반복 저장 (STAR_CONTENT_MEDIA)
            int sortOrder = 0;
            for (MultipartFile file : mediaFiles) {
                if (file.isEmpty()) continue;
                
                String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
                String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
                boolean isVideo = file.getContentType().startsWith("video");
                
                Map<String, Object> mediaMap = new HashMap<>();
                mediaMap.put("CON_ID", conId);
                mediaMap.put("SORT_ORDER", sortOrder++);
                mediaMap.put("MEDIA_TYPE", isVideo ? "VIDEO" : "PHOTO");

                if (isVideo) {
                    // [동영상 로직]
                    String videoName = uuid + ext;
                    String thumbName = uuid + "_thumb.jpg"; // [규칙] 썸네일 명명 규칙 통일
                    
                    // 파일 저장
                    File target = new File(Constants._VIDEO_SAVE_PATH + videoName); //
                    file.transferTo(target);
                    
                    // 썸네일 생성 (ffmpeg)
                    generateVideoThumbnail(target.getAbsolutePath(), Constants._VIDEO_THUMNAIL_SAVE_PATH + thumbName); //

                    mediaMap.put("MEDIA_URL", Constants._VIDEO_FILE_URL + videoName);
                    mediaMap.put("THUMB_URL", Constants._VIDEO_THUMNAIL_FILE_URL + thumbName);
                } else {
                    // [사진 로직]
                    String imgName = uuid + ext;
                    File target = new File(Constants._FILE_SAVE_PATH + imgName); //
                    file.transferTo(target);

                    mediaMap.put("MEDIA_URL", Constants._FILE_URL + imgName);
                    mediaMap.put("THUMB_URL", Constants._FILE_URL + imgName); // 사진은 썸네일도 본인 자신
                }
                
                dao.insert("star.insertContentMedia", mediaMap);
            }

            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            return errorMap("업로드 실패: " + e.getMessage());
        }
        return result;
    }
    
    private Map<String, Object> errorMap(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "fail");
        m.put("msg", msg);
        return m;
    }

    /**
     * [Helper] 동영상 썸네일 추출 메소드
     * (서버에 ffmpeg가 설치되어 있다고 가정하거나, Java 라이브러리 사용)
     */
    private void generateVideoThumbnail(String videoPath, String thumbPath) {
        try {
            // 방법 1: FFmpeg 커맨드 라인 실행 (가장 확실한 방법)
            // 리눅스 서버 기준: /usr/bin/ffmpeg (경로 확인 필요)
            String[] cmd = {
                "ffmpeg", "-i", videoPath, 
                "-ss", "00:00:01", // 1초 지점 캡처
                "-vframes", "1", 
                "-y", // 덮어쓰기 허용
                thumbPath
            };
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
            
            // 방법 2: 만약 ffmpeg 설치가 안되어 있다면, 기본 이미지로 대체하는 예외처리 필요
        } catch (Exception e) {
            e.printStackTrace();
            // 썸네일 생성 실패 시 로그 남김
            System.out.println("썸네일 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * [기능] 내 피드 상세 조회 (View Modal용)
     */
    @RequestMapping(value = "/star/getFeedDetail.do")
    @ResponseBody
    public Map<String, Object> getFeedDetail(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        UserVO user = getLoginStar(request);
        
        try {
            params.put("PRS_ID", user.getPRS_ID()); // 본인 확인용

            // 1. 글 내용 가져오기
            Map<String, Object> master = dao.selectOne("star.selectFeedMaster", params);
            
            if (master != null) {
                // 2. 미디어 리스트 가져오기
                List<Map<String, Object>> medias = dao.selectList("star.selectFeedMedias", params);
                
                result.put("status", "success");
                result.put("master", master);
                result.put("medias", medias);
            } else {
                result.put("status", "fail");
                result.put("msg", "게시물을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "오류 발생: " + e.getMessage());
        }
        return result;
    }
}