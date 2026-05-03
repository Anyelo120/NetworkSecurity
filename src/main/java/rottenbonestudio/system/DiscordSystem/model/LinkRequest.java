package rottenbonestudio.system.DiscordSystem.model;

import java.util.UUID;

public class LinkRequest {
	private final UUID playerUUID;
	private final String discordName;
	private final String playerName;

	public LinkRequest(UUID playerUUID, String discordName, String playerName) {
		this.playerUUID = playerUUID;
		this.discordName = discordName;
		this.playerName = playerName;
	}

	public LinkRequest(UUID playerUUID, String discordName) {
		this(playerUUID, discordName, null);
	}

	public UUID getPlayerUUID() {
		return playerUUID;
	}

	public String getDiscordName() {
		return discordName;
	}

	public String getPlayerName() {
		return playerName;
	}
}
