package rottenbonestudio.system.DiscordSystem.api;

import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;

import java.util.UUID;

public class DiscordLinkAPI {

	/**
	 * Obtiene el ID de Discord vinculado a un jugador de Minecraft.
	 *
	 * @param playerUUID UUID del jugador
	 * @return Discord ID o null si no hay vínculo
	 */
	public static String getDiscordIdByMinecraft(UUID playerUUID) {
		return JsonLinkStorage.getDiscordIdByUUID(playerUUID.toString());
	}

	/**
	 * Obtiene el UUID de Minecraft vinculado a un ID de Discord.
	 *
	 * @param discordId ID del usuario de Discord
	 * @return UUID del jugador o null si no hay vínculo
	 */
	public static UUID getMinecraftUUIDByDiscordId(String discordId) {
		String uuid = JsonLinkStorage.getUUIDByDiscordId(discordId);
		if (uuid != null) {
			try {
				return UUID.fromString(uuid);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
