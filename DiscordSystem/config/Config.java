package rottenbonestudio.system.DiscordSystem.config;

import java.io.*;
import java.util.Properties;

public class Config {
	private static final Properties props = new Properties();
	private static File configFile;

	public static void init(File pluginFolder) {
		configFile = new File(pluginFolder, "bot-config.properties");

		if (!configFile.exists()) {
			try {
				pluginFolder.mkdirs();
				configFile.createNewFile();

				try (FileWriter writer = new FileWriter(configFile)) {
					writer.write("# Configuración del bot de Discord\n");
					writer.write("# Obtén tu token en: https://discord.com/developers/applications\n");
					writer.write("# Canal de texto donde se enviarán las solicitudes de vinculación\n");
					writer.write("bot.token=\n");
					writer.write("bot.channelId=\n");
				}

				System.out.println("✔ Archivo bot-config.properties creado en: " + configFile.getAbsolutePath());
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
	}

	public static String getToken() {
		return props.getProperty("bot.token");
	}

	public static String getChannelId() {
		return props.getProperty("bot.channelId");
	}

}
