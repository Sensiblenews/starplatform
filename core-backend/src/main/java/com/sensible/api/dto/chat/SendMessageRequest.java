package com.sensible.api.dto.chat;

import org.springframework.web.multipart.MultipartFile;

public class SendMessageRequest {
    private String message;             // 텍스트 메시지
    private String messageType;         // TEXT, IMAGE, etc.
    private MultipartFile file;         // 첨부 파일 (선택)
}

