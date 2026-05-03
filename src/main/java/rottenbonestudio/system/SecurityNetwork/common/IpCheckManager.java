package rottenbonestudio.system.SecurityNetwork.common;

import rottenbonestudio.system.SecurityNetwork.common.api.*;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpApiAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpGeolocationAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpInfoAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpWhoIsAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.VPN.IPQualityScoreAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.VPN.IpHubAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.VPN.ProxyCheckAPI;
import rottenbonestudio.system.SecurityNetwork.storage.*;
import rottenbonestudio.system.DiscordSystem.DiscordBot;
import rottenbonestudio.system.DiscordSystem.api.DiscordConfirmationAPI;
import rottenbonestudio.system.DiscordSystem.api.DiscordLinkAPI;
import rottenbonestudio.system.SecurityNetwork.common.analysis.GeoAnalyzer;
import rottenbonestudio.system.SecurityNetwork.common.analysis.ProxyAnalyzer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class IpCheckManager {

	private static final Logger logger = Logger.getLogger("NetworkSecurity");

	// VPN y PROXY
	private final ProxyCheckAPI proxyCheckAPI;
	private final IPQualityScoreAPI ipQualityScoreAPI;
	private final IpHubAPI ipHubAPI;
	// GEOLOCALIZADORES
	private final IpInfoAPI ipInfoAPI;
	private final IpGeolocationAPI ipGeolocationAPI;
	private final IpWhoIsAPI ipWhoIsAPI;
	private final IpApiAPI ipApiAPI;

	private final IpCheckConfig config;
	private final StorageProvider storage;
	private final DiscordWebhookNotifier webhookNotifier;
	private final GeoAnalyzer geoAnalyzer;
	private final ProxyAnalyzer proxyAnalyzer;
	private final LoginRateLimiter loginRateLimiter;
	private final Semaphore inFlightSemaphore;
	private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

	private final Map<String, Long> ipTempBlockedUntil = new ConcurrentHashMap<>();
	private final Map<String, CompletableFuture<IPAnalysisResult>> geoInFlight = new ConcurrentHashMap<>();
	private final Map<String, Long> apiFailureUntil = new ConcurrentHashMap<>();

	public IpCheckManager(IpCheckConfig config, DiscordBot discordBot) {
		this.config = config;

		// VPN y PROXY
		this.proxyCheckAPI = new ProxyCheckAPI(config.proxyCheckApiKey);
		this.ipQualityScoreAPI = new IPQualityScoreAPI(config.ipQualityScoreApiKey);
		this.ipHubAPI = new IpHubAPI(config.ipHubApiKey);

		// GEOLOCALIZADORES
		this.ipInfoAPI = new IpInfoAPI(config.ipInfoApiKey);
		this.ipGeolocationAPI = new IpGeolocationAPI(config.ipGeoApiKey);
		this.ipWhoIsAPI = new IpWhoIsAPI();
		this.ipApiAPI = new IpApiAPI();

		this.storage = createStorageProvider(config);
		this.webhookNotifier = new DiscordWebhookNotifier(config.getWebhookUrl(), config.getWebhookQueueSize(),
				config.getWebhookTimeoutMs());
		this.geoAnalyzer = new GeoAnalyzer(config, ipInfoAPI, ipGeolocationAPI, ipWhoIsAPI, ipApiAPI, logger);
		this.proxyAnalyzer = new ProxyAnalyzer(config, proxyCheckAPI, ipQualityScoreAPI, ipHubAPI, logger);
		this.loginRateLimiter = new LoginRateLimiter(config.getLoginMaxAttemptsPerIp(), config.getLoginWindowSeconds(),
				config.getLoginTempBlockSeconds());
		this.inFlightSemaphore = new Semaphore(config.getMaxGlobalInFlightChecks());
		this.storage.initialize();
		this.cleanupExecutor.scheduleAtFixedRate(this::cleanupRuntimeState, 1, 1, TimeUnit.MINUTES);

		if (discordBot != null) {
			DiscordConfirmationAPI.init(discordBot);
		}
	}

	private StorageProvider createStorageProvider(IpCheckConfig config) {
		if ("mysql".equalsIgnoreCase(config.getStorageType())) {
			return new MysqlStorageProvider(config.getMySQLHost(), config.getMySQLPort(), config.getMySQLDatabase(),
					config.getMySQLUser(), config.getMySQLPassword());

		} else if ("json".equalsIgnoreCase(config.getStorageType())) {
			return new JsonStorageProvider();

		} else if ("Mariadb".equalsIgnoreCase(config.getStorageType())) {
			return new MariaDBStorageProvider(config.getMySQLHost(), config.getMySQLPort(), config.getMySQLDatabase(),
					config.getMySQLUser(), config.getMySQLPassword());

		} else if ("redis".equalsIgnoreCase(config.getStorageType())) {
			return new RedisStorageProvider(config.getRedisHost(), config.getRedisPort(), config.getRedisPassword());

		} else {
			return new SqliteStorageProvider();
		}
	}

	public boolean verifyPlayerAccess(String uuid, String ip, boolean bypassVPN, boolean bypassCountry,
			boolean bypassContinent, boolean bypassAll) {
		if (bypassAll) {
			logger.info("[Bypass All] Player UUID=" + uuid + " bypassed all checks.");
			return false;
		}

		if (bypassVPN) {
			logger.info("[Bypass VPN] Player UUID=" + uuid + " bypassed VPN check.");
		}

		if (!loginRateLimiter.tryAcquire(ip)) {
			ipTempBlockedUntil.put(ip, System.currentTimeMillis() + config.getLoginTempBlockSeconds() * 1000L);
			return true;
		}

		Long blockedUntil = ipTempBlockedUntil.get(ip);
		if (blockedUntil != null) {
			if (System.currentTimeMillis() < blockedUntil) {
				return true;
			} else {
				ipTempBlockedUntil.remove(ip);
			}
		}

		String lastIp = storage.getLastIP(uuid);
		if (lastIp != null && !lastIp.equals(ip)) {
			UUID playerUUID = UUID.fromString(uuid);
			String discordId = DiscordLinkAPI.getDiscordIdByMinecraft(playerUUID);

			if (discordId != null && DiscordConfirmationAPI.isInitialized()) {
				logger.info("[IP Change Detected] Solicitando confirmación por Discord...");
				try {
					IPAnalysisResult analysis = analyzeGeo(ip);
					String hora = java.time.LocalTime.now().toString();
					String cuentas = storage.getPlayersLinkedToIP(ip);
					String pais = analysis.getCountryCode();
					String continente = analysis.getContinent();

					DiscordConfirmationAPI.solicitarConfirmacion(playerUUID, ip, pais, continente, hora, cuentas)
							.thenAccept(confirmed -> {
								if (confirmed) {
									logger.info("[Access Confirmed] Actualizando IP...");
									ipTempBlockedUntil.remove(ip);
									storage.updatePlayerIP(uuid, ip);
								} else {
									logger.warning("[Access Denied] El jugador no confirmó acceso.");
									ipTempBlockedUntil.put(ip, System.currentTimeMillis() + 10 * 60 * 1000);
								}
							});

					return true;
				} catch (Exception e) {
					logger.warning("[Confirmation Error] No se pudo confirmar IP: " + e.getMessage());
					return true;
				}
			} else {
				logger.info("[IP Change] El jugador no tiene Discord vinculado, omitiendo validación.");
			}
		}

		IPAnalysisResult result = storage.getCachedAnalysis(ip);

		if (result != null) {
			logger.info("[Cache] IP=" + ip + ", Proxy=" + result.isProxy() + ", Country=" + result.getCountryCode());
			if ((config.isCountryBlocked(result.getCountryCode()) && !bypassCountry)
					|| (config.isContinentBlocked(result.getContinent()) && !bypassContinent)) {
				logger.info("[Access Blocked from Cache] Country=" + result.getCountryCode() + ", Continent="
						+ result.getContinent());

				webhookNotifier.sendSecurityAlert(uuid, "webhook.reason.country-continent", result.getCountryCode(),
						result.getContinent());
				return true;
			}
			return result.isProxy() && !bypassVPN || storage.isCountryMismatch(uuid, result.getCountryCode());
		}

		if (isExternalAnalysisSaturated(ip)) {
			return true;
		}

		IPAnalysisResult partial = analyzeGeo(ip);
		if ((config.isCountryBlocked(partial.getCountryCode()) && !bypassCountry)
				|| (config.isContinentBlocked(partial.getContinent()) && !bypassContinent)) {

			storage.saveIP(ip, true, partial.getCountryCode(), partial.getContinent());
			logger.info("[Blocked Early] IP=" + ip + ", Country=" + partial.getCountryCode() + ", Continent="
					+ partial.getContinent());

			webhookNotifier.sendSecurityAlert(uuid, "webhook.reason.early-block", partial.getCountryCode(),
					partial.getContinent());
			return true;
		}

		boolean isProxy = false;
		if (!bypassVPN) {
			if (isExternalAnalysisSaturated(ip)) {
				return true;
			}
			isProxy = analyzeProxy(ip);

			if (isProxy) {
				webhookNotifier.sendSecurityAlert(uuid, "webhook.reason.proxy", ip, uuid);
			}
		}

		storage.saveIP(ip, isProxy, partial.getCountryCode(), partial.getContinent());
		boolean mismatch = storage.isCountryMismatch(uuid, partial.getCountryCode());
		if (mismatch) {
			webhookNotifier.sendSecurityAlert(uuid, "webhook.reason.mismatch");
		}

		boolean block = (isProxy && !bypassVPN) || mismatch;
		logger.info("[Final Check] IP=" + ip + ", UUID=" + uuid + ", Proxy=" + isProxy + ", Country="
				+ partial.getCountryCode() + ", Continent=" + partial.getContinent() + ", Mismatch=" + mismatch
				+ ", Blocked=" + block);

		if (!block) {
			storage.updatePlayerIP(uuid, ip);
		}

		return block;
	}

	private boolean isExternalAnalysisSaturated(String ip) {
		if (inFlightSemaphore.availablePermits() > 0) {
			return false;
		}
		ipTempBlockedUntil.put(ip, System.currentTimeMillis() + config.getLoginTempBlockSeconds() * 1000L);
		logger.warning("[Access Temporarily Blocked] External analysis saturated for IP=" + ip);
		return true;
	}

	public boolean isTempBlocked(String ip) {
		if (loginRateLimiter.isBlocked(ip)) {
			return true;
		}
		Long blockedUntil = ipTempBlockedUntil.get(ip);
		if (blockedUntil == null)
			return false;

		if (System.currentTimeMillis() < blockedUntil) {
			return true;
		} else {
			ipTempBlockedUntil.remove(ip);
			return false;
		}
	}

	public void testAllApisImproved(String ip) {
		logger.info("=== Iniciando test de APIs (VPN y GEO) para IP: " + ip + " ===");

		logger.info(">>> Test de servicios VPN/Proxy:");
		try {
			IPAnalysisResult result = proxyCheckAPI.analyzeIP(ip);
			logger.info("[ProxyCheck API] Proxy=" + result.isProxy());
		} catch (Exception e) {
			logger.warning("[ProxyCheck API] Falló: " + e.getMessage());
		}

		try {
			IPAnalysisResult result = ipQualityScoreAPI.analyzeIP(ip);
			logger.info("[IPQualityScore API] Proxy=" + result.isProxy());
		} catch (Exception e) {
			logger.warning("[IPQualityScore API] Falló: " + e.getMessage());
		}

		try {
			IPAnalysisResult result = ipHubAPI.analyzeIP(ip);
			logger.info("[IpHub API] Proxy=" + result.isProxy());
		} catch (Exception e) {
			logger.warning("[IpHub API] Falló: " + e.getMessage());
		}

		logger.info(">>> Test de servicios de Geolocalización:");
		try {
			IPAnalysisResult result = ipWhoIsAPI.analyzeIP(ip);
			logger.info("[IpWhoIs API] Country=" + result.getCountryCode() + ", Continent=" + result.getContinent());
		} catch (Exception e) {
			logger.warning("[IpWhoIs API] Falló: " + e.getMessage());
		}

		try {
			IPAnalysisResult result = ipInfoAPI.analyzeIP(ip);
			logger.info("[IpInfo API] Country=" + result.getCountryCode() + ", Continent=" + result.getContinent());
		} catch (Exception e) {
			logger.warning("[IpInfo API] Falló: " + e.getMessage());
		}

		try {
			IPAnalysisResult result = ipGeolocationAPI.analyzeIP(ip);
			logger.info(
					"[IpGeolocation API] Country=" + result.getCountryCode() + ", Continent=" + result.getContinent());
		} catch (Exception e) {
			logger.warning("[IpGeolocation API] Falló: " + e.getMessage());
		}

		try {
			IPAnalysisResult result = ipApiAPI.analyzeIP(ip);
			logger.info("[IpApi API] Country=" + result.getCountryCode() + ", Continent=" + result.getContinent());
		} catch (Exception e) {
			logger.warning("[IpApi API] Falló: " + e.getMessage());
		}

		logger.info("=== Fin del test de APIs ===");
	}

	public void removeIP(String ip) {
		storage.deleteIP(ip);
	}

	public int getBlockedCount() {
		return storage.countBlockedIPs();
	}

	public int getAllowedCount() {
		return storage.countAllowedIPs();
	}

	public boolean wipeAllData(boolean confirm) {
		return confirm && storage.confirmAndWipe(true);
	}

	private IPAnalysisResult analyzeGeo(String ip) {
		if (isApiFailureCached(ip)) {
			return new IPAnalysisResult("??", "Unknown", false);
		}

		CompletableFuture<IPAnalysisResult> created = new CompletableFuture<>();
		CompletableFuture<IPAnalysisResult> existing = geoInFlight.putIfAbsent(ip, created);
		if (existing != null) {
			try {
				return existing.get(config.getInFlightWaitTimeoutMs(), TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				return new IPAnalysisResult("??", "Unknown", false);
			}
		}

		boolean acquired = false;
		try {
			acquired = inFlightSemaphore.tryAcquire(config.getInFlightWaitTimeoutMs(), TimeUnit.MILLISECONDS);
			if (!acquired) {
				cacheApiFailure(ip);
				created.complete(new IPAnalysisResult("??", "Unknown", false));
				return created.join();
			}
			IPAnalysisResult result = geoAnalyzer.analyze(ip);
			created.complete(result);
			return result;
		} catch (Exception e) {
			cacheApiFailure(ip);
			created.complete(new IPAnalysisResult("??", "Unknown", false));
			return created.join();
		} finally {
			if (acquired) {
				inFlightSemaphore.release();
			}
			geoInFlight.remove(ip);
		}
	}

	private boolean analyzeProxy(String ip) {
		if (isApiFailureCached(ip)) {
			return false;
		}
		boolean acquired = false;
		try {
			acquired = inFlightSemaphore.tryAcquire(config.getInFlightWaitTimeoutMs(), TimeUnit.MILLISECONDS);
			if (!acquired) {
				cacheApiFailure(ip);
				return false;
			}
			return proxyAnalyzer.isProxy(ip);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			cacheApiFailure(ip);
			return false;
		} catch (Exception e) {
			cacheApiFailure(ip);
			return false;
		} finally {
			if (acquired) {
				inFlightSemaphore.release();
			}
		}
	}

	private boolean isApiFailureCached(String ip) {
		Long until = apiFailureUntil.get(ip);
		if (until == null) {
			return false;
		}
		if (System.currentTimeMillis() < until) {
			return true;
		}
		apiFailureUntil.remove(ip);
		return false;
	}

	private void cacheApiFailure(String ip) {
		apiFailureUntil.put(ip, System.currentTimeMillis() + config.getApiFailureCacheSeconds() * 1000L);
	}

	private void cleanupRuntimeState() {
		long now = System.currentTimeMillis();
		ipTempBlockedUntil.entrySet().removeIf(entry -> entry.getValue() < now);
		apiFailureUntil.entrySet().removeIf(entry -> entry.getValue() < now);
		loginRateLimiter.cleanup();
	}

	public void shutdown() {
		cleanupExecutor.shutdownNow();
		try {
			webhookNotifier.shutdown();
		} catch (Exception ignored) {
		}
		try {
			storage.shutdown();
		} catch (Exception ignored) {
		}
	}

}
