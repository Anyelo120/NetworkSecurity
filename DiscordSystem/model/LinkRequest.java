package rottenbonestudio.system.DiscordSystem.model;

import java.util.UUID;

public class LinkRequest {
    private final UUID playerUUID;
    private final String discordName;

    public LinkRequest(UUID playerUUID, String discordName) {
        this.playerUUID = playerUUID;
        this.discordName = discordName;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getDiscordName() {
        return discordName;
    }
    
}
