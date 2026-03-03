package it.cross.shared.request.operation;

/**
 * Represents a request to get the trade history for a specific month.
 *
 * It contains a string specifying the month and year of interest in
 * "MMYYYY" format.
 */
public class HistoryRequest implements OperationRequest {
    
	public final String month;

    public HistoryRequest(String month) {
        this.month = month;
    }
}