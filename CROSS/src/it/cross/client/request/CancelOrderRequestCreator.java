package it.cross.client.request;

import it.cross.client.cli.ClientView;
import it.cross.shared.request.Operation;
import it.cross.shared.request.Request;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.operation.CancelOrder;

/**
 * A specific creator for building a request to cancel an existing order.
 * It prompts the user for the ID of the order they wish to cancel.
 */
class CancelOrderRequestCreator extends AbstractOrderRequestCreator {

	@Override
	public String createRequest(ClientView view) {
		RequestInterface orderPayload = createOrderPayload(view);
		return RequestFactory.gson.toJson(new Request(Operation.CANCEL_ORDER, orderPayload));
	}
	
	@Override
	protected RequestInterface gatherPayloadInput(ClientView view) {
		int orderId = readInt(view, "Insert orderID: ");
		return new CancelOrder(orderId);
	}

}
