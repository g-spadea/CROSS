package it.cross.shared.response;

import java.lang.reflect.Type;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import it.cross.shared.response.history.HistoryResponse;

/**
 * Custom deserializer for JSON responses from the server, using Gson.
 *
 * This class analyzes the structure of the response JSON to determine what
 * type of message it is (e.g., a standard response, an order ID, or a history)
 * and then delegates to Gson for deserialization into the correct Java object.
 */
public class ResponseDeserializer implements JsonDeserializer<ResponseInterface> {

	@Override
	public ResponseInterface deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		
		JsonObject jsonObject = json.getAsJsonObject();
		
		if(jsonObject.has("response")) {
			return context.deserialize(json, Response.class);
		}
		else if(jsonObject.has("orderId")) {
			return context.deserialize(json, OrderID.class);
		}
		else if(jsonObject.has("history") || jsonObject.has("readHistoryError")) {
			return context.deserialize(json, HistoryResponse.class);
		}
		else
    		throw new JsonParseException("Could not determine response type");
		
	}
}
