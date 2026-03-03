package it.cross.client.request;

import it.cross.client.cli.ClientView;

/**
 * Defines the contract for the request creation process.
 *
 * Each implementation of this interface represents a specific action
 * that gathers input from the user and constructs a request string
 * to be sent to the server.
 */
public interface RequestCreator {
	String createRequest(ClientView view);
}
