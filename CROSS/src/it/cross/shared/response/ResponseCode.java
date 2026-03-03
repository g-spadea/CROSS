package it.cross.shared.response;

/**
 * Defines the contract for all enums that represent server response codes.
 *
 * Each implementation of this interface will provide a set of status codes
 * and standard messages for a specific category of operations (e.g., login,
 * registration).
 */
public interface ResponseCode {
	
	/**
     * Returns the standard numeric code associated with the response.
     * @return an integer representing the code.
     */
    int getCode();
    
    /**
     * Returns the default message associated with the response.
     * @return a string representing the message.
     */
    String getMessage();
    
}
