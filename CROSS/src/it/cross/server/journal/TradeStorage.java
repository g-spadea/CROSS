package it.cross.server.journal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import it.cross.shared.Trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the low-level file I/O operations for reading and writing trade history.
 * <p>
 * This class is responsible for the physical persistence of trades. It uses the
 * Gson Streaming API for memory-efficient processing of potentially large JSON
 * history files. All its methods are designed to be robust and crash-safe,
 * particularly the save operation which uses a temporary file and an atomic move
 * to prevent data corruption.
 */
class TradeStorage {

    private static final Logger logger = LoggerFactory.getLogger(TradeStorage.class);
    private static final Gson gson = new Gson();
    private final String fileName;
    
    TradeStorage(String fileName) {
		this.fileName = fileName;
	}
	
    /**
     * Safely appends a list of new trades to the persistent history file.
     * <p>
     * This method implements a crash-safe "atomic write" pattern:
     * <ol>
     * <li>It creates a new temporary file with a unique name.</li>
     * <li>It reads the entire content of the original history file and writes it
     * into the temporary file, then appends the new trades.</li>
     * <li>Only after the temporary file is successfully and completely written,
     * it uses an atomic {@code Files.move} operation to replace the original
     * file with the new one.</li>
     * </ol>
     * This ensures that the main history file is never left in a corrupted state,
     * even if the server crashes mid-operation.
     * <p>
     * <b>Note on Production Systems:</b> This append-only approach is simplified.
     * A real-world system would require more robust algorithms, typical of
     * databases (e.g., Write-Ahead Logging, Commit Precedence Rule, Undo/Redo logs),
     * to ensure perfect consistency between the in-memory order book state and the
     * persistent history. However, implementing such transactional integrity would
     * be overly complex for the scope of this project.
     *
     * @param trades The list of new trades to persist.
     * @throws IOException if any file I/O error occurs.
     */
	void saveTrades(List<Trade> trades) throws IOException {
		
        File originalFile = new File(fileName);
        if (!originalFile.exists()) {
            logger.warn("History file '{}' does not exist. Creating a new one.", fileName);
            try (FileWriter writer = new FileWriter(originalFile)) {
                writer.write("{\n  \"trades\": []\n}");
            }
        }
        
        Path tempFile = null;
        try {
            
        	tempFile = Files.createTempFile("trades", ".tmp");

	        try (JsonReader reader = createReader();
	             JsonWriter writer = new JsonWriter(new FileWriter(tempFile.toFile()))) {
	
	        	writer.setIndent("  ");
	            reader.beginObject();
	            writer.beginObject();
	
	            if (reader.hasNext() && reader.nextName().equals("trades")) {
	                writer.name("trades");
	            } else {
                    throw new IOException("Invalid format in history file: " + fileName);
	            }
	
	            reader.beginArray();
	            writer.beginArray();
	
	            while (reader.peek() != JsonToken.END_ARRAY) {
	                Trade existingTrade = gson.fromJson(reader, Trade.class);
	                gson.toJson(existingTrade, Trade.class, writer);
	            }
	
	            for(Trade trade: trades)
	                gson.toJson(trade, Trade.class, writer);
	
	            writer.endArray();
	            reader.endArray();
	            writer.endObject();
	            reader.endObject();
	            
	        }

	        try {
	        	Files.move(tempFile, originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	        } catch(AtomicMoveNotSupportedException e) {
                logger.warn("The file cannot be moved as an atomic file system operation {}", e);
	        	Files.move(tempFile, originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	        }
        
        } finally {
        	if (tempFile != null)
                try { Files.deleteIfExists(tempFile); } catch (IOException e) {
                    logger.error("Failed to delete temporary file: {}", tempFile, e);
                }
        }
    }
	
	/**
     * Reads the history file and filters trades that occurred within a specific month and year.
     * <p>
     * For memory efficiency, this method uses a {@link JsonReader} to stream the file
     * instead of loading it all at once. It iterates through each trade, checks if its
     * timestamp falls within the requested month, and aggregates the results into a map
     * keyed by the day of the month.
     *
     * @param month A string representing the month and year in "MMYYYY" format (e.g., "082025").
     * @return A map where the key is the day of the month and the value is a list of trades for that day.
     * @throws IOException if there is an error reading the history file.
     */
	Map<Integer, List<Trade>> requestHistory(String month) throws IOException {
		
		Map<Integer, List<Trade>> map = new TreeMap<Integer, List<Trade>>();
		JsonReader reader = createReader();
		
		reader.beginObject();
        
        if (reader.hasNext() && reader.nextName().equals("trades")) {
	        reader.beginArray();
	        
	        while (reader.peek() != JsonToken.END_ARRAY) {
	        	Trade trade = gson.fromJson(reader, Trade.class);
	        	if(isTimestampInMonth(trade.timestamp, month)) {
	        		int day = timestampToDay(trade.timestamp);
	        		map.computeIfAbsent(day, k -> new ArrayList<Trade>())
	        			.add(trade);
	        	}
	        }
	        
        }
        return map;
		
	}
	
	private JsonReader createReader() throws FileNotFoundException {
		return new JsonReader(
				new InputStreamReader(
						new BufferedInputStream(
							new FileInputStream(fileName))));
	}
	
    /**
     * Converts a Unix timestamp (in seconds) to the corresponding day of the month in UTC.
     *
     * @param timestamp The Unix timestamp in seconds.
     * @return The day of the month (1-31).
     */
	private int timestampToDay(long timestamp) {
		Instant instant = Instant.ofEpochSecond(timestamp);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));
        return zonedDateTime.getDayOfMonth();
	}

	/**
     * Checks if a given Unix timestamp falls within the specified month and year.
     *
     * @param timestampSeconds The Unix timestamp in seconds.
     * @param monthYearString  The month and year in "MMYYYY" format.
     * @return {@code true} if the timestamp is within the month, {@code false} otherwise.
     */
	private boolean isTimestampInMonth(long timestampSeconds, String monthYearString) {

		if (monthYearString == null || monthYearString.length() != 6)
			throw new IllegalArgumentException("Invalid format. Expected MMYYYY.");

		try {

			int month = Integer.parseInt(monthYearString.substring(0, 2));
			int year = Integer.parseInt(monthYearString.substring(2, 6));

			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.set(year, month - 1, 1, 0, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long startOfMonth = cal.getTimeInMillis() / 1000L;
			cal.add(Calendar.MONTH, 1);
			long startOfNextMonth = cal.getTimeInMillis() / 1000L;

			return timestampSeconds >= startOfMonth && timestampSeconds < startOfNextMonth;

		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid numeric format in string.", e);
		} catch (Exception e) {
			return false;
		}

	}
	
	
}
