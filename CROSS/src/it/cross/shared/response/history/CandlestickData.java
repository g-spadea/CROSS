package it.cross.shared.response.history;

/**
 * Represents aggregated data for a trading period in the format of
 * candlestick.
 *
 * This class contains the four fundamental values:
 * - {@code open}: the opening price of the period.
 * - {@code close}: the closing price of the period.
 * - {@code high}: the highest price reached during the period.
 * - {@code low}: the lowest price reached during the period.
 */
public class CandlestickData {
    public final int open;
    public final int close;
    public final int high;
    public final int low;

    public CandlestickData(int open, int close, int high, int low) {
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
    }

    @Override
    public String toString() {
        return String.format("Open: %d, Close: %d, Max: %d, Min: %d", open, close, high, low);
    }
}
