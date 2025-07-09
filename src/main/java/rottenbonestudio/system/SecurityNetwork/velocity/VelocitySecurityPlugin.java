package rottenbonestudio.system.SecurityNetwork.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import com.google.inject.Inject;
import org.slf4j.Logger;

import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.LinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.UnlinkDiscordCommand;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.VelocityAdminCommand;
import rottenbonestudio.system.SecurityNetwork.velocity.commands.VelocityTestCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VelocitySecurityPlugin {

	@SuppressWarnings("unused")
	private final ProxyServer server;
	private final Logger logger;
	private IpCheckManager ipCheckManager;

	private DiscordBot discordBot;

	@Inject
	public VelocitySecurityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		this.server = server;
		this.logger = logger;

		logger.info("[SecurityNetwork] Iniciando la carga de la configuración.");

		try {
			Path pluginFolder = dataDirectory.toFile().getAbsoluteFile().toPath();
			discordBot = new DiscordBot(pluginFolder.toFile());
			discordBot.start();
			DiscordConfirmationAPI.init(discordBot);
			logger.info("[SecurityNetwork] DiscordBot iniciado correctamente.");
		} catch (Exception e) {
			logger.error("[SecurityNetwork] No se pudo iniciar el DiscordBot: " + e.getMessage(), e);
		}

		try {
			Files.createDirectories(dataDirectory);
			Path configPath = dataDirectory.resolve("config.yml");
			logger.info("[SecurityNetwork] Ruta de la configuración: " + configPath.toString());

			if (!Files.exists(configPath)) {
				logger.info("[SecurityNetwork] No se encontró config.yml, creando uno nuevo.");
				Files.copy(getClass().getResourceAsStream("/config.yml"), configPath);
			}

			IpCheckConfig config = new VelocityConfigLoader(configPath).load();
			LangManager.init(dataDirectory, config.getLangCode());
			
			this.ipCheckManager = new IpCheckManager(config, discordBot);

			server.getCommandManager().register("ipchecktest", new VelocityTestCommand(ipCheckManager));

			server.getCommandManager().register(
					server.getCommandManager().metaBuilder("ipadmin")
					        .plugin(this)
					        .build(),
					new VelocityAdminCommand(ipCheckManager)
			);
			server.getCommandManager().register(
					server.getCommandManager().metaBuilder("vincular-discord")
					        .plugin(this)
					        .build(),
					new LinkDiscordCommand(discordBot)
			);
			server.getCommandManager().register(
					server.getCommandManager().metaBuilder("desvincular-discord")
					        .plugin(this)
					        .build(),
					new UnlinkDiscordCommand(discordBot)
			);

			logger.info("[SecurityNetwork] Configuración y mensajes cargados correctamente.");
		} catch (IOException e) {
			logger.error("[SecurityNetwork] Error cargando configuración: " + e.getMessage(), e);
		}
	}

	public DiscordBot getDiscordBot() {
		return discordBot;
	}

	@Subscribe
	public void onLogin(LoginEvent event) {
		String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
		String uuid = event.getPlayer().getUniqueId().toString();

		Player player = event.getPlayer();
		boolean bypassVPN = player.hasPermission("securitynetwork.bypass.vpn");
		boolean bypassCountry = player.hasPermission("securitynetwork.bypass.country");
		boolean bypassContinent = player.hasPermission("securitynetwork.bypass.continent");
		boolean bypassAll = player.hasPermission("securitynetwork.bypass.all");

		logger.info("[SecurityNetwork] Verificando acceso para el jugador con IP: " + ip + " y UUID: " + uuid);

		if (ipCheckManager.isTempBlocked(ip)) {
			logger.warn("[SecurityNetwork] IP bloqueada temporalmente: " + ip);
			event.setResult(LoginEvent.ComponentResult.denied(
					net.kyori.adventure.text.Component.text(LangManager.get("ip.blocked.temp"))
			));
			return;
		}

		if (ipCheckManager.verifyPlayerAccess(uuid, ip, bypassVPN, bypassCountry, bypassContinent, bypassAll)) {
			event.setResult(LoginEvent.ComponentResult.denied(
					net.kyori.adventure.text.Component.text(LangManager.get("ip.blocked"))
			));
		}
	}
}
