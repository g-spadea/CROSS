package it.cross.server.disruptor;

import it.cross.server.orderbook.OrderBookInterface;
import it.cross.server.notification.NotificationService;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the high-performance order processing pipeline using the LMAX Disruptor pattern.
 * <p>
 * This class acts as the central nervous system for the order book. It decouples the
 * network threads (which receive orders) from the single-threaded, performance-critical
 * order matching engine ({@link OrderBookInterface}). By using a lock-free ring buffer,
 * it ensures that all order events are processed sequentially by a single consumer thread,
 * eliminating lock contention and maximizing throughput.
 * <p>
 * Its main responsibilities include:
 * <ul>
 * <li>Configuring and initializing the Disruptor with a ring buffer, an event factory,
 * and an event handler.</li>
 * <li>Providing a thread-safe Singleton instance to ensure a single point of entry
 * to the order processing pipeline.</li>
 * <li>Exposing an {@link OrderEventProducer} to allow producer threads (e.g., Workers)
 * to safely publish order events onto the ring buffer.</li>
 * <li>Managing the lifecycle of the Disruptor, including a clean shutdown process.</li>
 * </ul>
 * This implementation is thread-safe and designed for low-latency, high-throughput scenarios.
 */
public class OrderBookDisruptor {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookDisruptor.class);
    private volatile static OrderBookDisruptor instance;
	private final Disruptor<OrderEvent> disruptor;
    private final RingBuffer<OrderEvent> ringBuffer;
    private final OrderEventProducer producer;
    private final OrderEventHandler handler;

    private OrderBookDisruptor(OrderBookInterface book, int bufferSize, int maxDelay) {
    	disruptor = new Disruptor<>(OrderEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);
    	handler = new OrderEventHandler(book, maxDelay);
    	disruptor.handleEventsWith(handler);
        ringBuffer = disruptor.start();
        producer = new OrderEventProducer(ringBuffer);
        logger.info("Disruptor has been initialized successfully");
    }
    
    public static OrderBookDisruptor getInstance(OrderBookInterface book, int bufferSize, int maxDelay) {
        if (instance == null) {
            synchronized(OrderBookDisruptor.class) {
                if (instance == null) {
                	 /*
                     * The Disruptor's ring buffer size must be a power of 2.
                     * This is a critical design choice for performance. It allows the Disruptor
                     * to calculate the array index by using a fast bitwise AND operation
                     * (e.g., sequence & (bufferSize - 1)) instead of a much slower
                     * modulo operator (sequence % bufferSize).
                     */
                    if (bufferSize <= 0 || (bufferSize & (bufferSize - 1)) != 0)
                        throw new IllegalArgumentException("bufferSize must be a positive power of 2.");
                    instance = new OrderBookDisruptor(book, bufferSize, maxDelay);
                }
            }
        }
        return instance;
    }
    
    public static OrderBookDisruptor getInstance() {
        if (instance == null)
        	throw new IllegalStateException("OrderBookDisruptor has not been initialized. Call getInstance first.");
        return instance;
    }

    public OrderEventProducer getProducer() {
        return producer;
    }

    public void shutdown() {
        try {
            handler.shutdown();
            NotificationService.getInstance().shutdown();
            disruptor.shutdown();
            logger.info("Disruptor has been shutdown successfully");
        } catch (Exception e) {
            logger.error("An error occurred during OrderBookDisruptor shutdown: {}", e);
        }
    }
	
}
