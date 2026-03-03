package it.cross.shared.response.auth;

import it.cross.shared.response.ResponseCode;

/**
 * Defines the response codes for a credential (password) update operation.
 */
public enum UpdateCredentialCode implements ResponseCode{

	OK(100, "OK"),
    INVALID_PASSWORD(101, "invalid new password"),
    MISMATCH_OR_NOT_EXISTENT(102, "username/old_password mismatch or non existent username"),
    EQUAL(103, "new password equal to old one"),
    LOGGED(104, "user currently logged in"),
    OTHER(105, "other error cases");
    
    final int code;
    final String message;
    
    UpdateCredentialCode(int code, String message) {
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
