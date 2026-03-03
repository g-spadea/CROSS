package it.cross.server.orderbook;

import it.cross.shared.request.operation.RequestSide;
import java.util.Optional;

/**
 * Processes an incoming stop order.
 * <p>
 * This strategy is conditional. It checks the current market price against
 * the order's stop price to decide on the action:
 * <ol>
 * <li><b>If a market price exists and the stop condition is met</b> (market price
 * is at or above a buy-stop's price, or at or below a sell-stop's price),
 * the stop order is immediately triggered and executed as a market order.</li>
 * <li><b>If the stop condition is not yet met</b>, the order is not sent to the
 * main book. Instead, it is passed to the {@link StopOrderManager}, which will
 * hold it and monitor the market price, triggering it only when the
 * condition is eventually met.</li>
 * </ol>
 *
 * @param order The {@link StopOrder} to be processed.
 * @param book  The main {@link OrderBook} instance.
 * @return A {@link MatchResult} if the order is triggered immediately, otherwise
 * an empty {@code MatchResult} as the order is simply parked.
 */
class StopOrderStrategy implements OrderProcessingStrategy {

	private final StopOrderManager stopOrderManager;

    public StopOrderStrategy(StopOrderManager stopOrderManager) {
        this.stopOrderManager = stopOrderManager;
    }
	
	@Override
	public MatchResult process(Order order, OrderBook book, TransactionState transaction) {

		if(!(order instanceof StopOrder))
			throw new IllegalArgumentException();
        
		StopOrder stopOrder = (StopOrder) order;
        Optional<Integer> currentMarketPrice = book.getLastMarketPrice();
        boolean triggerNow = false;
        
        if (currentMarketPrice.isPresent()) {
            int marketPrice = currentMarketPrice.get();
            int stopPrice = stopOrder.price;
            if (stopOrder.side == RequestSide.BID && marketPrice >= stopPrice)
                triggerNow = true;
            else if (stopOrder.side == RequestSide.ASK && marketPrice <= stopPrice)
                triggerNow = true;
        }
        
        if (triggerNow)
            return book.executeAsMarket(stopOrder, transaction);
        stopOrderManager.addOrder(stopOrder);
        return null;

	}

}
