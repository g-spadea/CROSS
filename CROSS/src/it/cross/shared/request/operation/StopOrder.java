package it.cross.shared.request.operation;

/**
 * Represents a request to place a new stop order.
 *
 * It contains the details for placing the order:
 * - {@code type}: the side of the order (ask/sell or bid/buy).
 * - {@code size}: the quantity of the instrument to be traded.
 * - {@code price}: the activation (trigger) price. The order will be
 * submitted to the market as a market order only when the market price
 * reaches this level.
 */
public class StopOrder implements OperationRequest {

	public final String type;
	public final int size;
	public final int price;
	
	public StopOrder(RequestSide type, int size, int price) {
		this.type = type.getType();
		this.size = size;
		this.price = price;
	}
}
