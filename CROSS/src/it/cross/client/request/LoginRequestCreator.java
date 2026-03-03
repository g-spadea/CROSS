package it.cross.client.request;

import it.cross.shared.request.Request;
import it.cross.shared.request.auth.LoginAndRegister;
import it.cross.client.cli.ClientView;
import it.cross.shared.request.Operation;

/**
 * A specific creator for building requests for login operations.
 */
class LoginRequestCreator implements RequestCreator {
	@Override
	public String createRequest(ClientView view) {
		view.displayString("\n" + "Insert username");
		String username = view.getScanner().nextLine();
		view.displayString("Insert password");
		String password = view.getScanner().nextLine();
		return RequestFactory.gson.toJson(new Request(Operation.LOGIN, new LoginAndRegister(username, password)));
	}
}
