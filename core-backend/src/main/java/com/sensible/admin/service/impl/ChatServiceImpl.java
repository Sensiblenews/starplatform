package com.sensible.admin.service.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sensible.admin.service.ChatService;
import com.sensible.api.service.MemberService;

@Service("chatService")
public class ChatServiceImpl implements ChatService {
	
	private static final Logger logger = LoggerFactory.getLogger(MemberService.class);

	@Override
	public Map<String, Object> createOrGetRoom(Long myId, Long opponentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getChatRooms(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getMessages(Long roomId, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> sendMessage(Long roomId, Long senderId, String message, String messageType, MultipartFile file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> markMessagesAsRead(Long roomId, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getTotalUnreadCount(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
