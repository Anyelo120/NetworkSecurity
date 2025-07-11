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

	private final Map<String, Long> ipTempBlockedUntil = new ConcurrentHashMap<>();

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
		this.webhookNotifier = new DiscordWebhookNotifier(config.getWebhookUrl());
		this.geoAnalyzer = new GeoAnalyzer(config, ipInfoAPI, ipGeolocationAPI, ipWhoIsAPI, ipApiAPI, logger);
		this.proxyAnalyzer = new ProxyAnalyzer(config, proxyCheckAPI, ipQualityScoreAPI, ipHubAPI, logger);
		this.storage.initialize();

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
					IPAnalysisResult analysis = geoAnalyzer.analyze(ip);
					String hora = java.time.LocalTime.now().toString();
					String cuentas = storage.getPlayersLinkedToIP(ip);
					String pais = analysis.getCountryCode();
					String continente = analysis.getContinent();

					boolean confirmed = DiscordConfirmationAPI
							.solicitarConfirmacion(playerUUID, ip, pais, continente, hora, cuentas).get();

					if (!confirmed) {
						logger.warning("[Access Denied] El jugador no confirmó acceso desde nueva IP.");
						ipTempBlockedUntil.put(ip, System.currentTimeMillis() + 10 * 60 * 1000);
						return true;
					} else {
						logger.info("[Access Confirmed] Desbloqueando IP y actualizando datos...");
						ipTempBlockedUntil.remove(ip);
						storage.updatePlayerIP(uuid, ip);
						return false;
					}
				} catch (Exception e) {
					logger.warning("[Confirmation Error] No se pudo confirmar IP por Discord: " + e.getMessage());
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

		IPAnalysisResult partial = geoAnalyzer.analyze(ip);
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
			isProxy = proxyAnalyzer.isProxy(ip);

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

	public boolean isTempBlocked(String ip) {
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

}
