package com.giggles.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Objects;

// Firebase configuration disabled - Firebase OTP verification removed
// @Configuration
@Slf4j
public class FirebaseConfig {
    
    @Value("${firebase.project-id}")
    private String projectId;
    
    @Value("${firebase.credentials.path:}")
    private String credentialsPath;
    
    // @Bean
    public FirebaseApp initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions.Builder builder = FirebaseOptions.builder()
                        .setProjectId(projectId);
                
                if (Objects.nonNull(credentialsPath) && !credentialsPath.isEmpty() && !Objects.equals(credentialsPath, "classpath:firebase-credentials.json")) {
                    try {
                        InputStream serviceAccount = new ClassPathResource("firebase-credentials.json").getInputStream();
                        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                        builder.setCredentials(credentials);
                    } catch (Exception e) {
                        log.warn("Could not load Firebase credentials from classpath. Using default credentials: {}", e.getMessage());
                    }
                }
                
                FirebaseOptions options = builder.build();
                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
                return app;
            } else {
                return FirebaseApp.getInstance();
            }
        } catch (Exception e) {
            log.error("Error initializing Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}

