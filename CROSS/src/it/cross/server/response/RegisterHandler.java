package it.cross.server.response;

import it.cross.server.user.Session;
import it.cross.server.user.SessionManager;
import it.cross.server.user.UserDB;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.auth.LoginAndRegister;
import it.cross.shared.response.Response;
import it.cross.shared.response.ResponseInterface;
import it.cross.shared.response.auth.RegisterCode;

class RegisterHandler implements RequestHandlerInterface {

	private final UserDB userDB;

    public RegisterHandler(UserDB dataStore) {
        this.userDB = dataStore;
    }

    /**
     * Processes a user registration request.
     * <p>
     * It performs the following checks:
     * <ol>
     * <li>Ensures the user is not already logged in.</li>
     * <li>Validates that the username and password are not empty. </li>
     * <li>Checks if the username is already taken in the database.</li>
     * </ol>
     * If all checks pass, the user is created, and their session is immediately
     * authenticated.
     * <p>
     * <b>Note:</b> The validation logic is intentionally simplified for the
     * scope of this project. A production system would require more complex
     * rules (e.g., password strength, username format).
     *
     * @param request     The request payload, expected to be an {@link LoginAndRegister} object.
     * @param sessionUser The client's session, which will be authenticated upon success.
     * @return A {@link Response} with the corresponding {@link RegisterCode}.
     */
	@Override
	public ResponseInterface handle(RequestInterface request, Session sessionUser) {
		
		if (!(request instanceof LoginAndRegister))
            return new Response(RegisterCode.OTHER, "Operation not allowed!");
		if(sessionUser.isAuthenticated())
			return new Response(RegisterCode.OTHER, "User is already logged in, operation not allowed!");
		
		LoginAndRegister auth = (LoginAndRegister) request;
		
		if(auth.password == null || auth.password.trim().isEmpty() || auth.username.trim().isEmpty())
			return new Response(RegisterCode.INVALID_PASSWORD);
		if(!userDB.registerUser(auth))
			return new Response(RegisterCode.NOT_AVAILABLE);
	
		sessionUser.authenticate(auth);
        SessionManager.getInstance().registerSession(auth.username, sessionUser);
		return new Response(RegisterCode.OK);
		
	}

}
