package rottenbonestudio.system.SecurityNetwork.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.UUID;

public class LinkDiscordCommand extends Command {

	private final DiscordBot discordBot;

	public LinkDiscordCommand(DiscordBot discordBot) {
		super("vincular-discord");
		this.discordBot = discordBot;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new TextComponent(LangManager.get("command.link.only-player")));
			return;
		}

		if (args.length != 1) {
			sender.sendMessage(new TextComponent(LangManager.get("command.link.usage")));
			return;
		}

		ProxiedPlayer player = (ProxiedPlayer) sender;
		UUID uuid = player.getUniqueId();
		String discordName = args[0];

		String resultado = discordBot.solicitarVinculacion(uuid, discordName);
		player.sendMessage(new TextComponent(LangManager.get("command.link.success", resultado)));
	}
	
}
