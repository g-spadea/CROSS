package it.cross.server.notification;

import it.cross.shared.Trade;
import it.cross.shared.response.notification.TradesNotification;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the sending of asynchronous, out-of-band notifications to clients via UDP.
 * <p>
 * It is implemented as a thread-safe Singleton using the initialization-on-demand
 * holder idiom, ensuring a single, globally accessible {@link DatagramSocket}
 * for all outgoing notifications.
 */
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final DatagramSocket socket;
    private static final Gson gson = new GsonBuilder().create();

    private NotificationService() {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            logger.error("FATAL: Could not create the UDP socket for the notification service.", e);
            throw new RuntimeException("Failed to initialize NotificationService", e);
        }
        logger.info("NotificationService initialized!");
    }
    
    private static class SingletonHolder {
        private static final NotificationService INSTANCE = new NotificationService();
    }

    public static NotificationService getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    /**
     * Sends a UDP datagram containing a list of executed trades to a specific client.
     * <p>
     * This method serializes the list of trades into a JSON payload and sends it
     * to the IP address and port that the client provided during the initial
     * handshake. Any exception during the sending process is logged, but not
     * re-thrown, to prevent a notification failure from affecting the main
     * order processing loop.
     *
     * @param trades        The list of {@link Trade} objects to be sent.
     * @param clientAddress The IP address of the target client.
     * @param clientUdpPort The UDP port on which the client is listening.
     */
    public void sendTradeNotification(List<Trade> trades, InetAddress clientAddress, int clientUdpPort) {
        
    	if (trades == null || trades.isEmpty() || clientAddress == null || clientUdpPort <= 0)
            return;
        
    	try {
            TradesNotification notificationPayload = new TradesNotification(trades);
            String jsonMessage = gson.toJson(notificationPayload);
            byte[] buffer = jsonMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, clientUdpPort);
            socket.send(packet);
        } catch (Exception e) {
            logger.error("Error sending UDP notification to {}:{}", clientAddress, clientUdpPort, e);
        }
    	
    }

    public void shutdown() {
        if (socket != null && !socket.isClosed())
            socket.close();
    }
}