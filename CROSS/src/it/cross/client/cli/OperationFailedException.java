package it.cross.client.cli;

/**
 * A custom exception thrown when a client-initiated operation fails
 * due to a specific error response from the server.
 */
class OperationFailedException extends Exception {

	/**
     * A unique identifier for the serializable version of the class.
     */
	private static final long serialVersionUID = 1L;
	
	OperationFailedException(String message) {
        super(message);
	}

}
