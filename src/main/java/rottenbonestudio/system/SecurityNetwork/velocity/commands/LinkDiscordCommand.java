package rottenbonestudio.system.SecurityNetwork.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.List;
import java.util.UUID;

public class LinkDiscordCommand implements SimpleCommand {

	private final DiscordBot discordBot;

	public LinkDiscordCommand(DiscordBot discordBot) {
		this.discordBot = discordBot;
	}

	@Override
	public void execute(Invocation invocation) {
		if (!(invocation.source() instanceof Player)) {
			invocation.source().sendMessage(deserialize(LangManager.get("command.link.only-player")));
			return;
		}

		String[] args = invocation.arguments();
		if (args.length != 1) {
			invocation.source().sendMessage(deserialize(LangManager.get("command.link.usage")));
			return;
		}

		Player player = (Player) invocation.source();
		UUID uuid = player.getUniqueId();
		String discordName = args[0];

		String result = discordBot.solicitarVinculacion(uuid, discordName);
		player.sendMessage(deserialize(LangManager.get("command.link.success", result)));
	}

	@Override
	public List<String> suggest(Invocation invocation) {
		return List.of("Usuario#1234", "MiNombre#0001");
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("securitynetwork.discord.link");
	}

	private Component deserialize(String text) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
	}
	
}
