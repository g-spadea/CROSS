package it.cross.server.response;

import it.cross.server.disruptor.OrderBookDisruptor;
import it.cross.server.journal.IDLoader;
import it.cross.server.orderbook.Order;
import it.cross.server.orderbook.OrderBookInterface;
import it.cross.server.user.Session;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.request.operation.CancelOrder;
import it.cross.shared.request.operation.LimitOrder;
import it.cross.shared.request.operation.MarketOrder;
import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;
import it.cross.shared.request.operation.StopOrder;
import it.cross.shared.response.OrderID;
import it.cross.shared.response.Response;
import it.cross.shared.response.ResponseInterface;
import it.cross.shared.response.auth.CancelCode;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for handlers that process trading operations.
 * <p>
 * This class uses the Template Method design pattern: it first checks 
 * if the user is authenticated. If not, it rejects the request 
 * immediately. If the user is authenticated, it delegates the
 * specific processing logic to the concrete subclass through the
 * {@link #handleAuthenticated(RequestInterface, Session)} method.
 * <p>
 * The interaction of these subclass with the Disruptor is non-blocking. It returns a
 * {@link java.util.concurrent.CompletableFuture} which allows the worker thread
 * to wait for the result of the operation without blocking the entire system.
 * The response to the client is then built based on the outcome of this future.
 */
abstract class OperationHandler implements RequestHandlerInterface {

    protected final Logger logger = LoggerFactory.getLogger(OperationHandler.class);
    protected final OrderBookDisruptor disruptor = OrderBookDisruptor.getInstance();
    protected final IDLoader idLoader = IDLoader.getInstance();

    @Override
    public ResponseInterface handle(RequestInterface request, Session sessionUser) {
    	if (!sessionUser.isAuthenticated())
        	return new OrderID(-1);
        return handleAuthenticated(request, sessionUser);
    }

    protected abstract ResponseInterface handleAuthenticated(RequestInterface request, Session sessionUser);
}

/**
 * Handles the submission of a new Limit Order.
 * <p>
 * This handler processes a {@link LimitOrder} request, validates its input,
 * and then publishes the order to the Disruptor for asynchronous processing.
 */
class InsertLimitOrderHandler extends OperationHandler {
    @Override
    protected ResponseInterface handleAuthenticated(RequestInterface request, Session sessionUser) {
        
    	LimitOrder orderRequest = (LimitOrder) request;
    	if(orderRequest.size <= 0 || orderRequest.price <= 0)
    		return new OrderID(-1);

    	CompletableFuture<Integer> result = disruptor.getProducer().onData(
    			RequestSide.valueOf(orderRequest.type.toUpperCase()),
    			OrderType.LIMIT,
    			orderRequest.size,
    			orderRequest.price,
    			sessionUser.getUsername()
    			);

    	try {
        	return new OrderID(result.get());
		} catch (Exception e) {
            logger.error("An unexpected error occurred while processing limit order: {}", e);
			return new OrderID(-1);
		}
        
    }
}

/**
 * Handles the submission of a new Market Order.
 * <p>
 * This handler processes a {@link MarketOrder} request, validates that its size
 * is positive and then publishes the order details to the Disruptor for immediate
 * execution at the best available price.
 */
class InsertMarketOrderHandler extends OperationHandler {
	@Override
    protected ResponseInterface handleAuthenticated(RequestInterface request, Session sessionUser) {
        
    	MarketOrder orderRequest = (MarketOrder) request;
        if(orderRequest.size <= 0)
        	return new OrderID(-1);
        
        CompletableFuture<Integer> result = disruptor.getProducer().onData(
            RequestSide.valueOf(orderRequest.type.toUpperCase()),
            OrderType.MARKET,
            orderRequest.size,
            0,
            sessionUser.getUsername()
        );

        try {
			return new OrderID(result.get());
		} catch (Exception e) {
            logger.error("An unexpected error occurred while processing market order: {}", e);
			return new OrderID(-1);
		}
        
    }
}

/**
 * Handles the submission of a new Stop Order.
 * <p>
 * This handler processes a {@link StopOrder} request, validates that its size and
 * trigger price are positive and then publishes the order details to the Disruptor.
 * The order will be held until the market price reaches the specified trigger price.
 */
class InsertStopOrderHandler extends OperationHandler {
    @Override
    protected ResponseInterface handleAuthenticated(RequestInterface request, Session sessionUser) {
        
    	StopOrder orderRequest = (StopOrder) request;
        if(orderRequest.size <= 0 || orderRequest.price <= 0)
        	return new OrderID(-1);
        
        CompletableFuture<Integer> result = disruptor.getProducer().onData(
            RequestSide.valueOf(orderRequest.type.toUpperCase()),
            OrderType.STOP,
            orderRequest.size,
            orderRequest.price,
            sessionUser.getUsername()
        );

        try {
			return new OrderID(result.get());
		} catch (Exception e) {
            logger.error("An unexpected error occurred while processing stop order: {}", e);
			return new OrderID(-1);
		}
        
    }
}

/**
 * Handles the cancellation of a previously submitted order.
 * <p>
 * This handler processes a {@link CancelOrder} request. It performs several
 * validation checks before publishing a special cancellation event to the Disruptor:
 * <ul>
 * <li>It verifies that the order to be cancelled actually exists in the active order book.</li>
 * <li>It ensures that the user attempting to cancel the order is the same user who placed it.</li>
 * </ul>
 * If validation passes, it asynchronously publishes a cancellation event for processing.
 */
class CancelOrderHandler extends OperationHandler {
	
	OrderBookInterface orderbook;
	
	CancelOrderHandler(OrderBookInterface book) {
		this.orderbook = book;
	}
	
    @Override
    protected ResponseInterface handleAuthenticated(RequestInterface request, Session sessionUser) {
        
    	CancelOrder cancelRequest = (CancelOrder) request;
        String currentUsername = sessionUser.getUsername();
        int orderIdToCancel = cancelRequest.orderId;
        Order orderToCancel = orderbook.getOrderById(orderIdToCancel);
        
        if (orderToCancel == null && idLoader.getID() >= orderIdToCancel)
            return new Response(CancelCode.OTHER, "has already been finalized");
        else if(orderToCancel == null)
            return new Response(CancelCode.OTHER, "order does not exist!");
        else if (!orderToCancel.username.equals(currentUsername))
            return new Response(CancelCode.OTHER, "belongs to different user");
        
        CompletableFuture<Integer> result = disruptor.getProducer().publishCancellationEvent(cancelRequest.orderId);
        try {
			if(result.get() != -1)
				return new Response(CancelCode.OK);
			return new Response(CancelCode.OTHER);
		} catch (Exception e) {
            logger.error("An unexpected error occurred while processing cancel order: {}", e);
			return new Response(CancelCode.OTHER);
		}
        
    }
}