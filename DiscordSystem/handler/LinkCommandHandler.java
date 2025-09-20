package rottenbonestudio.system.DiscordSystem.handler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import rottenbonestudio.system.DiscordSystem.model.LinkRequest;
import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

public class LinkCommandHandler {
	private final Map<UUID, LinkRequest> pendingLinks = new ConcurrentHashMap<>();
	private final JDA jda;

	public LinkCommandHandler(JDA jda) {
		this.jda = jda;
	}

	public String handleLinkCommand(UUID playerUUID, String[] args) {
		if (args.length != 1)
			return "Uso: /vincular-discord <nombre_discord>";

		if (JsonLinkStorage.isUuidLinked(playerUUID.toString()))
			return "❌ Ya tienes una cuenta de Discord vinculada.";

		String discordName = args[0];
		User discordUser = jda.getUsersByName(discordName, true).stream().findFirst().orElse(null);

		if (discordUser == null)
			return "❌ No se encontró el usuario de Discord con nombre: " + discordName;

		if (JsonLinkStorage.isDiscordIdLinked(discordUser.getId()))
			return "❌ Este usuario de Discord ya está vinculado con otra cuenta.";

		pendingLinks.put(playerUUID, new LinkRequest(playerUUID, discordName));
		return "✔ Solicitud de vinculación enviada para: " + discordName;
	}

	public boolean isPending(UUID playerUUID) {
		return pendingLinks.containsKey(playerUUID);
	}

	public LinkRequest getRequest(UUID playerUUID) {
		return pendingLinks.get(playerUUID);
	}

	public void complete(UUID playerUUID) {
		pendingLinks.remove(playerUUID);
	}

}
