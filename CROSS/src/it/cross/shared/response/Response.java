package it.cross.shared.response;

import it.cross.shared.response.auth.CancelCode;
import it.cross.shared.response.auth.LoginCode;
import it.cross.shared.response.auth.LogoutCode;
import it.cross.shared.response.auth.RegisterCode;
import it.cross.shared.response.auth.UpdateCredentialCode;

/**
 * Represents a generic, standard response from the server.
 *
 * This class encapsulates a numeric status code and a descriptive message,
 * allowing the client to interpret the outcome of an operation (e.g., login,
 * registration, cancellation).
 */
public class Response implements ResponseInterface{

	public final int response;
	public final String errorMessage;
	
	public Response(ResponseCode response){
		this.response = response.getCode();
		this.errorMessage = response.getMessage();
	}
	
	public Response(ResponseCode responseCode, String customMessage) {
		
		if(!(
				responseCode == LoginCode.OTHER || 
				responseCode == RegisterCode.OTHER ||
				responseCode == LogoutCode.NOT_LOGGED_OR_OTHER ||
				responseCode == UpdateCredentialCode.OTHER ||
				responseCode == CancelCode.OTHER
		  ))
			throw new IllegalArgumentException("Invalid response type!");
			
        this.response = responseCode.getCode();
        this.errorMessage = customMessage;
    }
}
