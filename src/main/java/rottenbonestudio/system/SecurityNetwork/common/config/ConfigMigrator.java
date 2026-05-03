package rottenbonestudio.system.SecurityNetwork.common.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigMigrator {

	private static final int CONFIG_VERSION = 2;

	private final Path dataFolder;
	private final Consumer<String> info;
	private final Consumer<String> warning;

	public ConfigMigrator(Path dataFolder, Consumer<String> info, Consumer<String> warning) {
		this.dataFolder = dataFolder;
		this.info = info;
		this.warning = warning;
	}

	public void run() {
		try {
			Files.createDirectories(dataFolder);
			backupIfExists("config.yml");
			backupIfExists("bot-config.properties");
			backupFolderIfExists("lang");

			migrateConfig();
			migrateBotConfig();
			migrateLanguages();
			writeMigrationMarker();
		} catch (Exception e) {
			warning.accept("[SecurityNetwork] Config migration failed: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void migrateConfig() throws IOException {
		Path configPath = dataFolder.resolve("config.yml");
		if (!Files.exists(configPath)) {
			return;
		}

		Yaml yaml = yaml();
		Map<String, Object> root;
		try (InputStream in = Files.newInputStream(configPath)) {
			Object loaded = yaml.load(in);
			root = loaded instanceof Map ? (Map<String, Object>) loaded : new LinkedHashMap<>();
		}

		Map<String, Object> storage = section(root, "storage");
		Map<String, Object> mysql = section(storage, "mysql");
		move(storage, mysql, "mysqlHost", "host");
		move(storage, mysql, "mysqlPort", "port");
		move(storage, mysql, "mysqlDatabase", "database");
		move(storage, mysql, "mysqlUser", "user");
		move(storage, mysql, "mysqlPassword", "password");
		if (!mysql.isEmpty()) {
			storage.put("mysql", mysql);
		}

		Map<String, Object> redis = section(storage, "redis");
		move(storage, redis, "redisHost", "host");
		move(storage, redis, "redisPort", "port");
		move(storage, redis, "redisPassword", "password");
		if (!redis.isEmpty()) {
			storage.put("redis", redis);
		}

		root.put("storage", storage);
		Map<String, Object> performance = section(root, "performance");
		Map<String, Object> login = section(performance, "login");
		putIfMissing(login, "max-attempts-per-ip", 8);
		putIfMissing(login, "window-seconds", 10);
		putIfMissing(login, "temp-block-seconds", 60);
		performance.put("login", login);

		Map<String, Object> api = section(performance, "api");
		putIfMissing(api, "max-global-in-flight", 32);
		putIfMissing(api, "in-flight-wait-timeout-ms", 4000);
		putIfMissing(api, "failure-cache-seconds", 30);
		performance.put("api", api);

		Map<String, Object> webhook = section(performance, "webhook");
		putIfMissing(webhook, "queue-size", 256);
		putIfMissing(webhook, "timeout-ms", 2500);
		performance.put("webhook", webhook);
		root.put("performance", performance);
		root.put("config-version", CONFIG_VERSION);

		try (Writer writer = Files.newBufferedWriter(configPath)) {
			yaml.dump(root, writer);
		}
	}

	private void migrateBotConfig() throws IOException {
		Path path = dataFolder.resolve("bot-config.properties");
		if (!Files.exists(path)) {
			return;
		}

		Properties props = new Properties();
		try (Reader reader = Files.newBufferedReader(path)) {
			props.load(reader);
		}

		copyIfMissing(props, "bot.dailyReportChannelId", "");
		copyIfMissing(props, "bot.dailyReportTime", "20:00");
		copyIfMissing(props, "bot.dailyReportEnabled", "true");
		copyIfMissing(props, "bot.serverName", "Mi Servidor");
		copyIfMissing(props, "bot.logWebhookUrl", props.getProperty("logWebhookUrl", ""));
		props.remove("logWebhookUrl");

		try (Writer writer = Files.newBufferedWriter(path)) {
			props.store(writer, "SecurityNetwork migrated config");
		}
	}

	private void migrateLanguages() throws IOException {
		Path langFolder = dataFolder.resolve("lang");
		if (!Files.exists(langFolder)) {
			return;
		}

		try (var stream = Files.list(langFolder)) {
			for (Path path : (Iterable<Path>) stream.filter(p -> p.getFileName().toString().endsWith(".lang"))::iterator) {
				appendMissingLanguageKeys(path);
			}
		}
	}

	private void appendMissingLanguageKeys(Path path) throws IOException {
		String fileName = path.getFileName().toString();
		Map<String, String> defaults = readLangResource("/lang/" + fileName);
		if (defaults.isEmpty() && !"es-es.lang".equals(fileName)) {
			defaults = readLangResource("/lang/es-es.lang");
		}
		if (defaults.isEmpty()) {
			return;
		}

		List<String> lines = Files.readAllLines(path);
		List<String> existing = new ArrayList<>();
		for (String line : lines) {
			int index = line.indexOf('=');
			if (index > 0 && !line.startsWith("#")) {
				existing.add(line.substring(0, index).trim());
			}
		}

		List<String> additions = new ArrayList<>();
		for (Map.Entry<String, String> entry : defaults.entrySet()) {
			if (!existing.contains(entry.getKey())) {
				additions.add(entry.getKey() + "=" + entry.getValue());
			}
		}

		if (!additions.isEmpty()) {
			lines.add("");
			lines.add("# Added by SecurityNetwork migration");
			lines.addAll(additions);
			Files.write(path, lines);
		}
	}

	private Map<String, String> readLangResource(String resource) throws IOException {
		Map<String, String> values = new LinkedHashMap<>();
		try (InputStream in = ConfigMigrator.class.getResourceAsStream(resource)) {
			if (in == null) {
				return values;
			}
			List<String> lines = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines()
					.collect(Collectors.toList());
			for (String line : lines) {
				int index = line.indexOf('=');
				if (index > 0 && !line.startsWith("#")) {
					values.put(line.substring(0, index).trim(), line.substring(index + 1));
				}
			}
		}
		return values;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> section(Map<String, Object> root, String key) {
		Object value = root.get(key);
		if (value instanceof Map) {
			return (Map<String, Object>) value;
		}
		return new LinkedHashMap<>();
	}

	private void move(Map<String, Object> oldSection, Map<String, Object> newSection, String oldKey, String newKey) {
		if (!newSection.containsKey(newKey) && oldSection.containsKey(oldKey)) {
			newSection.put(newKey, oldSection.get(oldKey));
		}
		oldSection.remove(oldKey);
	}

	private void putIfMissing(Map<String, Object> section, String key, Object value) {
		if (!section.containsKey(key)) {
			section.put(key, value);
		}
	}

	private void copyIfMissing(Properties props, String key, String value) {
		if (!props.containsKey(key)) {
			props.setProperty(key, value);
		}
	}

	private void backupIfExists(String fileName) throws IOException {
		Path source = dataFolder.resolve(fileName);
		if (!Files.exists(source)) {
			return;
		}
		Path target = backupFolder().resolve(fileName);
		Files.createDirectories(target.getParent());
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
	}

	private void backupFolderIfExists(String folderName) throws IOException {
		Path source = dataFolder.resolve(folderName);
		if (!Files.isDirectory(source)) {
			return;
		}
		Path target = backupFolder().resolve(folderName);
		Files.createDirectories(target);
		try (var stream = Files.list(source)) {
			for (Path path : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
				Files.copy(path, target.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private Path backupFolder() {
		return dataFolder.resolve("backup").resolve("config-v" + CONFIG_VERSION);
	}

	private void writeMigrationMarker() throws IOException {
		Properties props = new Properties();
		props.setProperty("configVersion", String.valueOf(CONFIG_VERSION));
		props.setProperty("lastMigration", LocalDateTime.now().toString());
		try (Writer writer = Files.newBufferedWriter(dataFolder.resolve("migration.properties"))) {
			props.store(writer, "SecurityNetwork migration marker");
		}
		info.accept("[SecurityNetwork] Config migration checked.");
	}

	private Yaml yaml() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		return new Yaml(options);
	}
}
