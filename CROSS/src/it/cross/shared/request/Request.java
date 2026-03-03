package it.cross.shared.request;

/**
 * Represents a generic request sent from the client to the server.
 *
 * This class acts as a wrapper that encapsulates two fundamental pieces of information:
 * <ul>
 * <li>{@code operation}: a string identifying the type of operation
 * (e.g., "login", "insertLimitOrder"), derived from the {@link Operation} enum.</li>
 * <li>{@code values}: an object containing the specific data (payload)
 * required for that operation.</li>
 * </ul>
 * This design allows all requests to be handled polymorphically.
 */
public class Request implements RequestInterface {
	
	public final String operation;
    public final RequestInterface values;

    public Request(Operation operation, RequestInterface values) {
    	this.operation = operation.toString();
    	this.values = values;
    }
}