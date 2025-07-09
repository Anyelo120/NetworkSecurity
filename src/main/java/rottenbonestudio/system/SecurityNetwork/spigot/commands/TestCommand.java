package rottenbonestudio.system.SecurityNetwork.spigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

public class TestCommand implements CommandExecutor {

	private final IpCheckManager manager;

	public TestCommand(IpCheckManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof ConsoleCommandSender)) {
			sender.sendMessage(LangManager.get("command.test.only-console"));
			return true;
		}

		if (args.length != 1) {
			sender.sendMessage(LangManager.get("command.test.usage"));
			return true;
		}

		String ip = args[0];
		sender.sendMessage(LangManager.get("command.test.success", ip));
		manager.testAllApisImproved(ip);
		return true;
	}
	
}
