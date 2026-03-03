package it.cross.shared.response.history;

import java.util.Map;

import it.cross.shared.response.ResponseInterface;

/**
 * Contains the response to a history request.
 *
 * It can either contain a map of candlestick data, where the key is the day
 * of the month, or an error message if the data retrieval was unsuccessful.
 */
public class HistoryResponse implements ResponseInterface {
	
	public final Map<Integer, CandlestickData> history;
	public final String readHistoryError;

    public HistoryResponse(Map<Integer, CandlestickData> history, String error) {
        this.history = history;
        this.readHistoryError = error;
    }
}