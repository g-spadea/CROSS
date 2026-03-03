package it.cross.server.response;

import it.cross.server.user.Session;
import it.cross.shared.request.RequestInterface;
import it.cross.shared.response.ResponseInterface;

/**
 * Defines the contract for a request handler using the Strategy design pattern.
 * <p>
 * Each implementation of this interface represents a specific "strategy" for
 * handling a particular type of client request (e.g., login, register, place order).
 * This allows the main {@link RequestDispatcher} to be decoupled from the specific
 * logic of each operation, making the system extensible and easy to maintain.
 */
interface RequestHandlerInterface {
	ResponseInterface handle(RequestInterface request, Session sessionUser);
}
