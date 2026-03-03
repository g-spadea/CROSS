package it.cross.server.response;

import it.cross.server.user.Session;
import it.cross.server.user.UserDB;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.auth.UpdateCredentials;
import it.cross.shared.response.Response;
import it.cross.shared.response.ResponseInterface;
import it.cross.shared.response.auth.UpdateCredentialCode;

class UpdateCredentialHandler implements RequestHandlerInterface {

	private final UserDB userDB;

    public UpdateCredentialHandler(UserDB userDB) {
        this.userDB = userDB;
    }

    /**
     * Processes a request to update a user's credentials (password).
     * <p>
     * It performs several validation checks: the user must not be logged in,
     * the new password must not be empty or the same as the old one, and the
     * old password must match the one stored in the database.
     * <p>
     * <b>Note:</b> The validation logic is simplified for this project. A production
     * system would enforce stricter password policies.
     *
     * @param request     The request payload, expected to be an {@link UpdateCredentials} object.
     * @param sessionUser The client's session (used here to ensure they are not logged in).
     * @return A {@link Response} with the corresponding {@link UpdateCredentialCode}.
     */
	@Override
	public ResponseInterface handle(RequestInterface request, Session sessionUser) {
		
		if (!(request instanceof UpdateCredentials))
            return new Response(UpdateCredentialCode.OTHER, "Operation not allowed!");
		if(sessionUser.isAuthenticated())
			return new Response(UpdateCredentialCode.LOGGED);
		
		UpdateCredentials newCredential = (UpdateCredentials) request;
		
        if (newCredential.new_password == null || newCredential.new_password.trim().isEmpty())
			return new Response(UpdateCredentialCode.INVALID_PASSWORD);
        if(newCredential.new_password.equals(newCredential.old_password))
        	return new Response(UpdateCredentialCode.EQUAL);
        if(!userDB.updateUserCredentials(newCredential.username, newCredential.old_password, newCredential.new_password))
        	return new Response(UpdateCredentialCode.MISMATCH_OR_NOT_EXISTENT);
        
        return new Response(UpdateCredentialCode.OK);
	}

}
