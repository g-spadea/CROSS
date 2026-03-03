package it.cross.shared.request;

/**
 * A marker interface for all request-related objects.
 *
 * This interface defines no methods but serves to create a common type for all
 * possible request payloads (e.g., {@link it.cross.shared.request.auth.LoginAndRegister},
 * {@link it.cross.shared.request.operation.LimitOrder}). This allows them to be
 * handled polymorphically within the {@link Request} class.
 */
public interface RequestInterface {}
