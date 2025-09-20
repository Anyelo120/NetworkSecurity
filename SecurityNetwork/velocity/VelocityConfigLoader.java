package rottenbonestudio.system.SecurityNetwork.velocity;

import org.yaml.snakeyaml.Yaml;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.FilterRule;
import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig.StorageConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VelocityConfigLoader {

	private final Path configPath;
	private static final Logger logger = LoggerFactory.getLogger(VelocityConfigLoader.class);

	public VelocityConfigLoader(Path configPath) {
		this.configPath = configPath;
	}

	@SuppressWarnings("unchecked")
	public IpCheckConfig load() throws IOException {
		Yaml yaml = new Yaml();
		try (InputStream in = new FileInputStream(configPath.toFile())) {
			logger.info("Cargando configuración desde: " + configPath.toString());
			Map<String, Object> root = yaml.load(in);

			IpCheckConfig config = new IpCheckConfig();

			Object webhookSection = root.get("webhook");
			if (webhookSection instanceof Map) {
				Map<String, Object> webhookMap = (Map<String, Object>) webhookSection;
				config.webhookUrl = (String) webhookMap.get("discord");
				logger.info("Discord webhook loaded: " + (config.webhookUrl != null ? "Present" : "Missing"));
			} else {
				logger.warn("Webhook section missing or malformed in config.yml");
			}

			Map<String, Object> apiSection = (Map<String, Object>) root.get("api");
			if (apiSection != null) {
				// VPN Y PROXY
				config.proxyCheckApiKey = (String) apiSection.get("proxycheck");
				config.ipQualityScoreApiKey = (String) apiSection.get("ipQualityScore");
				config.ipHubApiKey = (String) apiSection.get("ipHub");

				// GEOLOCALIZADORES
				config.ipInfoApiKey = (String) apiSection.get("ipinfo");
				config.ipGeoApiKey = (String) apiSection.get("ipgeolocation");
				logger.info("API keys loaded successfully.");
			} else {
				logger.error("API section is missing in config.yml");
				throw new IllegalArgumentException("API section is missing in config.yml");
			}

			Map<String, Object> storageSection = (Map<String, Object>) root.get("storage");
			if (storageSection != null) {
				StorageConfig storage = new StorageConfig();
				storage.type = (String) storageSection.get("type");
				if ("mysql".equalsIgnoreCase(storage.type) || "mariadb".equalsIgnoreCase(storage.type)) {
					storage.mysqlHost = (String) storageSection.get("mysqlHost");
					storage.mysqlPort = (int) storageSection.get("mysqlPort");
					storage.mysqlDatabase = (String) storageSection.get("mysqlDatabase");
					storage.mysqlUser = (String) storageSection.get("mysqlUser");
					storage.mysqlPassword = (String) storageSection.get("mysqlPassword");
					logger.info("MySQL storage configuration loaded.");
				} else if ("redis".equalsIgnoreCase(storage.type)) {
					storage.RedisHost = (String) storageSection.get("storage.redis.host");
					storage.RedisPort = (int) storageSection.get("storage.redis.port");
					storage.RedisPassword = (String) storageSection.get("storage.redis.password");
				} else {
					logger.warn("Using default storage type.");
				}
				config.storage = storage;
			} else {
				logger.error("Storage section is missing in config.yml");
				throw new IllegalArgumentException("Storage section is missing in config.yml");
			}

			Map<String, Object> countrySection = (Map<String, Object>) root.get("countries");
			if (countrySection != null) {
				FilterRule countries = new FilterRule();
				countries.mode = (String) countrySection.get("mode");
				countries.list = (java.util.List<String>) countrySection.get("list");
				config.countries = countries;
				logger.info("Countries filter loaded.");
			} else {
				logger.error("Countries section is missing in config.yml");
				throw new IllegalArgumentException("Countries section is missing in config.yml");
			}

			Map<String, Object> continentSection = (Map<String, Object>) root.get("continents");
			if (continentSection != null) {
				FilterRule continents = new FilterRule();
				continents.mode = (String) continentSection.get("mode");
				continents.list = (java.util.List<String>) continentSection.get("list");
				config.continents = continents;
				logger.info("Continents filter loaded.");
			} else {
				logger.error("Continents section is missing in config.yml");
				throw new IllegalArgumentException("Continents section is missing in config.yml");
			}

			logger.info("Configuration loaded successfully.");
			return config;

		} catch (Exception e) {
			logger.error("Error loading configuration: " + e.getMessage());
			throw new IOException("Error loading config", e);
		}
	}

}
