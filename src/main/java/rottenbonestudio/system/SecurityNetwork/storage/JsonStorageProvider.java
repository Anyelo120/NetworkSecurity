package rottenbonestudio.system.SecurityNetwork.storage;

import org.json.JSONArray;
import org.json.JSONObject;
import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class JsonStorageProvider implements StorageProvider {

	private static final String BASE_FOLDER = "plugins/securitynetwork";
	private static final String STORAGE_FOLDER = BASE_FOLDER + "/storage/json";

	private static final String CACHE_FILE = STORAGE_FOLDER + "/cache.json";
	private static final String PLAYER_COUNTRY_FILE = STORAGE_FOLDER + "/player_country.json";
	private static final String PLAYER_IP_FILE = STORAGE_FOLDER + "/player_ip.json";

	private static final Logger logger = Logger.getLogger("NetworkSecurity");

	private Map<String, IPAnalysisResult> ipCache = new HashMap<>();
	private Map<String, String> playerCountry = new HashMap<>();
	private Map<String, String> playerIpMap = new HashMap<>();

	@Override
	public void initialize() {
		try {
			File folder = new File(STORAGE_FOLDER);
			if (!folder.exists() && folder.mkdirs()) {
				logger.info("[JSON] Created storage folder: " + folder.getPath());
			}

			migrateIfExists(BASE_FOLDER + "/cache.json", CACHE_FILE);
			migrateIfExists(BASE_FOLDER + "/player_country.json", PLAYER_COUNTRY_FILE);
			migrateIfExists(BASE_FOLDER + "/player_ip.json", PLAYER_IP_FILE);

			ensureFileExists(CACHE_FILE);
			ensureFileExists(PLAYER_COUNTRY_FILE);
			ensureFileExists(PLAYER_IP_FILE);

			loadPlayerIP();
			loadCache();
			loadPlayerCountry();

		} catch (Exception e) {
			logger.warning("[JSON] Initialization error: " + e.getMessage());
		}
	}

	private void migrateIfExists(String oldPathStr, String newPathStr) throws IOException {
		Path oldPath = Paths.get(oldPathStr);
		Path newPath = Paths.get(newPathStr);
		if (Files.exists(oldPath) && !Files.exists(newPath)) {
			Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
			logger.info("[JSON] Migrated file: " + oldPath.getFileName() + " -> " + newPath);
		}
	}

	private void ensureFileExists(String filePath) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			file.createNewFile();
			logger.info("[JSON] Created missing file: " + filePath);
		}
	}

	@Override
	public void saveIP(String ip, boolean isBlocked, String country, String continent) {
		try {
			IPAnalysisResult result = new IPAnalysisResult(country, continent, isBlocked);
			ipCache.put(ip, result);

			JSONArray jsonArray = new JSONArray();
			for (Map.Entry<String, IPAnalysisResult> entry : ipCache.entrySet()) {
				JSONObject ipJson = new JSONObject();
				ipJson.put("ip", entry.getKey());
				ipJson.put("blocked", entry.getValue().isProxy());
				ipJson.put("country", entry.getValue().getCountryCode());
				ipJson.put("continent", entry.getValue().getContinent());
				jsonArray.put(ipJson);
			}

			try (FileWriter writer = new FileWriter(CACHE_FILE)) {
				writer.write(jsonArray.toString());
			}

		} catch (IOException e) {
			logger.warning("[JSON] Save IP error: " + e.getMessage());
		}
	}

	@Override
	public IPAnalysisResult getCachedAnalysis(String ip) {
		return ipCache.getOrDefault(ip, null);
	}

	@Override
	public boolean isCountryMismatch(String uuid, String currentCountry) {
		String savedCountry = playerCountry.get(uuid);
		if (savedCountry != null) {
			return !savedCountry.equalsIgnoreCase(currentCountry);
		}

		playerCountry.put(uuid, currentCountry);
		savePlayerCountry();

		return false;
	}

	private void loadCache() {
		try (FileReader reader = new FileReader(CACHE_FILE)) {
			StringBuilder jsonContent = new StringBuilder();
			int i;
			while ((i = reader.read()) != -1) {
				jsonContent.append((char) i);
			}

			JSONArray jsonArray = new JSONArray(jsonContent.toString());
			for (int j = 0; j < jsonArray.length(); j++) {
				JSONObject json = jsonArray.getJSONObject(j);
				String ip = json.getString("ip");
				boolean blocked = json.getBoolean("blocked");
				String country = json.getString("country");
				String continent = json.getString("continent");

				ipCache.put(ip, new IPAnalysisResult(country, continent, blocked));
			}

		} catch (IOException e) {
			logger.warning("[JSON] Error loading IP cache: " + e.getMessage());
		}
	}

	private void loadPlayerCountry() {
		try (FileReader reader = new FileReader(PLAYER_COUNTRY_FILE)) {
			StringBuilder jsonContent = new StringBuilder();
			int i;
			while ((i = reader.read()) != -1) {
				jsonContent.append((char) i);
			}

			JSONObject json = new JSONObject(jsonContent.toString());
			json.keys().forEachRemaining(uuid -> {
				String country = json.getString(uuid);
				playerCountry.put(uuid, country);
			});

		} catch (IOException e) {
			logger.warning("[JSON] Error loading player country data: " + e.getMessage());
		}
	}

	private void savePlayerCountry() {
		try {
			JSONObject json = new JSONObject(playerCountry);
			try (FileWriter writer = new FileWriter(PLAYER_COUNTRY_FILE)) {
				writer.write(json.toString());
			}

		} catch (IOException e) {
			logger.warning("[JSON] Error saving player country data: " + e.getMessage());
		}
	}

	@Override
	public void deleteIP(String ip) {
		if (ipCache.remove(ip) != null) {
			saveCacheToFile();
			logger.info("[JSON] Deleted IP from cache: " + ip);
		} else {
			logger.info("[JSON] IP not found in cache: " + ip);
		}
	}

	@Override
	public int countBlockedIPs() {
		return (int) ipCache.values().stream().filter(IPAnalysisResult::isProxy).count();
	}

	@Override
	public int countAllowedIPs() {
		return (int) ipCache.values().stream().filter(result -> !result.isProxy()).count();
	}

	@Override
	public boolean confirmAndWipe(boolean confirm) {
		if (!confirm)
			return false;

		ipCache.clear();
		playerCountry.clear();
		saveCacheToFile();
		savePlayerCountry();
		logger.warning("[JSON] All cache and player data wiped.");
		return true;
	}

	private void saveCacheToFile() {
		try {
			JSONArray jsonArray = new JSONArray();
			for (Map.Entry<String, IPAnalysisResult> entry : ipCache.entrySet()) {
				JSONObject ipJson = new JSONObject();
				ipJson.put("ip", entry.getKey());
				ipJson.put("blocked", entry.getValue().isProxy());
				ipJson.put("country", entry.getValue().getCountryCode());
				ipJson.put("continent", entry.getValue().getContinent());
				jsonArray.put(ipJson);
			}

			try (FileWriter writer = new FileWriter(CACHE_FILE)) {
				writer.write(jsonArray.toString());
			}

		} catch (IOException e) {
			logger.warning("[JSON] Error saving cache to file: " + e.getMessage());
		}
	}

	@Override
	public void updatePlayerIP(String uuid, String ip) {
		playerIpMap.put(uuid, ip);
		savePlayerIP();
		logger.info("[JSON] Última IP actualizada para UUID: " + uuid);
	}

	private void savePlayerIP() {
		try {
			JSONObject json = new JSONObject(playerIpMap);
			try (FileWriter writer = new FileWriter(PLAYER_IP_FILE)) {
				writer.write(json.toString());
			}
		} catch (IOException e) {
			logger.warning("[JSON] Error saving player IP data: " + e.getMessage());
		}
	}

	private void loadPlayerIP() {
		try (FileReader reader = new FileReader(PLAYER_IP_FILE)) {
			StringBuilder jsonContent = new StringBuilder();
			int i;
			while ((i = reader.read()) != -1) {
				jsonContent.append((char) i);
			}

			JSONObject json = new JSONObject(jsonContent.toString());
			json.keys().forEachRemaining(uuid -> {
				String ip = json.getString(uuid);
				playerIpMap.put(uuid, ip);
			});

		} catch (IOException e) {
			logger.warning("[JSON] Error loading player IP data: " + e.getMessage());
		}
	}

	@Override
	public String getLastIP(String uuid) {
		return playerIpMap.get(uuid);
	}

}
