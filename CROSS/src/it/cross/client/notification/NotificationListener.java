package it.cross.client.notification;

import it.cross.client.cli.ClientView;
import it.cross.shared.response.notification.TradesNotification;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import com.google.gson.Gson;

/**
 * A background task that listens for incoming UDP notifications from the server.
 *
 * This class implements {@link Runnable} and is designed to be executed in a
 * separate daemon thread. It continuously waits for datagram packets on a given
 * socket, deserializes them into {@link TradesNotification} objects, and
 * forwards them to the {@link NotificationCenter}.
 */
public class NotificationListener implements Runnable {

    private final DatagramSocket socket;
    private final ClientView view;
    private static final Gson gson = new Gson();
    private final NotificationCenter notificationCenter = NotificationCenter.getInstance();

    public NotificationListener(DatagramSocket socket, ClientView clientView) {
        this.socket = socket;
        this.view = clientView;
    }

    @Override
    public void run() {
        
    	byte[] buffer = new byte[1024];
        while (!Thread.currentThread().isInterrupted()) {
            try {
                
            	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                TradesNotification notification = gson.fromJson(receivedMessage, TradesNotification.class);
                notificationCenter.addTradesFromNotification(notification);
                                
            } catch (SocketException e) {
                view.displayError("[Listener] Socket closed, terminating listener thread.");
                break;
            } catch (IOException e) {
                view.displayError("[Listener] An I/O error occurred: " + e);
            } catch (Exception e) {
                view.displayError("[Listener] An unexpected error occurred: " + e);
            }
        }
    }
}