package it.cross.server.journal;

import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class ensures that every order in the system receives a unique ID that
 * is greater than any previously used ID, even across server restarts. Upon startup,
 * it reads the specified trade history JSON file to find the highest existing
 * {@code orderId}. It then initializes a thread-safe {@link AtomicInteger}
 * to start generating new IDs from that point onward.
 * <p>
 * * For efficiency and to handle potentially large history files, it uses the
 * <b>Gson Streaming API</b> ({@link JsonReader}).
 * <p>
 * This component is implemented as a thread-safe Singleton to guarantee that there
 * is a single source of truth for order IDs throughout the application.
 * If the history file does not exist, it assumes a fresh start and begins
 * generating IDs from 1.
 */
public class IDLoader {

    private static final Logger logger = LoggerFactory.getLogger(IDLoader.class);
	private final AtomicInteger id;
    private volatile static IDLoader instance;
    
	private IDLoader(String fileName) {

		int lastIDFromFile = 0;

		try (
				JsonReader reader = 
					new JsonReader(
						new InputStreamReader(
								new BufferedInputStream(
										new FileInputStream(fileName))))
			){
			
			lastIDFromFile = readLastID(reader);
			
		} catch (FileNotFoundException e) {
            logger.warn("History file not found at '{}'. Starting order IDs from 1. A new file will be created.", fileName);
		}catch (IOException e) {
            logger.error("FATAL: Could not read history file: {}. The application cannot start safely.", fileName, e);
            throw new RuntimeException("Failed to initialize IDLoader", e);
		}

		this.id = new AtomicInteger(lastIDFromFile);
        logger.info("History file scanned successfull: IDLoader initialized!");

	}
    
	public static IDLoader getInstance(String fileName) {
		if (instance == null) {
			synchronized (IDLoader.class) {
				if (instance == null) {
					if (fileName == null || fileName.trim().isEmpty())
                        throw new IllegalArgumentException("Filename is required for IDLoader initialization.");
					instance = new IDLoader(fileName);
				}
			}
		}
		return instance;
	}
	
	public static IDLoader getInstance() {
        if (instance == null)
            throw new IllegalStateException("IDLoader has not been initialized. Call getInstance first.");
        return instance;
    }
    
    private int readLastID(JsonReader reader) throws IOException {
    	
    	int lastID = 0;
    	
        reader.beginObject();
        if (reader.hasNext() && reader.nextName().equals("trades")) {
	        reader.beginArray();
	        while (reader.hasNext()) {
	        	int tempId = readTradeID(reader);
	        	if(lastID < tempId)
	        		lastID = tempId;
	        }
        }
    	
        return lastID;
        
    }

    /**
     * This method is a key part of the streaming process. Instead of fully parsing
     * the entire trade object into a {@code Trade} class (which would be wasteful),
     * it iterates through the object's fields, reads the integer value only when it
     * finds the "orderId" key, and explicitly skips all other fields using
     * {@code reader.skipValue()}.
     *
     * @param reader The JsonReader positioned at the beginning of a trade object.
     * @return The integer value of the orderId.
     * @throws IOException if there is an error reading from the stream.
     */
	private int readTradeID(JsonReader reader) throws IOException {
		
		int id = 0;
		
		reader.beginObject();
		while (reader.peek() != JsonToken.END_OBJECT) {
			String name = reader.nextName();
            switch (name) {
            	case "orderId":
            		id = reader.nextInt();
            		break;
            	default:
            		reader.skipValue();
            }
		}
		reader.endObject();
		
		return id;
		
	}

	public int getID() {
		return id.get();
	}
	
	public int getNextID() {
		return id.incrementAndGet();
	}
	
}
