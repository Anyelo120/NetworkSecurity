package rottenbonestudio.system.SecurityNetwork.spigot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import rottenbonestudio.system.DiscordSystem.api.DiscordLinkAPI;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

	private final IpCheckManager ipCheckManager;

	public PlayerJoinListener(IpCheckManager ipCheckManager) {
		this.ipCheckManager = ipCheckManager;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		String ip = event.getAddress().getHostAddress();
		UUID playerUUID = event.getUniqueId();
		String uuid = playerUUID.toString();

		if (ipCheckManager.isTempBlocked(ip)) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, LangManager.get("ip.blocked.temp"));
			return;
		}

		boolean blocked = ipCheckManager.verifyPlayerAccess(uuid, ip, false, false, false, false);
		if (blocked) {
			String discordId = DiscordLinkAPI.getDiscordIdByMinecraft(playerUUID);
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
					discordId == null || discordId.isEmpty() ? LangManager.get("ip.blocked")
							: LangManager.get("ip.blockeddiscord"));
		}
	}
}
