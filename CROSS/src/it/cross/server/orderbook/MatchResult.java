package it.cross.server.orderbook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.cross.shared.Trade;

/**
 * Represents the outcome of an order matching operation in the order book.
 * <p>
 * It is designed to handle not only an initial match but also any subsequent
 * "cascading" matches that result from the activation of stop orders.
 * <p>
 * Its primary responsibilities include:
 * <ul>
 * <li><b>Grouping Trades by Participant:</b> Trades are stored in a map keyed
 * by the participant's username to facilitate targeted notification delivery.</li>
 * <li><b>Tracking Last Trade Price:</b> It maintains the price of the last
 * executed trade ({@code lastPrice}), which is critical for the
 * {@link StopOrderManager} to check for further stop order triggers.</li>
 * </ul>
 */
public class MatchResult {

    private final Map<String, List<Trade>> tradesByParticipant = new HashMap<>();
    private int lastPrice = -1;
    private List<Trade> trades = null;

    public void addTrade(String username, Trade trade) {
        this.tradesByParticipant
            .computeIfAbsent(username, k -> new ArrayList<>())
            .add(trade);
        this.lastPrice = trade.price;
    }
    
    private void addTrades(String username, List<Trade> tradesToAdd) {
        this.tradesByParticipant
            .computeIfAbsent(username, k -> new ArrayList<>())
            .addAll(tradesToAdd);
        if (!tradesToAdd.isEmpty())
            this.lastPrice = tradesToAdd.get(tradesToAdd.size() - 1).price;
    }
    
    public void mergeOtherMatch(MatchResult result) {
    	if(result != null && result.hasTrades())
    		for(Map.Entry<String, List<Trade>> entry: result.tradesByParticipant.entrySet())
    			addTrades(entry.getKey(), entry.getValue());
    }
        
    public boolean hasTrades() {
        return !tradesByParticipant.isEmpty();
    }
    
    public int getLastPrice() {
    	return lastPrice;
    }
    
    public List<Trade> getAllTrades() {
    	if(trades != null)
    		return trades;
    	
        List<Trade> allTrades = new ArrayList<>();
        for (List<Trade> userTrades : tradesByParticipant.values())
            allTrades.addAll(userTrades);
        this.trades = allTrades;
        return trades;
    }
    
    public Map<String, List<Trade>> getTradesByParticipant() {
        return Collections.unmodifiableMap(tradesByParticipant);
    }
    
}
