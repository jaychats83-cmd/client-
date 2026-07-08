package starry.util.config.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import starry.util.config.cloud.CloudConfigEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LocalConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String EXTENSION = ".qcloud.json";
    private String launchConfigId;

    public List<CloudConfigEntry> fetchAll() {
        List<CloudConfigEntry> entries = new ArrayList<>();
        try {
            Path dir = configDir();
            if (!Files.exists(dir)) return entries;

            try (var stream = Files.list(dir)) {
                stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(EXTENSION))
                        .forEach(path -> readEntry(path, entries));
            }

            entries.sort(Comparator.comparingLong((CloudConfigEntry e) -> e.timestamp).reversed());
        } catch (Exception ignored) {
        }
        return entries;
    }

    public CloudConfigEntry create(String name, String data) {
        try {
            String safeName = sanitizeName(name);
            Path path = uniquePath(safeName);
            CloudConfigEntry entry = new CloudConfigEntry(path.getFileName().toString(), name, data);
            writeEntry(path, entry);
            return entry;
        } catch (Exception ignored) {
            return null;
        }
    }

    public CloudConfigEntry update(String id, String data) {
        CloudConfigEntry entry = findById(id);
        if (entry == null) return null;
        entry.data = data;
        entry.timestamp = System.currentTimeMillis();
        writeEntry(configDir().resolve(id), entry);
        return entry;
    }

    public CloudConfigEntry findById(String id) {
        Path path = configDir().resolve(id);
        if (!Files.exists(path)) return null;
        List<CloudConfigEntry> entries = new ArrayList<>();
        readEntry(path, entries);
        return entries.isEmpty() ? null : entries.getFirst();
    }

    public boolean delete(String id) {
        try {
            if (id != null && id.equals(launchConfigId)) {
                launchConfigId = null;
                saveLaunchConfig();
            }
            return Files.deleteIfExists(configDir().resolve(id));
        } catch (IOException ignored) {
            return false;
        }
    }

    public CloudConfigEntry importFile(String rawPath) {
        try {
            Path source = Paths.get(rawPath.trim().replace("\"", ""));
            if (!Files.exists(source) || !Files.isRegularFile(source)) return null;

            String json = Files.readString(source, StandardCharsets.UTF_8);
            CloudConfigEntry imported = parseEntry(json, source.getFileName().toString());
            if (imported == null || imported.data == null || imported.data.isBlank()) return null;

            String baseName = sanitizeName(imported.name == null || imported.name.isBlank()
                    ? stripExtension(source.getFileName().toString())
                    : imported.name);
            Path target = uniquePath(baseName);
            imported.id = target.getFileName().toString();
            imported.timestamp = System.currentTimeMillis();
            writeEntry(target, imported);
            return imported;
        } catch (Exception ignored) {
            return null;
        }
    }

    public String getLaunchConfigId() {
        if (launchConfigId == null) loadLaunchConfig();
        return launchConfigId;
    }

    public boolean setLaunchConfig(String id) {
        if (findById(id) == null) return false;
        launchConfigId = id;
        return saveLaunchConfig();
    }

    public boolean clearLaunchConfig() {
        launchConfigId = null;
        return saveLaunchConfig();
    }

    public Path configDir() {
        Path dir = Paths.get("qcloud", "configs", "users", UserConfigPath.currentUser(), "exports");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    private void readEntry(Path path, List<CloudConfigEntry> entries) {
        try {
            CloudConfigEntry entry = parseEntry(Files.readString(path, StandardCharsets.UTF_8), path.getFileName().toString());
            if (entry != null) entries.add(entry);
        } catch (Exception ignored) {
        }
    }

    private CloudConfigEntry parseEntry(String json, String fallbackId) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        CloudConfigEntry entry = new CloudConfigEntry();
        entry.id = fallbackId;
        entry.name = root.has("name") ? root.get("name").getAsString() : stripExtension(fallbackId);
        entry.timestamp = root.has("timestamp") ? root.get("timestamp").getAsLong() : System.currentTimeMillis();
        entry.data = root.has("data") ? root.get("data").getAsString() : json;
        return entry;
    }

    private void writeEntry(Path path, CloudConfigEntry entry) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("name", entry.name);
            root.addProperty("timestamp", entry.timestamp);
            root.addProperty("data", entry.data);
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private Path uniquePath(String baseName) {
        Path dir = configDir();
        String name = baseName.isBlank() ? "config" : baseName;
        Path path = dir.resolve(name + EXTENSION);
        int index = 2;
        while (Files.exists(path)) {
            path = dir.resolve(name + "-" + index + EXTENSION);
            index++;
        }
        return path;
    }

    private boolean saveLaunchConfig() {
        try {
            Path path = launchPath();
            if (launchConfigId == null || launchConfigId.isBlank()) {
                Files.deleteIfExists(path);
            } else {
                Files.writeString(path, launchConfigId, StandardCharsets.UTF_8);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void loadLaunchConfig() {
        try {
            Path path = launchPath();
            launchConfigId = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8).trim() : null;
        } catch (IOException ignored) {
            launchConfigId = null;
        }
    }

    private Path launchPath() {
        return configDir().resolve("launch.txt");
    }

    private String sanitizeName(String name) {
        return name.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String stripExtension(String name) {
        return name.endsWith(EXTENSION) ? name.substring(0, name.length() - EXTENSION.length()) : name;
    }
}
