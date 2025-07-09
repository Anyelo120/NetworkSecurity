package rottenbonestudio.system.SecurityNetwork.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.UUID;

public class UnlinkDiscordCommand extends Command {

	private final DiscordBot discordBot;

	public UnlinkDiscordCommand(DiscordBot discordBot) {
		super("desvincular-discord");
		this.discordBot = discordBot;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new TextComponent(LangManager.get("command.unlink.only-player")));
			return;
		}

		ProxiedPlayer player = (ProxiedPlayer) sender;
		UUID uuid = player.getUniqueId();

		String resultado = discordBot.solicitarDesvinculacion(uuid);
		player.sendMessage(new TextComponent(LangManager.get("command.unlink.success", resultado)));
	}
	
}
