package rottenbonestudio.system.SecurityNetwork.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

public class TestCommand extends Command {

	private final IpCheckManager ipCheckManager;

	public TestCommand(IpCheckManager ipCheckManager) {
		super("ipchecktest", null, new String[0]);
		this.ipCheckManager = ipCheckManager;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!sender.getName().equalsIgnoreCase("CONSOLE")) {
			sender.sendMessage(new TextComponent(LangManager.get("command.test.only-console")));
			return;
		}

		if (args.length != 1) {
			sender.sendMessage(new TextComponent(LangManager.get("command.test.usage")));
			return;
		}

		String ip = args[0];
		try {
			ipCheckManager.testAllApisImproved(ip);
			sender.sendMessage(new TextComponent(LangManager.get("command.test.success", ip)));
		} catch (Exception e) {
			sender.sendMessage(new TextComponent(LangManager.get("command.test.error", e.getMessage())));
		}
	}
	
}
