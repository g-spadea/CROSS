package it.cross.server.user;

import it.cross.server.notification.NotificationEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages all active user sessions on the server.
 * <p>
 * This class acts as a central repository to track which users are currently
 * online and their notification endpoints. It is implemented as a
 * thread-safe Singleton using the initialization-on-demand holder idiom.
 */
public class SessionManager {

    private final ConcurrentMap<String, Session> activeSessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    private static class SingletonHolder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    public static SessionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void registerSession(String username, Session session) {
    	if(UserDB.getInstance().findUser(username) == null)
        	throw new IllegalStateException("Attempted to register a session for a non-existent user: " + username);
        activeSessions.put(username, session);
    }

    public void removeSession(String username) {
    	if (username == null)
            return;
        activeSessions.remove(username);
    }

    public NotificationEndpoint getEndpoint(String username) {
        return activeSessions.get(username).getNotificationEndpoint();
    }
}
