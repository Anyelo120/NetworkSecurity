package rottenbonestudio.system.SecurityNetwork.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;
import rottenbonestudio.system.SecurityNetwork.bungee.BungeeSecurityPlugin;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ForceUnlinkDiscordCommand extends Command implements TabExecutor {

	private final BungeeSecurityPlugin plugin;

	public ForceUnlinkDiscordCommand(BungeeSecurityPlugin plugin) {
		super("forzar-desvincular-discord", "securitynetwork.discord.forceunlink");
		this.plugin = plugin;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!sender.hasPermission("securitynetwork.discord.forceunlink")) {
			sender.sendMessage(new TextComponent(LangManager.get("command.admin.no-permission")));
			return;
		}

		if (args.length < 1) {
			sender.sendMessage(new TextComponent(LangManager.get("command.forceunlink.usage")));
			return;
		}

		String targetArg = args[0];
		UUID uuid;
		String displayName = targetArg;

		ProxiedPlayer target = plugin.getProxy().getPlayer(targetArg);
		if (target != null) {
			uuid = target.getUniqueId();
			displayName = target.getName();
		} else {
			try {
				uuid = UUID.fromString(targetArg);
			} catch (IllegalArgumentException ex) {
				sender.sendMessage(new TextComponent(LangManager.get("command.forceunlink.invalid-target", targetArg)));
				return;
			}
		}

		boolean removed = JsonLinkStorage.deleteLinkByUUID(uuid.toString());
		sender.sendMessage(new TextComponent(removed ? LangManager.get("command.forceunlink.success", displayName)
				: LangManager.get("command.forceunlink.not-linked", displayName)));
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		if (!sender.hasPermission("securitynetwork.discord.forceunlink") || args.length != 1) {
			return List.of();
		}
		String prefix = args[0].toLowerCase();
		return plugin.getProxy().getPlayers().stream().map(ProxiedPlayer::getName)
				.filter(name -> name.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
	}
}
