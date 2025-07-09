package rottenbonestudio.system.SecurityNetwork.spigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.UUID;

public class LinkDiscordCommand implements CommandExecutor {

	private final DiscordBot discordBot;

	public LinkDiscordCommand(DiscordBot discordBot) {
		this.discordBot = discordBot;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(LangManager.get("command.link.only-player"));
			return true;
		}

		if (args.length != 1) {
			sender.sendMessage(LangManager.get("command.link.usage"));
			return true;
		}

		Player player = (Player) sender;
		UUID uuid = player.getUniqueId();
		String discordName = args[0];

		String resultado = discordBot.solicitarVinculacion(uuid, discordName);
		player.sendMessage(LangManager.get("command.link.success", resultado));
		return true;
	}
	
}
