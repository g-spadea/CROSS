package it.cross.server.orderbook;

class MarketOrderStrategy implements OrderProcessingStrategy {

	/**
     * Processes an incoming market order.
     * <p>
     * A market order is designed for immediate execution. This strategy attempts
     * to fill the order by matching it against the resting orders on the opposite
     * side of the book at the best available prices.
     * <p>
     * Before matching, it performs a crucial check: if the total volume on the
     * opposite side is less than the market order's size, the order is rejected
     * entirely by returning {@code null}. This prevents partial fills for this
     * simple market order type.
     *
     * @param order The {@link MarketOrder} to be processed.
     * @param book  The main {@link OrderBook} instance.
     * @param transaction The active transaction context for state tracking.
     * @return A {@link MatchResult} containing the generated trades, or {@code null}
     * if there is insufficient liquidity to fill the order.
     */
	@Override
	public MatchResult process(Order order, OrderBook book, TransactionState transaction) {
		
		if(!(order instanceof MarketOrder) && !(order instanceof StopOrder))
			throw new IllegalArgumentException();
		
		MatchResult matched = null;
		
        switch (order.side) {        
            case ASK:
            	BookSideInterface bidSide = book.getBidSide();
            	if(order.getSize() > bidSide.getSideSize())
    				return null;
            	matched = bidSide.matchOrder(order, transaction);
                break;

            case BID:
            	BookSideInterface askSide = book.getAskSide();
            	if(order.getSize() > askSide.getSideSize())
    				return null;            	
                matched = askSide.matchOrder(order, transaction);
                break;                
        }

        return matched;
	}

}
