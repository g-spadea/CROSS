package it.cross.shared.request.operation;

/**
 * Defines the different types of orders that can be processed by the order book.
 * <p>
 * This enumeration is used to route an incoming order to the correct
 * strategy. It also includes a special type for cancellation operations.
 */
public enum OrderType {
	
	MARKET("market"),
	LIMIT("limit"),
	STOP("stop"),
	CANCEL("cancel");
	
	final String orderType;
	
	private OrderType(String type) {
		this.orderType = type;
	}
	
	public String getType() {
        return orderType;
    }
}
