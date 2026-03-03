package it.cross.server.response;

import it.cross.server.journal.TradeDB;
import it.cross.server.user.Session;
import it.cross.shared.Trade;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.operation.HistoryRequest;
import it.cross.shared.response.ResponseInterface;
import it.cross.shared.response.history.CandlestickData;
import it.cross.shared.response.history.HistoryResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles requests for historical trade data for a specific month.
 * <p>
 * This handler retrieves raw trade data from the {@link TradeDB}, processes it,
 * and transforms it into a structured candlestick format suitable for client-side
 * charting or analysis.
 */
class GetHistoryHandler extends OperationHandler {

    private final TradeDB tradeDB = TradeDB.getInstance();

	/**
     * Processes an authenticated request for monthly historical data.
     * <p>
     * It fetches the raw list of trades for the requested month from the database,
     * then calls a helper method to aggregate this data into daily candlestick charts.
     *
     * @param request     The request payload, expected to be a {@link HistoryRequest}.
     * @param sessionUser The client's session (used for authentication).
     * @return A {@link HistoryResponse} containing the candlestick data, or {@code null} if an error occurs.
     */
	@Override
	protected ResponseInterface handleAuthenticated(RequestInterface request, Session sessionUser) {
		
		HistoryRequest historyRequest = (HistoryRequest) request;
        
        try {
        	
        	Map<Integer, List<Trade>> rawHistory = tradeDB.getHistory(historyRequest.month);
        	Map<Integer, CandlestickData> candlestickHistory = calculateCandlestickData(rawHistory);
        	return new HistoryResponse(candlestickHistory, null);
            
        } catch(FileNotFoundException e) {
            super.logger.error("History file doesn't exist!");
            return new HistoryResponse(null, "History file doesn't exist!");
        } catch (IOException e) {
            super.logger.error("Error reading trade history for month '{}': {}", historyRequest.month, e);
            return new HistoryResponse(null, "Error reading trade history");
        } catch(Exception e) {
            super.logger.error("Error: {}", e);
            return new HistoryResponse(null, e.getMessage());
        }
	}

	/**
     * Aggregates a list of trades grouped by day into daily candlestick data.
     * <p>
     * For each day's list of trades, this method calculates the four key values
     * required for a candlestick chart:
     * <ul>
     * <li><b>Open:</b> The price of the very first trade of the day.</li>
     * <li><b>Close:</b> The price of the very last trade of the day.</li>
     * <li><b>High:</b> The highest trade price recorded during the day.</li>
     * <li><b>Low:</b> The lowest trade price recorded during the day.</li>
     * </ul>
     * The trades for each day are sorted by timestamp to ensure correct open/close values.
     *
     * @param rawHistory A map where the key is the day of the month and the value is the list of trades for that day.
     * @return A map containing the calculated {@link CandlestickData} for each day.
     */
	private Map<Integer, CandlestickData> calculateCandlestickData(Map<Integer, List<Trade>> rawHistory) {
		
		Map<Integer, CandlestickData> map = new TreeMap<Integer, CandlestickData>();
		rawHistory.forEach((day, trades) -> {
			trades.sort(Comparator.comparingLong(t -> t.timestamp));
			
			int priceMin=Integer.MAX_VALUE, priceMax=0;
			int open = trades.get(0).price;
            int close = trades.get(trades.size() - 1).price;
            
			for(Trade trade: trades) {
				if(trade.price < priceMin)
					priceMin = trade.price;
				if(trade.price > priceMax)
					priceMax = trade.price;
			}
			
			map.put(day, new CandlestickData(open, close, priceMax, priceMin));
			
		});
		
		return map;
		
	}
	
}
