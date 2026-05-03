package rottenbonestudio.system.SecurityNetwork.velocity.commands;

import java.util.List;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

public class VelocityTestCommand implements SimpleCommand {

	private final IpCheckManager manager;

	public VelocityTestCommand(IpCheckManager manager) {
		this.manager = manager;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource source = invocation.source();
		String[] args = invocation.arguments();

		if (args.length != 1) {
			source.sendMessage(deserialize(LangManager.get("command.test.usage")));
			return;
		}

		String ip = args[0];
		try {
			manager.testAllApisImproved(ip);
			source.sendMessage(deserialize(LangManager.get("command.test.success", ip)));
		} catch (Exception e) {
			source.sendMessage(deserialize(LangManager.get("command.test.error", e.getMessage())));
		}
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("securitynetwork.ipchecktest");
	}

	@Override
	public List<String> suggest(Invocation invocation) {
		return List.of();
	}

	private Component deserialize(String text) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
	}
	
}
