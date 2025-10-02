package rottenbonestudio.system.DiscordSystem.config;

import java.io.*;
import java.security.SecureRandom;
import java.util.Properties;

public class Config {
	private static final Properties props = new Properties();
	private static File configFile;
	private static final SecureRandom secureRandom = new SecureRandom();

	public static void init(File pluginFolder) {
		configFile = new File(pluginFolder, "bot-config.properties");

		if (!configFile.exists()) {
			try {
				pluginFolder.mkdirs();
				try (FileWriter writer = new FileWriter(configFile)) {
					writer.write("bot.token=\n");
					writer.write("bot.channelId=\n");
					writer.write("api.token=\n");
					writer.write("api.port=8080\n");
				}
			} catch (IOException e) {
				System.err.println("❌ Error al crear bot-config.properties: " + e.getMessage());
				return;
			}
		}

		try (FileInputStream fis = new FileInputStream(configFile)) {
			props.load(fis);
		} catch (IOException e) {
			System.err.println("❌ Error cargando la configuración del bot: " + e.getMessage());
		}

		if (getApiToken() == null || getApiToken().isEmpty()) {
			String token = generateSecureTokenHex(32);
			props.setProperty("api.token", token);
			saveProps();
			System.out.println("✔ API token generado y guardado en bot-config.properties");
		}

		String portStr = props.getProperty("api.port");
		if (portStr == null || portStr.isEmpty()) {
			props.setProperty("api.port", "8080");
			saveProps();
		}
	}

	public static String getToken() {
		return props.getProperty("bot.token");
	}

	public static String getChannelId() {
		return props.getProperty("bot.channelId");
	}

	public static String getApiToken() {
		return props.getProperty("api.token");
	}

	public static int getApiPort() {
		String p = props.getProperty("api.port");
		try {
			return Integer.parseInt(p);
		} catch (Exception e) {
			return 8080;
		}
	}

	private static String generateSecureTokenHex(int bytes) {
		byte[] b = new byte[bytes];
		secureRandom.nextBytes(b);
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (byte value : b) {
			sb.append(String.format("%02x", value));
		}
		return sb.toString();
	}

	private static void saveProps() {
		try (FileOutputStream fos = new FileOutputStream(configFile)) {
			props.store(fos, null);
		} catch (IOException e) {
			System.err.println("❌ Error guardando configuración: " + e.getMessage());
		}
	}

}
