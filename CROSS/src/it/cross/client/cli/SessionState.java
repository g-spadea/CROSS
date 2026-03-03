package it.cross.client.cli;

import it.cross.client.request.RequestCreator;
import it.cross.client.request.RequestFactory;
import it.cross.shared.request.Operation;
import it.cross.shared.response.OrderID;
import it.cross.shared.response.Response;
import it.cross.shared.response.ResponseInterface;
import it.cross.shared.response.history.CandlestickData;
import it.cross.shared.response.history.HistoryResponse;

import java.io.IOException;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * An abstract class representing a state in the client's session,
 * implementing the State design pattern.
 *
 * This class provides a common structure for handling user input and
 * server communication, delegating the specific logic to concrete state
 * implementations like {@link AuthenticatedState} and {@link UnauthenticatedState}.
 */
abstract class SessionState {

	protected final ClientController controller;
	protected final Gson gson;
	
	SessionState(ClientController controller, Gson gson) {
		this.controller = controller;
		this.gson = gson;
	}
	
	/**
     * Orchestrates the process of creating a request, sending it to the server,
     * and handling the response.
     *
     * @param operation The operation to be performed.
     * @param successMessage The message to display if the operation is successful.
     * @throws OperationFailedException if the server returns an error response.
     * @throws JsonSyntaxException if the server's response is not valid JSON.
     * @throws IOException if a network error occurs.
     */
	protected void talkWithServer(Operation operation, String message) throws OperationFailedException, JsonSyntaxException, IOException {
		RequestCreator authAction = RequestFactory.getInstance().getRequestCreator(operation);
        String requestString = authAction.createRequest(controller.getView());
        String responseString = controller.getConnection().sendAndReceive(requestString);
    	ResponseInterface response = gson.fromJson(responseString, ResponseInterface.class);
        handleResponse(response, message);
	}
	
	private void handleResponse(ResponseInterface response, String message) throws OperationFailedException {
		
		if(response instanceof Response) {
    		if(((Response) response).response != 100)
    			throw new OperationFailedException(((Response) response).errorMessage);
    		controller.getView().displaySuccess(message);
    	}
		
    	else if(response instanceof OrderID) {
    		int orderId = ((OrderID) response).orderId;
    		if(orderId == -1)
    			throw new OperationFailedException(message);
            controller.getView().displaySuccess("Order successfully placed with ID: " + orderId);
    	}
		
    	else if(response instanceof HistoryResponse) {
    		HistoryResponse historyResponse = (HistoryResponse) response;
    		if (historyResponse != null && historyResponse.history != null)
                displayHistory(historyResponse.history);
            else
                controller.getView().displayError(historyResponse.readHistoryError);
    	}
		
    	else
            throw new OperationFailedException("Received an unrecognized response from the server!");
    }
	
    private void displayHistory(Map<Integer, CandlestickData> history) {
    	StringBuilder sb = new StringBuilder();
        sb.append("\n--- MONTHLY HISTORICAL DATA ---\n");
        history.forEach((day, data) -> {
            sb.append("\n--- Day: ").append(day).append(" ---\n");
            sb.append(data.toString()).append("\n");
        });
        sb.append("------------------------------------------\n");
        controller.getView().displayString(sb.toString());
	}

    /**
     * Handles user input specific to this state.
     *
     * @param choice The user's input.
     * @return true if the connection was lost, false otherwise.
     */
    abstract boolean handleInput(String choice);
    
    /**
     * Displays the menu appropriate for this state.
     */
    abstract void displayMenu();


}
