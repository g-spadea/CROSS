package it.cross.server.orderbook;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * A concrete implementation of one side of the order book.
 * <p>
 * This class extends {@link AbstractBookSide} and is made generic by accepting a
 * {@link java.util.Comparator} in its constructor. This allows the same class
 * to be used for both the Ask and Bid sides by simply providing the correct
 * sorting strategy:
 * <ul>
 * <li>For the Ask side (sell orders), a natural order comparator is used to
 * sort prices from lowest to highest.</li>
 * <li>For the Bid side (buy orders), a reverse order comparator is used to
 * sort prices from highest to lowest.</li>
 * </ul>
 */
class BookSide extends AbstractBookSide {

	public BookSide(Comparator<Integer> comparator) {
        super(new TreeMap<>(comparator));
    }

}
