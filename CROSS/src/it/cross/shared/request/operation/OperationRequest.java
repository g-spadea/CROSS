package it.cross.shared.request.operation;

import it.cross.shared.request.RequestInterface;

/**
 * This interface is used to distinguish trading operation payloads
 * (e.g., {@link LimitOrder}, {@link MarketOrder}) from other types of
 * requests, such as authentication requests.
 */
public interface OperationRequest extends RequestInterface{}
