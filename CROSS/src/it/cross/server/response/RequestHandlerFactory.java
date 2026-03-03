package it.cross.server.response;

import it.cross.server.orderbook.OrderBookInterface;
import it.cross.server.user.UserDB;
import it.cross.shared.request.Operation;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory responsible for creating and providing request handler strategies.
 * <p>
 * This class implements the Factory design pattern. It maintains a map that
 * associates each operation string (e.g., "login", "register") with a
 * specific {@link RequestHandlerInterface} implementation capable of handling
 * that operation.
 * <p>
 * This design decouples the main dispatcher ({@link RequestHandler}) from the
 * concrete handler implementations. To add a new operation to the server, one
 * only needs to create a new handler class and register it here, without
 * modifying the dispatcher's logic.
 * <p>
 * It is implemented as a thread-safe Singleton, as the mapping is initialized
 * once and then only read.
 */
public class RequestHandlerFactory {
	
	private static volatile RequestHandlerFactory instance;
    private final Map<Operation, RequestHandlerInterface> handlers = new HashMap<>();

	private RequestHandlerFactory(UserDB dataStore, OrderBookInterface orderbook) {
		handlers.put(Operation.REGISTER, new RegisterHandler(dataStore));
		handlers.put(Operation.LOGIN, new LoginHandler(dataStore));
		handlers.put(Operation.UPDATE_CREDENTIALS, new UpdateCredentialHandler(dataStore));
		handlers.put(Operation.LOGOUT, new LogoutHandler());
		handlers.put(Operation.INSERT_LIMIT_ORDER, new InsertLimitOrderHandler());
		handlers.put(Operation.INSERT_MARKET_ORDER, new InsertMarketOrderHandler());
		handlers.put(Operation.INSERT_STOP_ORDER, new InsertStopOrderHandler());
		handlers.put(Operation.CANCEL_ORDER, new CancelOrderHandler(orderbook));
		handlers.put(Operation.GET_HISTORY, new GetHistoryHandler());
	}
	
	public static RequestHandlerFactory getInstance(OrderBookInterface orderBook) {
        if (instance == null) {
            synchronized (RequestHandlerFactory.class) {
                if (instance == null)
                    instance = new RequestHandlerFactory(UserDB.getInstance(), orderBook);
            }
        }
        return instance;
    }

	public static RequestHandlerFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Factory not initialized. Call getInstance(dataStore, orderBook) first.");
        }
        return instance;
    }

    public RequestHandlerInterface getHandler(Operation operation) {
        return handlers.get(operation);
    }
	
}
