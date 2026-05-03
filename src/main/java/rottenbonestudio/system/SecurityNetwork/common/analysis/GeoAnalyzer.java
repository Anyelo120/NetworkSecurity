package rottenbonestudio.system.SecurityNetwork.common.analysis;

import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpApiAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpGeolocationAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpInfoAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.GEO.IpWhoIsAPI;

import java.util.logging.Logger;

public class GeoAnalyzer {

	private final IpCheckConfig config;

	private final IpInfoAPI ipInfoAPI;
	private final IpGeolocationAPI ipGeolocationAPI;
	private final IpWhoIsAPI ipWhoIsAPI;
	private final IpApiAPI ipApiAPI;

	private final Logger logger;

	public GeoAnalyzer(IpCheckConfig config, IpInfoAPI ipInfoAPI, IpGeolocationAPI ipGeolocationAPI,
			IpWhoIsAPI ipWhoIsAPI, IpApiAPI ipApiAPI, Logger logger) {
		this.config = config;
		this.ipInfoAPI = ipInfoAPI;
		this.ipGeolocationAPI = ipGeolocationAPI;
		this.ipWhoIsAPI = ipWhoIsAPI;
		this.ipApiAPI = ipApiAPI;
		this.logger = logger;
	}

	public IPAnalysisResult analyze(String ip) {
		String country = "??";
		String continent = "Unknown";

		logger.info("[GeoAnalyzer] Iniciando análisis de IP: " + ip);

		try {
			IPAnalysisResult result = ipWhoIsAPI.analyzeIP(ip);
			country = result.getCountryCode();
			continent = result.getContinent();
			logger.info("[IpWhoIs] Country=" + country + ", Continent=" + continent);
			return new IPAnalysisResult(country, continent, false);
		} catch (Exception e) {
			logger.warning("IpWhoIs falló: " + e.getMessage());
		}

		if (config.ipInfoApiKey == null || config.ipInfoApiKey.isEmpty()) {
			logger.warning("[IpInfo] API Key no configurada.");
		} else {
			try {
				IPAnalysisResult result = ipInfoAPI.analyzeIP(ip);
				country = result.getCountryCode();
				continent = result.getContinent();
				logger.info("[IpInfo] Country=" + country + ", Continent=" + continent);
				return new IPAnalysisResult(country, continent, false);
			} catch (Exception e) {
				logger.warning("IpInfo falló: " + e.getMessage());
			}
		}

		if (config.ipGeoApiKey == null || config.ipGeoApiKey.isEmpty()) {
			logger.warning("[IpGeolocation] API Key no configurada.");
		} else {
			try {
				IPAnalysisResult result = ipGeolocationAPI.analyzeIP(ip);
				country = result.getCountryCode();
				continent = result.getContinent();
				logger.info("[IpGeo] Country=" + country + ", Continent=" + continent);
				return new IPAnalysisResult(country, continent, false);
			} catch (Exception e) {
				logger.warning("IpGeolocation falló: " + e.getMessage());
			}
		}

		try {
			IPAnalysisResult result = ipApiAPI.analyzeIP(ip);
			country = result.getCountryCode();
			continent = result.getContinent();
			logger.info("[IpApi] Country=" + country + ", Continent=" + continent);
			return new IPAnalysisResult(country, continent, false);
		} catch (Exception e) {
			logger.warning("IpApi falló: " + e.getMessage());
		}

		return new IPAnalysisResult(country, continent, false);
	}

}
