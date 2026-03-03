package it.cross.shared.request.operation;

/**
 * Defines the two possible sides of a trading operation:
 * ASK (offer/sell) and BID (demand/buy).
 */
public enum RequestSide {

	ASK("ask"),
	BID("bid");
	
	final String requestType;
	
	private RequestSide(String type) {
		this.requestType = type;
	}
	
	public String getType() {
        return requestType;
    }
}
