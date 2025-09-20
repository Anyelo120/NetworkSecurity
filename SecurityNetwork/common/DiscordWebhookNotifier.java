package rottenbonestudio.system.SecurityNetwork.common;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class DiscordWebhookNotifier {

    private static final Logger logger = Logger.getLogger("NetworkSecurity");

    private final String webhookUrl;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ZoneId timeZone = ZoneId.of("America/Santiago");

    public DiscordWebhookNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendSecurityAlert(String playerId, String reasonKey, String... reasonArgs) {
    	if (webhookUrl == null || webhookUrl.isEmpty()) {
    		logger.warning("[DiscordWebhook] Webhook URL not configured.");
    		return;
    	}

    	executorService.submit(() -> {
    		try {
    			String time = ZonedDateTime.now(timeZone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    			String prefix = LangManager.get("webhook.prefix.time", time);
    			String blockedMsg = LangManager.get("webhook.message.blocked", playerId);
    			String reason = LangManager.get(reasonKey, reasonArgs);

    			String message = prefix + " " + blockedMsg + "\n" + reason;
    			String jsonPayload = "{\"content\": \"" + escapeJson(message) + "\"}";

    			URL url = new URL(webhookUrl);
    			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    			connection.setRequestMethod("POST");
    			connection.setRequestProperty("Content-Type", "application/json");
    			connection.setDoOutput(true);

    			try (OutputStream os = connection.getOutputStream()) {
    				byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
    				os.write(input, 0, input.length);
    			}

    			int responseCode = connection.getResponseCode();
    			if (responseCode == 204 || responseCode == 200) {
    				logger.info("[DiscordWebhook] Alert sent for " + playerId);
    			} else {
    				logger.warning("[DiscordWebhook] Failed to send alert (HTTP " + responseCode + ")");
    			}

    		} catch (Exception e) {
    			logger.warning("[DiscordWebhook] Error sending alert: " + e.getMessage());
    		}
    	});
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    public void shutdown() {
        executorService.shutdown();
    }
    
}
