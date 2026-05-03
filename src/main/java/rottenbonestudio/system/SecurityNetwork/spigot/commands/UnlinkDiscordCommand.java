package rottenbonestudio.system.SecurityNetwork.spigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.List;
import java.util.UUID;

public class UnlinkDiscordCommand implements TabExecutor {

	private final DiscordBot discordBot;

	public UnlinkDiscordCommand(DiscordBot discordBot) {
		this.discordBot = discordBot;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("securitynetwork.discord.unlink")) {
			sender.sendMessage(LangManager.get("command.admin.no-permission"));
			return true;
		}

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

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return List.of();
	}
	
}
