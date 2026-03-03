package it.cross.server.disruptor;

import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;
import java.util.concurrent.CompletableFuture;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for publishing events onto the Disruptor's ring buffer.
 * <p>
 * It handles the process of claiming the next available slot (event) in the
 * buffer, populating it with data, and then publishing it for the consumer
 * ({@link OrderEventHandler}) to process.
 */
public class OrderEventProducer {
	
    private static final Logger logger = LoggerFactory.getLogger(OrderEventProducer.class);
	private final RingBuffer<OrderEvent> ringBuffer;

    OrderEventProducer(RingBuffer<OrderEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * Publishes a new order creation event onto the ring buffer.
     * <p>
     * The {@code try-finally} block is essential for this process. It guarantees
     * that {@code ringBuffer.publish()} is <b>always called</b>, even if an unexpected
     * error occurs while populating the event. Without the {@code finally} block,
     * if an error happened, the sequence would be claimed but never published,
     * causing the entire Disruptor pipeline to halt indefinitely.
     *
     * @param clientAddress The IP address of the client for UDP notifications.
     * @param clientUdpPort The UDP port of the client for notifications.
     * @return A {@link CompletableFuture<Boolean>} that will be completed with the
     * result of the operation (true if trades were executed, false otherwise).
     */
    public CompletableFuture<Integer> onData(
    		RequestSide type,
    		OrderType orderType,
    		int size,
    		int price,
    		String username
    		) {
        
        CompletableFuture<Integer> resultFuture = new CompletableFuture<>();
    	long sequence = ringBuffer.next();
        try {
        	
            OrderEvent event = ringBuffer.get(sequence);
            event.setSide(type);
            event.setOrderType(orderType);
            event.setSize(size);
            event.setPrice(price);
            event.setTimestamp(System.currentTimeMillis() / 1000L);
            event.setResultFuture(resultFuture);
            event.setUsername(username);
            
            
        } catch(Exception e){
        	logger.error("Failed to populate event data", e);
            resultFuture.completeExceptionally(e);
        } finally { ringBuffer.publish(sequence); }
        
        return resultFuture;
        
    }
    
    /**
     * Publishes a special event to cancel an existing order.
     *
     * @param orderIdToCancel The ID of the order to be cancelled.
     * @return A {@link CompletableFuture<Boolean>} that will be completed with
     * {@code true} if the cancellation was successful, or {@code false} otherwise.
     */
    public CompletableFuture<Integer> publishCancellationEvent(int orderIdToCancel) {
        
    	CompletableFuture<Integer> resultFuture = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
        	
            OrderEvent event = ringBuffer.get(sequence);
            event.clear();
            event.setOrderType(OrderType.CANCEL);
            event.setOrderId(orderIdToCancel);
            event.setResultFuture(resultFuture);
            
        } catch(Exception e){
        	logger.error("Failed to publish cancel event: {}", e);
            resultFuture.completeExceptionally(e);
        } finally { ringBuffer.publish(sequence); }
        
        return resultFuture;
        
    }

}
