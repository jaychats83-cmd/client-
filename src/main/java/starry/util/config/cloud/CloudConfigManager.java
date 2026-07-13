package starry.util.config.cloud;

import com.google.gson.*;
import starry.Initialization;
import starry.util.string.StringHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CloudConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_PATH = "configs/cloud.json";
    private static final String API_BASE = "https://api.github.com/repos";

    private final String owner;
    private final String repo;
    private final String token;
    private String cachedSha;
    private volatile String launchConfigId;

    public CloudConfigManager() {
        this.owner = "p1aegg";
        this.repo = "client";
        String a = new StringBuilder("fU3iEKHaKAg1h9IZDwtrYKa5fGgroaL").reverse().toString();
        String b = new StringBuilder("n9kwoFNywlEIKIDaDn/sZZPfGgIkIAi").reverse().toString();
        String c = new StringBuilder("roBNGxCjn41aeRPjieUYBJHlr68KKjy").reverse().toString();
        String d = new StringBuilder("rsdNOKThhF/XqeR8B8ZfJjWkuJKeiaH").reverse().toString();
        this.token = StringHelper.decrypt(a + b + c + d);
    }

    public String getLaunchConfigId() {
        return launchConfigId;
    }

    public List<CloudConfigEntry> fetchAll() {
        try {
            String url = API_BASE + "/" + owner + "/" + repo + "/contents/" + FILE_PATH;
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                String body = readBody(conn);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("sha")) cachedSha = json.get("sha").getAsString();
                if (json.has("content")) {
                    String decoded = new String(Base64.getDecoder().decode(json.get("content").getAsString().replaceAll("\\s", "")), StandardCharsets.UTF_8);
                    return parseEntries(decoded);
                }
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }

    public CloudConfigEntry create(String name, String data) {
        String id = generateId();
        CloudConfigEntry entry = new CloudConfigEntry(id, name, data);
        List<CloudConfigEntry> entries = fetchAll();
        entries.add(entry);
        if (saveAll(entries)) return entry;
        return null;
    }

    public CloudConfigEntry update(String id, String data) {
        List<CloudConfigEntry> entries = fetchAll();
        for (CloudConfigEntry e : entries) {
            if (e.id.equals(id)) {
                e.data = data;
                e.timestamp = System.currentTimeMillis();
                if (saveAll(entries)) return e;
                return null;
            }
        }
        return null;
    }

    public CloudConfigEntry findById(String id) {
        List<CloudConfigEntry> entries = fetchAll();
        for (CloudConfigEntry e : entries) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    public CloudConfigEntry findByName(String name) {
        List<CloudConfigEntry> entries = fetchAll();
        for (CloudConfigEntry e : entries) {
            if (e.name.equals(name)) return e;
        }
        return null;
    }

    public boolean delete(String id) {
        List<CloudConfigEntry> entries = fetchAll();
        if (launchConfigId != null && launchConfigId.equals(id)) {
            launchConfigId = null;
        }
        entries.removeIf(e -> e.id.equals(id));
        return saveAll(entries);
    }

    public boolean setLaunchConfig(String id) {
        List<CloudConfigEntry> entries = fetchAll();
        boolean exists = false;
        for (CloudConfigEntry e : entries) {
            if (e.id.equals(id)) { exists = true; break; }
        }
        if (!exists) return false;
        launchConfigId = id;
        return saveAll(entries);
    }

    public boolean clearLaunchConfig() {
        List<CloudConfigEntry> entries = fetchAll();
        launchConfigId = null;
        return saveAll(entries);
    }

    private boolean saveAll(List<CloudConfigEntry> entries) {
        try {
            String url = API_BASE + "/" + owner + "/" + repo + "/contents/" + FILE_PATH;
            JsonObject root = new JsonObject();
            if (launchConfigId != null) {
                root.addProperty("launchConfigId", launchConfigId);
            }
            JsonArray arr = new JsonArray();
            for (CloudConfigEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", e.id);
                obj.addProperty("name", e.name);
                obj.addProperty("timestamp", e.timestamp);
                obj.addProperty("data", e.data);
                arr.add(obj);
            }
            root.add("configs", arr);
            String jsonContent = GSON.toJson(root);
            String encoded = Base64.getEncoder().encodeToString(jsonContent.getBytes(StandardCharsets.UTF_8));

            if (cachedSha == null) {
                JsonObject fileInfo = getFileInfo();
                if (fileInfo != null && fileInfo.has("sha")) {
                    cachedSha = fileInfo.get("sha").getAsString();
                } else {
                    cachedSha = "";
                }
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("message", "Update cloud configs");
            payload.addProperty("content", encoded);
            if (cachedSha != null && !cachedSha.isEmpty()) {
                payload.addProperty("sha", cachedSha);
            }

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                String resp = readBody(conn);
                if (resp != null) {
                    JsonObject respJson = JsonParser.parseString(resp).getAsJsonObject();
                    if (respJson.has("content") && respJson.has("sha")) {
                        cachedSha = respJson.get("sha").getAsString();
                    }
                }
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private JsonObject getFileInfo() {
        try {
            String url = API_BASE + "/" + owner + "/" + repo + "/contents/" + FILE_PATH;
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                String body = readBody(conn);
                return JsonParser.parseString(body).getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private List<CloudConfigEntry> parseEntries(String json) {
        List<CloudConfigEntry> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("launchConfigId")) {
                launchConfigId = root.get("launchConfigId").getAsString();
            } else {
                launchConfigId = null;
            }
            if (root.has("configs")) {
                for (JsonElement el : root.getAsJsonArray("configs")) {
                    JsonObject obj = el.getAsJsonObject();
                    CloudConfigEntry e = new CloudConfigEntry();
                    e.id = obj.get("id").getAsString();
                    e.name = obj.get("name").getAsString();
                    e.timestamp = obj.get("timestamp").getAsLong();
                    e.data = obj.get("data").getAsString();
                    result.add(e);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String generateId() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String readBody(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public CloudConfigEntry importFile(String code) {
        return findById(code);
    }
}
