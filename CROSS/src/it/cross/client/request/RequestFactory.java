package it.cross.client.request;

import it.cross.shared.request.Operation;
import java.util.EnumMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A singleton factory for providing concrete {@link RequestCreator} objects.
 * This class uses the Factory design pattern and pre-initializes all creators
 * for efficiency, storing them in a map for quick retrieval.
 */
public class RequestFactory {

	/**
     * A shared Gson instance for serializing requests to JSON.
     */
	static final Gson gson = new GsonBuilder().create();
	private static final Map<Operation, RequestCreator> creatorRegistry = new EnumMap<>(Operation.class);
    
    private RequestFactory() {
    	creatorRegistry.put(Operation.LOGIN, new LoginRequestCreator());
        creatorRegistry.put(Operation.REGISTER, new RegisterRequestCreator());
        creatorRegistry.put(Operation.UPDATE_CREDENTIALS, new UpdateRequestCreator());
        creatorRegistry.put(Operation.LOGOUT, new LogoutRequestCreator());
        creatorRegistry.put(Operation.INSERT_LIMIT_ORDER, new LimitOrderRequestCreator());
        creatorRegistry.put(Operation.INSERT_MARKET_ORDER, new MarketOrderRequestCreator());
        creatorRegistry.put(Operation.INSERT_STOP_ORDER, new StopOrderRequestCreator());
        creatorRegistry.put(Operation.CANCEL_ORDER, new CancelOrderRequestCreator());
        creatorRegistry.put(Operation.GET_HISTORY, new GetHistoryRequestCreator());
    }
    
    private static class SingletonHolder {
        private static final RequestFactory INSTANCE = new RequestFactory();
    }
    
    public static RequestFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Retrieves the appropriate {@link RequestCreator} for the given operation.
     *
     * @param operation The operation for which to get a request creator.
     * @return The pre-initialized, singleton instance of the {@link RequestCreator}.
     * @throws IllegalArgumentException if no creator is registered for the operation.
     */
    public RequestCreator getRequestCreator(Operation operation) {
        RequestCreator creator = creatorRegistry.get(operation);
        if (creator == null) {
            throw new IllegalArgumentException("No creator registered for operation: " + operation);
        }
        return creator;
    }
}
