package it.cross.shared.request.operation;

/**
 * Represents a request to place a new market order.
 *
 * It contains the essential details for the order:
 * - {@code type}: the side of the order (ask/sell or bid/buy).
 * - {@code size}: the quantity of the instrument to be traded.
 * A market order does not have a specified price, as it is executed
 * immediately at the best available price on the opposite side of the book.
 */
public class MarketOrder implements OperationRequest {

	public final String type;
	public final int size;
	
	public MarketOrder(RequestSide type, int size) {
		this.type = type.getType();
		this.size = size;
	}
}
