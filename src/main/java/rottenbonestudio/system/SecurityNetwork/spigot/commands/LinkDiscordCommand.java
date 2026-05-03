package rottenbonestudio.system.SecurityNetwork.spigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.List;
import java.util.UUID;

public class LinkDiscordCommand implements TabExecutor {

	private final DiscordBot discordBot;

	public LinkDiscordCommand(DiscordBot discordBot) {
		this.discordBot = discordBot;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("securitynetwork.discord.link")) {
			sender.sendMessage(LangManager.get("command.admin.no-permission"));
			return true;
		}

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

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("securitynetwork.discord.link")) {
			return List.of();
		}
		if (args.length == 1) {
			return List.of("Usuario#1234", "MiNombre#0001");
		}
		return List.of();
	}
	
}
