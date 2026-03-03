package it.cross.shared.response;

import it.cross.shared.response.history.HistoryResponse;

/**
 * A marker interface for all objects related to a server response.
 *
 * It serves to create a common type for all possible response classes
 * (e.g., {@link Response}, {@link OrderID}, {@link HistoryResponse}),
 * allowing them to be handled polymorphically in the client's code.
 */
public interface ResponseInterface {}
