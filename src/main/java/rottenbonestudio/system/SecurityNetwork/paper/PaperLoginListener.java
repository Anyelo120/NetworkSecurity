package rottenbonestudio.system.SecurityNetwork.paper;

import rottenbonestudio.system.SecurityNetwork.common.IpCheckManager;
import rottenbonestudio.system.SecurityNetwork.spigot.PlayerJoinListener;

public class PaperLoginListener extends PlayerJoinListener {

	public PaperLoginListener(IpCheckManager ipCheckManager) {
		super(ipCheckManager);
	}
}
