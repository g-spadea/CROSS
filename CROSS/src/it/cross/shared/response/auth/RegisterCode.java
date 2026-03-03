package it.cross.shared.response.auth;

import it.cross.shared.response.ResponseCode;

/**
 * Defines the specific response codes for a registration operation.
 */
public enum RegisterCode implements ResponseCode {

    OK(100, "OK"),
    INVALID_PASSWORD(101, "invalid password"),
    NOT_AVAILABLE(102, "username not available"),
    OTHER(103, "other error cases");
    
    final int code;
    final String message;
    
    RegisterCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
