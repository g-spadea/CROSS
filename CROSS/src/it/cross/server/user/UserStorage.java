package it.cross.server.user;

import it.cross.shared.request.auth.LoginAndRegister;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the low-level file I/O operations for reading and writing user data.
 * <p>
 * This class is responsible for the physical persistence of the user database
 * to a JSON file. Its primary design goal is safety against data corruption.
 */
class UserStorage {
	
    private static final Logger logger = LoggerFactory.getLogger(UserStorage.class);
	private final String filename;
	private final File userFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type USER_MAP_TYPE = new TypeToken<ConcurrentHashMap<String, LoginAndRegister>>() {}.getType();
    
    UserStorage(String filename){
    	this.filename = filename;
        userFile = new File(filename);
    }
    
    /**
     * Loads the user map from the JSON file on disk.
     * <p>
     * If the file does not exist, it logs a warning and returns an empty map,
     * assuming a fresh server start. If any other I/O error occurs during
     * reading, it is treated as a fatal error, and the application will terminate
     * to prevent running with a potentially corrupted user state.
     *
     * @return A {@link ConcurrentMap} containing the loaded user data.
     */
    ConcurrentMap<String, LoginAndRegister> loadUsers() {
        
    	if (!userFile.exists()) {
            logger.warn("User file '{}' not found. Starting with an empty user database.", filename);    	
    		return new ConcurrentHashMap<>();
    	}
    	
    	ConcurrentMap<String, LoginAndRegister> loadedUsers = null;
        try (FileReader reader = new FileReader(userFile)) {
        	
            loadedUsers = gson.fromJson(reader, USER_MAP_TYPE);

        } catch (FileNotFoundException e) {
        	return new ConcurrentHashMap<>();
		} catch (IOException e) {
            logger.error("FATAL: Could not read user file: {}. The application cannot start safely.", filename, e);
            throw new RuntimeException("Failed to initialize UserDB", e);
		}
        
        return loadedUsers != null ? loadedUsers : new ConcurrentHashMap<>();
        
    }
    
    /**
     * Safely saves the provided user map to the persistent JSON file.
     * <p>
	 * The save operation uses a robust "atomic write" pattern (write-to-temp,
	 * then move) to ensure that the main user data file is never left in a
	 * corrupted state, even in the event of a server crash.
     *
     * @param users The complete map of users to be persisted.
     */
    void saveUsers(ConcurrentMap<String, LoginAndRegister> users) {
        
    	Path originalPath = userFile.toPath();
        Path tempFile = null;
        
        try {
            
        	tempFile = Files.createTempFile("users", ".tmp");
            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                gson.toJson(users, writer);
            }
            
            try {
	        	Files.move(tempFile, originalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	        } catch(AtomicMoveNotSupportedException e) {
                logger.error("The file cannot be moved as an atomic file system operation {}", e.getMessage());
	        	Files.move(tempFile, originalPath, StandardCopyOption.REPLACE_EXISTING);
	        }

        } catch (IOException e) {
            logger.error("Failed to save user database to file: {}", filename, e);
        } finally {
            if (tempFile != null)
                try { Files.deleteIfExists(tempFile); } catch (IOException e) {
                    logger.error("Failed to delete temporary file: {}", tempFile, e);
                }
        }
    }
       
}
