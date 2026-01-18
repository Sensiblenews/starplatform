package com.sensible.common.config;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
public class FirebaseConfig {

    private final Resource account = new ClassPathResource("firebase/push-44795-firebase-adminsdk-6jzud-c063ede481.json");

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try (final InputStream inputStream = account.getInputStream()) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }

    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) {
        final FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        if(FirebaseApp.getApps().isEmpty()){
        	FirebaseApp.initializeApp(firebaseOptions);
        }
        
        return FirebaseApp.getApps().get(0);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
    	return FirebaseMessaging.getInstance(firebaseApp);
    }

}
