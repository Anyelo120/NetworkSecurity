package rottenbonestudio.system.SecurityNetwork.spigot;

import javax.security.auth.login.LoginException;

import org.bukkit.plugin.java.JavaPlugin;

import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.FilterRule;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.StorageConfig;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.AdminCommand;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.LinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.TestCommand;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.UnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

public class SpigotSecurityPlugin extends JavaPlugin {

	private IpCheckManager ipCheckManager;
	private DiscordBot discordBot;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		discordBot = new DiscordBot(getDataFolder());
		try {
			discordBot.start();
			DiscordConfirmationAPI.init(discordBot);
			getLogger().info("DiscordBot iniciado correctamente.");
		} catch (LoginException e) {
			getLogger().severe("No se pudo iniciar el DiscordBot: " + e.getMessage());
		}
		
		getCommand("vincular-discord").setExecutor(new LinkDiscordCommand(discordBot));
		getCommand("desvincular-discord").setExecutor(new UnlinkDiscordCommand(discordBot));

		IpCheckConfig config = loadConfig();
		LangManager.init(getDataFolder().toPath(), getConfig().getString("lang", "es-es"));
		
		ipCheckManager = new IpCheckManager(config, discordBot);
		getCommand("ipchecktest").setExecutor(new TestCommand(ipCheckManager));
		getCommand("ipadmin").setExecutor(new AdminCommand(ipCheckManager));

		getServer().getPluginManager().registerEvents(new PlayerJoinListener(ipCheckManager), this);

		getLogger().info("SecurityNetwork activado correctamente.");
	}
	
	public DiscordBot getDiscordBot() {
		return discordBot;
	}

	@Override
	public void onDisable() {
		if (discordBot != null && discordBot.getJda() != null) {
			discordBot.getJda().shutdownNow();
			getLogger().info("DiscordBot detenido.");
		}
	}

	private IpCheckConfig loadConfig() {
		IpCheckConfig config = new IpCheckConfig();

		// VPN Y PROXY
		config.proxyCheckApiKey = getConfig().getString("api.proxycheck");
		config.ipQualityScoreApiKey = getConfig().getString("api.ipQualityScore");
		config.ipHubApiKey = getConfig().getString("api.ipHub");

		// GEOLOCALIZADORES
		config.ipInfoApiKey = getConfig().getString("api.ipinfo");
		config.ipGeoApiKey = getConfig().getString("api.ipgeolocation");
		config.webhookUrl = getConfig().getString("webhook.discord");

		getLogger().info("Loaded API keys:");
		getLogger().info("ProxyCheck API Key: " + (config.proxyCheckApiKey != null ? "Loaded" : "Not Set"));
		getLogger().info("ipQualityScore API Key: " + (config.ipQualityScoreApiKey != null ? "Loaded" : "Not Set"));
		getLogger().info("ipHub API Key: " + (config.ipHubApiKey != null ? "Loaded" : "Not Set"));

		getLogger().info("IpInfo API Key: " + (config.ipInfoApiKey != null ? "Loaded" : "Not Set"));
		getLogger().info("IpGeo API Key: " + (config.ipGeoApiKey != null ? "Loaded" : "Not Set"));

		StorageConfig storage = new StorageConfig();
		storage.type = getConfig().getString("storage.type");
		storage.mysqlHost = getConfig().getString("storage.mysql.host");
		storage.mysqlPort = getConfig().getInt("storage.mysql.port");
		storage.mysqlDatabase = getConfig().getString("storage.mysql.database");
		storage.mysqlUser = getConfig().getString("storage.mysql.user");
		storage.mysqlPassword = getConfig().getString("storage.mysql.password");
		config.storage = storage;

		getLogger().info("Loaded Storage configuration:");
		getLogger().info("Storage Type: " + config.getStorageType());
		getLogger().info("MySQL Host: " + config.getMySQLHost());
		getLogger().info("MySQL Port: " + config.getMySQLPort());
		getLogger().info("MySQL Database: " + config.getMySQLDatabase());
		getLogger().info("MySQL User: " + config.getMySQLUser());

		FilterRule countries = new FilterRule();
		countries.mode = getConfig().getString("countries.mode");
		countries.list = getConfig().getStringList("countries.list");
		config.countries = countries;

		getLogger().info("Loaded Countries configuration:");
		getLogger().info("Countries Mode: " + countries.mode);
		getLogger()
				.info("Blocked Countries: " + (countries.list.isEmpty() ? "None" : String.join(", ", countries.list)));

		FilterRule continents = new FilterRule();
		continents.mode = getConfig().getString("continents.mode");
		continents.list = getConfig().getStringList("continents.list");
		config.continents = continents;

		getLogger().info("Loaded Continents configuration:");
		getLogger().info("Continents Mode: " + continents.mode);
		getLogger().info(
				"Blocked Continents: " + (continents.list.isEmpty() ? "None" : String.join(", ", continents.list)));

		return config;
	}

	public IpCheckManager getIpCheckManager() {
		return ipCheckManager;
	}
}
