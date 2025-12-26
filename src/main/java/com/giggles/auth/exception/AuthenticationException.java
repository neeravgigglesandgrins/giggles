package com.giggles.auth.exception;

public class AuthenticationException extends RuntimeException {
    
    private final String errorCode;
    private final int statusCode;
    
    public AuthenticationException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}

