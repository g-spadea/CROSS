package it.cross.server.orderbook;

import java.util.HashSet;
import java.util.Stack;

/**
 * Manages the state for a single, atomic order book operation.
 * <p>
 * This class ensures that any operation on the {@link OrderBook} is transactional.
 * It uses a combination of a {@link Stack} and a {@link Set} to achieve this:
 * <ul>
 * <li>A <b>Stack</b> stores the original state of modified orders, ensuring that a
 * rollback operation can undo changes in the correct Last-In, First-Out (LIFO) sequence.</li>
 * <li>A <b>Set</b> tracks the IDs of orders already saved in the current transaction,
 * preventing the same order's state from being recorded multiple times.</li>
 * </ul>
 * This "dirty tracking" approach is highly efficient as its memory overhead is
 * proportional to the complexity of the operation, not the size of the entire book.
 */
class TransactionState {

    private final HashSet<Integer> recordedOrderIds = new HashSet<>();
    private final Stack<Order> transitionStack = new Stack<Order>();
    
    void recordOriginalState(Order order) {
    	if(recordedOrderIds.add(order.orderId))
    		transitionStack.push(order.clone());
    }

    void rollback(OrderBook book) {
        while(!transitionStack.empty())
            book.restoreOrder(transitionStack.pop());
    }
}