package rottenbonestudio.system.SecurityNetwork.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.List;

public class VelocityAdminCommand implements SimpleCommand {

	private final IpCheckManager manager;

	public VelocityAdminCommand(IpCheckManager manager) {
		this.manager = manager;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource source = invocation.source();
		String[] args = invocation.arguments();

		if (!source.hasPermission("securitynetwork.admin")) {
			source.sendMessage(deserialize(LangManager.get("command.admin.no-permission")));
			return;
		}

		if (args.length == 0) {
			sendHelp(source);
			return;
		}

		String sub = args[0].toLowerCase();

		switch (sub) {
			case "delete":
				if (args.length != 2) {
					source.sendMessage(deserialize(LangManager.get("command.admin.usage.delete")));
					return;
				}
				String ip = args[1];
				manager.removeIP(ip);
				source.sendMessage(deserialize(LangManager.get("command.admin.ip-removed", ip)));
				break;

			case "stats":
				int blocked = manager.getBlockedCount();
				int allowed = manager.getAllowedCount();
				source.sendMessage(deserialize(LangManager.get("command.admin.stats.title")));
				source.sendMessage(deserialize(LangManager.get("command.admin.stats.blocked", String.valueOf(blocked))));
				source.sendMessage(deserialize(LangManager.get("command.admin.stats.allowed", String.valueOf(allowed))));
				break;

			case "wipe":
				if (args.length != 2 || !args[1].equalsIgnoreCase("confirm")) {
					source.sendMessage(deserialize(LangManager.get("command.admin.usage.wipe")));
					return;
				}

				boolean wiped = manager.wipeAllData(true);
				if (wiped) {
					source.sendMessage(deserialize(LangManager.get("command.admin.wipe.success")));
				} else {
					source.sendMessage(deserialize(LangManager.get("command.admin.wipe.failed")));
				}
				break;

			default:
				source.sendMessage(deserialize(LangManager.get("command.admin.unknown-subcommand")));
		}
	}

	private void sendHelp(CommandSource source) {
		source.sendMessage(deserialize(LangManager.get("command.admin.help.header")));
		source.sendMessage(deserialize(LangManager.get("command.admin.help.delete")));
		source.sendMessage(deserialize(LangManager.get("command.admin.help.stats")));
		source.sendMessage(deserialize(LangManager.get("command.admin.help.wipe")));
	}

	private Component deserialize(String string) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(string);
	}

	@Override
	public List<String> suggest(Invocation invocation) {
		return List.of("delete", "stats", "wipe");
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("securitynetwork.admin");
	}
	
}
