package it.cross.server.orderbook;

import it.cross.shared.Trade;
import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the central order book, serving as the core of the matching engine.
 * <p>
 * This class orchestrates all trading operations. It maintains two distinct sides:
 * <ul>
 * <li><b>Ask Side:</b> Contains all resting sell orders, sorted by price from lowest to highest.</li>
 * <li><b>Bid Side:</b> Contains all resting buy orders, sorted by price from highest to lowest.</li>
 * </ul>
 * It uses the <b>Strategy Pattern</b> to process different order types (Limit, Market, Stop),
 * delegating the specific logic to dedicated {@link OrderProcessingStrategy} implementations.
 * A {@link StopOrderManager} is used to hold and manage stop orders that have not yet been triggered.
 * If any step of processing fails, the entire operation is rolled back to maintain a consistent state.
 * <p>
 * The class is designed to be manipulated by a single thread to ensure data consistency
 * without the need for complex locking mechanisms.
 */
public class OrderBook implements OrderBookInterface {

	private static final Logger logger = LoggerFactory.getLogger(OrderBook.class);
    private final BookSideInterface askSide = new BookSide(Comparator.naturalOrder());
    private final BookSideInterface bidSide = new BookSide(Comparator.reverseOrder());
    private final StopOrderManager stopManager = new StopOrderManager(this);
    private final Map<OrderType, OrderProcessingStrategy> strategies;
    /** A map holding all currently active (unfilled and uncancelled) limit and stop orders. */
    private final Map<Integer, Order> activeOrders = new HashMap<Integer, Order>();
    /** The price of the last executed trade. */
    private Integer lastMarketPrice = null;

    public OrderBook() {
        strategies = new EnumMap<>(OrderType.class);
        strategies.put(OrderType.LIMIT, new LimitOrderStrategy()); 
        strategies.put(OrderType.MARKET, new MarketOrderStrategy());
        strategies.put(OrderType.STOP, new StopOrderStrategy(stopManager));
        logger.info("OrderBook has been initialized successfully.");
    }

    /**
     * Processes a new incoming order within a safe transactional context.
     * <p>
     * This is the main public entry point for all orders. It selects the appropriate
     * {@link OrderProcessingStrategy} and executes it. After the initial match, it
     * calls {@link #handlePostMatching} to manage all consequences, such as triggering
     * cascading stop orders.
     * <p>
     * The entire operation is wrapped in a try-catch block. If any exception occurs,
     * the transaction is rolled back via {@link TransactionState#rollback} to prevent
     * leaving the order book in a corrupted state.
     *
     * @param order The order to be processed.
     * @return A {@link MatchResult} containing all generated trades, or {@code null} if the
     * operation failed and was rolled back.
     */
    public MatchResult processOrder(Order order) {
        
    	TransactionState transaction = new TransactionState();
    	try {
    	
			OrderProcessingStrategy strategy = strategies.get(order.orderType);
		    if (strategy == null)
                throw new IllegalStateException("No strategy for order type: " + order.orderType);
		    if (order.orderType != OrderType.MARKET)
		    	activeOrders.put(order.orderId, order);
		    MatchResult matched = strategy.process(order, this, transaction);
		    handlePostMatching(matched, transaction);
		    return matched;
		    
    	} catch(Exception e) {
            logger.error("Failed to process order ID={}. Rolling back transaction.", order.orderId, e);
            transaction.rollback(this);
    		return null;
    	}
    	
    }
    
    /**
     * Cancels an active limit or stop order from the book within a safe transactional context.
     * <p>
     * This method removes the specified order from the active order map and delegates
     * the removal from the specific book side (for a LimitOrder) or the StopOrderManager.
     * If any part of the operation fails, the transaction is rolled back to ensure the
     * order is restored to its original state.
     *
     * @param orderId The ID of the order to be cancelled.
     * @return {@code true} if the order was found and successfully cancelled,
     * {@code false} otherwise.
     */
    @Override
    public boolean cancelOrder(int orderId) {
        
        Order orderToCancel = activeOrders.get(orderId);
        if (orderToCancel == null)
            return false;
        TransactionState transaction = new TransactionState();
        transaction.recordOriginalState(orderToCancel);
        
        try {
        	activeOrders.remove(orderId);
	        switch(orderToCancel.orderType) {
	        	case LIMIT:
	        		LimitOrder limitOrder = (LimitOrder) orderToCancel;
	                if (limitOrder.side == RequestSide.ASK)
	                    return askSide.cancelOrder(limitOrder);
	                else
	                    return bidSide.cancelOrder(limitOrder);
	                
	        	case STOP:
	        		StopOrder stopOrder = (StopOrder) orderToCancel;
	        		return stopManager.cancelOrder(stopOrder);
	        		
	        	default:
	        		return false;
	        } 
        } catch(Exception e) {
            logger.error("Failed to cancel order ID={}! Rollback transaction...", orderId, e);
        	transaction.rollback(this);
        	return false;
        }
    }
    
