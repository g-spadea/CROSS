package it.cross.shared.response.auth;

import it.cross.shared.response.ResponseCode;

/**
 * Defines the specific response codes for an order cancellation operation.
 */
public enum CancelCode implements ResponseCode {
	
	OK(100, "OK"),
    OTHER(101, "other error cases");
    
    final int code;
    final String message;
    
    CancelCode(int code, String message) {
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
