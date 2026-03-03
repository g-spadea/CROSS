package it.cross.shared;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages the loading of configuration properties from a file.
 *
 * This class provides a simple mechanism to read key-value pairs from a
 * properties file (e.g., `server.properties`, `client.properties`)
 * located in the application's classpath.
 */
public class Configuration {

	private final Properties properties = new Properties();

	public Configuration(String resourceFileName) throws FileNotFoundException {

		try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourceFileName)) {

			if (input == null)
                throw new FileNotFoundException("Configuration file not found in classpath: " + resourceFileName);
			properties.load(input);

		} catch (IOException e) {
            throw new RuntimeException("Could not load configuration file: " + resourceFileName, e);
		}
	}

	public String getString(String key) {
		String value = properties.getProperty(key);
		return value;
	}

	public Integer getInt(String key) {
		String value = getString(key);
		if (value == null)
			return null;
		return Integer.parseInt(value);
	}

}
