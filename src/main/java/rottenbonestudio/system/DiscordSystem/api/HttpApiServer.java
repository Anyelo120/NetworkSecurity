package rottenbonestudio.system.DiscordSystem.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import rottenbonestudio.system.DiscordSystem.config.Config;
import rottenbonestudio.system.DiscordSystem.storage.JsonLinkStorage;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class HttpApiServer {
	private static HttpServer server;
	private static final Pattern UUID_PATTERN = Pattern
			.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	private static final Pattern DISCORD_ID_PATTERN = Pattern.compile("^\\d{4,20}$");
	private static final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, FailedAttempts> failedAuth = new ConcurrentHashMap<>();
	private static final ScheduledExecutorService cleaner = Executors.newScheduledThreadPool(1);
	private static final int BUCKET_CAPACITY = 10;
	private static final double BUCKET_REFILL_PER_SECOND = 1.0;
	private static final int FAILED_AUTH_THRESHOLD = 5;
	private static final int FAILED_AUTH_BLOCK_SECONDS = 300;
	private static final int MAX_CONCURRENT = 50;
	private static final AtomicInteger concurrentRequests = new AtomicInteger(0);
	private static final AtomicLong rejectedTasksCounter = new AtomicLong(0);

	public static void start() throws IOException {
		int port = Config.getApiPort();
		server = HttpServer.create(new InetSocketAddress(port), 0);

		ThreadFactory threadFactory = r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("http-api-worker-" + t.getId());
			return t;
		};

		ThreadPoolExecutor executor = new ThreadPoolExecutor(4, MAX_CONCURRENT, 60L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(200), threadFactory, new RejectedExecutionHandler() {
					private final ThreadPoolExecutor.CallerRunsPolicy fallback = new ThreadPoolExecutor.CallerRunsPolicy();

					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor exec) {
						long totalRejected = rejectedTasksCounter.incrementAndGet();
						System.err.println("[HttpApiServer] Task rejected (count=" + totalRejected + "). active="
								+ exec.getActiveCount() + " queue=" + exec.getQueue().size() + " max="
								+ exec.getMaximumPoolSize());
						try {
							fallback.rejectedExecution(r, exec);
						} catch (Throwable t) {
							System.err.println("[HttpApiServer] CallerRunsPolicy failed: " + t.getMessage());
							try {
								attemptRespond503(r);
							} catch (Throwable ignored) {
							}
						}
					}
				});

		executor.allowCoreThreadTimeOut(true);
		server.setExecutor(executor);

		server.createContext("/api/getDiscordIdByMinecraft", exchange -> {
			if (!tryEnter(exchange))
				return;
			try {
				if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
					sendPlain(exchange, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
					return;
				}
				String ip = getRequesterIp(exchange);
				if (isBlocked(ip)) {
					sendRateLimited(exchange, "Too many failed auth attempts", FAILED_AUTH_BLOCK_SECONDS);
					return;
				}
				if (!consumeToken(ip)) {
					sendRateLimited(exchange, "Too many requests", 60);
					return;
				}
				Map<String, String> q = parseQuery(exchange.getRequestURI());
				String token = q.get("token");
				if (!isValidToken(token)) {
					recordFailedAuth(ip);
					sendJson(exchange, 401, new JSONObject().put("success", false).put("error", "Invalid token"));
					return;
				}
				resetFailedAuth(ip);
				String uuid = q.get("uuid");
				if (uuid == null || uuid.isEmpty()) {
					sendJson(exchange, 400,
							new JSONObject().put("success", false).put("error", "Missing uuid parameter"));
					return;
				}
				if (!UUID_PATTERN.matcher(uuid).matches()) {
					sendJson(exchange, 400, new JSONObject().put("success", false).put("error", "Invalid uuid format"));
					return;
				}
				String discordId = JsonLinkStorage.getDiscordIdByUUID(uuid);
				if (discordId == null) {
					sendJson(exchange, 200, new JSONObject().put("success", false).put("error", "Not linked"));
					return;
				}
				sendJson(exchange, 200, new JSONObject().put("success", true).put("discordId", discordId));
			} finally {
				exit();
			}
		});
		server.createContext("/api/getMinecraftUUIDByDiscordId", exchange -> {
			if (!tryEnter(exchange))
				return;
			try {
				if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
					sendPlain(exchange, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
					return;
				}
				String ip = getRequesterIp(exchange);
				if (isBlocked(ip)) {
					sendRateLimited(exchange, "Too many failed auth attempts", FAILED_AUTH_BLOCK_SECONDS);
					return;
				}
				if (!consumeToken(ip)) {
					sendRateLimited(exchange, "Too many requests", 60);
					return;
				}
				Map<String, String> q = parseQuery(exchange.getRequestURI());
				String token = q.get("token");
				if (!isValidToken(token)) {
					recordFailedAuth(ip);
					sendJson(exchange, 401, new JSONObject().put("success", false).put("error", "Invalid token"));
					return;
				}
				resetFailedAuth(ip);
				String discordId = q.get("discordId");
				if (discordId == null || discordId.isEmpty()) {
					sendJson(exchange, 400,
							new JSONObject().put("success", false).put("error", "Missing discordId parameter"));
					return;
				}
				if (!DISCORD_ID_PATTERN.matcher(discordId).matches()) {
					sendJson(exchange, 400,
							new JSONObject().put("success", false).put("error", "Invalid discordId format"));
					return;
				}
				String uuid = JsonLinkStorage.getUUIDByDiscordId(discordId);
				if (uuid == null) {
					sendJson(exchange, 200, new JSONObject().put("success", false).put("error", "Not linked"));
					return;
				}
				sendJson(exchange, 200, new JSONObject().put("success", true).put("uuid", uuid));
			} finally {
				exit();
			}
		});
		cleaner.scheduleAtFixedRate(HttpApiServer::cleanupMaps, 5, 5, TimeUnit.MINUTES);
		server.start();
		System.out.println("✔ HTTP API started on port " + port);
	}

	public static void stop() {
		if (server != null) {
			server.stop(0);
		}
		cleaner.shutdownNow();
	}

	private static Map<String, String> parseQuery(URI uri) {
		String q = uri.getRawQuery();
		if (q == null || q.isEmpty())
			return Collections.emptyMap();
		return Arrays.stream(q.split("&")).map(s -> {
			int idx = s.indexOf('=');
			if (idx == -1)
				return new String[] { s, "" };
			String k = urlDecode(s.substring(0, idx));
			String v = urlDecode(s.substring(idx + 1));
			return new String[] { k, v };
		}).collect(Collectors.toMap(a -> a[0], a -> a[1], (a, b) -> b));
	}

	private static String urlDecode(String s) {
		try {
			return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
		} catch (Exception e) {
			return s;
		}
	}

	private static void sendJson(HttpExchange exchange, int status, JSONObject json) throws IOException {
		byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
		Headers h = exchange.getResponseHeaders();
		h.set("Content-Type", "application/json; charset=utf-8");
		h.set("X-Content-Type-Options", "nosniff");
		h.set("Access-Control-Allow-Origin", "*");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static void sendPlain(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		Headers h = exchange.getResponseHeaders();
		h.set("Content-Type", "application/json; charset=utf-8");
		h.set("X-Content-Type-Options", "nosniff");
		h.set("Access-Control-Allow-Origin", "*");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static String getRequesterIp(HttpExchange exchange) {
		Headers headers = exchange.getRequestHeaders();
		if (headers.containsKey("X-Forwarded-For")) {
			String v = headers.getFirst("X-Forwarded-For");
			if (v != null && !v.isEmpty()) {
				int idx = v.indexOf(',');
				return idx > 0 ? v.substring(0, idx).trim() : v.trim();
			}
		}
		if (exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null) {
			return exchange.getRemoteAddress().getAddress().getHostAddress();
		}
		return "unknown";
	}

	private static boolean isValidToken(String provided) {
		String cfg = Config.getApiToken();
		if (cfg == null || cfg.isEmpty() || provided == null)
			return false;
		byte[] a = provided.getBytes(StandardCharsets.UTF_8);
		byte[] b = cfg.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(a, b);
	}

	private static boolean consumeToken(String ip) {
		TokenBucket tb = buckets.computeIfAbsent(ip, k -> new TokenBucket(BUCKET_CAPACITY, BUCKET_REFILL_PER_SECOND));
		return tb.tryConsume();
	}

	private static boolean isBlocked(String ip) {
		FailedAttempts fa = failedAuth.get(ip);
		if (fa == null)
			return false;
		if (fa.blockedUntil == 0)
			return false;
		return Instant.now().getEpochSecond() < fa.blockedUntil;
	}

	private static void recordFailedAuth(String ip) {
		FailedAttempts fa = failedAuth.computeIfAbsent(ip, k -> new FailedAttempts());
		int attempts = fa.increment();
		if (attempts >= FAILED_AUTH_THRESHOLD) {
			fa.blockedUntil = Instant.now().getEpochSecond() + FAILED_AUTH_BLOCK_SECONDS;
			fa.reset();
		}
	}

	private static void resetFailedAuth(String ip) {
		FailedAttempts fa = failedAuth.get(ip);
		if (fa != null)
			fa.reset();
	}

	private static void sendRateLimited(HttpExchange exchange, String reason, int retryAfterSeconds)
			throws IOException {
		Headers h = exchange.getResponseHeaders();
		h.set("Retry-After", String.valueOf(retryAfterSeconds));
		h.set("Content-Type", "application/json; charset=utf-8");
		h.set("X-Content-Type-Options", "nosniff");
		h.set("Access-Control-Allow-Origin", "*");
		JSONObject json = new JSONObject().put("success", false).put("error", "Rate limited").put("reason", reason);
		byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(429, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static void cleanupMaps() {
		long cutoff = Instant.now().getEpochSecond() - 10 * 60;
		buckets.entrySet().removeIf(e -> e.getValue().lastSeen < cutoff);
		failedAuth.entrySet().removeIf(e -> e.getValue().lastSeen < cutoff && e.getValue().blockedUntil == 0);
	}

	private static boolean tryEnter(HttpExchange exchange) throws IOException {
		int cur = concurrentRequests.incrementAndGet();
		if (cur > MAX_CONCURRENT) {
			concurrentRequests.decrementAndGet();
			sendPlain(exchange, 503, "{\"success\":false,\"error\":\"Server too busy\"}");
			return false;
		}
		return true;
	}

	private static void exit() {
		concurrentRequests.decrementAndGet();
	}

	private static void attemptRespond503(Runnable r) {
		try {
			if (r == null)
				return;
			Field[] fields = r.getClass().getDeclaredFields();
			for (Field f : fields) {
				try {
					f.setAccessible(true);
					Object val = f.get(r);
					if (val instanceof HttpExchange) {
						HttpExchange ex = (HttpExchange) val;
						sendPlain(ex, 503, "{\"success\":false,\"error\":\"Server too busy\"}");
						return;
					}
				} catch (Throwable ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
	}

	private static class TokenBucket {
		private final int capacity;
		private final double refillPerSecond;
		private double tokens;
		private long lastRefill;
		private long lastSeen;

		TokenBucket(int capacity, double refillPerSecond) {
			this.capacity = capacity;
			this.refillPerSecond = refillPerSecond;
			this.tokens = capacity;
			this.lastRefill = System.nanoTime();
			this.lastSeen = Instant.now().getEpochSecond();
		}

		synchronized boolean tryConsume() {
			long now = System.nanoTime();
			double seconds = (now - lastRefill) / 1_000_000_000.0;
			if (seconds > 0) {
				double add = seconds * refillPerSecond;
				tokens = Math.min(capacity, tokens + add);
				lastRefill = now;
			}
			lastSeen = Instant.now().getEpochSecond();
			if (tokens >= 1.0) {
				tokens -= 1.0;
				return true;
			}
			return false;
		}
	}

	private static class FailedAttempts {
		private int attempts = 0;
		private long blockedUntil = 0;
		private long lastSeen = Instant.now().getEpochSecond();

		synchronized int increment() {
			lastSeen = Instant.now().getEpochSecond();
			attempts++;
			return attempts;
		}

		synchronized void reset() {
			attempts = 0;
			lastSeen = Instant.now().getEpochSecond();
		}
	}

}
