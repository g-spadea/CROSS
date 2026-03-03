package it.cross.server.orderbook;

import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;

/**
 * Represents a limit order within the order book.
 * <p>
 * A limit order is an order to buy or sell at a specific price or better.
 * It will rest on the book until it is either matched or cancelled.
 * This class extends the base {@link Order} and adds the specific
 * price at which the user wishes to trade.
 */
public class LimitOrder extends Order {
	public final int price; 
    public LimitOrder(int orderId, RequestSide type, OrderType ordertype, int size, int price, long timestamp, String username) {
		super(orderId, type, ordertype, size, timestamp, username);
		this.price = price;
	}  
}
