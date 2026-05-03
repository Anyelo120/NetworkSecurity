package rottenbonestudio.system.SecurityNetwork.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import rottenbonestudio.system.DiscordSystem.DailyPlayerReport;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.ServerStatsProvider;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;
import rottenbonestudio.system.SecurityNetwork.common.config.ConfigMigrator;
import rottenbonestudio.system.SecurityNetwork.common.config.SecurityConfigLoader;
import rottenbonestudio.system.SecurityNetwork.spigot.PlayerJoinListener;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.AdminCommand;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.ForceUnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.LinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.TestCommand;
import rottenbonestudio.system.SecurityNetwork.spigot.commands.UnlinkDiscordCommand;

import java.io.File;
import java.util.logging.Level;

public abstract class BukkitSecurityPluginBase extends JavaPlugin {

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
					return Bukkit.getOnlinePlayers().size();
				}

				@Override
				public int getMaxPlayers() {
					return Bukkit.getMaxPlayers();
				}
			}, discordBot);
			dailyReport.start();

			ipCheckManager = new IpCheckManager(config, discordBot);
			registerCommands();
			getServer().getPluginManager().registerEvents(createLoginListener(ipCheckManager), this);

			getLogger().info("SecurityNetwork (" + getPlatformName() + ") enabled.");
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Could not enable SecurityNetwork (" + getPlatformName() + "): "
					+ e.getMessage(), e);
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

	protected Listener createLoginListener(IpCheckManager manager) {
		return new PlayerJoinListener(manager);
	}

	protected abstract String getPlatformName();

	private void registerCommands() {
		LinkDiscordCommand link = new LinkDiscordCommand(discordBot);
		UnlinkDiscordCommand unlink = new UnlinkDiscordCommand(discordBot);
		ForceUnlinkDiscordCommand forceUnlink = new ForceUnlinkDiscordCommand();
		TestCommand test = new TestCommand(ipCheckManager);
		AdminCommand admin = new AdminCommand(ipCheckManager);

		register("vincular-discord", link);
		register("desvincular-discord", unlink);
		register("forzar-desvincular-discord", forceUnlink);
		register("ipchecktest", test);
		register("ipadmin", admin);
	}

	private void register(String name, org.bukkit.command.TabExecutor executor) {
		PluginCommand command = getCommand(name);
		if (command != null) {
			command.setExecutor(executor);
			command.setTabCompleter(executor);
		}
	}

	public DiscordBot getDiscordBot() {
		return discordBot;
	}

	public IpCheckManager getIpCheckManager() {
		return ipCheckManager;
	}
}
