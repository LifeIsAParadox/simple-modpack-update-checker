package de.hysky.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("simple-modpack-update-checker");
    private static final String CONFIG_DIR = "config";
    private static final String OLD_CONFIG_FILE = CONFIG_DIR + "/simple-modpack-update-checker.txt";
    private static final String NEW_CONFIG_FILE = CONFIG_DIR + "/simple-modpack-update-checker.json";
    private static final String BACKUP_CONFIG_FILE = CONFIG_DIR + "/simple-modpack-update-checker.txt.backup";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_CONFIG_VERSION = 2;

    public static class Config {
        public int configVersion = CURRENT_CONFIG_VERSION;
        public String localVersion = "1.0.0";
        public String identifier = "";

        // optional fields
        public List<String> minecraftVersions;
        public String releaseChannel;

        // migration from old config
        public Config(String localVersion, String identifier) {
            this.localVersion = localVersion;
            this.identifier = identifier;
        }

        public Config() {
        }

        public List<String> getMinecraftVersions() {
            return minecraftVersions != null ? minecraftVersions : Collections.emptyList();
        }

        public String getReleaseChannel() {
            return releaseChannel != null && !releaseChannel.isEmpty() ? releaseChannel : "release";
        }
    }

    public static Config loadConfig() {
        try {
            if (Files.exists(Paths.get(NEW_CONFIG_FILE))) {
                return loadJsonConfig();
            }

            if (Files.exists(Paths.get(OLD_CONFIG_FILE))) {
                LOGGER.info("[simple-modpack-update-checker] Migrating old TXT config to JSON format...");
                Config config = migrateFromTxtConfig();
                saveConfig(config);

                try {
                    Files.copy(Paths.get(OLD_CONFIG_FILE), Paths.get(BACKUP_CONFIG_FILE));
                    Files.delete(Paths.get(OLD_CONFIG_FILE));
                    LOGGER.info("[simple-modpack-update-checker] Migration completed. Old config backed up as .backup");
                } catch (IOException e) {
                    LOGGER.warn("[simple-modpack-update-checker] Could not backup/delete old config: ", e);
                }

                return config;
            }

            LOGGER.info("[simple-modpack-update-checker] No config found. Creating default config.");
            Config config = createDefaultConfig();
            saveConfig(config);
            return config;

        } catch (IOException e) {
            LOGGER.error("[simple-modpack-update-checker] Error loading config: ", e);
            return createDefaultConfig();
        }
    }

    private static Config loadJsonConfig() throws IOException {
        String content = Files.readString(Paths.get(NEW_CONFIG_FILE));
        Config config = GSON.fromJson(content, Config.class);

        if (config.configVersion < CURRENT_CONFIG_VERSION) {
            LOGGER.info("[simple-modpack-update-checker] Upgrading config from version {} to {}", config.configVersion, CURRENT_CONFIG_VERSION);
            config = upgradeConfig(config);
            saveConfig(config);
        }

        return config;
    }

    private static Config migrateFromTxtConfig() throws IOException {
        String[] lines = Files.readAllLines(Paths.get(OLD_CONFIG_FILE)).toArray(new String[0]);

        if (lines.length >= 2) {
            String localVersion = lines[0].trim();
            String identifier = lines[1].trim();

            return new Config(localVersion, identifier);
        } else {
            throw new IOException("Invalid old config format");
        }
    }

    private static Config upgradeConfig(Config oldConfig) {
        oldConfig.configVersion = CURRENT_CONFIG_VERSION;
        return oldConfig;
    }

    private static Config createDefaultConfig() {
        Config config = new Config();
        config.localVersion = "1.0.0";
        config.identifier = "https://example.com/your-url-here-to-text-file-or-modrinth-project-id";
        return config;
    }

    public static void saveConfig(Config config) {
        try {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists() && !configDir.mkdirs()) {
                LOGGER.warn("[simple-modpack-update-checker] Failed to create config directory");
                return;
            }

            String json = GSON.toJson(config);
            Files.writeString(Paths.get(NEW_CONFIG_FILE), json);

        } catch (IOException e) {
            LOGGER.error("[simple-modpack-update-checker] Error saving config: ", e);
        }
    }
}
