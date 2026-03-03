package it.cross.server.orderbook;

import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;

/**
 * Represents a stop order within the order book.
 * <p>
 * A stop order is a conditional order that becomes a market order only once
 * the market price reaches a specified "stop price". It is typically used to
 * limit losses (stop-loss) or to enter a position once a certain price trend
 * is confirmed. This class holds the trigger price for the condition.
 */
public class StopOrder extends Order {
    public final int price; 
	public StopOrder(int orderId, RequestSide type, OrderType ordertype, int size, int price, long timestamp, String username) {
		super(orderId, type, ordertype, size, timestamp, username);
		this.price = price;
	}
}
