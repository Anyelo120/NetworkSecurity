package rottenbonestudio.system.SecurityNetwork.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import rottenbonestudio.system.DiscordSystem.DailyPlayerReport;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.ServerStatsProvider;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.DiscordSystem.api.DiscordLinkAPI;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;
import rottenbonestudio.system.SecurityNetwork.common.config.ConfigMigrator;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.ForceUnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.LinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.UnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.VelocityAdminCommand;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.VelocityTestCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class VelocitySecurityPlugin {

	private final ProxyServer server;
	private final Logger logger;
	private IpCheckManager ipCheckManager;
	private DiscordBot discordBot;
	private DailyPlayerReport dailyReport;

	@Inject
	public VelocitySecurityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		this.server = server;
		this.logger = logger;

		try {
			Files.createDirectories(dataDirectory);
			Path configPath = dataDirectory.resolve("config.yml");

			if (!Files.exists(configPath)) {
				Files.copy(getClass().getResourceAsStream("/config.yml"), configPath);
			}

			new ConfigMigrator(dataDirectory, logger::info, logger::warn).run();

			IpCheckConfig config = new VelocityConfigLoader(configPath).load();
			LangManager.init(dataDirectory, config.getLangCode());

			discordBot = new DiscordBot(dataDirectory.toFile());
			discordBot.start();
			DiscordConfirmationAPI.init(discordBot);

			dailyReport = new DailyPlayerReport(new ServerStatsProvider() {
				@Override
				public int getOnlinePlayers() {
					return server.getPlayerCount();
				}

				@Override
				public int getMaxPlayers() {
					return server.getConfiguration().getShowMaxPlayers();
				}
			}, discordBot);
			dailyReport.start();

			ipCheckManager = new IpCheckManager(config, discordBot);
			registerCommands();

			logger.info("[SecurityNetwork] Velocity enabled.");
		} catch (IOException e) {
			logger.error("[SecurityNetwork] Error loading configuration: " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("[SecurityNetwork] Error enabling plugin: " + e.getMessage(), e);
		}
	}

	private void registerCommands() {
		server.getCommandManager().register("ipchecktest", new VelocityTestCommand(ipCheckManager));
		server.getCommandManager().register(server.getCommandManager().metaBuilder("ipadmin").plugin(this).build(),
				new VelocityAdminCommand(ipCheckManager));
		server.getCommandManager().register(server.getCommandManager().metaBuilder("vincular-discord").plugin(this).build(),
				new LinkDiscordCommand(discordBot));
		server.getCommandManager().register(
				server.getCommandManager().metaBuilder("desvincular-discord").plugin(this).build(),
				new UnlinkDiscordCommand(discordBot));
		server.getCommandManager().register(
				server.getCommandManager().metaBuilder("forzar-desvincular-discord").plugin(this).build(),
				new ForceUnlinkDiscordCommand(server));
	}

	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
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
				logger.error("Error stopping DiscordBot: " + e.getMessage(), e);
			}
			discordBot = null;
		}
	}

	public DiscordBot getDiscordBot() {
		return discordBot;
	}

	@Subscribe
	public void onLogin(LoginEvent event) {
		Player player = event.getPlayer();
		String ip = player.getRemoteAddress().getAddress().getHostAddress();
		String uuid = player.getUniqueId().toString();

		boolean bypassVPN = player.hasPermission("securitynetwork.bypass.vpn");
		boolean bypassCountry = player.hasPermission("securitynetwork.bypass.country");
		boolean bypassContinent = player.hasPermission("securitynetwork.bypass.continent");
		boolean bypassAll = player.hasPermission("securitynetwork.bypass.all");

		if (ipCheckManager.isTempBlocked(ip)) {
			event.setResult(LoginEvent.ComponentResult.denied(Component.text(LangManager.get("ip.blocked.temp"))));
			return;
		}

		boolean blocked = ipCheckManager.verifyPlayerAccess(uuid, ip, bypassVPN, bypassCountry, bypassContinent,
				bypassAll);

		if (blocked) {
			UUID playerUUID = player.getUniqueId();
			String discordId = DiscordLinkAPI.getDiscordIdByMinecraft(playerUUID);
			String message = discordId == null || discordId.isEmpty() ? LangManager.get("ip.blocked")
					: LangManager.get("ip.blockeddiscord");
			event.setResult(LoginEvent.ComponentResult.denied(Component.text(message)));
		}
	}
}
