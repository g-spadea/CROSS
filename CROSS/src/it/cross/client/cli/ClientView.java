package it.cross.client.cli;

import java.util.Scanner;

/**
 * Manages all console output and user input for the client application.
 *
 * This class is responsible for displaying menus, messages, and errors to the
 * user, as well as providing a centralized {@link Scanner} instance for
 * reading console input. All display methods are synchronized to prevent
 * interleaved output from different threads (e.g., the main thread and the
 * notification listener thread).
 */
public class ClientView {

	private final Scanner scanner = new Scanner(System.in);
	private final String ANSI_RESET = "\u001B[0m";
	private final String ANSI_GREEN = "\u001B[32m";
	private final String ANSI_RED = "\u001B[31m";
	private final String ANSI_YELLOW = "\u001B[33m";
	
	public final synchronized void displayString(String message) {
		System.out.println(message);
	}
    
	public final synchronized void displaySuccess(String message) {
		System.out.println("\n" + ANSI_GREEN + message + ANSI_RESET);
	}
    
    public final synchronized void displayError(String errorMessage) {
        System.err.println("\n" + ANSI_RED + errorMessage + ANSI_RESET);
    }
    
    public final Scanner getScanner() {
        return scanner;
    }
	
    synchronized void displayNotAuthenticatedMenu() {
        printHeader("MAIN MENU (Not Authenticated)");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Update Credentials");
        System.out.println("4. Exit");
    	System.out.print(ANSI_YELLOW + "--> Your choice: " + ANSI_RESET);    
    }

    synchronized void displayAuthenticatedMenu(boolean hasNewNotifications) {
    	printHeader("MAIN MENU (Authenticated)");
    	if(hasNewNotifications)
    		System.out.println(ANSI_GREEN + "[!] New notification received." + ANSI_RESET);
        System.out.println("1. Place a Limit Order");
        System.out.println("2. Place a Market Order");
        System.out.println("3. Place a Stop Order");
        System.out.println("4. Cancel an Order");
        System.out.println("5. View Notification History");
        System.out.println("6. Request Monthly History");
        System.out.println("7. Clear console");
        System.out.println("8. Logout");
    	System.out.print(ANSI_YELLOW + "--> Your choice: " + ANSI_RESET);    
    }
    
    private void printHeader(String title) {
        System.out.println("\n========================================");
        System.out.println("     " + title);
        System.out.println("========================================");
    }
    
    void clearConsole() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            displayError(e.getMessage());
        }
    }

}
