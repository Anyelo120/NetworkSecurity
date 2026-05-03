package rottenbonestudio.system.SecurityNetwork.common.analysis;

import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;
import rottenbonestudio.system.SecurityNetwork.common.api.VPN.IPQualityScoreAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.VPN.IpHubAPI;
import rottenbonestudio.system.SecurityNetwork.common.api.VPN.ProxyCheckAPI;

import java.util.logging.Logger;

public class ProxyAnalyzer {

	private final IpCheckConfig config;

	private final ProxyCheckAPI proxyCheckAPI;
	private final IPQualityScoreAPI ipQualityScoreAPI;
	private final IpHubAPI ipHubAPI;

	private final Logger logger;

	public ProxyAnalyzer(IpCheckConfig config, ProxyCheckAPI proxyCheckAPI, IPQualityScoreAPI ipQualityScoreAPI,
			IpHubAPI ipHubAPI, Logger logger) {
		this.config = config;
		this.proxyCheckAPI = proxyCheckAPI;
		this.ipQualityScoreAPI = ipQualityScoreAPI;
		this.ipHubAPI = ipHubAPI;
		this.logger = logger;
	}

	public boolean isProxy(String ip) {
		if (!config.isProxyCheckEnabled())
			return false;

		if (config.proxyCheckApiKey == null || config.proxyCheckApiKey.isEmpty()) {
			logger.warning("[ProxyCheck] API Key no configurada.");
		} else {
			try {
				IPAnalysisResult result = proxyCheckAPI.analyzeIP(ip);
				if (result.isProxy()) {
					logger.info("[ProxyCheck] Proxy detected.");
					return true;
				}
			} catch (Exception e) {
				logger.warning("ProxyCheck falló: " + e.getMessage());
			}
		}

		if (config.ipQualityScoreApiKey == null || config.ipQualityScoreApiKey.isEmpty()) {
			logger.warning("[IPQualityScore] API Key no configurada.");
		} else {
			try {
				IPAnalysisResult result = ipQualityScoreAPI.analyzeIP(ip);
				if (result.isProxy()) {
					logger.info("[IPQualityScore] Proxy detected.");
					return true;
				}
			} catch (Exception e) {
				logger.warning("IPQualityScore falló: " + e.getMessage());
			}
		}

		if (config.ipHubApiKey == null || config.ipHubApiKey.isEmpty()) {
			logger.warning("[IpHub] API Key no configurada.");
		} else {
			try {
				IPAnalysisResult result = ipHubAPI.analyzeIP(ip);
				if (result.isProxy()) {
					logger.info("[IpHub] Proxy detected.");
					return true;
				}
			} catch (Exception e) {
				logger.warning("IpHub falló: " + e.getMessage());
			}
		}

		return false;
	}

}
