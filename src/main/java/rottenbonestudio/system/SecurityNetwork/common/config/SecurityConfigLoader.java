package rottenbonestudio.system.SecurityNetwork.common.config;

import org.yaml.snakeyaml.Yaml;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.FilterRule;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.PerformanceConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.StorageConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SecurityConfigLoader {

	@SuppressWarnings("unchecked")
	public static IpCheckConfig load(Path configPath) throws IOException {
		Yaml yaml = new Yaml();
		try (InputStream in = Files.newInputStream(configPath)) {
			Object loaded = yaml.load(in);
			if (!(loaded instanceof Map)) {
				throw new IOException("config.yml is empty or invalid");
			}

			Map<String, Object> root = (Map<String, Object>) loaded;
			IpCheckConfig config = new IpCheckConfig();

			config.setLangCode(string(root, "lang", "es-es"));

			Map<String, Object> api = section(root, "api");
			config.proxyCheckApiKey = string(api, "proxycheck", "");
			config.ipQualityScoreApiKey = string(api, "ipQualityScore", "");
			config.ipHubApiKey = string(api, "ipHub", "");
			config.ipInfoApiKey = string(api, "ipinfo", "");
			config.ipGeoApiKey = string(api, "ipgeolocation", "");

			Map<String, Object> webhook = section(root, "webhook");
			config.webhookUrl = string(webhook, "discord", "");

			StorageConfig storage = new StorageConfig();
			Map<String, Object> storageSection = section(root, "storage");
			storage.type = string(storageSection, "type", "json");

			Map<String, Object> mysql = section(storageSection, "mysql");
			storage.mysqlHost = firstString(storageSection, mysql, "mysqlHost", "host", "localhost");
			storage.mysqlPort = firstInt(storageSection, mysql, "mysqlPort", "port", 3306);
			storage.mysqlDatabase = firstString(storageSection, mysql, "mysqlDatabase", "database", "network_security");
			storage.mysqlUser = firstString(storageSection, mysql, "mysqlUser", "user", "root");
			storage.mysqlPassword = firstString(storageSection, mysql, "mysqlPassword", "password", "");

			Map<String, Object> redis = section(storageSection, "redis");
			storage.RedisHost = string(redis, "host", "localhost");
			storage.RedisPort = integer(redis, "port", 6379);
			storage.RedisPassword = string(redis, "password", "");
			config.storage = storage;
			config.performance = performance(section(root, "performance"));

			config.countries = filterRule(section(root, "countries"), "blacklist");
			config.continents = filterRule(section(root, "continents"), "blacklist");

			return config;
		}
	}

	private static PerformanceConfig performance(Map<String, Object> section) {
		PerformanceConfig config = new PerformanceConfig();
		Map<String, Object> login = section(section, "login");
		Map<String, Object> api = section(section, "api");
		Map<String, Object> webhook = section(section, "webhook");

		config.loginMaxAttemptsPerIp = integer(login, "max-attempts-per-ip", config.loginMaxAttemptsPerIp);
		config.loginWindowSeconds = integer(login, "window-seconds", config.loginWindowSeconds);
		config.loginTempBlockSeconds = integer(login, "temp-block-seconds", config.loginTempBlockSeconds);
		config.maxGlobalInFlightChecks = integer(api, "max-global-in-flight", config.maxGlobalInFlightChecks);
		config.inFlightWaitTimeoutMs = integer(api, "in-flight-wait-timeout-ms", config.inFlightWaitTimeoutMs);
		config.apiFailureCacheSeconds = integer(api, "failure-cache-seconds", config.apiFailureCacheSeconds);
		config.webhookQueueSize = integer(webhook, "queue-size", config.webhookQueueSize);
		config.webhookTimeoutMs = integer(webhook, "timeout-ms", config.webhookTimeoutMs);
		return config;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> section(Map<String, Object> root, String key) {
		Object value = root.get(key);
		if (value instanceof Map) {
			return (Map<String, Object>) value;
		}
		return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	private static FilterRule filterRule(Map<String, Object> section, String defaultMode) {
		FilterRule rule = new FilterRule();
		rule.mode = string(section, "mode", defaultMode);
		Object list = section.get("list");
		rule.list = list instanceof List ? (List<String>) list : Collections.emptyList();
		return rule;
	}

	private static String firstString(Map<String, Object> flat, Map<String, Object> nested, String flatKey,
			String nestedKey, String fallback) {
		String nestedValue = string(nested, nestedKey, null);
		if (nestedValue != null) {
			return nestedValue;
		}
		return string(flat, flatKey, fallback);
	}

	private static int firstInt(Map<String, Object> flat, Map<String, Object> nested, String flatKey, String nestedKey,
			int fallback) {
		Object nestedValue = nested.get(nestedKey);
		if (nestedValue != null) {
			return integer(nested, nestedKey, fallback);
		}
		return integer(flat, flatKey, fallback);
	}

	private static String string(Map<String, Object> map, String key, String fallback) {
		Object value = map.get(key);
		if (value == null) {
			return fallback;
		}
		return String.valueOf(value);
	}

	private static int integer(Map<String, Object> map, String key, int fallback) {
		Object value = map.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value != null) {
			try {
				return Integer.parseInt(String.valueOf(value));
			} catch (NumberFormatException ignored) {
			}
		}
		return fallback;
	}
}
