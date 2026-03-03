package it.cross.server.disruptor;

import it.cross.server.journal.IDLoader;
import it.cross.server.journal.TradeDB;
import it.cross.server.notification.NotificationEndpoint;
import it.cross.server.notification.NotificationService;
import it.cross.server.orderbook.LimitOrder;
import it.cross.server.orderbook.MarketOrder;
import it.cross.server.orderbook.MatchResult;
import it.cross.server.orderbook.Order;
import it.cross.server.orderbook.OrderBookInterface;
import it.cross.server.orderbook.StopOrder;
import it.cross.server.user.SessionManager;
import it.cross.shared.Trade;
import it.cross.shared.request.operation.OrderType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the single-threaded component that processes all events from the
 * ring buffer sequentially. It is the only place in the system where the
 * {@link OrderBookInterface} is mutated, which eliminates the need for
 * locking and ensures deterministic, high-speed order processing.
 * <p>
 * Its primary responsibilities are:
 * <ul>
 * <li>Consuming {@link OrderEvent}s from the ring buffer one at a time.</li>
 * <li>Assigning a unique, sequential ID to each new order.</li>
 * <li>Translating the event data into a concrete {@link Order} object.</li>
 * <li>Submitting the order to the {@link OrderBookInterface} for processing.</li>
 * <li>Handling the results of the operation, including persisting trades and
 * sending notifications via the {@link NotificationService}.</li>
 * <li>Completing or throwing an exception on the {@link CompletableFuture} to signal
 * the outcome back to the original producer thread.</li>
 * </ul>
 */
class OrderEventHandler implements EventHandler<OrderEvent> {

	private static final Logger logger = LoggerFactory.getLogger(OrderEventHandler.class);
	private final OrderBookInterface book;
    private final IDLoader idLoader = IDLoader.getInstance();
    private final ExecutorService postMatchExecutor = Executors.newSingleThreadExecutor();
    private final int maxDelay;
    
	OrderEventHandler(OrderBookInterface orderBook, int maxDelay) {
		book = orderBook;
		this.maxDelay = maxDelay;
	}
	
	/**
     * This method is called by the Disruptor for every event published to the ring buffer.
     * <p>
     * It acts as the main entry point for the consumer. The logic is wrapped in a
     * try-catch block to ensure that any processing exception is caught and
     * propagated back to the producer via the {@link CompletableFuture}.
     * <p>
     * For new orders, it first assigns a sequential ID before processing them, ensuring
     * a perfect correlation between the ID and the execution sequence.
     */
	@Override
	public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
		
        CompletableFuture<Integer> future = event.getResultFuture();
        
        try {
	        
        	if (event.getOrderType() == OrderType.CANCEL) {
                boolean result = book.cancelOrder(event.getOrderId());
                if (future != null)
                    future.complete(result ? 1 : -1);
                return;
        	}
        	
        	int newOrderId = idLoader.getNextID();
            event.setOrderId(newOrderId);
        	
        	boolean hasTrades = tryToExecuteOrder(event);
        	int marketCondition = event.getOrderType() == OrderType.MARKET && !hasTrades ? -1 : newOrderId;
        	if(future != null)
        		future.complete(marketCondition);
        
        } catch (Exception e) {
            logger.error("Exception while processing event for OrderID {}. Propagating to future.", event.getOrderId(), e);
            if (future != null)
                future.completeExceptionally(e);
        }
		
	}
	
	void shutdown() {
        postMatchExecutor.shutdown();
        try {
            if (!postMatchExecutor.awaitTermination(maxDelay, TimeUnit.MILLISECONDS)) {
                logger.warn("Scheduler did not terminate within the grace period. Forcing shutdown.");
            	postMatchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            postMatchExecutor.shutdownNow();
        }
    }
	
    /**
     * This method performs the following steps in sequence:
     * <ol>
     * <li>Creates a concrete {@link Order} instance from the event data.</li>
     * <li>Submits the order to the {@link OrderBookInterface} for matching.</li>
     * <li>If any trades are generated, it persists them via {@link TradeDB}.</li>
     * <li>Finally, it sends a real-time UDP notification of the executed trades
     * to the client via the {@link NotificationService}.</li>
     * </ol>
     * Post-matching operations (persistence and notification) are performed
     * asynchronously to avoid blocking the main Disruptor thread.
     *
     * @param event The event containing the order details.
     * @return {@code true} if at least one trade was executed, {@code false} otherwise.
     */
	private boolean tryToExecuteOrder(OrderEvent event) {
		
		Order order = createOrderFromEvent(event);
		MatchResult result = book.processOrder(order);
        
        if(result == null || !result.hasTrades())
        	return false;
        
        CompletableFuture.runAsync(() -> {
            persistTrades(result);
            notifyParticipants(result);
        }, postMatchExecutor).exceptionally(ex -> {
            logger.error("An error occurred during asynchronous post-match processing: {} ", ex);
            return null;
        });
        
        return true;
		
	}
	
	private void persistTrades(MatchResult result) {
	    List<Trade> allTradesForDB = result.getAllTrades();
	    TradeDB.getInstance().addTrades(allTradesForDB);
	}
	
	private void notifyParticipants(MatchResult result) {
		Set<Entry<String, List<Trade>>> iterator = result.getTradesByParticipant().entrySet();
		for(Map.Entry<String, List<Trade>> entry: iterator) {
    		String username = entry.getKey();
            List<Trade> participantTrades = entry.getValue();
            NotificationEndpoint udpEndpoint = SessionManager.getInstance().getEndpoint(username);
            
            if(participantTrades != null)
            	NotificationService.getInstance().sendTradeNotification(participantTrades, udpEndpoint.address, udpEndpoint.udpPort);
    	}
    }
	
	private Order createOrderFromEvent(OrderEvent event) {
        switch (event.getOrderType()) {
            case LIMIT:
                return new LimitOrder(event.getOrderId(), event.getSide(), event.getOrderType(), event.getSize(), event.getPrice(), event.getTimestamp() ,event.getUsername());
            case MARKET:
                return new MarketOrder(event.getOrderId(), event.getSide(), event.getOrderType(), event.getSize(), event.getTimestamp(), event.getUsername());
            case STOP:
                return new StopOrder(event.getOrderId(), event.getSide(), event.getOrderType(), event.getSize(), event.getPrice(), event.getTimestamp(), event.getUsername());
            default:
            	throw new IllegalArgumentException("Unsupported order type: " + event.getOrderType());
        }
    }
	    
}
