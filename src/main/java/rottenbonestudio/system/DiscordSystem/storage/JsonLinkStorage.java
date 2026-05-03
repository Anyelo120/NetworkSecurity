package rottenbonestudio.system.DiscordSystem.storage;

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

	private static synchronized void load() {
		uuidToDiscord.clear();
		discordToUuid.clear();
		try (java.io.InputStream in = new java.io.FileInputStream(dataFile)) {
			byte[] all = in.readAllBytes();
			String text = new String(all, java.nio.charset.StandardCharsets.UTF_8);
			org.json.JSONObject json = new org.json.JSONObject(text.isEmpty() ? "{}" : text);
			for (String uuid : json.keySet()) {
				String discordId = json.getString(uuid);
				uuidToDiscord.put(uuid, discordId);
				discordToUuid.put(discordId, uuid);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static synchronized void save() {
		try (java.io.OutputStream out = new java.io.FileOutputStream(dataFile)) {
			org.json.JSONObject json = new org.json.JSONObject(uuidToDiscord);
			byte[] bytes = json.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
			out.write(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static synchronized void saveLink(String uuid, String discordId) {
		uuidToDiscord.put(uuid, discordId);
		discordToUuid.put(discordId, uuid);
		save();
	}

	public static synchronized boolean isUuidLinked(String uuid) {
		return uuidToDiscord.containsKey(uuid);
	}

	public static synchronized boolean isDiscordIdLinked(String discordId) {
		return discordToUuid.containsKey(discordId);
	}

	public static synchronized boolean deleteLinkByUUID(String uuid) {
		String discordId = uuidToDiscord.remove(uuid);
		if (discordId != null) {
			discordToUuid.remove(discordId);
			save();
			return true;
		}
		return false;
	}

	public static synchronized String getDiscordIdByUUID(String uuid) {
		return uuidToDiscord.get(uuid);
	}

	public static synchronized String getUUIDByDiscordId(String discordId) {
		return discordToUuid.get(discordId);
	}

}
