package it.cross.server.orderbook;

import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;

/**
 * Represents a market order within the order book.
 * <p>
 * A market order is an order to buy or sell immediately at the best currently
 * available market price.
 * Unlike a limit order, it does not have a specific price; it consumes liquidity
 * from the opposite side of the book as soon as it is processed.
 */
public class MarketOrder extends Order {
	public MarketOrder(int orderId, RequestSide type, OrderType ordertype, int size, long timestamp, String username) {
		super(orderId, type, ordertype, size, timestamp, username);
	}
}
