package it.cross.server;

import it.cross.server.disruptor.OrderBookDisruptor;
import it.cross.server.journal.TradeDB;
import it.cross.server.user.UserDB;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the graceful shutdown of the server.
 * <p>
 * Its primary responsibilities are:
 * <ol>
 * <li>Shutting down all persistent services like {@link UserDB} and {@link TradeDB}
 * to ensure any pending data is saved to disk.</li>
 * <li>Stopping the high-performance {@link OrderBookDisruptor}.</li>
 * <li>Closing the main {@link ServerSocket} to prevent new client connections.</li>
 * <li>Shutting down the client connection thread pool ({@link ExecutorService}),
 * allowing a grace period for active tasks to complete before forcing termination.</li>
 * </ol>
 * This ensures that the server terminates in a clean and predictable state,
 * preventing data loss.
 */
class TerminationHandler extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(TerminationHandler.class);
	private int maxDelay;
	private ExecutorService pool;
	private ServerSocket serverSocket;

	public TerminationHandler(int maxDelay, ExecutorService pool, ServerSocket serverSocket) {
		this.maxDelay = maxDelay;
		this.pool = pool;
		this.serverSocket = serverSocket;
	}

	public void run() {

	    logger.info("Shutdown hook initiated. Shutting down server gracefully...");
		
	    pool.shutdown();
	    try {
	        logger.info("Waiting for active client threads to terminate...");
	    	if (!pool.awaitTermination(maxDelay, TimeUnit.MILLISECONDS)) {
                logger.warn("Pool did not terminate within the grace period. Forcing shutdown.");
	    		pool.shutdownNow();
	    	}
	    } catch (InterruptedException e) {
	    	pool.shutdownNow();
	    	logger.warn("Thread pool shutdown was interrupted. Forcing immediate shutdown.");
	    }
	    
	    try { serverSocket.close(); } catch (IOException e) {
	    	logger.error("Error while closing the server socket. {}", e);
	    }
	    
	    OrderBookDisruptor.getInstance().shutdown();
	    UserDB.getInstance().shutdown();
		TradeDB.getInstance().shutdown();

		logger.info("Server terminated successfully.");
		
	}

}
