package com.sensible.api.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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

	/**
	 * Android 알림 채널 id (2-31차 후속). 앱(app.component.ts / notifications.service.ts)과 같은 값이어야 한다.
	 *
	 * 채널은 한번 만들어지면 소리·진동을 앱이 바꿀 수 없다. 예전 star_visitor_channel 은 2026-06 에 소리 없이
	 * 만들어졌고 소리는 2026-09 에 추가됐으므로, 그 사이 설치된 기기는 영구 무음이었다. id 를 바꿔 새 채널로 보낸다.
	 */
	public static final String CHANNEL_VISITOR = "star_visitor_channel_v2";
	public static final String CHANNEL_DM = "dm_channel_v2";
	public static final String CHANNEL_BROADCAST = "broadcast_v2";

	/** iOS 배지 카운터 키. 앱을 열면 리셋한다 */
	static final String BADGE_KEY_PREFIX = "push:badge:";
	static final long BADGE_TTL_DAYS = 30L;

	@Autowired
	private final FirebaseMessaging firebaseMessaging;

	/** 없으면(테스트·Redis 장애) 배지는 1 로 간다 */
	@Resource(name = "redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	public FirebaseService(FirebaseMessaging firebaseMessaging){
		this.firebaseMessaging = firebaseMessaging;
	}

	static String badgeKey(String token) {
		return BADGE_KEY_PREFIX + token;
	}

	/**
	 * iOS 배지 숫자 = 마지막으로 앱을 연 뒤 받은 알림 수 (Android 런처가 세는 것과 같은 정의).
	 * 예전에는 항상 1 이었다. Redis 가 없거나 실패하면 예전처럼 1.
	 */
	int nextBadge(String token) {
		if (redisTemplate == null || token == null || token.trim().isEmpty()) return 1;
		try {
			String key = badgeKey(token);
			Long count = redisTemplate.opsForValue().increment(key, 1L);
			if (count == null) return 1;
			if (count == 1L) {
				redisTemplate.expire(key, BADGE_TTL_DAYS, TimeUnit.DAYS);
			}
			return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : count.intValue();
		} catch (Exception e) {
			logger.warn("[push] badge counter unavailable, falling back to 1: {}", e.getMessage());
			return 1;
		}
	}

	/** 앱을 열었을 때 (Badge.clear 와 함께) 카운터를 지운다 */
	public boolean resetBadge(String token) {
		if (redisTemplate == null || token == null || token.trim().isEmpty()) return false;
		try {
			redisTemplate.delete(badgeKey(token));
			return true;
		} catch (Exception e) {
			logger.warn("[push] badge reset failed: {}", e.getMessage());
			return false;
		}
	}

	public String notify(final String topic, Notification content){
		AndroidConfig androidConfig = AndroidConfig.builder()
									.setPriority(AndroidConfig.Priority.HIGH)
									.setTtl(Duration.ofMinutes(2).toMillis())
									.setNotification(AndroidNotification.builder()
											.setChannelId(CHANNEL_BROADCAST)
											.setSound("tick.mp3")
											.build())
									.build();
		ApnsConfig apnsConfig = ApnsConfig.builder()
				.setAps(Aps.builder().setCategory(topic).setThreadId(topic).setSound("tick.wav").build())
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
	 * badge 는 토큰별 카운터(2-31차 후속) — 앱을 열면 리셋된다.
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
						.setBadge(nextBadge(token))
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
                		.setChannelId(CHANNEL_VISITOR)
                		.setSound("tick.mp3")
                		.build())
                .build();

        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                		.setSound("tick.wav")
                		.setBadge(nextBadge(token))
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
