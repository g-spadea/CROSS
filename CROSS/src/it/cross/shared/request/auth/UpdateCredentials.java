package it.cross.shared.request.auth;

/**
 * Contains the data necessary for a password update operation.
 */
public class UpdateCredentials implements AuthInterface {

	public final String username;
	public final String old_password;
	public final String new_password;
	
	public UpdateCredentials(String username, String old_password, String new_password) {
		this.username = username;
		this.old_password = old_password;
		this.new_password = new_password;
	}
}
