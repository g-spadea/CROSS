package it.cross.shared.request.auth;

/**
 * Represents a logout request from a user.
 *
 * This class contains no fields (it is "empty") because the logout operation
 * does not require additional data; the user's identity is already known to
 * the server through their connection session.
 */
public class Logout implements AuthInterface {}
