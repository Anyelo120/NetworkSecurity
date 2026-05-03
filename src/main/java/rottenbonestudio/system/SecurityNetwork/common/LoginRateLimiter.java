package rottenbonestudio.system.SecurityNetwork.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginRateLimiter {

	private final int maxAttempts;
	private final long windowMillis;
	private final long blockMillis;
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	public LoginRateLimiter(int maxAttempts, int windowSeconds, int blockSeconds) {
		this.maxAttempts = Math.max(1, maxAttempts);
		this.windowMillis = Math.max(1, windowSeconds) * 1000L;
		this.blockMillis = Math.max(1, blockSeconds) * 1000L;
	}

	public boolean isBlocked(String ip) {
		Bucket bucket = buckets.get(ip);
		if (bucket == null) {
			return false;
		}
		long now = System.currentTimeMillis();
		if (bucket.blockedUntil > now) {
			return true;
		}
		if (bucket.blockedUntil != 0) {
			buckets.remove(ip);
		}
		return false;
	}

	public boolean tryAcquire(String ip) {
		long now = System.currentTimeMillis();
		Bucket bucket = buckets.computeIfAbsent(ip, ignored -> new Bucket(now));
		synchronized (bucket) {
			if (bucket.blockedUntil > now) {
				return false;
			}
			if (now - bucket.windowStart > windowMillis) {
				bucket.windowStart = now;
				bucket.attempts = 0;
				bucket.blockedUntil = 0;
			}
			bucket.attempts++;
			bucket.lastSeen = now;
			if (bucket.attempts > maxAttempts) {
				bucket.blockedUntil = now + blockMillis;
				return false;
			}
			return true;
		}
	}

	public void cleanup() {
		long now = System.currentTimeMillis();
		long staleAfter = Math.max(windowMillis, blockMillis) * 2;
		buckets.entrySet().removeIf(entry -> {
			Bucket bucket = entry.getValue();
			if (bucket.blockedUntil == 0 && now - bucket.lastSeen > staleAfter) {
				return true;
			}
			return bucket.blockedUntil != 0 && bucket.blockedUntil < now && now - bucket.lastSeen > staleAfter;
		});
	}

	private static class Bucket {
		private long windowStart;
		private long lastSeen;
		private int attempts;
		private long blockedUntil;

		private Bucket(long now) {
			this.windowStart = now;
			this.lastSeen = now;
		}
	}
}
