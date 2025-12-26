package com.giggles.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

// Firebase verification removed - service kept for future use
// @Service
@Slf4j
public class FirebaseService {
    
    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            log.info("Firebase token verified successfully for UID: {}", decodedToken.getUid());
            return decodedToken;
        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            throw e;
        }
    }
    
    public boolean validatePhoneNumber(String idToken, String phoneNumber) {
        try {
            FirebaseToken decodedToken = verifyIdToken(idToken);
            String tokenPhoneNumber = Objects.nonNull(decodedToken.getClaims().get("phone_number"))
                    ? decodedToken.getClaims().get("phone_number").toString() 
                    : null;
            
            if (Objects.isNull(tokenPhoneNumber)) {
                log.warn("Phone number not found in Firebase token");
                return false;
            }
            
            boolean matches = Objects.equals(tokenPhoneNumber, phoneNumber) || 
                            Objects.equals(tokenPhoneNumber, "+" + phoneNumber) ||
                            Objects.equals("+" + tokenPhoneNumber, phoneNumber);
            
            if (!matches) {
                log.warn("Phone number mismatch. Token: {}, Request: {}", tokenPhoneNumber, phoneNumber);
            }
            
            return matches;
        } catch (FirebaseAuthException e) {
            log.error("Error validating phone number: {}", e.getMessage());
            return false;
        }
    }
}

