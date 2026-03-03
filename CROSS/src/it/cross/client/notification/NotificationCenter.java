package it.cross.client.notification;

import it.cross.shared.Trade;
import it.cross.shared.response.notification.TradesNotification;

import java.util.LinkedList;
import java.util.List;

/**
 * A singleton class that acts as a central repository for trade
 * notifications received from the server.
 *
 * This class is thread-safe and manages a list of trades received via
 * UDP notifications. It allows different parts of the application to
 * access a unified history of these trades.
 */
public class NotificationCenter {

    private final List<Trade> trades;
    private boolean newNotificationAvailable = false;

    private NotificationCenter() {
        this.trades = new LinkedList<>();
    }
    
    private static class SingletonHolder {
        private static final NotificationCenter INSTANCE = new NotificationCenter();
    }

    public static NotificationCenter getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public boolean hasNewNotifications() {
        return newNotificationAvailable;
    }

    /**
     * Adds a list of trades from a received notification to the history.
     * This method is synchronized to ensure thread-safe access.
     *
     * @param notification The {@link TradesNotification} object containing the trades to add.
     */
    public synchronized void addTradesFromNotification(TradesNotification notification) {
        if (notification != null && notification.trades != null && !notification.trades.isEmpty())
            trades.addAll(0, notification.trades);
        newNotificationAvailable = true;
    }
    
    public synchronized void clearNotification() {
    	trades.clear();
    	newNotificationAvailable=false;
    }

    @Override
    public String toString() {

    	if (trades.isEmpty())
            return "No trade notifications received yet.";
    	
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- NOTIFICATION HISTORY ---\n");
        for (Trade trade : trades)
            sb.append(trade.toString()).append("\n");
        sb.append("-----------------------------");
        newNotificationAvailable = false;
        return sb.toString();
        
    }
}