package com.sensible.common.util;

import java.io.File;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import com.sensible.common.Constants;

public class FileUtil {

    /**
     * 파일 업로드 처리
     * @return 저장된 파일의 풀 URL (DB 저장용)
     */
    public static String uploadFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. 저장 경로 확인 및 생성
        File saveDir = new File(Constants._FILE_SAVE_PATH);
        if (!saveDir.exists()) {
            saveDir.mkdirs(); // 디렉토리가 없으면 생성
        }

        // 2. 랜덤 파일명 생성 (UUID)
        String originalName = file.getOriginalFilename();
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String randomName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 3. 서버에 파일 저장
        File targetFile = new File(Constants._FILE_SAVE_PATH + randomName);
        file.transferTo(targetFile);

        // 4. DB에 저장할 풀 URL 반환 (예: https://witch-hunting.com/img/sdf23...jpg)
        return Constants._FILE_URL + randomName;
    }
}