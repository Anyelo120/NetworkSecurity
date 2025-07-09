package rottenbonestudio.system.SecurityNetwork.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.BungeeAdminCommand;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.LinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.TestCommand;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.UnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.FilterRule;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.StorageConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.io.File;
import java.io.IOException;

public class BungeeSecurityPlugin extends Plugin {

	private IpCheckManager ipCheckManager;
	private DiscordBot discordBot;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		LangManager.init(getDataFolder().toPath(), getLangCodeFromConfig());

		try {
			File pluginFolder = getDataFolder();
			discordBot = new DiscordBot(pluginFolder);
			discordBot.start();
			DiscordConfirmationAPI.init(discordBot);
			getLogger().info("DiscordBot iniciado correctamente.");
		} catch (Exception e) {
			getLogger().severe("No se pudo iniciar el DiscordBot: " + e.getMessage());
			e.printStackTrace();
		}
		
		getProxy().getPluginManager().registerCommand(this, new LinkDiscordCommand(discordBot));
		getProxy().getPluginManager().registerCommand(this, new UnlinkDiscordCommand(discordBot));

		IpCheckConfig config = loadConfig();
		this.ipCheckManager = new IpCheckManager(config, discordBot);

		PluginManager pm = getProxy().getPluginManager();
		pm.registerListener(this, new BungeeLoginListener(ipCheckManager));
		getProxy().getPluginManager().registerCommand(this, new TestCommand(ipCheckManager));
		getProxy().getPluginManager().registerCommand(this, new BungeeAdminCommand(ipCheckManager));

		getLogger().info("SecurityNetwork (Bungee) activado correctamente.");
	}

	@Override
	public void onDisable() {
		if (discordBot != null && discordBot.getJda() != null) {
			discordBot.getJda().shutdownNow();
		}
	}

	public DiscordBot getDiscordBot() {
		return discordBot;
	}

	private void saveDefaultConfig() {
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				getDataFolder().mkdirs();
				try (var in = getResourceAsStream("config.yml")) {
					java.nio.file.Files.copy(in, configFile.toPath());
				}
			} catch (IOException e) {
				getLogger().severe("No se pudo guardar el archivo de configuración por defecto: " + e.getMessage());
			}
		}
	}

	private IpCheckConfig loadConfig() {
		IpCheckConfig config = new IpCheckConfig();

		try {
			Configuration bungeeConfig = ConfigurationProvider.getProvider(YamlConfiguration.class)
					.load(new File(getDataFolder(), "config.yml"));

			// VPN Y PROXY
			config.proxyCheckApiKey = bungeeConfig.getString("api.proxycheck");
			config.ipQualityScoreApiKey = bungeeConfig.getString("api.ipQualityScore");
			config.ipHubApiKey = bungeeConfig.getString("api.ipHub");

			// GEOLOCALIZADORES
			config.ipInfoApiKey = bungeeConfig.getString("api.ipinfo");
			config.ipGeoApiKey = bungeeConfig.getString("api.ipgeolocation");
			config.webhookUrl = bungeeConfig.getString("webhook.discord");

			getLogger().info("Loaded API keys:");
			getLogger().info("ProxyCheck API Key: " + (config.proxyCheckApiKey != null ? "Loaded" : "Not Set"));
			getLogger().info("ipQualityScore API Key: " + (config.ipQualityScoreApiKey != null ? "Loaded" : "Not Set"));
			getLogger().info("ipHub API Key: " + (config.ipHubApiKey != null ? "Loaded" : "Not Set"));

			getLogger().info("IpInfo API Key: " + (config.ipInfoApiKey != null ? "Loaded" : "Not Set"));
			getLogger().info("IpGeo API Key: " + (config.ipGeoApiKey != null ? "Loaded" : "Not Set"));

			StorageConfig storage = new StorageConfig();
			storage.type = bungeeConfig.getString("storage.type");
			storage.mysqlHost = bungeeConfig.getString("storage.mysql.host");
			storage.mysqlPort = bungeeConfig.getInt("storage.mysql.port");
			storage.mysqlDatabase = bungeeConfig.getString("storage.mysql.database");
			storage.mysqlUser = bungeeConfig.getString("storage.mysql.user");
			storage.mysqlPassword = bungeeConfig.getString("storage.mysql.password");
			config.storage = storage;

			FilterRule countries = new FilterRule();
			countries.mode = bungeeConfig.getString("countries.mode");
			countries.list = bungeeConfig.getStringList("countries.list");
			config.countries = countries;

			FilterRule continents = new FilterRule();
			continents.mode = bungeeConfig.getString("continents.mode");
			continents.list = bungeeConfig.getStringList("continents.list");
			config.continents = continents;

		} catch (IOException e) {
			getLogger().severe("Error al cargar la configuración: " + e.getMessage());
		}

		return config;
	}
	
	private String getLangCodeFromConfig() {
		try {
			Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class)
					.load(new File(getDataFolder(), "config.yml"));
			return config.getString("lang", "es-es");
		} catch (IOException e) {
			getLogger().warning("No se pudo leer 'lang' desde config.yml, usando 'es-es' por defecto.");
			return "es-es";
		}
	}

	public IpCheckManager getIpCheckManager() {
		return ipCheckManager;
	}

}
