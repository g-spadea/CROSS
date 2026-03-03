package it.cross.server.disruptor;

import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;
import java.util.concurrent.CompletableFuture;


/**
 * Represents the data structure that travels through the Disruptor's ring buffer.
 * <p>
 * This class acts as a carrier for all the information required by the
 * {@link OrderEventHandler} to process a single trading operation (like creating
 * or cancelling an order).
 * <p>
 * As a core component of the Disruptor pattern, instances of this class are pre-allocated
 * in the ring buffer and are continuously reused to avoid garbage collection and
 * reduce latency. The {@link #clear()} method is essential for resetting the event's
 * state before it is reused.
 * <p>
 * It also holds a {@link CompletableFuture} to communicate the result of the
 * operation back to the producer thread in an asynchronous, non-blocking way.
 */
class OrderEvent {

    private int orderId;
    private RequestSide side;
    private OrderType orderType;
    private int size;
    private int price;
    private long timestamp;
    private CompletableFuture<Integer> resultFuture;
    private String username;
    
    public void clear() {
        this.orderId = 0;
        this.side = null;
        this.orderType = null;
        this.size = 0;
        this.price = 0;
        this.resultFuture = null;
        this.username = null;
    }
    
    public int getOrderId() { return orderId; }
    public RequestSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public int getSize() { return size; }
    public int getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
    public CompletableFuture<Integer> getResultFuture() { return resultFuture; }
    public String getUsername() { return this.username; }
    
    public void setPrice(int price) { this.price = price; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setSide(RequestSide side) { this.side = side; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public void setSize(int size) { this.size = size; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setResultFuture(CompletableFuture<Integer> resultFuture) { this.resultFuture = resultFuture; }
    public void setUsername(String username) { this.username = username; }
}