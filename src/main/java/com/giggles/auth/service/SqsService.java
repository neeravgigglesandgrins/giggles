package com.giggles.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SqsService {
    
    @Value("${aws.sqs.signup-queue-url}")
    private String queueUrl;
    
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    
    public SqsService(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }
    
    public void sendSignupEvent(Long userId, String phoneNumber, String role) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("phoneNumber", phoneNumber);
            event.put("role", role);
            event.put("eventType", "USER_SIGNUP");
            event.put("timestamp", System.currentTimeMillis());
            
            String messageBody = objectMapper.writeValueAsString(event);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            
            sqsClient.sendMessage(request);
            log.info("Signup event sent to SQS for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error sending signup event to SQS: {}", e.getMessage(), e);
            // Don't throw exception - SQS failure shouldn't block signup
        }
    }
}

