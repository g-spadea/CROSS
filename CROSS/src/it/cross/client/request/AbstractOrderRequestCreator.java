package it.cross.client.request;

import it.cross.client.cli.ClientView;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.operation.RequestSide;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * An abstract base class for creating trading operation requests.
 *
 * This class uses the Template Method pattern to manage the general flow of
 * user interaction and error handling, leaving subclasses to define the
 * specific details of the order payload. It provides utility methods for
 * reading and validating common user inputs.
 */
abstract class AbstractOrderRequestCreator implements RequestCreator {

	@Override
	public abstract String createRequest(ClientView view);

    /**
     * Gathers the specific data needed for the request payload from the user.
     * This method is called within a loop that handles input errors.
     *
     * @param in The {@link Scanner} instance to read user input.
     * @return An object implementing {@link RequestInterface} containing the payload data.
     */
	abstract RequestInterface gatherPayloadInput(ClientView view);
	
	protected RequestInterface createOrderPayload(ClientView view) {
		while (true) {
			try {
				return gatherPayloadInput(view);
			} catch (NumberFormatException e) {
                view.displayError(e.getMessage());
			} catch (InputMismatchException e) {
				view.displayError(e.getMessage());
			}
		}
	}
	
	protected int readInt(ClientView view, String prompt) throws NumberFormatException {
		view.displayString(prompt);
		int number = Integer.parseInt(view.getScanner().nextLine());
		if(number <= 0)
	        throw new NumberFormatException("The value must be greater than 0!");
		return number;
	}
	
	protected RequestSide readRequestType(ClientView view, String prompt) throws InputMismatchException {
		view.displayString("\n" + prompt);
        String input = view.getScanner().nextLine().toLowerCase();
		switch(input) {
			case "ask": return RequestSide.ASK;
			case "bid": return RequestSide.BID;
			default:
				throw new InputMismatchException("Error: you must enter 'ask' or 'bid'!");
		}
	}
}
