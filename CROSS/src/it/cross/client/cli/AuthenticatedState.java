package it.cross.client.cli;

import it.cross.client.notification.NotificationCenter;
import it.cross.shared.request.Operation;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Represents the state of the client when the user is authenticated.
 *
 * In this state, the user has access to the full range of application features,
 * including placing and canceling orders, viewing history, and logging out.
 * A successful logout will transition the context back to the
 * {@link UnauthenticatedState}.
 */
class AuthenticatedState extends SessionState {
	
	private final NotificationCenter notificationsStore = NotificationCenter.getInstance();
	public AuthenticatedState(ClientController controller, Gson gson) {
		super(controller, gson);
	}
	
	@Override
	public void displayMenu() {
       controller.getView().displayAuthenticatedMenu(notificationsStore.hasNewNotifications());
	}
	
	private void showNotificationHistory() {
        String history = notificationsStore.toString();
        controller.getView().displayString(history);
    }
	
	/**
     * Handles user input for the authenticated menu.
     *
     * @param choice The user's selected menu option.
     * @return true if the connection was lost, prompting a reconnect; false otherwise.
     */
    @Override
    public boolean handleInput(String choice) {
        try {
            switch (choice) {
                case "1": // Place a Limit Order
                    talkWithServer(Operation.INSERT_LIMIT_ORDER, "Operation failed!");
                    break;
                case "2": // Place a Market Order
                    talkWithServer(Operation.INSERT_MARKET_ORDER, "Operation failed!");
                    break;
                case "3": // Place a Stop Order
                    talkWithServer(Operation.INSERT_STOP_ORDER, "Operation failed!");
                    break;
                case "4": // Cancel an Order
                    talkWithServer(Operation.CANCEL_ORDER, "Order canceled successfully!");
                    break;
                case "5": // View Notification History
                    showNotificationHistory();
                    break;
                case "6": // Request Monthly History
                    talkWithServer(Operation.GET_HISTORY, "Monthly history retrieved!");
                    break;
                case "7": //Clear console
                	controller.getView().clearConsole();
                	break;
                case "8": // Logout
                    talkWithServer(Operation.LOGOUT, "Logout successful!");
                    controller.setState(PossibleState.UNAUTHENTICATED);
                    break;
                default:
                    controller.getView().displayError("Invalid choice. Please try again.");
                    break;
            }
        } catch (OperationFailedException e) {
            // Handles logical errors returned by the server (e.g., "order not found").
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
