package it.cross.server.user;

import java.net.InetAddress;

import it.cross.server.notification.NotificationEndpoint;
import it.cross.shared.request.auth.LoginAndRegister;

/**
 * Represents the state of a single, connected client session.
 * <p>
 * An instance of this class is created for each worker thread and holds all
 * session-specific information for a client, such as their authentication
 * status and the endpoint for UDP notifications. This object is not designed
 * to be thread-safe, as it is intended to be used only by its owning
 * {@link Worker} thread.
 */
public class Session {
    
	private LoginAndRegister authenticatedUser;
	private NotificationEndpoint udpEndpoit;

    public Session() {
        this.authenticatedUser = null;
    }

    public boolean isAuthenticated() {
        return this.authenticatedUser != null;
    }
    
    public String getUsername() {
    	if(isAuthenticated())
    		return authenticatedUser.username;
    	return null;
    }

    public void authenticate(LoginAndRegister user) {
        this.authenticatedUser = user;
    }

    public void invalidate() {
        this.authenticatedUser = null;
    }

	public void setNotificationEndpoint(InetAddress inetAddress, int udpPort) {
		udpEndpoit = new NotificationEndpoint(udpPort, inetAddress);
	}

	public NotificationEndpoint getNotificationEndpoint() {
		return udpEndpoit;
	}
    
}
