package it.cross.server.journal;

/**
 * This class implements the {@link Runnable} interface so it can be executed
 * by a {@link java.util.concurrent.ScheduledExecutorService}. Its sole
 * responsibility is to trigger the save operation on the {@link TradeDB} instance.
 * <p>
 * This decouples the high-frequency trading logic (which writes trades to an
 * in-memory list) from the slower, I/O-bound operation of writing to the
 * file system, improving overall performance and responsiveness.
 */
class SaveTrade implements Runnable {

	private final TradeDB tradeDB;
	
	SaveTrade(TradeDB tradeDB) {
		this.tradeDB = tradeDB;
	}
    
    @Override
    public void run() {
    	tradeDB.saveTrade();
    }

}
