package it.cross.server.user;

/**
 * A background task responsible for periodically persisting user data to disk.
 * <p>
 * This class implements the {@link Runnable} interface to be executed by a
 * {@link java.util.concurrent.ScheduledExecutorService}. Its sole responsibility
 * is to trigger the save operation on the {@link UserDB} instance.
 * <p>
 * This design decouples the application's main logic from the slower, I/O-bound
 * operation of writing to the file system, improving overall performance.
 */
class SaveUsersTask implements Runnable {

	private final UserDB userDB;
	
	SaveUsersTask(UserDB userDB) {
		this.userDB = userDB;
	}
	
	@Override
	public void run() {
		userDB.saveUsers();
	}

}
