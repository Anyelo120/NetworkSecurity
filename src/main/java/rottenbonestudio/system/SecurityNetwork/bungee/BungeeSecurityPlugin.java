package rottenbonestudio.system.SecurityNetwork.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import rottenbonestudio.system.DiscordSystem.DailyPlayerReport;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.ServerStatsProvider;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.BungeeAdminCommand;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.ForceUnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.LinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.TestCommand;
import rottenbonestudio.system.SecurityNetwork.bungee.commands.UnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;
import rottenbonestudio.system.SecurityNetwork.common.config.ConfigMigrator;
import rottenbonestudio.system.SecurityNetwork.common.config.SecurityConfigLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public class BungeeSecurityPlugin extends Plugin {

	private IpCheckManager ipCheckManager;
	private DiscordBot discordBot;
	private DailyPlayerReport dailyReport;

	@Override
	public void onEnable() {
		try {
			saveDefaultConfig();
			new ConfigMigrator(getDataFolder().toPath(), getLogger()::info, getLogger()::warning).run();

			IpCheckConfig config = SecurityConfigLoader.load(new File(getDataFolder(), "config.yml").toPath());
			LangManager.init(getDataFolder().toPath(), config.getLangCode());

			discordBot = new DiscordBot(getDataFolder());
			discordBot.start();
			DiscordConfirmationAPI.init(discordBot);

			dailyReport = new DailyPlayerReport(new ServerStatsProvider() {
				@Override
				public int getOnlinePlayers() {
					return getProxy().getOnlineCount();
				}

				@SuppressWarnings("deprecation")
				@Override
				public int getMaxPlayers() {
					return getProxy().getConfig().getPlayerLimit();
				}
			}, discordBot);
			dailyReport.start();

			ipCheckManager = new IpCheckManager(config, discordBot);
			registerCommandsAndListeners();

			getLogger().info("SecurityNetwork (Bungee) enabled.");
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Could not enable SecurityNetwork: " + e.getMessage(), e);
		}
	}

	@Override
	public void onDisable() {
		if (ipCheckManager != null) {
			ipCheckManager.shutdown();
			ipCheckManager = null;
		}
		if (dailyReport != null) {
			dailyReport.stop();
			dailyReport = null;
		}
		try {
			DiscordConfirmationAPI.shutdown();
		} catch (Exception ignored) {
		}
		if (discordBot != null) {
			try {
				discordBot.stop();
			} catch (Exception e) {
				getLogger().severe("Error stopping DiscordBot: " + e.getMessage());
			}
			discordBot = null;
		}
	}

	private void registerCommandsAndListeners() {
		PluginManager pm = getProxy().getPluginManager();
		pm.registerListener(this, new BungeeLoginListener(this, ipCheckManager));
		pm.registerCommand(this, new LinkDiscordCommand(discordBot));
		pm.registerCommand(this, new UnlinkDiscordCommand(discordBot));
		pm.registerCommand(this, new ForceUnlinkDiscordCommand(this));
		pm.registerCommand(this, new TestCommand(ipCheckManager));
		pm.registerCommand(this, new BungeeAdminCommand(ipCheckManager));
	}

	private void saveDefaultConfig() throws IOException {
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			getDataFolder().mkdirs();
			try (var in = getResourceAsStream("config.yml")) {
				Files.copy(in, configFile.toPath());
			}
		}
	}

	public DiscordBot getDiscordBot() {
		return discordBot;
	}

	public IpCheckManager getIpCheckManager() {
		return ipCheckManager;
	}
}
