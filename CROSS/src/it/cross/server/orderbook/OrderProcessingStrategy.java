package it.cross.server.orderbook;

/**
 * Defines the contract for a strategy that processes a specific type of order.
 * <p>
 * This interface is the core of the Strategy design pattern used within the
 * {@link OrderBook}. Each implementation (e.g., {@link LimitOrderStrategy},
 * {@link MarketOrderStrategy}) encapsulates the unique logic required to handle
 * a particular order type, allowing the {@code OrderBook} to remain decoupled
 * from these specific implementation details.
 */
interface OrderProcessingStrategy {
	MatchResult process(Order order, OrderBook book, TransactionState transaction);
}
