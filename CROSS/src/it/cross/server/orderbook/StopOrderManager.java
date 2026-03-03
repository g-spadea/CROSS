package it.cross.server.orderbook;

import it.cross.shared.request.operation.RequestSide;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Manages all resting stop orders that have not yet been triggered.
 * <p>
 * This manager is responsible for holding stop orders off the main order book
 * and continuously monitoring the market price. When a trade occurs, the
 * {@link #update} method is called to check if the new price triggers any
 * of the resting stop orders.
 * <p>
 * It maintains two separate, sorted {@link TreeMap}s for efficient checking:
 * <ul>
 * <li><b>Buy-Stop Orders:</b> Sorted in ascending price order. Triggered when the
 * market price rises to or above their stop price (e.g. Market is at 95.
 * Buy if the price reaches 100 to catch an upward trend.)</li>
 * <li><b>Sell-Stop Orders:</b> Sorted in descending price order. Triggered when
 * the market price falls to or below their stop price. (e.g. Market price is at
 * 105. Sell if the price drops to 100 to limit losses.)</li>
 * </ul>
 * This sorted structure is a key performance optimization, as it allows the
 * manager to check only the most relevant price levels.
 */
class StopOrderManager {

    private final OrderBook mainOrderBook;
    private final TreeMap<Integer, List<StopOrder>> bidStopOrders = new TreeMap<>();
    private final TreeMap<Integer, List<StopOrder>> askStopOrder = new TreeMap<>(Comparator.reverseOrder());

    StopOrderManager(OrderBook mainOrderBook) {
        this.mainOrderBook = mainOrderBook;
    }
    
    /**
     * Attempts to cancel a resting stop order.
     *
     * @param order The stop order to be cancelled.
     * @return {@code true} if the order was found and removed, {@code false} otherwise.
     */
    boolean cancelOrder(StopOrder order) {
        
    	Map<Integer, List<StopOrder>> stopPriceSide = 
        		order.side == RequestSide.BID ? bidStopOrders : askStopOrder;
        
        List<StopOrder> stopPriceLevel = stopPriceSide.get(order.price);
        if (stopPriceLevel != null) {
            if (stopPriceLevel.remove(order)) {
                if (stopPriceLevel.isEmpty())
                    stopPriceSide.remove(order.price);
                return true;
            }
        }
        return false;
        
    }

    void addOrder(StopOrder order) {
        Map<Integer, List<StopOrder>> StopPriceSide = 
        		order.side == RequestSide.BID ? bidStopOrders : askStopOrder;
        StopPriceSide.computeIfAbsent(order.price, k -> new ArrayList<>()).add(order);
    }

    /**
     * Called by the OrderBook after a trade to check for triggered stop orders.
     * <p>
     * This method checks both buy-stop and sell-stop orders against the new
     * market price and executes any that meet their trigger condition.
     *
     * @param newMarketPrice The latest market price from an executed trade.
     * @param transaction    The active transaction context.
     * @return A {@link MatchResult} containing any trades from newly triggered orders.
     */
    MatchResult update(int newMarketPrice, TransactionState transaction) {
        MatchResult triggeredTrades = new MatchResult();
    	triggerOrders(triggeredTrades, newMarketPrice, bidStopOrders, true, transaction);  
        triggerOrders(triggeredTrades, newMarketPrice, askStopOrder, false, transaction);
        return triggeredTrades;
    }

    /**
     * Iterates through a map of stop orders and triggers them if the market
     * price condition is met.
     * <p>
     * Triggered orders are immediately executed as market orders via the main
     * {@code OrderBook}. The use of a sorted map allows the method to efficiently
     * stop checking as soon as a price level is found that does not meet the
     * trigger condition.
     *
     * @param matched        The {@link MatchResult} to be populated with trades.
     * @param marketPrice    The current market price.
     * @param stopPriceSide       The map of stop orders to check (either bid or ask stops).
     * @param isBuyStop      A boolean indicating if the orders are buy-stops or sell-stops.
     * @param transaction    The active transaction context for state tracking.
     */
    private void triggerOrders(
    		MatchResult matched, 
    		int marketPrice, 
    		TreeMap<Integer, List<StopOrder>> stopPriceSide, 
    		boolean isBuyStop, 
    		TransactionState transaction){
    	
    	Iterator<Entry<Integer, List<StopOrder>>> iteratorPriceLevel = stopPriceSide.entrySet().iterator();
        while(iteratorPriceLevel.hasNext()) {
        	Entry<Integer, List<StopOrder>> priceLevel = iteratorPriceLevel.next();
            int stopPrice = priceLevel.getKey();
            boolean conditionMet = isBuyStop ? marketPrice >= stopPrice : marketPrice <= stopPrice;
            
            if (conditionMet) {
            	List<StopOrder> triggeredOrders = priceLevel.getValue();
            	for (StopOrder stopOrder : triggeredOrders)
            		matched.mergeOtherMatch(mainOrderBook.executeAsMarket(stopOrder, transaction));
                iteratorPriceLevel.remove(); 
            } else
                break;
        }
    }
}
