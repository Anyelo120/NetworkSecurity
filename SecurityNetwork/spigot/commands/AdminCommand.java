package rottenbonestudio.system.SecurityNetwork.spigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

public class AdminCommand implements CommandExecutor {

	private final IpCheckManager manager;

	public AdminCommand(IpCheckManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!sender.hasPermission("securitynetwork.admin")) {
			sender.sendMessage(LangManager.get("command.admin.no-permission"));
			return true;
		}

		if (args.length == 0) {
			sender.sendMessage(LangManager.get("command.admin.help.header"));
			sender.sendMessage(LangManager.get("command.admin.help.delete"));
			sender.sendMessage(LangManager.get("command.admin.help.stats"));
			sender.sendMessage(LangManager.get("command.admin.help.wipe"));
			return true;
		}

		String sub = args[0].toLowerCase();

		switch (sub) {
			case "delete":
				if (args.length != 2) {
					sender.sendMessage(LangManager.get("command.admin.usage.delete"));
					return true;
				}
				String ip = args[1];
				manager.removeIP(ip);
				sender.sendMessage(LangManager.get("command.admin.ip-removed", ip));
				return true;

			case "stats":
				int blocked = manager.getBlockedCount();
				int allowed = manager.getAllowedCount();
				sender.sendMessage(LangManager.get("command.admin.stats.title"));
				sender.sendMessage(LangManager.get("command.admin.stats.blocked", String.valueOf(blocked)));
				sender.sendMessage(LangManager.get("command.admin.stats.allowed", String.valueOf(allowed)));
				return true;

			case "wipe":
				if (args.length != 2 || !args[1].equalsIgnoreCase("confirm")) {
					sender.sendMessage(LangManager.get("command.admin.usage.wipe"));
					return true;
				}

				boolean wiped = manager.wipeAllData(true);
				if (wiped) {
					sender.sendMessage(LangManager.get("command.admin.wipe.success"));
				} else {
					sender.sendMessage(LangManager.get("command.admin.wipe.failed"));
				}
				return true;

			default:
				sender.sendMessage(LangManager.get("command.admin.unknown-subcommand"));
				return true;
		}
	}
	
}
