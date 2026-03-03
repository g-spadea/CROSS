package it.cross.shared.request.auth;

/**
 * Contains the basic credentials (username and password) for login
 * and registration operations.
 */
public class LoginAndRegister implements AuthInterface {

	public final String username;
	public final String password;

	public LoginAndRegister(String username, String password) {
		this.username = username;
		this.password = password;
	}
}
