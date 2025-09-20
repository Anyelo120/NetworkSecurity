package rottenbonestudio.system.SecurityNetwork.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

public class BungeeLoginListener implements Listener {

	private final IpCheckManager ipCheckManager;

	public BungeeLoginListener(IpCheckManager ipCheckManager) {
		this.ipCheckManager = ipCheckManager;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPostLogin(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		String uuid = player.getUniqueId().toString();

		if (uuid == null) {
			System.out.println("[SecurityNetwork] Error: UUID del jugador no encontrado.");
			player.disconnect(LangManager.get("uuid.missing"));
			return;
		}

		String ip = player.getAddress().getAddress().getHostAddress();

		if (ip == null || ip.isEmpty()) {
			System.out.println("[SecurityNetwork] Error: IP del jugador no encontrada.");
			player.disconnect(LangManager.get("ip.missing"));
			return;
		}

		boolean bypassVPN = player.hasPermission("securitynetwork.bypass.vpn");
		boolean bypassCountry = player.hasPermission("securitynetwork.bypass.country");
		boolean bypassContinent = player.hasPermission("securitynetwork.bypass.continent");
		boolean bypassAll = player.hasPermission("securitynetwork.bypass.all");

		System.out.println("[SecurityNetwork] Verificando acceso para el jugador con IP: " + ip + " y UUID: " + uuid);

		if (ipCheckManager.isTempBlocked(ip)) {
			System.out.println("[SecurityNetwork] IP bloqueada temporalmente: " + ip);
			player.disconnect(LangManager.get("ip.blocked.temp"));
			return;
		}

		if (ipCheckManager.verifyPlayerAccess(uuid, ip, bypassVPN, bypassCountry, bypassContinent, bypassAll)) {
			player.disconnect(LangManager.get("ip.blocked"));
		}
	}
	
}
