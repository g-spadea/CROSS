package it.cross.server.response;

import it.cross.server.user.Session;
import it.cross.shared.request.Operation;
import it.cross.shared.request.Request;
import it.cross.shared.request.RequestDeserializer;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.response.ResponseInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class acts as a central entry point that receives raw JSON strings from
 * the {@link Worker} threads. It uses the Strategy and Factory design patterns
 * to dispatch requests.
 * <p>
 * It is implemented as a thread-safe Singleton to provide a single, efficient
 * point of processing for the entire application.
 */
public class RequestDispatcher {

	private final Gson gson;
    private final RequestHandlerFactory handlerFactory;

	private RequestDispatcher() {
		this.gson = new GsonBuilder()
                .registerTypeAdapter(RequestInterface.class, new RequestDeserializer())
                .create();
        this.handlerFactory = RequestHandlerFactory.getInstance();
	}
	
	private static class SingletonHolder {
        private static final RequestDispatcher INSTANCE = new RequestDispatcher();
    }
	
	public static RequestDispatcher getInstance() {
        return SingletonHolder.INSTANCE;
    }
	
    /**
     * This is the core method of the dispatcher. It is fully thread-safe and can be
     * called concurrently by multiple {@link Worker} threads.
     * <p>
     * 
     * Its responsibilities are:
	 * <ul>
	 * <li>Deserializing the JSON string into a structured {@link RequestInterface} object.</li>
	 * <li>Using a {@link RequestHandlerFactory} to obtain the correct strategy
	 * (an implementation of {@link RequestHandlerInterface}) for the given operation.</li>
	 * <li>Delegating the actual processing to the chosen handler.</li>
	 * <li>Serializing the resulting {@link ResponseInterface} object back into a JSON string
	 * to be sent to the client.</li>
	 * </ul>
	 * 
     * @param jsonString  The raw JSON request string received from the client.
     * @param sessionUser The client's {@link Session} object, containing their state.
     * @return A JSON string representing the response to be sent back to the client.
     * @throws IllegalArgumentException if the request contains an unknown or unsupported operation.
     */
	public String dispatchRequest(String jsonString, Session sessionUser) {
        
		Request request = (Request) gson.fromJson(jsonString, RequestInterface.class);
        RequestHandlerInterface handler = handlerFactory.getHandler(Operation.fromString(request.operation));

        if (handler != null) {
            ResponseInterface responsePayload = handler.handle(request.values, sessionUser);
            return gson.toJson(responsePayload);
        } else
            throw new IllegalArgumentException("Operation not recognized/supported!");
        
    }

}
