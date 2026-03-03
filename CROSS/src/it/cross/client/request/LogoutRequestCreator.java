package it.cross.client.request;

import it.cross.shared.request.Request;
import it.cross.shared.request.auth.Logout;
import it.cross.client.cli.ClientView;
import it.cross.shared.request.Operation;

/**
 * A specific creator for building a request to log out the current user.
 */
class LogoutRequestCreator implements RequestCreator {
	@Override
	public String createRequest(ClientView view) {
		return RequestFactory.gson.toJson(new Request(Operation.LOGOUT, new Logout()));
	}
}
