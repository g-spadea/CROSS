package it.cross.client.cli;

import java.io.IOException;
import java.net.UnknownHostException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.cross.client.connection.ServerConnector;
import it.cross.client.notification.NotificationCenter;
import it.cross.shared.response.ResponseDeserializer;
import it.cross.shared.response.ResponseInterface;

public class ClientController {

    private SessionState currentState;
    private final ClientView view;
    private final ServerConnector connection;
    private boolean running = true;
    private final Gson gson = new GsonBuilder()
    		.registerTypeAdapter(ResponseInterface.class, new ResponseDeserializer())
    		.create();
    private final NotAuthenticatedState unauthenticated;
    private final AuthenticatedState authenticated;

    public ClientController(ClientView view, ServerConnector connection) {
        this.view = view;
        this.connection = connection;
        this.unauthenticated = new NotAuthenticatedState(this, gson);
        this.authenticated =  new AuthenticatedState(this, gson);
        this.currentState = this.unauthenticated;
    }
    
    ClientView getView() {
    	return view;
    }
    
    ServerConnector getConnection() {
    	return connection;
    }

    /**
     * Changes the current state of the application. This method is called by
     * state objects to transition the controller to a new state.
     *
     * @param state The {@link PossibleState} to transition to.
     */
    void setState(PossibleState state) {
        if(state == PossibleState.AUTHENTICATED) {
        	this.currentState = authenticated;
        	view.clearConsole();
        }
        else if(state == PossibleState.UNAUTHENTICATED) {
        	this.currentState = unauthenticated;
        	NotificationCenter.getInstance().clearNotification();
        	view.clearConsole();
        }
        else
            throw new IllegalArgumentException("Unsupported state provided!");
    }
    
    /**
     * Stops the main application loop, effectively terminating the client.
     */
    public void stop() {
        this.running = false;
    }

    /**
     * Starts and manages the main application loop.
     * <p>
     * The loop continuously displays the menu for the current state,
     * waits for user input, and delegates the input handling to the
     * current state object.
     *
     * @return true if the loop was exited due to a connection loss, false for a normal exit.
     */
    public boolean run() throws UnknownHostException, IOException {  
		while (running) {
            currentState.displayMenu();
            String choice = view.getScanner().nextLine();
            if(currentState.handleInput(choice))
            	return true;
        }
		return false;
    }
    
}
