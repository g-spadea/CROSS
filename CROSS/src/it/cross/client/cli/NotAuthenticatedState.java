package it.cross.client.cli;

import it.cross.shared.request.Operation;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Represents the state of the client when the user is not authenticated.
 *
 * In this state, the user is presented with a limited set of options, such as
 * logging in, registering, updating credentials, or exiting the application.
 * A successful login or registration will transition the context to the
 * {@link AuthenticatedState}.
 */
class NotAuthenticatedState extends SessionState {
	
	NotAuthenticatedState(ClientController controller, Gson gson) {
		super(controller, gson);
	}
	
	@Override
    public void displayMenu() {
        controller.getView().displayNotAuthenticatedMenu();
    }
	
	/**
     * Handles user input for the unauthenticated menu.
     *
     * @param choice The user's selected menu option.
     * @return true if the connection was lost, prompting a reconnect; false otherwise.
     */
    @Override
    public boolean handleInput(String choice) {
        try {
            switch (choice) {
                case "1": // Register
                    talkWithServer(Operation.REGISTER, "Registration successful!");
                    controller.setState(PossibleState.AUTHENTICATED);
                    break;
                case "2": // Login
                    talkWithServer(Operation.LOGIN, "Login successful!");
                    controller.setState(PossibleState.AUTHENTICATED);
                    break;
                case "3": // Update Credentials
                    talkWithServer(Operation.UPDATE_CREDENTIALS, "Credentials updated successfully!");
                    break;
                case "4": // Exit
                    controller.stop();
                    break;
                default:
                    controller.getView().displayError("Invalid choice. Please try again.");
            }
        } catch (OperationFailedException e) {
            // Handles logical errors returned by the server (e.g., "user already exists").
            controller.getView().displayError(e.getMessage());
        } catch (JsonSyntaxException e) {
            // Handles cases where the server's response is malformed.
            controller.getView().displayError("Received a malformed response from the server.");
        } catch (IOException e) {
            // Handles network errors, signaling that a reconnection attempt is needed.
            controller.getView().displayError("Connection to the server has been lost!");
            return true; // Signal that the connection was lost
        }
        return false;
    }
}
