package rottenbonestudio.system.SecurityNetwork.paper;

import org.bukkit.event.Listener;
import rottenbonestudio.system.SecurityNetwork.bukkit.BukkitSecurityPluginBase;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;

public class PaperSecurityPlugin extends BukkitSecurityPluginBase {

	@Override
	protected Listener createLoginListener(IpCheckManager manager) {
		return new PaperLoginListener(manager);
	}

	@Override
	protected String getPlatformName() {
		return "Paper";
	}
}
