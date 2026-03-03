package it.cross.server;

import it.cross.server.response.RequestDispatcher;
import it.cross.server.user.Session;
import it.cross.server.user.SessionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles all communication for a single client connection on a dedicated thread.
 * <p>
 * An instance of this class is created by the main server thread for each
 * accepted client socket. It manages the entire lifecycle of the client
 * interaction, from the initial handshake to processing requests and finally
 * closing the connection. It delegates the business logic of processing
 * requests to a {@link RequestDispatcher}.
 */
class Worker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Worker.class);
	private final Socket socket;
	private final int socketTimeout;
	private final RequestDispatcher requestHandler = RequestDispatcher.getInstance();
	private final Session sessionUser = new Session();

	Worker(Socket socket, int socketTimeout) {
		this.socket = socket;
		this.socketTimeout = socketTimeout;
	}

	@Override
	public void run() {

		try { socket.setSoTimeout(socketTimeout); } catch (SocketException e) {
			logger.error("Failed to set socket timeout.", e);
			return;
		}

		try (
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			){

			performHandshakes(in, out);
			String clientRequestJson;
			while ((clientRequestJson = in.readLine()) != null) {
				
				try {
					String responseJson = requestHandler.dispatchRequest(clientRequestJson, sessionUser);
					out.println(responseJson);
				} catch (JsonSyntaxException e) {
					logger.warn("Received malformed JSON from client {}: {}", socket.getInetAddress(), clientRequestJson);
				}
				
			}
			
		} catch (SocketTimeoutException e) {
			logger.warn("Connection closed due to inactivity for client {}", socket.getInetAddress());
		} catch (SocketException e) {
			logger.error("Connection is closed for client {}", socket.getInetAddress());
		} catch (Exception e) {
			logger.error("Error during worker execution for client {}", socket.getInetAddress(), e);
		} finally {

			logger.info("Terminating worker for client {}", socket.getInetAddress());
		    SessionManager.getInstance().removeSession(sessionUser.getUsername());
			if (socket != null && !socket.isClosed())
				try { socket.close(); } catch (IOException e) {
					logger.error("Error closing socket!", e);
				}

		}

	}

    /**
     * Executes the initial handshake protocol with the client.
     * It allows the server to receive the client's specific UDP port, which is
     * essential for sending asynchronous notifications back to the client.
     * <p>
     * This protocol ensures that both server and client are ready for communication.
     * It involves:
     * <ol>
     * <li>Sending a "SERVER_READY" message to the client.</li>
     * <li>Receiving the client's UDP port for notification callbacks.</li>
     * <li>Sending a final "HANDSHAKE_OK" to confirm the connection is established.</li>
     * </ol>
     *
     * @param in  The BufferedReader to read messages from the client.
     * @param out The PrintWriter to send messages to the client.
     * @throws IOException if an I/O error occurs during communication.
     * @throws IllegalAccessException if the client provides an invalid UDP port number.
     */
	private void performHandshakes(BufferedReader in, PrintWriter out) throws IOException, IllegalAccessException {
		out.println("SERVER_READY");

		String udpPortString = in.readLine();
		
		/* readLine() returns null when it reaches the "end-of-stream".
		In a network socket connection, the end-of-stream occurs when
		the other end of the connection is closed. */
		if (udpPortString == null)
		    throw new IOException("Client closed the connection during handshake.");

		int udpPort = Integer.parseInt(udpPortString);
		if (udpPort <= 1024 || udpPort >= 65535)
		    throw new IllegalAccessException("Invalid UDP port number received: " + udpPort);
		
		sessionUser.setNotificationEndpoint(socket.getInetAddress(), udpPort);
		out.println("HANDSHAKE_OK");
		logger.info("Handshake completed successfully for client {}", socket.getInetAddress());

	}

}
