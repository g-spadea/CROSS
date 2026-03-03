package it.cross.shared.request.operation;

/**
 * Represents a request to cancel an existing order.
 *
 * It contains the unique identifier (ID) of the order that the client
 * wishes to cancel.
 */
public class CancelOrder implements OperationRequest {

	public final int orderId;
	
	public CancelOrder(int id) {
		this.orderId = id;
	}
}
