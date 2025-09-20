package rottenbonestudio.system.SecurityNetwork.spigot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

public class PlayerJoinListener implements Listener {

	private final IpCheckManager ipCheckManager;
	private final JavaPlugin plugin;

	public PlayerJoinListener(IpCheckManager ipCheckManager, JavaPlugin plugin) {
		this.ipCheckManager = ipCheckManager;
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		String ip = event.getAddress().getHostAddress();
		String uuid = player.getUniqueId().toString();

		boolean bypassVPN = player.hasPermission("securitynetwork.bypass.vpn");
		boolean bypassCountry = player.hasPermission("securitynetwork.bypass.country");
		boolean bypassContinent = player.hasPermission("securitynetwork.bypass.continent");
		boolean bypassAll = player.hasPermission("securitynetwork.bypass.all");

		System.out.println("[SecurityNetwork] Verificando acceso para el jugador con IP: " + ip + " y UUID: " + uuid);

		if (ipCheckManager.isTempBlocked(ip)) {
			event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
			event.setKickMessage(LangManager.get("ip.blocked.temp"));
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			boolean blocked = ipCheckManager.verifyPlayerAccess(uuid, ip, bypassVPN, bypassCountry, bypassContinent,
					bypassAll);
			if (blocked) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
					event.setKickMessage(LangManager.get("ip.blocked"));
				});
			}
		});
	}
}
