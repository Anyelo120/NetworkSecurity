package rottenbonestudio.system.SecurityNetwork.common;

import java.util.List;

public class IpCheckConfig {

	private String langCode = "es-es";
	
	// DETECTOR DE VPN Y PROXY
	public String proxyCheckApiKey;
	public String ipQualityScoreApiKey;
	public String ipHubApiKey;

	// LOCALIZADOR GEOGRAFICO
	public String ipInfoApiKey;
	public String ipGeoApiKey;

	public String webhookUrl;

	public FilterRule countries;
	public FilterRule continents;
	public StorageConfig storage;

	public String getLangCode() {
		return langCode;
	}
	
	public boolean isProxyCheckEnabled() {
		return (proxyCheckApiKey != null && !proxyCheckApiKey.isEmpty())
				|| (ipQualityScoreApiKey != null && !ipQualityScoreApiKey.isEmpty())
				|| (ipHubApiKey != null && !ipHubApiKey.isEmpty());
	}

	public boolean isGeoCheckEnabled() {
		return (ipInfoApiKey != null && !ipInfoApiKey.isEmpty() || ipGeoApiKey != null && !ipGeoApiKey.isEmpty());
	}

	public boolean isCountryBlocked(String countryCode) {
		return countries != null && countries.isBlocked(countryCode.toUpperCase());
	}

	public boolean isContinentBlocked(String continent) {
		return continents != null && continents.isBlocked(continent);
	}

	public String getStorageType() {
		return storage != null ? storage.type : "sqlite";
	}

	public String getMySQLHost() {
		return storage != null ? storage.mysqlHost : "localhost";
	}

	public int getMySQLPort() {
		return storage != null ? storage.mysqlPort : 3306;
	}

	public String getMySQLDatabase() {
		return storage != null ? storage.mysqlDatabase : "network_security";
	}

	public String getMySQLUser() {
		return storage != null ? storage.mysqlUser : "root";
	}

	public String getMySQLPassword() {
		return storage != null ? storage.mysqlPassword : "";
	}

	public String getWebhookUrl() {
		return this.webhookUrl;
	}

	public static class FilterRule {
		public String mode;
		public List<String> list;

		public boolean isBlocked(String value) {
			if (list == null || list.isEmpty())
				return false;

			if ("whitelist".equalsIgnoreCase(mode)) {
				return !list.contains(value);
			} else {
				return list.contains(value);
			}
		}
	}

	public static class StorageConfig {
		public String type;
		public String mysqlHost;
		public int mysqlPort;
		public String mysqlDatabase;
		public String mysqlUser;
		public String mysqlPassword;
	}

}
