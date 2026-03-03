package it.cross.server.orderbook;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;

/**
 * An abstract representation of an order in the trading system.
 * <p>
 * This class encapsulates the common properties shared by all concrete order
 * types, such as {@link LimitOrder} and {@link MarketOrder}. It tracks both the
 * initial and current size of the order and is clonable to support the
 * order book's transactional rollback mechanism.
 */
abstract public class Order implements Cloneable {

	public final int orderId;
	public final RequestSide side;
	public final OrderType orderType;
	private int size;
	public final int initialSize;
    public final long timestamp;
    public final String username;
    
    Order(
    		int orderId, 
    		RequestSide side, 
    		OrderType ordertype,
    		int size,
    		long timestamp,
    		String username
    		){
    	
    	this.orderId = orderId;
    	this.side = side;
    	this.orderType = ordertype;
    	this.size = size;
    	this.initialSize = size;
    	this.timestamp = timestamp;
    	this.username = username;
    	
    }

	public int getSize() {
		return size;
	}

	public void decreaseSize(int size) {
		this.size -= size;
	}
	
	public void increseSize(int size) {
		this.size += size;
	}
	
	@Override
	public Order clone() {
		try {
			return (Order) super.clone();
		} catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
		}
	}
	
	@Override
    public String toString() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        String formattedDate = sdf.format(new Date(timestamp * 1000L));

        return String.format(
            "Order[ID: %d, Side: %s, Type: %s, Size: %d, Date: %s]",
            orderId, side, orderType, size, formattedDate
        );
    }
    
}
