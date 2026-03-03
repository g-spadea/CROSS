package it.cross.server.response;

import it.cross.server.user.Session;
import it.cross.server.user.SessionManager;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.auth.Logout;
import it.cross.shared.response.Response;
import it.cross.shared.response.ResponseInterface;
import it.cross.shared.response.auth.LogoutCode;

class LogoutHandler implements RequestHandlerInterface {

    /**
     * Processes a user logout request.
     * <p>
     * It checks if the user is currently authenticated. If so, it invalidates
     * the session, effectively logging the user out.
     *
     * @param request     The request payload, expected to be a {@link Logout} object.
     * @param sessionUser The client's session to be invalidated.
     * @return A {@link Response} with the corresponding {@link LogoutCode}.
     */
	@Override
	public ResponseInterface handle(RequestInterface request, Session sessionUser) {
		
		if (!(request instanceof Logout))
			return new Response(LogoutCode.NOT_LOGGED_OR_OTHER, "Operation not allowed!");
		if(!sessionUser.isAuthenticated())
			return new Response(LogoutCode.NOT_LOGGED_OR_OTHER);
		
		sessionUser.invalidate();
        SessionManager.getInstance().removeSession(sessionUser.getUsername());
		return new Response(LogoutCode.OK);

	}

}
