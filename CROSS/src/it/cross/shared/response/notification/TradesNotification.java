package it.cross.shared.response.notification;

import java.util.List;

import it.cross.shared.Trade;

/**
 * Represents a notification containing a list of executed trades.
 *
 * This class acts as a wrapper for a list of {@link Trade} objects. It is used
 * to serialize trade data into a structured JSON format, ready to be sent to
 * clients via the UDP notification service.
 */
public class TradesNotification {
	public final String notification = "closedTrades";
	public final List<Trade> trades;

    public TradesNotification(List<Trade> trades) {
        this.trades = trades;
    }
}