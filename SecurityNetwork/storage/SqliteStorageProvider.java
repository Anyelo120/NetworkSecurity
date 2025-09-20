package rottenbonestudio.system.SecurityNetwork.storage;

import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.logging.Logger;

public class SqliteStorageProvider implements StorageProvider {

	private static final String BASE_FOLDER = "plugins/securitynetwork";
	private static final String STORAGE_FOLDER = BASE_FOLDER + "/storage/sqlite";
	private static final String DB_FILENAME = "cache.db";
	private static final String DB_URL = "jdbc:sqlite:" + STORAGE_FOLDER + "/" + DB_FILENAME;

	private static final Logger logger = Logger.getLogger("NetworkSecurity");

	@Override
	public void initialize() {
		try {
			File storageDir = new File(STORAGE_FOLDER);
			if (!storageDir.exists() && storageDir.mkdirs()) {
				logger.info("[SQLite] Created storage directory: " + storageDir.getPath());
			}

			Path oldPath = Paths.get(BASE_FOLDER, DB_FILENAME);
			Path newPath = Paths.get(STORAGE_FOLDER, DB_FILENAME);
			if (Files.exists(oldPath) && !Files.exists(newPath)) {
				Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
				logger.info("[SQLite] Migrated cache.db to storage/sqlite.");
			}

			try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {

				stmt.execute("CREATE TABLE IF NOT EXISTS ip_cache (" + "ip TEXT PRIMARY KEY, "
						+ "blocked BOOLEAN NOT NULL, " + "country TEXT NOT NULL, " + "continent TEXT NOT NULL)");

				stmt.execute("CREATE TABLE IF NOT EXISTS player_country (" + "uuid TEXT PRIMARY KEY, "
						+ "country TEXT NOT NULL)");

				stmt.execute("CREATE TABLE IF NOT EXISTS player_ips (" + "uuid TEXT PRIMARY KEY, "
						+ "last_ip TEXT NOT NULL)");

			}
		} catch (SQLException | IOException e) {
			logger.warning("[SQLite] Initialization error: " + e.getMessage());
		}
	}

	@Override
	public void saveIP(String ip, boolean isBlocked, String country, String continent) {
		try (Connection conn = DriverManager.getConnection(DB_URL)) {
			PreparedStatement stmt = conn.prepareStatement(
					"INSERT OR REPLACE INTO ip_cache(ip, blocked, country, continent) VALUES (?, ?, ?, ?)");
			stmt.setString(1, ip);
			stmt.setBoolean(2, isBlocked);
			stmt.setString(3, country);
			stmt.setString(4, continent);
			stmt.executeUpdate();
		} catch (SQLException e) {
			logger.warning("[SQLite] Insert error: " + e.getMessage());
		}
	}

	@Override
	public IPAnalysisResult getCachedAnalysis(String ip) {
		try (Connection conn = DriverManager.getConnection(DB_URL)) {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT blocked, country, continent FROM ip_cache WHERE ip = ?");
			stmt.setString(1, ip);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				boolean blocked = rs.getBoolean("blocked");
				String country = rs.getString("country");
				String continent = rs.getString("continent");
				return new IPAnalysisResult(country, continent, blocked);
			}
		} catch (SQLException e) {
			logger.warning("[SQLite] Read error: " + e.getMessage());
		}
		return null;
	}

	@Override
	public boolean isCountryMismatch(String uuid, String currentCountry) {
		try (Connection conn = DriverManager.getConnection(DB_URL)) {
			PreparedStatement stmt = conn.prepareStatement("SELECT country FROM player_country WHERE uuid = ?");
			stmt.setString(1, uuid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				String savedCountry = rs.getString("country");
				return !savedCountry.equalsIgnoreCase(currentCountry);
			} else {
				PreparedStatement insert = conn
						.prepareStatement("INSERT INTO player_country(uuid, country) VALUES (?, ?)");
				insert.setString(1, uuid);
				insert.setString(2, currentCountry);
				insert.executeUpdate();
				return false;
			}
		} catch (SQLException e) {
			logger.warning("[SQLite] Country check error: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void deleteIP(String ip) {
		try (Connection conn = DriverManager.getConnection(DB_URL)) {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM ip_cache WHERE ip = ?");
			stmt.setString(1, ip);
			stmt.executeUpdate();
			logger.info("[SQLite] IP " + ip + " removed from cache.");
		} catch (SQLException e) {
			logger.warning("[SQLite] Error deleting IP: " + e.getMessage());
		}
	}

	@Override
	public int countBlockedIPs() {
		try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ip_cache WHERE blocked = 1");
			return rs.next() ? rs.getInt(1) : 0;
		} catch (SQLException e) {
			logger.warning("[SQLite] Count blocked error: " + e.getMessage());
			return 0;
		}
	}

	@Override
	public int countAllowedIPs() {
		try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ip_cache WHERE blocked = 0");
			return rs.next() ? rs.getInt(1) : 0;
		} catch (SQLException e) {
			logger.warning("[SQLite] Count allowed error: " + e.getMessage());
			return 0;
		}
	}

	@Override
	public boolean confirmAndWipe(boolean confirm) {
		if (!confirm)
			return false;
		try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
			stmt.execute("DELETE FROM ip_cache");
			stmt.execute("DELETE FROM player_country");
			logger.warning("[SQLite] All data wiped from cache and player_country.");
			return true;
		} catch (SQLException e) {
			logger.warning("[SQLite] Wipe error: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void updatePlayerIP(String uuid, String ip) {
		try (Connection conn = DriverManager.getConnection(DB_URL)) {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO player_ips(uuid, last_ip) VALUES (?, ?) "
					+ "ON CONFLICT(uuid) DO UPDATE SET last_ip = excluded.last_ip");
			stmt.setString(1, uuid);
			stmt.setString(2, ip);
			stmt.executeUpdate();
			logger.info("[SQLite] Actualizada última IP para UUID: " + uuid);
		} catch (SQLException e) {
			logger.warning("[SQLite] Error al actualizar última IP: " + e.getMessage());
		}
	}

	@Override
	public String getLastIP(String uuid) {
		try (Connection conn = DriverManager.getConnection(DB_URL)) {
			PreparedStatement stmt = conn.prepareStatement("SELECT last_ip FROM player_ip WHERE uuid = ?");
			stmt.setString(1, uuid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				return rs.getString("last_ip");
			}
		} catch (SQLException e) {
			logger.warning("[SQLite] Error retrieving last IP: " + e.getMessage());
		}
		return null;
	}

	@Override
	public String getPlayersLinkedToIP(String ip) {
		try (Connection conn = DriverManager.getConnection(DB_URL)) {
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT uuid FROM player_ips WHERE last_ip = ?");
			stmt.setString(1, ip);
			ResultSet rs = stmt.executeQuery();

			StringBuilder sb = new StringBuilder();
			while (rs.next()) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(rs.getString("uuid"));
			}
			return sb.toString();
		} catch (SQLException e) {
			logger.warning("[SQLite] Error al obtener jugadores por IP: " + e.getMessage());
			return "";
		}
	}
	
}