    @Override
    public Order getOrderById(int orderId) {
        Order order = activeOrders.get(orderId);
        if(order != null)
        	return order.clone();
        return null;
    }
    
    /**
     * Restores a single order to its pre-transaction state during a rollback.
     * <p>
     * This method is a critical component of the transactional system, called exclusively
     * by {@link TransactionState#rollback} when an operation fails. It re-inserts the
     * original cloned order back into the active orders map and delegates its
     * placement to the appropriate structure (either a {@link BookSide} or the
     * {@link StopOrderManager}).
     *
     * @param originalOrder The cloned order containing the state to be restored.
     */
    void restoreOrder(Order originalOrder) {
		if (originalOrder.orderType == OrderType.MARKET || originalOrder.orderType == OrderType.CANCEL)
			return;

    	activeOrders.put(originalOrder.orderId, originalOrder);
    	if(originalOrder.orderType == OrderType.LIMIT)
    		switch(originalOrder.side) {
    			case ASK:
    				askSide.restoreOrder((LimitOrder) originalOrder);
    				return;
    			case BID:
    				bidSide.restoreOrder((LimitOrder) originalOrder);
    				return;
    		}
    	
    	stopManager.addOrder((StopOrder) originalOrder);
    }
    
    /**
     * Executes an order using the market order strategy without triggering post-match handling.
     * <p>
     * This is a specialized internal method, primarily called by the {@link StopOrderManager}
     * when a stop order is triggered. It purely executes the trade and returns the result,
     * deliberately avoiding a call to {@code handlePostMatching} to prevent recursive loops.
     *
     * @param order The order to be executed as a market order.
     * @param transaction The active transaction context for state tracking.
     * @return A {@link MatchResult} containing the trades from the execution.
     */
    MatchResult executeAsMarket(Order order, TransactionState transaction) {
		OrderProcessingStrategy marketStrategy = strategies.get(OrderType.MARKET);
		MatchResult matched = marketStrategy.process(order, this, transaction);
        return matched;
	}

    Optional<Integer> getLastMarketPrice() {
        return Optional.ofNullable(this.lastMarketPrice);
    }

    BookSideInterface getAskSide() {
        return askSide;
    }

    BookSideInterface getBidSide() {
        return bidSide;
    }
    
    /**
     * Handles all post-trade logic after an initial match occurs.
     * <p>
     * This method is the control center for managing the consequences of a trade. It first
     * processes the results of the initial match and then enters an iterative loop to handle
     * any subsequent "cascading" stop order activations. The loop continues as long as new
     * stop orders are triggered, ensuring that all chained events are fully processed within
     * a single operation.
     *
     * @param initialResult The {@link MatchResult} from the first completed trade.
     * @param transaction The active transaction context for state tracking.
     */
	private void handlePostMatching(MatchResult initialResult, TransactionState transactionState) {
		
		if (initialResult == null || !initialResult.hasTrades())
			return;
		
		this.lastMarketPrice = initialResult.getLastPrice();
		for (Trade trade : initialResult.getAllTrades())
			activeOrders.remove(trade.orderId);
		
		while(true) {
			MatchResult triggeredResult = stopManager.update(this.lastMarketPrice, transactionState);
			if (!triggeredResult.hasTrades())
				break;
			this.lastMarketPrice = triggeredResult.getLastPrice();
			for (Trade trade : triggeredResult.getAllTrades())
				activeOrders.remove(trade.orderId);
			initialResult.mergeOtherMatch(triggeredResult);
		}
		
	}

}