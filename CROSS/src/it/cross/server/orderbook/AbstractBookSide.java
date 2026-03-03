package it.cross.server.orderbook;

import it.cross.shared.request.operation.RequestSide;
import java.util.NavigableMap;
import java.util.Optional;

/**
 * Provides a skeletal implementation for one side of an order book (either Ask or Bid).
 * <p>
 * This abstract class contains the common logic and data structures for managing
 * price levels. The core of this implementation is a {@link java.util.NavigableMap}, chosen
 * for its ability to keep price levels sorted, which is essential for efficient
 * order matching and price discovery.
 * <p>
 * <b>Why NavigableMap?</b>
 * The {@code NavigableMap} (typically a {@code TreeMap}) is the ideal data structure
 * for this purpose. It automatically maintains keys (prices) in a sorted order, allowing for:
 * <ul>
 * <li><b>Fast Best Price Lookup:</b> Finding the best price (highest bid or lowest ask)
 * is an extremely fast O(log n) operation using methods like {@code firstEntry()}.</li>
 * <li><b>Efficient Matching:</b> The matching algorithm can iterate through price levels
 * in their natural priority order without needing to sort them first.</li>
 * <li><b>Powerful Navigation:</b> It offers advanced navigation methods beyond a simple
 * {@code SortedMap}, making the implementation of more complex features easier in the future.</li>
 * </ul>
 * Concrete subclasses will provide a specific comparator to define the sorting
 * order (ascending for Ask, descending for Bid).
 */
abstract class AbstractBookSide implements BookSideInterface {

    private final NavigableMap<Integer, PriceLevel> side;
    private int sideSize = 0;
    
    protected AbstractBookSide(NavigableMap<Integer, PriceLevel> bookMap) {
        this.side = bookMap;
    }
    
    @Override
    public void addOrder(LimitOrder order) {
    	side.computeIfAbsent(order.price, k -> new PriceLevel())
            .addOrder(order);
        this.sideSize += order.getSize();
    }
    
    /**
     * Attempts to cancel an existing limit order from this side of the book.
     * <p>
     * It efficiently finds the corresponding price level and removes the specific order.
     * To conserve memory, if the price level becomes empty after the removal, the
     * level itself is removed from the book.
     *
     * @param order The {@link LimitOrder} to be cancelled.
     * @return {@code true} if the order was found and successfully cancelled,
     * {@code false} otherwise.
     */
    @Override
    public boolean cancelOrder(LimitOrder order) {
    	
        PriceLevel level = side.get(order.price);
        
        boolean result = false;
        if (level != null)
        	result = level.cancelOrder(order);
        
        if(result) {
        	this.sideSize -= order.getSize();
            if (level.getPriceLevelSize() == 0)
            	side.remove(order.price);
        }
        
        return result;
        
    }

    /**
	 * Matches an incoming order against the resting orders on this side of the book.
	 * <p>
	 * This implementation iterates through price levels in order of priority.
	 * For a <b>MarketOrder</b>, a match is always attempted as there are no price
	 * constraints. For a <b>LimitOrder</b>, it first verifies via the
	 * {@code isLimitPriceMet} method that the current price level is advantageous
	 * (i.e., at or better than the limit price). If the condition is not met, the
	 * matching process stops.
	 * <p>
	 * The operation continues until the incoming order is fully filled or no more
	 * matchable orders exist. If a price level is completely consumed, it is
	 * removed from the book.
	 *
	 * @param order The incoming {@link Order} to be matched.
	 * @param transaction The active transaction context for state tracking.
	 * @return A non-null {@link MatchResult} object containing the generated trades.
	 * The result will be empty if no matches occurred.
     */
    @Override
    public MatchResult matchOrder(Order order, TransactionState transaction) {
        
    	MatchResult matched = new MatchResult();
        int matchSize = 0;
    	
        final boolean isLimitOrder = order instanceof LimitOrder;
    	while (order.getSize() > 0 && !side.isEmpty()) {
            if(isLimitOrder && !isLimitPriceMet(order))
            	break;
            PriceLevel bestPriceLevel = side.firstEntry().getValue();
            matchSize += bestPriceLevel.match(order, matched, transaction);
            if (bestPriceLevel.getPriceLevelSize() == 0)
                side.pollFirstEntry();
        }
        
        this.sideSize -= matchSize;
        return matched;
        
    }
    
    @Override
    public void restoreOrder(LimitOrder originalOrder) {
    	PriceLevel level = side.computeIfAbsent(originalOrder.price, k -> new PriceLevel());
    	int restoredSize = level.restoreOrder(originalOrder);
    	this.sideSize += restoredSize;
    }
    
    @Override
    public Optional<Integer> getBestPrice() {
        return side.isEmpty() ? Optional.empty() : Optional.of(side.firstKey());
    }
    
    @Override
	public int getSideSize() {
		return sideSize;
	}
    
    private boolean isLimitPriceMet(Order order) {
    	LimitOrder limitOrder = (LimitOrder) order;
        int bestPrice = side.firstKey();
    	boolean isPriceValid = false;
    	if (limitOrder != null)
            isPriceValid = 
            		(limitOrder.side == RequestSide.BID && bestPrice <= limitOrder.price) ||
            		(limitOrder.side == RequestSide.ASK && bestPrice >= limitOrder.price);
    	return isPriceValid;
    }
    
}
