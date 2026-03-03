package it.cross.client.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Manages the network connection to the server.
 *
 * This class encapsulates the logic for establishing a TCP socket connection,
 * performing the initial handshake, and handling the sending and receiving of
 * data. It implements {@link AutoCloseable} to ensure that network resources
 * are automatically and safely closed when used in a try-with-resources block.
 */
public class ServerConnector implements AutoCloseable {

	private Socket socket;
    private final PrintWriter sendToServer;
    private final BufferedReader readFromServer;

    public ServerConnector(String host, int port, int udpPort) throws UnknownHostException, IOException  {
    	this.socket = new Socket(host, port);
        this.sendToServer = new PrintWriter(socket.getOutputStream(), true);
        this.readFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        performHandshake(udpPort);
    }
    
    /**
     * Executes the handshake protocol with the server to exchange UDP port information.
     *
     * @param udpPort The client's UDP port to be sent to the server.
     * @throws IOException if the server's response is unexpected or the connection is dropped.
     */
    private void performHandshake(int udpPort) throws IOException {
        
        String serverResponse = readFromServer.readLine();
        if (serverResponse == null || !serverResponse.equals("SERVER_READY"))
            throw new IOException("Server is not ready or sent an unexpected response: " + serverResponse);
        
        sendToServer.println(udpPort);
        
        serverResponse = readFromServer.readLine();
        if (serverResponse == null || !serverResponse.equals("HANDSHAKE_OK"))
            throw new IOException("Handshake failed. Server response: " + serverResponse);
            
    }

    /**
     * Sends a request to the server and waits for a response.
     *
     * @param requestJson The request formatted as a JSON string.
     * @return The server's response as a string.
     * @throws IOException if the server closes the connection unexpectedly.
     */
    public String sendAndReceive(String requestJson) throws IOException {
    	sendToServer.println(requestJson);
        String response = readFromServer.readLine();
        if (response == null)
            throw new IOException("Server closed the connection unexpectedly.");
        return response;
    }

    /**
     * Closes the connection and all associated resources (streams and socket).
     * This method is called automatically when the object is used in a
     * try-with-resources block.
     *
     * @throws IOException if an I/O error occurs when closing the socket.
     */
    @Override
    public void close() throws IOException {
    	sendToServer.close();
    	readFromServer.close();
    	if (socket != null && !socket.isClosed())
            socket.close();
    }

}
