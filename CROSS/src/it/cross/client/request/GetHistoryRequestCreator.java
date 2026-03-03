package it.cross.client.request;

import it.cross.client.cli.ClientView;
import it.cross.shared.request.Operation;
import it.cross.shared.request.Request;
import it.cross.shared.request.operation.HistoryRequest;
import java.util.Scanner;

/**
 * A specific creator for building a request to fetch historical trade data.
 * It prompts the user to enter a month and year in a specific format (MMYYYY)
 * and validates the input before creating the request.
 */
class GetHistoryRequestCreator implements RequestCreator {

    /**
     * Creates the final JSON request string for the get history operation.
     * It handles user interaction to get the required month and year.
     *
     * @param in The {@link Scanner} instance to read user input.
     * @return A JSON string representing the complete get history request.
     */
    @Override
    public String createRequest(ClientView view) {
        String month;
        while (true) {
            view.displayString("\n" + "Enter the month and year in MMYYYY format (e.g., 082025):");
            month = view.getScanner().nextLine();
            if (month != null && month.matches("(?:0[1-9]|1[0-2])\\d{4}"))
                break;
            else
            	view.displayError("Invalid format. Please try again.");
        }
        
        HistoryRequest payload = new HistoryRequest(month);
        return RequestFactory.gson.toJson(new Request(Operation.GET_HISTORY, payload));
    }
}