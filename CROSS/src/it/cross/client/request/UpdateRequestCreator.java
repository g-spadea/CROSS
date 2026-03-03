package it.cross.client.request;

import it.cross.shared.request.Request;
import it.cross.shared.request.auth.UpdateCredentials;
import it.cross.client.cli.ClientView;
import it.cross.shared.request.Operation;

/**
 * A specific creator for building a request to update a user's credentials.
 */
class UpdateRequestCreator implements RequestCreator {
	@Override
	public String createRequest(ClientView view) {
			view.displayString("\n" + "Insert username");
			String username = view.getScanner().nextLine();
			view.displayString("Insert old password: ");
			String old_password = view.getScanner().nextLine();
			view.displayString("Insert new password: ");
			String new_password = view.getScanner().nextLine();
	        return RequestFactory.gson.toJson(new Request(Operation.UPDATE_CREDENTIALS, new UpdateCredentials(username, old_password, new_password)));
	}
}
