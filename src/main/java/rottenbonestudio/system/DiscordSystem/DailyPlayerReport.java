package rottenbonestudio.system.DiscordSystem;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import rottenbonestudio.system.DiscordSystem.config.Config;
import rottenbonestudio.system.SecurityNetwork.common.LangManager;

import java.awt.Color;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyPlayerReport {

	private final ServerStatsProvider statsProvider;
	private final DiscordBot discordBot;
	private final ScheduledExecutorService scheduler;

	public DailyPlayerReport(ServerStatsProvider statsProvider, DiscordBot discordBot) {
		this.statsProvider = statsProvider;
		this.discordBot = discordBot;
		this.scheduler = Executors.newScheduledThreadPool(1);
	}

	public void start() {
		if (!Config.isDailyReportEnabled()) {
			System.out.println(LangManager.get("daily-report.disabled"));
			return;
		}

		String dailyReportChannelId = Config.getDailyReportChannelId();
		if (dailyReportChannelId == null || dailyReportChannelId.isEmpty()) {
			System.err.println(LangManager.get("daily-report.channel-missing"));
			return;
		}

		String reportTime = Config.getDailyReportTime();
		String[] timeParts = reportTime.split(":");

		if (timeParts.length != 2) {
			System.err.println(LangManager.get("daily-report.invalid-time", reportTime));
			return;
		}

		try {
			int targetHour = Integer.parseInt(timeParts[0]);
			int targetMinute = Integer.parseInt(timeParts[1]);

			ZoneId chileZone = ZoneId.of("America/Santiago");
			ZonedDateTime now = ZonedDateTime.now(chileZone);
			ZonedDateTime nextRun = now.withHour(targetHour).withMinute(targetMinute).withSecond(0);

			if (now.compareTo(nextRun) > 0) {
				nextRun = nextRun.plusDays(1);
			}

			long initialDelay = java.time.Duration.between(now, nextRun).getSeconds();

			scheduler.scheduleAtFixedRate(this::sendDailyReport, initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);

			System.out.println(LangManager.get("daily-report.scheduled", reportTime));

		} catch (NumberFormatException e) {
			System.err.println(LangManager.get("daily-report.time-parse-error", e.getMessage()));
		}
	}

	private void sendDailyReport() {
		try {
			int onlinePlayers = statsProvider.getOnlinePlayers();
			String serverName = Config.getServerName();
			String dailyReportChannelId = Config.getDailyReportChannelId();

			if (dailyReportChannelId == null || dailyReportChannelId.isEmpty()) {
				System.err.println(LangManager.get("daily-report.channel-missing"));
				return;
			}

			EmbedBuilder embed = new EmbedBuilder();
			embed.setTitle(LangManager.get("daily-report.embed.title", serverName));
			embed.setColor(getColorByPlayerCount(onlinePlayers));

			embed.addField(LangManager.get("daily-report.embed.players.title"),
					LangManager.get("daily-report.embed.players.value", String.valueOf(onlinePlayers)), false);

			embed.addField(LangManager.get("daily-report.embed.status.title"),
					onlinePlayers > 0 ? LangManager.get("daily-report.embed.status.online")
							: LangManager.get("daily-report.embed.status.empty"),
					false);

			String maxPlayers = String.valueOf(statsProvider.getMaxPlayers());
			embed.addField(LangManager.get("daily-report.embed.capacity.title"),
					LangManager.get("daily-report.embed.capacity.value", maxPlayers), false);

			ZonedDateTime chileTime = ZonedDateTime.now(ZoneId.of("America/Santiago"));
			String formattedTime = chileTime.getHour() + ":" + String.format("%02d", chileTime.getMinute());
			embed.setFooter(LangManager.get("daily-report.embed.footer", formattedTime));

			TextChannel channel = discordBot.getJda().getTextChannelById(dailyReportChannelId);
			if (channel != null) {
				channel.sendMessageEmbeds(embed.build()).queue(success -> System.out.println(LangManager.get("daily-report.sent")),
						error -> System.err.println(LangManager.get("daily-report.send-error", error.getMessage())));
			} else {
				System.err.println(LangManager.get("daily-report.channel-not-found", dailyReportChannelId));
			}

		} catch (Exception e) {
			System.err.println(LangManager.get("daily-report.error", e.getMessage()));
			e.printStackTrace();
		}
	}

	private Color getColorByPlayerCount(int playerCount) {
		if (playerCount == 0)
			return Color.RED;
		if (playerCount < 5)
			return Color.ORANGE;
		if (playerCount < 15)
			return Color.YELLOW;
		return Color.GREEN;
	}

	public void stop() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}
}
