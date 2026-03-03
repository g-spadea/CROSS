package it.cross.server.user;

import it.cross.shared.request.auth.LoginAndRegister;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the user database, providing a thread-safe in-memory cache
 * with a background persistence mechanism.
 * <p>
 * This class acts as the single source of truth for user account information.
 * It loads users from a JSON file on startup and uses a background scheduler
 * to periodically save any changes back to the file.
 */
public class UserDB {

    private static final Logger logger = LoggerFactory.getLogger(UserDB.class);
    private volatile static UserDB instance;
    private final ConcurrentMap<String, LoginAndRegister> users;
    private final UserStorage storage;
    private final ScheduledExecutorService scheduler;
    private final int maxDelay;
    
    
    /**
     * A thread-safe flag that indicates whether the in-memory user map contains
     * changes that have not yet been persisted to disk.
     * <p>
     * It is implemented as an {@link AtomicBoolean} to ensure that modifications
     * (writes) from multiple worker threads and reads from the scheduler thread
     * are visible and handled atomically, preventing race conditions without
     * needing external locks on every access. This optimizes performance by
     * ensuring that the slow disk-write operation is only performed when
     * absolutely necessary.
     */
    private final AtomicBoolean isDirty = new AtomicBoolean(false);
	
    private UserDB(String fileName, int initialDelaySchedule, int rateSchedule, int maxDelay) throws RuntimeException{    
    	this.storage = new UserStorage(fileName);
    	this.users = storage.loadUsers();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(new SaveUsersTask(this), initialDelaySchedule, rateSchedule, TimeUnit.MINUTES); 
        this.maxDelay = maxDelay;
        logger.info("UserDB has been initialized successfully.");
    }

    /**
     * This method uses a thread-safe, double-checked locking pattern to ensure
     * that only one instance is created throughout the application's lifecycle.
     *
     * @param fileName             The name of the file for user persistence.
     * @param initialDelaySchedule The delay before the first save task runs.
     * @param rateSchedule         The interval between subsequent save tasks.
     * @return The singleton UserDB instance.
     */
    public static UserDB getInstance(String fileName, int initialDelaySchedule, int rateSchedule, int maxDelay) {
        if (instance == null) {
            synchronized (UserDB.class) {
                if (instance == null) {
					if (fileName == null || fileName.trim().isEmpty() || initialDelaySchedule < 0 || rateSchedule <= 0)
						throw new IllegalArgumentException("A valid filename and positive schedule rates are required for UserDB initialization.");
                    instance = new UserDB(fileName, initialDelaySchedule, rateSchedule, maxDelay);
                }
            }
        }
        return instance;
    }

    public static UserDB getInstance() {
        if (instance == null)
            throw new IllegalStateException("UserDB has not been initialized. Call getInstance(fileName, ...) first.");
        return instance;
    }


    public boolean registerUser(LoginAndRegister credentials) {
        if (users.putIfAbsent(credentials.username, credentials) == null) {
            isDirty.set(true);
            return true;
        }
        return false;
    }

    public LoginAndRegister findUser(String username) {
        return users.get(username);
    }

    public boolean updateUserCredentials(String username, String oldPassword, String newPassword) {
        LoginAndRegister currentUser = users.get(username);
        if (currentUser != null && currentUser.password.equals(oldPassword)) {
            LoginAndRegister newAuth = new LoginAndRegister(username, newPassword);
            if(users.replace(username, currentUser, newAuth)){
                 isDirty.set(true);
                 return true;
            }
        }
       return false;
    }

    /**
     * This method is designed to be lock-free to maximize performance. It uses the
     * atomic {@code compareAndSet} operation on the "dirty" flag to ensure that
     * only one thread will trigger a save operation at any given time.
     * <p>
     * To ensure data consistency during the save, a "snapshot" of the user map
     * is created. This is a <b>shallow copy</b>: it does not duplicate all the user
     * objects (immutable), only the map's structure. Therefore, the temporary impact on memory
     * is minimal, making this a highly efficient approach.
     */
    void saveUsers() {
    	if (isDirty.compareAndSet(true, false)) {
            ConcurrentMap<String, LoginAndRegister> usersToSave = new ConcurrentHashMap<>(this.users);
            storage.saveUsers(usersToSave);
        }
    }

    /**
     * Ensures a graceful shutdown of the UserDB service.
     * <p>
     * This method stops the background scheduler, waits for any in-progress
     * save task to complete, and then performs one final save to ensure that
     * any pending changes are persisted, preventing data loss.
     */
    public void shutdown() {
    	scheduler.shutdown();
        try {
            logger.info("Waiting for UserDB persistence scheduler to terminate...");
            if (!scheduler.awaitTermination(maxDelay, TimeUnit.MILLISECONDS)) {
                logger.warn("Scheduler did not terminate within the grace period. Forcing shutdown.");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Shutdown of the scheduler was interrupted. Forcing shutdown.");
            scheduler.shutdownNow();
        }
        logger.info("OrderBook has been shutdown successfully.");
        saveUsers();
    }
}