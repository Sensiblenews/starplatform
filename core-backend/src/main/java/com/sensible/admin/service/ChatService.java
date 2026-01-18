package com.sensible.admin.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface ChatService {
	Map<String, Object> createOrGetRoom(Long myId, Long opponentId);
	
	Map<String, Object> getChatRooms(Long userId);
	
	Map<String, Object> getMessages(Long roomId, Long userId);
	
	Map<String, Object> sendMessage(Long roomId, Long senderId, String message, String messageType, MultipartFile file);

	Map<String, Object> markMessagesAsRead(Long roomId, Long userId);
	
	Map<String, Object> getTotalUnreadCount(Long userId);
}
