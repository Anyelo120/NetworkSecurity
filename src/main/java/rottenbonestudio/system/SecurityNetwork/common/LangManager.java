package rottenbonestudio.system.SecurityNetwork.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LangManager {
	private static final Map<String, String> messages = new HashMap<>();
	private static final List<String> availableLanguages = new ArrayList<>();
	private static String currentLang = "es-es";
	private static boolean initialized = false;

	public static void init(Path dataFolder, String langCode) {
		currentLang = langCode;
		Path langFolder = dataFolder.resolve("lang");

		try {
			if (!Files.exists(langFolder)) {
				Files.createDirectories(langFolder);
			}
		} catch (IOException e) {
			System.err.println("[LangManager] No se pudo crear la carpeta de idioma: " + e.getMessage());
			return;
		}

		extractLangFiles(langFolder);

		Path langFile = langFolder.resolve(currentLang + ".lang");

		if (!Files.exists(langFile)) {
			System.err.println("[LangManager] No se encontró el archivo de idioma: " + langFile);
			return;
		}

		messages.clear();
		try (BufferedReader reader = Files.newBufferedReader(langFile)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || !line.contains("="))
					continue;
				String[] parts = line.split("=", 2);
				String key = parts[0].trim();
				String value = parts[1].trim().replace("\\n", "\n").replace("&", "§");
				messages.put(key, value);
			}
			initialized = true;
			System.out.println("[LangManager] Idioma cargado: " + currentLang);
		} catch (IOException e) {
			System.err.println("[LangManager] Error cargando archivo de idioma: " + e.getMessage());
		}
	}

	public static String get(String key, String... args) {
		if (!initialized)
			return "§c[Lang] No inicializado";
		String base = messages.getOrDefault(key, "§c[Missing lang: " + key + "]");
		for (int i = 0; i < args.length; i++) {
			base = base.replace("{" + i + "}", args[i]);
		}
		return base;
	}

	public static List<String> getAvailableLanguages() {
		return new ArrayList<>(availableLanguages);
	}

	private static void extractLangFiles(Path langFolder) {
		try {
			String jarPath = LangManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			if (!jarPath.endsWith(".jar"))
				return;

			try (JarFile jar = new JarFile(jarPath)) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();

					if (name.startsWith("lang/") && name.endsWith(".lang")) {
						String fileName = name.substring("lang/".length());

						if (!availableLanguages.contains(fileName.replace(".lang", ""))) {
							availableLanguages.add(fileName.replace(".lang", ""));
						}

						Path targetFile = langFolder.resolve(fileName);
						if (!Files.exists(targetFile)) {
							try (InputStream in = LangManager.class.getClassLoader().getResourceAsStream(name)) {
								if (in != null) {
									Files.copy(in, targetFile);
									System.out.println("[LangManager] Archivo de idioma extraído: " + fileName);
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			System.err.println("[LangManager] No se pudieron extraer los archivos de idioma: " + e.getMessage());
		}
	}

}
