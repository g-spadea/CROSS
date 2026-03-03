package it.cross.server.orderbook;

import java.util.ArrayDeque;
import java.util.Deque;

import it.cross.shared.Trade;

/**
 * Represents all orders resting at a single price point in the order book.
 * <p>
 * This class manages a queue of limit orders, ensuring that they are processed
 * according to time priority (First-In, First-Out).
 * <p>
 * <b>Why ArrayDeque?</b> An {@link ArrayDeque} is used to store the orders as it
 * is the ideal choice for a FIFO queue. It is backed by a resizable array, which
 * provides superior performance for queue operations (adding to the tail, removing
 * from the head) and lower memory overhead compared to a {@link java.util.LinkedList}.
 */
class PriceLevel {
	
	private int priceLevelSize = 0;
	private final Deque<LimitOrder> orderQueue = new ArrayDeque<>();
    
    void addOrder(LimitOrder order) {  	
        orderQueue.add(order);
        priceLevelSize += order.getSize();
    }
    
    boolean cancelOrder(Order order) {
        if (orderQueue.remove(order)) {
            priceLevelSize -= order.getSize();
        	return true;
        }
        return false;
    }
    
    int getPriceLevelSize() {
        return priceLevelSize;
    }

    /**
     * Matches an incoming (aggressor) order against the resting orders in this level's queue.
     * <p>
     * It processes orders from the front of the queue (respecting time priority)
     * until the incoming order is completely filled or the price level runs out
     * of volume. For each partial or full execution, it creates {@link Trade} objects
     * and populates the provided {@link MatchResult}.
     *
     * @param orderToMatch The incoming order to be matched.
     * @param matched      The {@link MatchResult} object to be populated.
     * @param transaction  The active transaction context for state tracking.
     * @return The total size matched at this price level.
     */
    int match(Order orderToMatch, MatchResult matched, TransactionState transaction) {
        
    	int matchSize = 0;
    	while (orderToMatch.getSize() > 0 && priceLevelSize != 0) {
            LimitOrder topOrder = orderQueue.peek();
            int tradeSize = Math.min(orderToMatch.getSize(), topOrder.getSize());

            transaction.recordOriginalState(orderToMatch);
            transaction.recordOriginalState(topOrder);
            topOrder.decreaseSize(tradeSize);
            orderToMatch.decreaseSize(tradeSize);
            matched.addTrade(topOrder.username, createFromOrder(topOrder, tradeSize, topOrder.price));
            matched.addTrade(orderToMatch.username, createFromOrder(orderToMatch, tradeSize, topOrder.price));
            
            priceLevelSize -= tradeSize;
            matchSize += tradeSize;
            if (topOrder.getSize() == 0)
                orderQueue.poll();
        }
    	return matchSize;
    	
    }

    /**
     * Restores a limit order to its pre-transaction state during a rollback.
     * <p>
     * This method is called by {@link AbstractBookSide#restoreOrder}. It finds the
     * corresponding order in the queue and restores its original size, or re-adds
     * the order if it was completely removed during the failed transaction.
     *
     * @param originalOrder The cloned order containing the state to restore.
     * @return The size that was actually restored.
     */
    int restoreOrder(LimitOrder originalOrder) {
    	
    	boolean find = false;
    	int restoreSize = originalOrder.getSize();
    	for(LimitOrder order: orderQueue) {
    		if(order.orderId == originalOrder.orderId) {
    			restoreSize = originalOrder.getSize() - order.getSize();
    			order.increseSize(restoreSize);
    			this.priceLevelSize += restoreSize;
    			find = true;
    			break;
    		}
    	}
    	
    	if(!find) {
    		orderQueue.addFirst(originalOrder);
            priceLevelSize += restoreSize;
    	}
    	
    	return restoreSize;
    }
    
    private Trade createFromOrder(Order order, int size, int price) {
    	return new Trade(
    			order.orderId,
    			order.side,
    			order.orderType,
    			size,
    			price,
    			order.timestamp
    			);
    }


}
