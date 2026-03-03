package it.cross.client.request;

import it.cross.client.cli.ClientView;
import it.cross.shared.request.Operation;
import it.cross.shared.request.Request;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.operation.LimitOrder;
import it.cross.shared.request.operation.RequestSide;

/**
 * A specific creator for building a request to place a new limit order.
 * It prompts the user for the order type (ask/bid), size, and price.
 */
class LimitOrderRequestCreator extends AbstractOrderRequestCreator {

	@Override
	public String createRequest(ClientView view) {
		RequestInterface orderPayload = createOrderPayload(view);
		return RequestFactory.gson.toJson(new Request(Operation.INSERT_LIMIT_ORDER, orderPayload));
	}
	
	@Override
	protected RequestInterface gatherPayloadInput(ClientView view) {
		RequestSide type = readRequestType(view, "Insert type (ask/bid): ");
		int size = readInt(view, "Insert size: ");
		int price = readInt(view, "Insert price: ");
		return new LimitOrder(type, size, price);
	}

}
