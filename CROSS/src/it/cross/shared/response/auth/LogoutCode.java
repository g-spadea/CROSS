package it.cross.shared.response.auth;

import it.cross.shared.response.ResponseCode;

/**
 * Defines the response codes for a logout operation.
 */
public enum LogoutCode implements ResponseCode {

	OK(100, "OK"),
    NOT_LOGGED_OR_OTHER(101, "user not logged in");
    
    final int code;
    final String message;
    
    LogoutCode(int code, String message) {
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
