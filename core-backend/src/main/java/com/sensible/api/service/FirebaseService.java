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
