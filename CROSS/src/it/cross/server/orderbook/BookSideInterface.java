package it.cross.server.orderbook;

import java.util.Optional;

/**
 * Defines the contract for one side of the order book (either Ask or Bid).
 * <p>
 * An order book is typically composed of two sides: the Ask side, which contains
 * all sell orders, and the Bid side, which contains all buy orders. This interface
 * standardizes the core operations that can be performed on either side, such as
 * adding a new order, cancelling an existing one, or matching an incoming order
 * against the orders currently on this side.
 */
interface BookSideInterface {
	
    void addOrder(LimitOrder order);

    boolean cancelOrder(LimitOrder order);

    MatchResult matchOrder(Order order, TransactionState transaction);

	Optional<Integer> getBestPrice();
	
	int getSideSize();
	
	void restoreOrder(LimitOrder originalOrder);
		
}
