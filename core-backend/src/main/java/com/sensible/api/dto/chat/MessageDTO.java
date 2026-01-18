package com.sensible.api.dto.chat;

public class MessageDTO {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String message;
    private String messageType;   // TEXT, IMAGE, VIDEO, FILE
    private String fileUrl;
    private Boolean isRead;
    private String sentAt;        // ISO 8601 형식 권장 (ex. 2025-05-25T15:32:00)
}
