package it.cross.server.response;

import it.cross.server.user.Session;
import it.cross.server.user.SessionManager;
import it.cross.server.user.UserDB;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.auth.LoginAndRegister;
import it.cross.shared.response.Response;
import it.cross.shared.response.ResponseInterface;
import it.cross.shared.response.auth.LoginCode;

class LoginHandler implements RequestHandlerInterface {

	private final UserDB userDB;

    public LoginHandler(UserDB userDB) {
        this.userDB = userDB;
    }
	
    /**
     * Processes a user login request.
     * <p>
     * It validates the provided credentials against the records in the {@link UserDB}.
     * If the credentials are valid and the user is not already logged in,
     * the session is marked as authenticated.
     *
     * @param request     The request payload, expected to be an {@link LoginAndRegister} object.
     * @param sessionUser The client's session, which will be authenticated upon success.
     * @return A {@link Response} with the corresponding {@link LoginCode}.
     */
	@Override
	public ResponseInterface handle(RequestInterface request, Session sessionUser) {
		
		if (!(request instanceof LoginAndRegister))
            return new Response(LoginCode.OTHER, "Operation not allowed!");
		if(sessionUser.isAuthenticated())
			return new Response(LoginCode.LOGGED);

        LoginAndRegister auth = (LoginAndRegister) request;
        LoginAndRegister user = userDB.findUser(auth.username);
        
        if(user == null || !user.password.equals(auth.password))
        	return new Response(LoginCode.MISMATCH_OR_NOT_EXISTENT);

        sessionUser.authenticate(user);
        SessionManager.getInstance().registerSession(user.username, sessionUser);
        return new Response(LoginCode.OK);
        
	}

}
