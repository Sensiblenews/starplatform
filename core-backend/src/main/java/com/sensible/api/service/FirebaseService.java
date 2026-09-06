package com.sensible.api.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.threeten.bp.Duration;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service("firebaseService")
public class FirebaseService {
	private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);
	@Autowired
	private final FirebaseMessaging firebaseMessaging;
	
	@Autowired
	public FirebaseService(FirebaseMessaging firebaseMessaging){
		this.firebaseMessaging = firebaseMessaging;
	}
	
	public String notify(final String topic, Notification content){
		AndroidConfig androidConfig = AndroidConfig.builder()
									.setPriority(AndroidConfig.Priority.HIGH)
									.setTtl(Duration.ofMinutes(2).toMillis())
									.setNotification(AndroidNotification.builder().setChannelId("500").build())
									.build();
		ApnsConfig apnsConfig = ApnsConfig.builder()
				.setAps(Aps.builder().setCategory(topic).setThreadId(topic).build())
				.build();
		
		Message msg = Message.builder()
				.setTopic(topic)
				.setAndroidConfig(androidConfig)
				.setNotification(content)
				.setApnsConfig(apnsConfig)
				.build();
		try{
			return firebaseMessaging.send(msg);
		}
		catch(FirebaseMessagingException ex){
			throw new RuntimeException("Error sending notification: "+ex.getMessage());
		}
	}
	
	/**
	 * 데이터 페이로드·채널을 지정하는 개인 푸시 (1:1 메신저용, 2-29차).
	 * 기존 sendPersonalNotification은 방문자 알림 채널·badge=1이 고정이라 건드리지 않는다.
	 * badge는 클라이언트 확정대로 1(점 표기) — 앱을 열면 Badge.clear()가 지운다.
	 */
	public String sendDataNotification(final String token, Notification content,
			Map<String, String> data, String channelId) {
		String retData = "";
		AndroidConfig androidConfig = AndroidConfig.builder()
				.setPriority(AndroidConfig.Priority.HIGH)
				.setTtl(Duration.ofMinutes(5).toMillis())
				.setNotification(AndroidNotification.builder()
						.setChannelId(channelId)
						.setSound("tick.mp3")
						.build())
				.build();

		ApnsConfig apnsConfig = ApnsConfig.builder()
				.setAps(Aps.builder()
						.setSound("tick.wav")
						.setBadge(1)
						.build())
				.build();

		Message.Builder builder = Message.builder()
				.setToken(token)
				.setAndroidConfig(androidConfig)
				.setNotification(content)
				.setApnsConfig(apnsConfig);
		if (data != null && !data.isEmpty()) {
			builder.putAllData(data);
		}

		try {
			retData = firebaseMessaging.send(builder.build());
		} catch (FirebaseMessagingException ex) {
			throw new RuntimeException("Error sending notification: " + ex.getMessage());
		}
		return retData;
	}

	public String sendPersonalNotification(final String token, Notification content) {
		String retData = "";
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setTtl(Duration.ofMinutes(2).toMillis())
                .setNotification(AndroidNotification.builder()
                		.setChannelId("star_visitor_channel")
                		.setSound("tick.mp3")
                		.build())
                .build();
        
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                		.setSound("tick.wav")
                		.setBadge(1)
                		.build())
                .build();

        Message msg = Message.builder()
                .setToken(token)
                .setAndroidConfig(androidConfig)
                .setNotification(content)
                .setApnsConfig(apnsConfig)
                .build();

        try {
            retData = firebaseMessaging.send(msg);
        	logger.info("successfully sent message to: " + token);
        } catch (FirebaseMessagingException ex) {
            throw new RuntimeException("Error sending notification: " + ex.getMessage());
        }
        
        return retData;
	}
}
