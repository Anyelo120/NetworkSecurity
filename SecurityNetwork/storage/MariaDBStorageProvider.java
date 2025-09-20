package rottenbonestudio.system.SecurityNetwork.storage;

import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.sql.*;
import java.util.logging.Logger;

public class MariaDBStorageProvider implements StorageProvider {

	private final String host;
	private final String port;
	private final String database;
	private final String user;
	private final String password;
	private static final Logger logger = Logger.getLogger("NetworkSecurity");

	public MariaDBStorageProvider(String host, int port, String database, String user, String password) {
		this.host = host;
		this.port = String.valueOf(port);
		this.database = database;
		this.user = user;
		this.password = password;
	}

	private Connection getConnection() throws SQLException {
		try {
			return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database
					+ "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8", user, password);
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error connecting to the database: " + e.getMessage());
			throw new SQLException("[MariaDB] Error connecting to the database: " + e.getMessage());
		}
	}

	private Connection getConnectionWithoutDatabase() throws SQLException {
		try {
			return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port
					+ "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8", user, password);
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error connecting without database: " + e.getMessage());
			throw new SQLException("[MariaDB] Error connecting without database: " + e.getMessage());
		}
	}

	@Override
	public void initialize() {
		try {
			try (Connection conn = getConnectionWithoutDatabase(); Statement stmt = conn.createStatement()) {
				stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "` DEFAULT CHARACTER SET utf8");
				logger.info("[MariaDB] Database checked/created: " + database);
			}

			try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE IF NOT EXISTS ip_cache (" + "ip VARCHAR(45) PRIMARY KEY, "
						+ "blocked BOOLEAN NOT NULL, " + "country VARCHAR(4) NOT NULL, "
						+ "continent VARCHAR(20) NOT NULL)");

				stmt.execute("CREATE TABLE IF NOT EXISTS player_country (" + "uuid VARCHAR(36) PRIMARY KEY, "
						+ "country VARCHAR(4) NOT NULL)");

				stmt.execute("CREATE TABLE IF NOT EXISTS player_ips (" + "uuid VARCHAR(36) PRIMARY KEY, "
						+ "last_ip VARCHAR(45) NOT NULL)");

				logger.info("[MariaDB] Tables initialized successfully.");
			}

		} catch (SQLException e) {
			logger.severe("[MariaDB] Initialization error: " + e.getMessage());
		}
	}

	@Override
	public void saveIP(String ip, boolean isBlocked, String country, String continent) {
		try (Connection conn = getConnection()) {
			PreparedStatement stmt = conn
					.prepareStatement("REPLACE INTO ip_cache(ip, blocked, country, continent) VALUES (?, ?, ?, ?)");
			stmt.setString(1, ip);
			stmt.setBoolean(2, isBlocked);
			stmt.setString(3, country);
			stmt.setString(4, continent);
			stmt.executeUpdate();
		} catch (SQLException e) {
			logger.severe("[MariaDB] Insert error: " + e.getMessage());
		}
	}

	@Override
	public IPAnalysisResult getCachedAnalysis(String ip) {
		try (Connection conn = getConnection()) {
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
			logger.severe("[MariaDB] Read error: " + e.getMessage());
		}
		return null;
	}

	@Override
	public boolean isCountryMismatch(String uuid, String currentCountry) {
		try (Connection conn = getConnection()) {
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
			logger.severe("[MariaDB] Country check error: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void deleteIP(String ip) {
		try (Connection conn = getConnection();
				PreparedStatement stmt = conn.prepareStatement("DELETE FROM ip_cache WHERE ip = ?")) {
			stmt.setString(1, ip);
			int affected = stmt.executeUpdate();
			logger.info("[MariaDB] Deleted IP: " + ip + " (Affected rows: " + affected + ")");
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error deleting IP: " + e.getMessage());
		}
	}

	@Override
	public int countBlockedIPs() {
		try (Connection conn = getConnection();
				PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM ip_cache WHERE blocked = TRUE");
				ResultSet rs = stmt.executeQuery()) {
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error counting blocked IPs: " + e.getMessage());
		}
		return 0;
	}

	@Override
	public int countAllowedIPs() {
		try (Connection conn = getConnection();
				PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM ip_cache WHERE blocked = FALSE");
				ResultSet rs = stmt.executeQuery()) {
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error counting allowed IPs: " + e.getMessage());
		}
		return 0;
	}

	@Override
	public boolean confirmAndWipe(boolean confirm) {
		if (!confirm)
			return false;

		try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
			stmt.execute("DELETE FROM ip_cache");
			stmt.execute("DELETE FROM player_country");
			logger.warning("[MariaDB] All IP and country data wiped successfully.");
			return true;
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error wiping data: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void updatePlayerIP(String uuid, String ip) {
		try (Connection conn = getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO player_ips(uuid, last_ip) VALUES (?, ?) "
					+ "ON DUPLICATE KEY UPDATE last_ip = VALUES(last_ip)");
			stmt.setString(1, uuid);
			stmt.setString(2, ip);
			stmt.executeUpdate();
			logger.info("[MariaDB] Last IP updated for UUID: " + uuid);
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error updating last IP: " + e.getMessage());
		}
	}

	@Override
	public String getLastIP(String uuid) {
		try (Connection conn = getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("SELECT last_ip FROM player_ips WHERE uuid = ?");
			stmt.setString(1, uuid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				return rs.getString("last_ip");
			}
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error retrieving last IP: " + e.getMessage());
		}
		return null;
	}

	@Override
	public String getPlayersLinkedToIP(String ip) {
		try (Connection conn = getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("SELECT uuid FROM player_ips WHERE last_ip = ?");
			stmt.setString(1, ip);
			ResultSet rs = stmt.executeQuery();

			StringBuilder sb = new StringBuilder();
			while (rs.next()) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(rs.getString("uuid"));
			}
			return sb.toString();
		} catch (SQLException e) {
			logger.severe("[MariaDB] Error retrieving players by IP: " + e.getMessage());
			return "";
		}
	}

}
