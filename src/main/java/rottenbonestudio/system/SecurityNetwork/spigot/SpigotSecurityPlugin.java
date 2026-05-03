package rottenbonestudio.system.SecurityNetwork.spigot;

import org.bukkit.event.Listener;
import rottenbonestudio.system.SecurityNetwork.bukkit.BukkitSecurityPluginBase;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.paper.PaperLoginListener;

public class SpigotSecurityPlugin extends BukkitSecurityPluginBase {

	@Override
	protected Listener createLoginListener(IpCheckManager manager) {
		if (isPaperServer()) {
			return new PaperLoginListener(manager);
		}
		return super.createLoginListener(manager);
	}

	@Override
	protected String getPlatformName() {
		if (isPaperServer()) {
			return "Paper";
		}
		return "Spigot";
	}

	private boolean isPaperServer() {
		return getServer().getName().toLowerCase().contains("paper");
	}
}
