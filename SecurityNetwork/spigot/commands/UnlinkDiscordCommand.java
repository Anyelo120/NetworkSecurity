package rottenbonestudio.system.SecurityNetwork.spigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.UUID;

public class UnlinkDiscordCommand implements CommandExecutor {

	private final DiscordBot discordBot;

	public UnlinkDiscordCommand(DiscordBot discordBot) {
		this.discordBot = discordBot;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(LangManager.get("command.unlink.only-player"));
			return true;
		}

		Player player = (Player) sender;
		UUID uuid = player.getUniqueId();

		String resultado = discordBot.solicitarDesvinculacion(uuid);
		player.sendMessage(LangManager.get("command.unlink.success", resultado));
		return true;
	}
	
}
