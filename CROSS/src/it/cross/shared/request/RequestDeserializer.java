package it.cross.shared.request;

import it.cross.shared.request.auth.LoginAndRegister;
import it.cross.shared.request.auth.Logout;
import it.cross.shared.request.auth.UpdateCredentials;
import it.cross.shared.request.operation.CancelOrder;
import it.cross.shared.request.operation.HistoryRequest;
import it.cross.shared.request.operation.LimitOrder;
import it.cross.shared.request.operation.MarketOrder;
import it.cross.shared.request.operation.StopOrder;
import java.lang.reflect.Type;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Custom deserializer for incoming JSON requests, using Gson.
 *
 * This class is crucial for the request routing mechanism.
 * It reads the "operation" field from the JSON to determine the request type
 * and then delegates to Gson for deserializing the payload ("values") into the
 * correct object (e.g., {@link LoginAndRegister}, {@link LimitOrder}, etc.).
 */
public class RequestDeserializer implements JsonDeserializer<RequestInterface> {

	@Override
    public RequestInterface deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        
		JsonObject jsonObject = json.getAsJsonObject();
        
		if(jsonObject.has("operation")) {
        	
        	final String operation = jsonObject.get("operation").getAsString();
        	final JsonElement values = jsonObject.get("values");
        	RequestInterface request;

        	switch(operation) {
        		case "login":
        			request = context.deserialize(values, LoginAndRegister.class);
        			return new Request(Operation.LOGIN, request);
				case "register":
					request = context.deserialize(values, LoginAndRegister.class);
        			return new Request(Operation.REGISTER, request);
				case "updateCredentials":
					request = context.deserialize(values, UpdateCredentials.class);
					return new Request(Operation.UPDATE_CREDENTIALS, request);
				case "logout":
					request = context.deserialize(values, Logout.class);
					return new Request(Operation.LOGOUT, request);
				case "insertLimitOrder":
					request = context.deserialize(values, LimitOrder.class);
					return new Request(Operation.INSERT_LIMIT_ORDER, request);
				case "insertMarketOrder":
					request = context.deserialize(values, MarketOrder.class);
					return new Request(Operation.INSERT_MARKET_ORDER, request);
				case "insertStopOrder":
	        		request = context.deserialize(values, StopOrder.class);
					return new Request(Operation.INSERT_STOP_ORDER, request);
				case "cancelOrder":
					request = context.deserialize(values, CancelOrder.class);
					return new Request(Operation.CANCEL_ORDER, request);
				case "getPriceHistory":
					request = context.deserialize(values, HistoryRequest.class);
					return new Request(Operation.GET_HISTORY, request);
				default:
	        		throw new JsonParseException("Could not determine request type");
        	}
        }
        
		else
        	throw new JsonParseException("Could not determine request type");
		
	}
}
