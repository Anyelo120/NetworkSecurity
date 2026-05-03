package rottenbonestudio.system.SecurityNetwork.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.List;

public class BungeeAdminCommand extends Command implements TabExecutor {

	private final IpCheckManager manager;

	public BungeeAdminCommand(IpCheckManager manager) {
		super("ipadmin", "securitynetwork.admin");
		this.manager = manager;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {

		if (!sender.hasPermission("securitynetwork.admin")) {
			sender.sendMessage(new TextComponent(LangManager.get("command.admin.no-permission")));
			return;
		}

		if (args.length == 0) {
			sender.sendMessage(new TextComponent(LangManager.get("command.admin.help.header")));
			sender.sendMessage(new TextComponent(LangManager.get("command.admin.help.delete")));
			sender.sendMessage(new TextComponent(LangManager.get("command.admin.help.stats")));
			sender.sendMessage(new TextComponent(LangManager.get("command.admin.help.wipe")));
			return;
		}

		String sub = args[0].toLowerCase();

		switch (sub) {
			case "delete":
				if (args.length != 2) {
					sender.sendMessage(new TextComponent(LangManager.get("command.admin.usage.delete")));
					return;
				}
				String ip = args[1];
				manager.removeIP(ip);
				sender.sendMessage(new TextComponent(LangManager.get("command.admin.ip-removed", ip)));
				return;

			case "stats":
				int blocked = manager.getBlockedCount();
				int allowed = manager.getAllowedCount();
				sender.sendMessage(new TextComponent(LangManager.get("command.admin.stats.title")));
				sender.sendMessage(new TextComponent(LangManager.get("command.admin.stats.blocked", String.valueOf(blocked))));
				sender.sendMessage(new TextComponent(LangManager.get("command.admin.stats.allowed", String.valueOf(allowed))));
				return;

			case "wipe":
				if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
					boolean wiped = manager.wipeAllData(true);
					if (wiped) {
						sender.sendMessage(new TextComponent(LangManager.get("command.admin.wipe.success")));
					} else {
						sender.sendMessage(new TextComponent(LangManager.get("command.admin.wipe.failed")));
					}
				} else {
					sender.sendMessage(new TextComponent(LangManager.get("command.admin.usage.wipe")));
				}
				break;

			default:
				sender.sendMessage(new TextComponent(LangManager.get("command.admin.unknown-subcommand")));
		}
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		if (!sender.hasPermission("securitynetwork.admin")) {
			return List.of();
		}
		if (args.length == 1) {
			return List.of("delete", "stats", "wipe");
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("wipe")) {
			return List.of("confirm");
		}
		return List.of();
	}
	
}
