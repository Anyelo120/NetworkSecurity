package rottenbonestudio.system.DiscordSystem.storage;

import org.json.JSONObject;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class JsonLinkStorage {

	private static File dataFile;
	private static final Map<String, String> uuidToDiscord = new HashMap<>();
	private static final Map<String, String> discordToUuid = new HashMap<>();

	public static void init(File pluginFolder) {
		dataFile = new File(pluginFolder, "linked_accounts.json");

		if (!dataFile.exists()) {
			try {
				dataFile.createNewFile();
				save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		load();
	}

	private static void load() {
		try (FileReader reader = new FileReader(dataFile)) {
			StringBuilder jsonText = new StringBuilder();
			int c;
			while ((c = reader.read()) != -1) {
				jsonText.append((char) c);
			}

			JSONObject json = new JSONObject(jsonText.toString());

			for (String uuid : json.keySet()) {
				String discordId = json.getString(uuid);
				uuidToDiscord.put(uuid, discordId);
				discordToUuid.put(discordId, uuid);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void save() {
		try (FileWriter writer = new FileWriter(dataFile)) {
			JSONObject json = new JSONObject(uuidToDiscord);
			writer.write(json.toString(2));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveLink(String uuid, String discordId) {
		uuidToDiscord.put(uuid, discordId);
		discordToUuid.put(discordId, uuid);
		save();
	}

	public static boolean isUuidLinked(String uuid) {
		return uuidToDiscord.containsKey(uuid);
	}

	public static boolean isDiscordIdLinked(String discordId) {
		return discordToUuid.containsKey(discordId);
	}

	public static boolean deleteLinkByUUID(String uuid) {
		String discordId = uuidToDiscord.remove(uuid);
		if (discordId != null) {
			discordToUuid.remove(discordId);
			save();
			return true;
		}
		return false;
	}

	public static String getDiscordIdByUUID(String uuid) {
		return uuidToDiscord.get(uuid);
	}

	public static String getUUIDByDiscordId(String discordId) {
		return discordToUuid.get(discordId);
	}

}
