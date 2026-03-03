package it.cross.shared;

import it.cross.shared.request.operation.OrderType;
import it.cross.shared.request.operation.RequestSide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Represents a successfully executed trade in the system.
 *
 * This class is a simple data carrier that captures all relevant details 
 * of a trade. It is used for both persistence in the trade history and 
 * for notifications sent to clients.
 */
public class Trade implements Comparable<Trade> {

	public final int orderId;
	public final String side;
	public final String orderType;
	public final int size;
	public final int price;
    public final long timestamp;
    
    public Trade(
    		int orderId, 
    		RequestSide side, 
    		OrderType ordertype,
    		int size,
    		int price,
    		long timestamp
    		){
    	
    	this.orderId = orderId;
    	this.side = side.getType();
    	this.orderType = ordertype.getType();
    	this.size = size;
    	this.price = price;
    	this.timestamp = timestamp;
    	
    }
    
    @Override
    public String toString() {
        // Format the timestamp to make it more readable
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        String formattedDate = sdf.format(new Date(timestamp * 1000L));

        return String.format(
            "Trade[ID: %d, Side: %s, Type: %s, Size: %d, Price: %d, Date: %s]",
            orderId, side, orderType, size, price, formattedDate
        );
    }

    @Override
	public int compareTo(Trade other) {
		return Integer.compare(this.orderId, other.orderId);
	}

}
