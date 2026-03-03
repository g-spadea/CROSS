package it.cross.server.orderbook;

/**
 * Defines the public contract for the core matching engine of the trading system.
 * <p>
 * Implementations of this interface are responsible for processing incoming orders,
 * handling cancellations, and providing access to active orders in a consistent
 * and transactionally safe manner.
 */
public interface OrderBookInterface {
	
    MatchResult processOrder(Order order);
    
    boolean cancelOrder(int orderId);
	
    Order getOrderById(int orderId);
    
}
