package de.hysky;

import de.hysky.utils.HttpUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static final String CONFIG_FILE_PATH = "config/simple-modpack-update-checker.txt";
    private static final String PLACEHOLDER_CONTENT = "3.3.3\nhttps://example.com/your-url-here-to-text-file-or-only-modrinth-project-id";
    public static Text updateMessage = null;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void onInitializeClient() {
        try {
            if (!Files.exists(Paths.get(CONFIG_FILE_PATH))) {
                createPlaceholderConfig();
                LOGGER.info("[simple-modpack-update-checker] Config file not found. Placeholder created. Mod will not proceed.");
                return;
            }

            String[] configData = readConfigFile();
            if (configData.length == 2) {
                String localVersion = configData[0];
                String identifier = configData[1];

                if (isValidURL(identifier)) {
                    fetchRemoteVersionAsync(identifier, localVersion);
                } else if (isValidModrinthProjectID(identifier)) {
                    fetchModrinthVersionAsync(identifier, localVersion);
                } else {
                    LOGGER.info("[simple-modpack-update-checker] No valid URL or Modrinth project ID. Mod will not start.");
                }
            } else {
                LOGGER.info("[simple-modpack-update-checker] Invalid config file. Mod will not start.");
            }
        } catch (IOException e) {
            LOGGER.warn(String.valueOf(e));
        }
    }

    private String[] readConfigFile() throws IOException {
        return Files.readAllLines(Paths.get(SimpleModpackUpdateChecker.CONFIG_FILE_PATH)).toArray(new String[0]);
    }

    private boolean isValidURL(String urlString) {
        try {
            new URI(urlString).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void createPlaceholderConfig() throws IOException {
        File file = new File(CONFIG_FILE_PATH);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            LOGGER.warn("[simple-modpack-update-checker] Failed to create directory {}", file.getParentFile());
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(PLACEHOLDER_CONTENT);
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

    private void fetchRemoteVersionAsync(String urlString, String localVersion) {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteVersion = fetchRemoteVersion(urlString);
                processRemoteVersion(remoteVersion, localVersion);
            } catch (IOException | URISyntaxException | InterruptedException e) {
                LOGGER.warn("[simple-modpack-update-checker] Error fetching remote version: ", e);
            }
        }, executor);
    }

    private void fetchModrinthVersionAsync(String projectID, String localVersion) {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteVersion = fetchModrinthVersion(projectID);
                processRemoteVersion(remoteVersion, localVersion);
            } catch (IOException | URISyntaxException | InterruptedException e) {
                LOGGER.warn("[simple-modpack-update-checker] Error fetching Modrinth version: ", e);
            }
        }, executor);
    }

    private void processRemoteVersion(String remoteVersion, String localVersion) {
        if (remoteVersion != null && !localVersion.equals(remoteVersion)) {
            updateMessage = Text.literal("Update to version ").formatted(Formatting.DARK_RED, Formatting.BOLD).append(Text.literal(remoteVersion).formatted(Formatting.GOLD, Formatting.BOLD));
        }
    }

    private String fetchRemoteVersion(String urlString) throws IOException, URISyntaxException, InterruptedException {
        String response = HttpUtils.sendGetRequest(urlString);
        return response.lines().filter(line -> line.startsWith("version = \"")).map(line -> line.substring(11, line.length() - 1)).findFirst().orElse(null);
    }

    private String fetchModrinthVersion(String projectID) throws IOException, URISyntaxException, InterruptedException {
        String apiUrl = "https://api.modrinth.com/v2/project/" + projectID + "/version";
        String response = HttpUtils.sendGetRequest(apiUrl);

        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            if (jsonObject.get("version_type").getAsString().equalsIgnoreCase("release")) {
                return jsonObject.get("version_number").getAsString();
            }
        }

        LOGGER.warn("[simple-modpack-update-checker] No release version found for project ID {}", projectID);
        return null;
    }
}
