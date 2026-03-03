package it.cross.shared.request.operation;

/**
 * Represents a request to place a new limit order.
 *
 * It contains the necessary details to place the order:
 * - {@code type}: the side of the order (ask/sell or bid/buy).
 * - {@code size}: the quantity of the instrument to be traded.
 * - {@code price}: the maximum price (for a buy) or minimum price (for a sell)
 * at which the user is willing to execute the trade.
 */
public class LimitOrder implements OperationRequest {

	public final String type;
	public final int size;
	public final int price;
	
	public LimitOrder(RequestSide type, int size, int price) {
		this.type = type.getType();
		this.size = size;
		this.price = price;
	}
}
