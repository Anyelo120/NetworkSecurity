package rottenbonestudio.system.SecurityNetwork.spigot.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ForceUnlinkDiscordCommand implements TabExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("securitynetwork.discord.forceunlink")) {
			sender.sendMessage(LangManager.get("command.admin.no-permission"));
			return true;
		}

		if (args.length < 1) {
			sender.sendMessage(LangManager.get("command.forceunlink.usage"));
			return true;
		}

		String targetArg = args[0];
		UUID uuid;
		String displayName = targetArg;

		Player target = Bukkit.getPlayerExact(targetArg);
		if (target != null) {
			uuid = target.getUniqueId();
			displayName = target.getName();
		} else {
			try {
				uuid = UUID.fromString(targetArg);
			} catch (IllegalArgumentException ex) {
				sender.sendMessage(LangManager.get("command.forceunlink.invalid-target", targetArg));
				return true;
			}
		}

		boolean removed = JsonLinkStorage.deleteLinkByUUID(uuid.toString());
		sender.sendMessage(removed ? LangManager.get("command.forceunlink.success", displayName)
				: LangManager.get("command.forceunlink.not-linked", displayName));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("securitynetwork.discord.forceunlink") || args.length != 1) {
			return List.of();
		}
		String prefix = args[0].toLowerCase();
		return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(prefix))
				.collect(Collectors.toList());
	}
}
