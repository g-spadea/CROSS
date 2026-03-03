package it.cross.shared.request.auth;

import it.cross.shared.request.Request;
import it.cross.shared.request.RequestInterface;

/**
 * A marker interface for different types of authentication-related payloads.
 *
 * It allows polymorphic handling of objects like {@link LoginAndRegister} and
 * {@link UpdateCredentials} within the {@link Request} class, distinguishing
 * them from other types of requests, such as trading operations.
 */
public interface AuthInterface extends RequestInterface {}
