package it.cross.shared.request;

/**
 * Defines all possible operations that a client can request from the server.
 *
 * Using an enum ensures consistency and prevents errors related to misspelled
 * strings by providing a fixed, controlled set of commands.
 */
public enum Operation {
	
    LOGIN("login"),
    REGISTER("register"),
    UPDATE_CREDENTIALS("updateCredentials"),
    LOGOUT("logout"),
	INSERT_LIMIT_ORDER("insertLimitOrder"),
	INSERT_MARKET_ORDER("insertMarketOrder"),
	INSERT_STOP_ORDER("insertStopOrder"),
	CANCEL_ORDER("cancelOrder"),
	GET_HISTORY("getPriceHistory");

    private final String jsonValue;

    Operation(String jsonValue) {
        this.jsonValue = jsonValue;
    }
    
    @Override
    public String toString() {
    	return jsonValue;
    }
    
    public static Operation fromString(String text) {
        if (text != null) {
            for (Operation op : Operation.values())
                if (text.equalsIgnoreCase(op.jsonValue))
                    return op;
        }
        throw new IllegalArgumentException("Operation not recognized: " + text);
    }
}
