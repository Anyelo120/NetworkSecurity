package rottenbonestudio.system.DiscordSystem.log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiscordLogManager {

	private static String webhookUrl;
	private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static volatile boolean initialized = false;

	public static void init(String url) {
		if (url == null || url.isEmpty()) {
			return;
		}
		webhookUrl = url;
		initialized = true;
		scheduler.scheduleWithFixedDelay(DiscordLogManager::processQueue, 0, 1, TimeUnit.SECONDS);
		System.out.println("[DiscordLogManager] Inicializado con webhook de logs.");
	}

	public static void shutdown() {
		initialized = false;
		try {
			scheduler.shutdownNow();
		} catch (Exception ignored) {
		}
		queue.clear();
	}

	public static void logLink(String uuid, String nick, String discordId) {
		if (!initialized)
			return;
		String namePart = nick != null && !nick.isEmpty() ? uuid + " (" + nick + ")" : uuid;
		String content = "✅ Vinculación exitosa\n" + "• Minecraft: `" + namePart + "`\n" + "• Discord ID: `" + discordId
				+ "`\n" + "• Timestamp: `" + Instant.now().toString() + "`";
		enqueue(content);
	}

	public static void logUnlink(String uuid, String nick, String discordId) {
		if (!initialized)
			return;
		String namePart = nick != null && !nick.isEmpty() ? uuid + " (" + nick + ")" : uuid;
		String content = "🗑️ Desvinculación exitosa\n" + "• Minecraft: `" + namePart + "`\n" + "• Discord ID: `"
				+ discordId + "`\n" + "• Timestamp: `" + Instant.now().toString() + "`";
		enqueue(content);
	}

	private static void enqueue(String content) {
		queue.offer(content);
	}

	private static void processQueue() {
		if (!initialized)
			return;
		String content = queue.poll();
		if (content == null)
			return;
		sendWebhook(content);
	}

	private static void sendWebhook(String content) {
		try {
			URL url = new URL(webhookUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/json");

			String payload = "{\"content\":" + toJsonString(content) + "}";

			try (OutputStream os = conn.getOutputStream()) {
				os.write(payload.getBytes(StandardCharsets.UTF_8));
			}

			int code = conn.getResponseCode();
			if (code / 100 != 2) {
				System.err.println("❌ Error enviando log a webhook: HTTP " + code);
			}
			conn.disconnect();
		} catch (Exception e) {
			System.err.println("❌ Error enviando log a webhook: " + e.getMessage());
		}
	}

	private static String toJsonString(String text) {
		String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
		return "\"" + escaped + "\"";
	}
}
