package rottenbonestudio.system.SecurityNetwork.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.Optional;
import java.util.UUID;

public class ForceUnlinkDiscordCommand implements SimpleCommand {

	private final ProxyServer server;

	public ForceUnlinkDiscordCommand(ProxyServer server) {
		this.server = server;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource source = invocation.source();
		String[] args = invocation.arguments();

		if (args.length < 1) {
			source.sendMessage(deserialize(LangManager.get("command.forceunlink.usage")));
			return;
		}

		String targetArg = args[0];

		UUID uuid = null;
		String displayName = targetArg;

		Optional<Player> optPlayer = server.getPlayer(targetArg);
		if (optPlayer.isPresent()) {
			Player targetPlayer = optPlayer.get();
			uuid = targetPlayer.getUniqueId();
			displayName = targetPlayer.getUsername();
		} else {
			try {
				uuid = UUID.fromString(targetArg);
				displayName = uuid.toString();
			} catch (IllegalArgumentException ex) {
				source.sendMessage(deserialize(LangManager.get("command.forceunlink.invalid-target", targetArg)));
				return;
			}
		}

		boolean removed = JsonLinkStorage.deleteLinkByUUID(uuid.toString());
		if (removed) {
			source.sendMessage(deserialize(LangManager.get("command.forceunlink.success", displayName)));
		} else {
			source.sendMessage(deserialize(LangManager.get("command.forceunlink.not-linked", displayName)));
		}
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		CommandSource source = invocation.source();

		if (!(source instanceof Player)) {
			return true;
		}

		Player player = (Player) source;
		return player.hasPermission("securitynetwork.discord.forceunlink");
	}

	private Component deserialize(String text) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
	}
}
