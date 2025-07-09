package rottenbonestudio.system.DiscordSystem;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.DiscordSystem.config.Config;
import rottenbonestudio.system.DiscordSystem.handler.LinkCommandHandler;
import rottenbonestudio.system.DiscordSystem.model.LinkRequest;
import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class DiscordBot extends ListenerAdapter {

	private JDA jda;
	private final File pluginFolder;
	private LinkCommandHandler commandHandler;

	private final Map<String, UUID> activeMessages = new ConcurrentHashMap<>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public DiscordBot(File pluginFolder) {
		this.pluginFolder = pluginFolder;
	}

	public void start() throws LoginException {
		Config.init(pluginFolder);
		JsonLinkStorage.init(pluginFolder);

		String token = Config.getToken();
		String channelId = Config.getChannelId();
		if (token == null || token.isEmpty() || channelId == null || channelId.isEmpty()) {
			System.err.println("[DiscordBot] Token o Canal ID no están configurados. El bot no será iniciado.");
			return;
		}

		EnumSet<GatewayIntent> intents = EnumSet.of(GatewayIntent.GUILD_MEMBERS);

		jda = JDABuilder.create(Config.getToken(), intents).setMemberCachePolicy(MemberCachePolicy.ALL)
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER,
						CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
				.addEventListeners(this).build();

		try {
			jda.awaitReady();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}

		this.commandHandler = new LinkCommandHandler(jda);
	}

	public String solicitarVinculacion(UUID playerUUID, String discordName) {
		String result = commandHandler.handleLinkCommand(playerUUID, new String[] { discordName });
		if (!result.startsWith("✔")) {
			return result;
		}

		User targetUser = jda.getUsersByName(discordName, true).stream().findFirst().orElse(null);
		if (targetUser == null) {
			commandHandler.complete(playerUUID);
			return LangManager.get("discord.link.not-found", discordName);
		}

		TextChannel channel = jda.getTextChannelById(Config.getChannelId());
		if (channel == null)
			return LangManager.get("discord.link.invalid-channel");

		channel.sendMessage(LangManager.get("discord.link.message", targetUser.getId(), playerUUID.toString()))
				.setActionRow(Button.success("accept_link", "✅ Aceptar")).queue(sentMessage -> {
					activeMessages.put(sentMessage.getId(), playerUUID);
					scheduler.schedule(() -> {
						if (activeMessages.remove(sentMessage.getId()) != null) {
							sentMessage.editMessage(LangManager.get("discord.link.expired")).setComponents().queue(null,
									throwable -> {
										System.err.println("❌ Error al editar mensaje de expiración (vinculación): "
												+ throwable.getMessage());
									});
							commandHandler.complete(playerUUID);
						}
					}, 5, TimeUnit.MINUTES);
				});

		return LangManager.get("discord.link.sent");
	}

	public String solicitarDesvinculacion(UUID playerUUID) {
		if (!JsonLinkStorage.isUuidLinked(playerUUID.toString())) {
			return LangManager.get("discord.unlink.not-linked");
		}

		String discordId = JsonLinkStorage.getDiscordIdByUUID(playerUUID.toString());
		if (discordId == null) {
			return LangManager.get("discord.unlink.error");
		}

		User user = jda.getUserById(discordId);
		if (user == null) {
			return LangManager.get("discord.unlink.not-found");
		}

		TextChannel channel = jda.getTextChannelById(Config.getChannelId());
		if (channel == null)
			return LangManager.get("discord.link.invalid-channel");

		channel.sendMessage(LangManager.get("discord.unlink.message", user.getId()))
				.setActionRow(Button.danger("confirm_unlink", "❌ Desvincular")).queue(sentMessage -> {
					activeMessages.put(sentMessage.getId(), playerUUID);
					scheduler.schedule(() -> {
						if (activeMessages.remove(sentMessage.getId()) != null) {
							sentMessage.editMessage(LangManager.get("discord.unlink.expired")).setComponents()
									.queue(null, throwable -> {
										System.err.println("❌ Error al editar mensaje de expiración (desvinculación): "
												+ throwable.getMessage());
									});
						}
					}, 5, TimeUnit.MINUTES);
				});

		return LangManager.get("discord.unlink.requested");
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String messageId = event.getMessageId();

		if (event.getComponentId().equals("accept_link")) {
			UUID uuid = activeMessages.get(messageId);
			if (uuid == null) {
				event.reply(LangManager.get("discord.accept.expired")).setEphemeral(true).queue();
				return;
			}

			LinkRequest req = commandHandler.getRequest(uuid);
			if (req == null || !event.getUser().getName().equalsIgnoreCase(req.getDiscordName())) {
				event.reply(LangManager.get("discord.accept.unauthorized")).setEphemeral(true).queue();
				return;
			}

			if (JsonLinkStorage.isUuidLinked(uuid.toString())) {
				event.reply(LangManager.get("discord.accept.already-linked")).setEphemeral(true).queue();
				return;
			}

			if (JsonLinkStorage.isDiscordIdLinked(event.getUser().getId())) {
				event.reply(LangManager.get("discord.accept.already-used")).setEphemeral(true).queue();
				return;
			}

			JsonLinkStorage.saveLink(uuid.toString(), event.getUser().getId());
			commandHandler.complete(uuid);
			activeMessages.remove(messageId);

			event.getMessage().delete().queue(null, error -> {
				System.err.println(LangManager.get("discord.accept.error") + ": " + error.getMessage());
			});

			event.reply(LangManager.get("discord.accept.success")).setEphemeral(true).queue();
			return;
		}

		if (event.getComponentId().equals("confirm_unlink")) {
			UUID uuid = activeMessages.get(messageId);
			if (uuid == null) {
				event.reply(LangManager.get("discord.accept.expired")).setEphemeral(true).queue();
				return;
			}

			String expectedDiscordId = JsonLinkStorage.getDiscordIdByUUID(uuid.toString());
			if (expectedDiscordId == null || !event.getUser().getId().equals(expectedDiscordId)) {
				event.reply(LangManager.get("discord.unlink.confirm.unauthorized")).setEphemeral(true).queue();
				return;
			}

			if (JsonLinkStorage.deleteLinkByUUID(uuid.toString())) {
				event.getMessage().delete().queue(null, error -> {
					System.err.println(LangManager.get("discord.accept.error") + ": " + error.getMessage());
				});

				event.reply(LangManager.get("discord.unlink.confirm.success")).setEphemeral(true).queue();
			} else {
				event.reply(LangManager.get("discord.unlink.confirm.error")).setEphemeral(true).queue();
			}

			activeMessages.remove(messageId);
		}

		if (event.getComponentId().startsWith("confirm_auth_yes_")
				|| event.getComponentId().startsWith("confirm_auth_no_")) {
			String[] split = event.getComponentId().split("_");
			if (split.length < 4)
				return;

			UUID uuid = UUID.fromString(split[3]);
			boolean confirmado = event.getComponentId().contains("yes");

			if (DiscordConfirmationAPI.manejarRespuesta(uuid, confirmado)) {
				event.reply(LangManager.get("discord.confirmation.response")).setEphemeral(true).queue();

				event.getMessage().delete().queue(null, error -> {
					System.err.println(LangManager.get("discord.confirmation.error") + ": " + error.getMessage());
				});
			} else {
				event.reply(LangManager.get("discord.accept.expired")).setEphemeral(true).queue();
			}
		}
	}

	public LinkCommandHandler getCommandHandler() {
		return commandHandler;
	}

	public JDA getJda() {
		return jda;
	}

	public String getChannelId() {
		return Config.getChannelId();
	}

}
