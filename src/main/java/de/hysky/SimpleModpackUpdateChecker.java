package de.hysky;

import de.hysky.config.ConfigManager;
import de.hysky.utils.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.fabricmc.api.ClientModInitializer;

public class SimpleModpackUpdateChecker implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("simple-modpack-update-checker");
    public static Text updateMessage = null;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void onInitializeClient() {
        try {
            ConfigManager.Config config = ConfigManager.loadConfig();

            if (config.identifier.isEmpty() || config.identifier.contains("example.com")) {
                LOGGER.info("[simple-modpack-update-checker] Config contains placeholder values. Please configure the mod properly.");
                return;
            }

            List<String> configMinecraftVersions = config.getMinecraftVersions();

            if (configMinecraftVersions.isEmpty()) {
                LOGGER.info("[simple-modpack-update-checker] No specific Minecraft versions configured, checking for updates across all versions");
            } else {
                LOGGER.info("[simple-modpack-update-checker] Checking for updates compatible with Minecraft versions: {}", configMinecraftVersions);
            }

            String releaseChannel = config.getReleaseChannel();

            if (isValidURL(config.identifier)) {
                fetchRemoteVersionAsync(config.identifier, config.localVersion, releaseChannel);
            } else if (isValidModrinthProjectID(config.identifier)) {
                fetchModrinthVersionAsync(config.identifier, config.localVersion, releaseChannel, configMinecraftVersions);
            } else {
                LOGGER.warn("[simple-modpack-update-checker] Invalid identifier in config: {}", config.identifier);
            }

        } catch (Exception e) {
            LOGGER.error("[simple-modpack-update-checker] Error during initialization: ", e);
        }
    }

    private boolean isValidURL(String urlString) {
        try {
            new URI(urlString).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidModrinthProjectID(String projectID) {
        try {
            String apiUrl = "https://api.modrinth.com/v2/project/" + projectID + "/check";
            String response = HttpUtils.sendGetRequest(apiUrl);
            return response != null;
        } catch (IOException | URISyntaxException | InterruptedException e) {
            LOGGER.warn("[simple-modpack-update-checker] Error checking Modrinth project ID validity: ", e);
            return false;
        }
    }

    private void fetchRemoteVersionAsync(String urlString, String localVersion, String releaseChannel) {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteVersion = fetchRemoteVersion(urlString);
                processRemoteVersion(remoteVersion, localVersion, releaseChannel);
            } catch (IOException | URISyntaxException | InterruptedException e) {
                LOGGER.warn("[simple-modpack-update-checker] Error fetching remote version: ", e);
            }
        }, executor);
    }

    private void fetchModrinthVersionAsync(String projectID, String localVersion, String releaseChannel, List<String> minecraftVersions) {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteVersion = fetchModrinthVersion(projectID, releaseChannel, minecraftVersions);
                processRemoteVersion(remoteVersion, localVersion, releaseChannel);
            } catch (IOException | URISyntaxException | InterruptedException e) {
                LOGGER.warn("[simple-modpack-update-checker] Error fetching Modrinth version: ", e);
            }
        }, executor);
    }

    private void processRemoteVersion(String remoteVersion, String localVersion, String releaseChannel) {
        if (remoteVersion != null && !localVersion.equals(remoteVersion)) {
            updateMessage = Text.literal("Update to version ").formatted(Formatting.DARK_RED, Formatting.BOLD).append(Text.literal(remoteVersion).formatted(Formatting.GOLD, Formatting.BOLD));

            LOGGER.info("[simple-modpack-update-checker] Update available: {} -> {} ({})", localVersion, remoteVersion, releaseChannel);
        }
    }

    private String fetchRemoteVersion(String urlString) throws IOException, URISyntaxException, InterruptedException {
        String response = HttpUtils.sendGetRequest(urlString);
        return response.lines().filter(line -> line.startsWith("version = \"")).map(line -> line.substring(11, line.length() - 1)).findFirst().orElse(null);
    }

    private String fetchModrinthVersion(String projectID, String releaseChannel, List<String> minecraftVersions) throws IOException, URISyntaxException, InterruptedException {

        String apiUrl = "https://api.modrinth.com/v2/project/" + projectID + "/version";

        if (!minecraftVersions.isEmpty()) {
            StringBuilder versionFilter = new StringBuilder("?game_versions=");

            StringBuilder jsonArray = new StringBuilder("[");
            for (int i = 0; i < minecraftVersions.size(); i++) {
                if (i > 0) jsonArray.append(",");
                jsonArray.append("\"").append(minecraftVersions.get(i)).append("\"");
            }
            jsonArray.append("]");

            versionFilter.append(URLEncoder.encode(jsonArray.toString(), StandardCharsets.UTF_8));
            apiUrl += versionFilter.toString();
        }

        String response = HttpUtils.sendGetRequest(apiUrl);
        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject version = jsonArray.get(i).getAsJsonObject();
            String versionType = version.get("version_type").getAsString();

            if (matchesReleaseChannel(versionType, releaseChannel)) {
                if (!minecraftVersions.isEmpty()) {
                    JsonArray gameVersions = version.getAsJsonArray("game_versions");
                    boolean supportsAnyVersion = false;
                    for (String targetVersion : minecraftVersions) {
                        for (int j = 0; j < gameVersions.size(); j++) {
                            if (gameVersions.get(j).getAsString().equals(targetVersion)) {
                                supportsAnyVersion = true;
                                break;
                            }
                        }
                        if (supportsAnyVersion) break;
                    }
                    if (!supportsAnyVersion) {
                        continue;
                    }
                }

                String versionNumber = version.get("version_number").getAsString();
                LOGGER.debug("[simple-modpack-update-checker] Found {} version: {}", versionType, versionNumber);
                return versionNumber;
            }
        }

        LOGGER.warn("[simple-modpack-update-checker] No {} version found for project ID {} (Minecraft {})", releaseChannel, projectID, minecraftVersions.isEmpty() ? "any" : minecraftVersions);
        return null;
    }

    private boolean matchesReleaseChannel(String versionType, String targetType) {
        return switch (targetType) {
            case "alpha" -> versionType.equals("alpha") || versionType.equals("beta") || versionType.equals("release");
            case "beta" -> versionType.equals("beta") || versionType.equals("release");
            default -> versionType.equals("release");
        };
    }
}
