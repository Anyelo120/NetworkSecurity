package rottenbonestudio.system.SecurityNetwork.bungee;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import rottenbonestudio.system.DiscordSystem.api.DiscordLinkAPI;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.net.InetSocketAddress;
import java.util.UUID;

public class BungeeLoginListener implements Listener {

	private final BungeeSecurityPlugin plugin;
	private final IpCheckManager ipCheckManager;

	public BungeeLoginListener(BungeeSecurityPlugin plugin, IpCheckManager ipCheckManager) {
		this.plugin = plugin;
		this.ipCheckManager = ipCheckManager;
	}

	@EventHandler
	public void onLogin(LoginEvent event) {
		event.registerIntent(plugin);
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			try {
				PendingConnection connection = event.getConnection();
				UUID uniqueId = connection.getUniqueId();
				if (uniqueId == null) {
					deny(event, LangManager.get("uuid.missing"));
					return;
				}

				if (!(connection.getSocketAddress() instanceof InetSocketAddress)) {
					deny(event, LangManager.get("ip.missing"));
					return;
				}

				String uuid = uniqueId.toString();
				String ip = ((InetSocketAddress) connection.getSocketAddress()).getAddress().getHostAddress();

				if (ip == null || ip.isEmpty()) {
					deny(event, LangManager.get("ip.missing"));
					return;
				}

				if (ipCheckManager.isTempBlocked(ip)) {
					deny(event, LangManager.get("ip.blocked.temp"));
					return;
				}

				boolean blocked = ipCheckManager.verifyPlayerAccess(uuid, ip, false, false, false, false);
				if (blocked) {
					String discordId = DiscordLinkAPI.getDiscordIdByMinecraft(uniqueId);
					deny(event, discordId == null || discordId.isEmpty() ? LangManager.get("ip.blocked")
							: LangManager.get("ip.blockeddiscord"));
				}
			} finally {
				event.completeIntent(plugin);
			}
		});
	}

	@SuppressWarnings("deprecation")
	private void deny(LoginEvent event, String message) {
		event.setCancelled(true);
		event.setCancelReason(TextComponent.fromLegacyText(message));
	}
}
