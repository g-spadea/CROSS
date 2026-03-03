package it.cross.server;

import it.cross.server.disruptor.OrderBookDisruptor;
import it.cross.server.journal.IDLoader;
import it.cross.server.journal.TradeDB;
import it.cross.server.orderbook.OrderBook;
import it.cross.server.orderbook.OrderBookInterface;
import it.cross.server.response.RequestHandlerFactory;
import it.cross.server.user.UserDB;
import it.cross.shared.Configuration;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point for the trading server application.
 * <p>
 * This class is responsible for the following key tasks:
 * <ul>
 * <li>Loading server configuration from the {@code server.properties} file.</li>
 * <li>Initializing all core components and services, such as {@link UserDB},
 * {@link TradeDB}, and the {@link OrderBookDisruptor}.</li>
 * <li>Setting up the main {@link ServerSocket} to listen for incoming client connections.</li>
 * <li>Managing a {@link ThreadPoolExecutor} to handle each client connection in a separate
 * {@link Worker} thread.</li>
 * <li>Registering a JVM shutdown hook ({@link TerminationHandler}) to ensure a graceful
 * shutdown of all services.</li>
 * </ul>
 */
public class ServerMain {

	private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
	private static Configuration serverConfig;
	private static Integer port;
	private static Integer socketTimeout;
	private static Integer maxDelay;
	private static Integer maximumPoolSize;
	private static Integer initialDelaySchedule;
	private static Integer rateSchedule;
	private static String userFileName;
	private static String journalFileName;	
	private static Integer ringBufferSize;
	private static ExecutorService tcpRequestPool;
	private static ServerSocket serverSocket;
	private final static OrderBookInterface orderBook = new OrderBook();
	
	public static void main(String[] args) {
		
		try {

			try {
				serverConfig = new Configuration("it/cross/server/server.properties");
	    	}catch(FileNotFoundException e) {
	    		logger.error("Configuration file not found! {}", e);
	        	System.exit(1);
	        }
	    	
			port = serverConfig.getInt("port");
			socketTimeout = serverConfig.getInt("socketTimeout");
			maxDelay = serverConfig.getInt("maxDelay");
			maximumPoolSize = serverConfig.getInt("maximumPoolSize");
	    	initialDelaySchedule = serverConfig.getInt("initialDelaySchedule");
	    	rateSchedule = serverConfig.getInt("rateSchedule");
	    	userFileName = serverConfig.getString("userFileName");
	    	journalFileName = serverConfig.getString("journalFileName");
	    	ringBufferSize = serverConfig.getInt("ringBufferSize");
	    	
	    	if(
	    			port == null ||
	    			socketTimeout == null ||
	    			maxDelay == null ||
	    			maximumPoolSize == null ||
	    			initialDelaySchedule == null ||
	    			rateSchedule == null ||
	    			userFileName == null ||
	    			journalFileName == null ||
	    			ringBufferSize == null
	    			) {
	    		
	    		logger.error("Configuration parameters are missing or invalid. Please check server.properties.");
	        	System.exit(1);
	        	
	    	}
	    	
	    	try {
		    	IDLoader.getInstance(journalFileName);
				TradeDB.getInstance(journalFileName, initialDelaySchedule, rateSchedule, maxDelay);
				UserDB.getInstance(userFileName, initialDelaySchedule, rateSchedule, maxDelay);
				OrderBookDisruptor.getInstance(orderBook, ringBufferSize, maxDelay);
				RequestHandlerFactory.getInstance(orderBook);
	    	} catch(Exception e) {
	            logger.error("Fatal error during server startup. Application will terminate. {}", e);
	            System.exit(1);
	    	}
	    	
			serverSocket = new ServerSocket(port);
			tcpRequestPool = new ThreadPoolExecutor(
					0,
					maximumPoolSize,
					3,
					TimeUnit.MINUTES,
					new SynchronousQueue<Runnable>(),
					new ThreadPoolExecutor.AbortPolicy()
					);

			Runtime.getRuntime().addShutdownHook(
					new TerminationHandler(maxDelay, tcpRequestPool, serverSocket)
					);

			logger.info("Server started. Listening on port: {}", port);

			while (true) {
				
				Socket socket = null;
				try {
					
					socket = serverSocket.accept();
					tcpRequestPool.execute(new Worker(socket, socketTimeout));
					
				}catch (SocketException e) {
				    logger.warn("ServerSocket has been closed, server is shutting down.");
					break;
				}catch(RejectedExecutionException e) {
				    logger.warn("ServerSocket has been closed, server is shutting down.");
				}
			}
			
		} catch (Exception e) {
			
		    logger.error("Fatal error during server startup. Application will terminate. {}", e);
			System.exit(1);
			
		}

	}

}
