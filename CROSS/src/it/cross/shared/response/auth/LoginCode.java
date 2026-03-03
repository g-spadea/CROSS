package it.cross.shared.response.auth;

import it.cross.shared.response.ResponseCode;

/**
 * Defines the response codes for a login operation.
 */
public enum LoginCode implements ResponseCode {
	
	OK(100, "OK"),
    MISMATCH_OR_NOT_EXISTENT(101, "username/password mismatch or non existent username"),
    LOGGED(102, "user already logged in"),
    OTHER(103, "other error cases");
    
    final int code;
    final String message;
    
    LoginCode(int code, String message) {
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
