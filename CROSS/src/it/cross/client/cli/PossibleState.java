package it.cross.client.cli;

/**
 * Defines the possible authentication states of the client application.
 *
 * This enum is used by the {@link ClientController} to manage the application's
 * behavior and available commands based on whether the user is logged in or not.
 */
enum PossibleState {
	UNAUTHENTICATED,
	AUTHENTICATED
}
