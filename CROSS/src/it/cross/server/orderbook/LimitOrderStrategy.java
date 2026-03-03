package it.cross.server.orderbook;

import java.util.Optional;

class LimitOrderStrategy implements OrderProcessingStrategy {

	/**
     * Processes an incoming limit order.
     * <p>
     * The logic depends on the order type (ASK or BID):
     * <ol>
     * <li><b>For an ASK (sell) order:</b> It checks the Bid side of the book for any
     * resting buy orders with a price greater than or equal to this order's limit price.
     * If matches are found, trades are executed (e.g., "I want to sell for a minimum of 50. 
     * If I find someone buying at 50 or more, I sell immediately. Otherwise, I wait in 
     * orderbook").</li>
     * <li><b>For a BID (buy) order:</b> It checks the Ask side for any resting sell
     * orders with a price less than or equal to this order's limit price. If
     * matches are found, trades are executed (e.g., "I want to buy for a maximum of 50. 
     * If I find someone selling at 50 or less, I buy. Otherwise, I wait in orderbook").</li>
     * </ol>
     * If any part of the limit order remains unfilled after the matching process,
     * it is added to its corresponding side of the book to rest as a passive order.
     *
     * @param order The {@link LimitOrder} to be processed.
     * @param book  The main {@link OrderBook} instance.
     * @param transaction The active transaction context for state tracking.
     * @return A {@link MatchResult} containing any trades generated from the matching process.
     */
	@Override
    public MatchResult process(Order order, OrderBook book, TransactionState transaction) {

		if(!(order instanceof LimitOrder))
			throw new IllegalArgumentException();
		
		LimitOrder limitOrder = (LimitOrder) order;
		MatchResult matched = null;
        Optional<Integer> bestPrice;
        BookSideInterface askSide = book.getAskSide();
        BookSideInterface bidSide = book.getBidSide();

        switch (order.side) {
            case ASK:
                bestPrice = bidSide.getBestPrice();
                if (bestPrice.isPresent() && bestPrice.get() >= limitOrder.price)
                    matched = bidSide.matchOrder(order, transaction);
                if (order.getSize() > 0) {
                	transaction.recordOriginalState(limitOrder);
                    askSide.addOrder(limitOrder);
                }
                break;

            case BID:
                bestPrice = askSide.getBestPrice();
                if (bestPrice.isPresent() && bestPrice.get() <= limitOrder.price)
                    matched = askSide.matchOrder(order, transaction);
                if (order.getSize() > 0) {
                	transaction.recordOriginalState(limitOrder);
                    bidSide.addOrder(limitOrder);
                }   
        }
        
        return matched;
    }

}
