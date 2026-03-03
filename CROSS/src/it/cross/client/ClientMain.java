package it.cross.client;

import it.cross.client.cli.ClientController;
import it.cross.client.cli.ClientView;
import it.cross.client.connection.ServerConnector;
import it.cross.client.notification.NotificationListener;
import it.cross.shared.Configuration;
import java.net.DatagramSocket;

/**
 * The main entry point for the client application.
 *
 * This class is responsible for initializing the client, connecting to the
 * server, and managing the main application loop. It sets up the necessary
 * components, such as the view and controller, and handles automatic
 * reconnection in case of a lost connection.
 */
public class ClientMain {

	private static String hostname;
	private static Integer port;

	public static void main(String[] args) {

		try {

			Configuration clientConfig = new Configuration("it/cross/client/client.properties");
			hostname = clientConfig.getString("hostname");
			port = clientConfig.getInt("port");
			
			if (hostname == null || port == null) {
				System.err.println("Configuration file not found!");
				System.exit(1);
			}

			while (true) {

				try (DatagramSocket notificationSocket = new DatagramSocket();
						ServerConnector connection = new ServerConnector(hostname, port,
								notificationSocket.getLocalPort());) {

                    // 1. Create the view
					ClientView view = new ClientView();

                    // 2. Create the controller, injecting dependencies
					ClientController controller = new ClientController(view, connection);

					Thread listenerThread = new Thread(new NotificationListener(notificationSocket, view));
					listenerThread.setDaemon(true);
					listenerThread.start();

					// 3. Avvia il controller
					if (!controller.run())
						break;
					else {
                        view.displaySuccess("[CLIENT] Connection lost, automatically reconnecting... ");
						continue;
					}
				}
			}

		} catch (Exception e) {
            e.printStackTrace();
		}

	}

}
