package rottenbonestudio.system.DiscordSystem.api;

import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class DiscordConfirmationAPI {

	private static DiscordBot bot;
	private static final Map<UUID, CompletableFuture<Boolean>> confirmationMap = new ConcurrentHashMap<>();
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static final Map<UUID, String[]> confirmationDetailsMap = new ConcurrentHashMap<>();

	public static void init(DiscordBot discordBot) {
		bot = discordBot;
	}

	/**
	 * Solicita al usuario de Discord vinculado a una cuenta de Minecraft que
	 * confirme si una acción fue realizada por él (por ejemplo: conexión).
	 *
	 * @param playerUUID UUID del jugador de Minecraft
	 * @return Future que se completa con true/false según la respuesta del usuario
	 */
	public static CompletableFuture<Boolean> solicitarConfirmacion(UUID playerUUID, String ip, String pais,
			String continente, String hora, String cuentas) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();

		String discordId = JsonLinkStorage.getDiscordIdByUUID(playerUUID.toString());
		if (discordId == null) {
			future.completeExceptionally(new IllegalStateException(LangManager.get("discord.confirmation.no-link")));
			return future;
		}

		User user = bot.getJda().getUserById(discordId);
		TextChannel channel = bot.getJda().getTextChannelById(bot.getChannelId());

		if (user == null || channel == null) {
			future.completeExceptionally(
					new IllegalStateException(LangManager.get("discord.confirmation.channel-error")));
			return future;
		}

		String displayMessage = LangManager.get("discord.confirmation.message", user.getId());

		Button accept = Button.success("confirm_auth_yes_" + playerUUID,
				"✅ " + LangManager.get("discord.confirmation.yes"));
		Button deny = Button.danger("confirm_auth_no_" + playerUUID, "❌ " + LangManager.get("discord.confirmation.no"));

		boolean datosValidos = ip != null && pais != null && continente != null && hora != null && cuentas != null;

		if (datosValidos) {
			Button info = Button.secondary("confirm_auth_info_" + playerUUID, "ℹ️");
			channel.sendMessage(displayMessage).setActionRow(accept, deny, info).queue(msg -> {
				confirmationMap.put(playerUUID, future);
				confirmationDetailsMap.put(playerUUID, new String[] { ip, pais, continente, hora, cuentas });

				scheduler.schedule(() -> {
					if (!future.isDone()) {
						future.complete(false);
						msg.editMessage(LangManager.get("discord.confirmation.expired")).setComponents().queue();
					}
					confirmationMap.remove(playerUUID);
					confirmationDetailsMap.remove(playerUUID);
				}, 5, TimeUnit.MINUTES);
			});

		} else {
			channel.sendMessage(displayMessage).setActionRow(accept, deny).queue(msg -> {
				confirmationMap.put(playerUUID, future);
				scheduler.schedule(() -> {
					if (!future.isDone()) {
						future.complete(false);
						msg.editMessage(LangManager.get("discord.confirmation.expired")).setComponents().queue();
					}
					confirmationMap.remove(playerUUID);
				}, 5, TimeUnit.MINUTES);
			});
		}

		return future;
	}

	public static boolean isInitialized() {
		return bot != null;
	}

	public static String[] getConfirmationDetails(UUID playerUUID) {
		return confirmationDetailsMap.get(playerUUID);
	}

	public static boolean manejarRespuesta(UUID playerUUID, boolean confirmado) {
		CompletableFuture<Boolean> future = confirmationMap.remove(playerUUID);
		if (future != null) {
			future.complete(confirmado);
			return true;
		}
		return false;
	}

}
