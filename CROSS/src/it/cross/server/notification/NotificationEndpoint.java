package it.cross.server.notification;

import java.net.InetAddress;

/**
 * Represents the network endpoint for sending UDP notifications to a client.
 * <p>
 * This class is a simple data holder that encapsulates the IP address and
 * UDP port required to send a datagram packet to a specific client. It is used
 * by the {@link NotificationService} to direct asynchronous trade notifications.
 */
public class NotificationEndpoint {
	
	public final int udpPort;
	public final InetAddress address;
	
	public NotificationEndpoint(int port, InetAddress address) {
		this.udpPort = port;
		this.address = address;
	}
	
}
