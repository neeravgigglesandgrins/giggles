package com.giggles.auth.exception;

public class CommonException extends RuntimeException {
    
    private final int statusCode;
    private final String errorCode;
    
    public CommonException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

