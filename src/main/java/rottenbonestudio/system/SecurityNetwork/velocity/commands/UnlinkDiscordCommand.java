package rottenbonestudio.system.SecurityNetwork.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.UUID;

public class UnlinkDiscordCommand implements SimpleCommand {

	private final DiscordBot discordBot;

	public UnlinkDiscordCommand(DiscordBot discordBot) {
		this.discordBot = discordBot;
	}

	@Override
	public void execute(Invocation invocation) {
		if (!(invocation.source() instanceof Player)) {
			invocation.source().sendMessage(deserialize(LangManager.get("command.unlink.only-player")));
			return;
		}

		Player player = (Player) invocation.source();
		UUID uuid = player.getUniqueId();

		String result = discordBot.solicitarDesvinculacion(uuid);
		player.sendMessage(deserialize(LangManager.get("command.unlink.success", result)));
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("securitynetwork.discord.unlink");
	}

	private Component deserialize(String text) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
	}
	
}
