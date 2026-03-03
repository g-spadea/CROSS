package it.cross.shared.response;

/**
 * Represents the server's response after placing a new order.
 *
 * It contains the unique ID assigned by the system to the newly placed order.
 * A value of -1 indicates that the order placement was unsuccessful.
 */
public class OrderID implements ResponseInterface {

	public final int orderId;
	
	public OrderID(int orderId) {
		this.orderId = orderId;
	}
}
