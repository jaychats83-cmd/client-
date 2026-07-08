package starry.util.subscription;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class LicenseData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<Entry> licenses = new ArrayList<>();

    public List<Entry> getLicenses() {
        return licenses;
    }

    public String serialize() {
        JsonArray arr = new JsonArray();
        for (Entry e : licenses) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", e.key);
            obj.addProperty("hwid", e.hwid != null ? e.hwid : "");
            obj.addProperty("expiry", e.expiry != null ? e.expiry : "");
            obj.addProperty("plan", e.plan != null ? e.plan : "");
            obj.addProperty("revoked", e.revoked);
            obj.addProperty("discordId", e.discordId != null ? e.discordId : "");
            arr.add(obj);
        }
        JsonObject root = new JsonObject();
        root.add("licenses", arr);
        return GSON.toJson(root);
    }

    public static LicenseData parse(String json) {
        LicenseData result = new LicenseData();
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            JsonArray arr = root.getAsJsonArray("licenses");
            if (arr == null) return result;

            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String key = getString(obj, "key");
                String hwid = getString(obj, "hwid");
                String expiry = getString(obj, "expiry");
                String plan = getString(obj, "plan");
                boolean revoked = getBool(obj, "revoked");
                String discordId = getString(obj, "discordId");
                result.licenses.add(new Entry(key, hwid, expiry, plan, revoked, discordId));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }

    private static boolean getBool(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() && el.getAsBoolean();
    }

    public static class Entry {
        public String key;
        public String hwid;
        public String expiry;
        public String plan;
        public boolean revoked;
        public String discordId;

        public Entry(String key, String hwid, String expiry, String plan, boolean revoked, String discordId) {
            this.key = key;
            this.hwid = hwid;
            this.expiry = expiry;
            this.plan = plan;
            this.revoked = revoked;
            this.discordId = discordId;
        }
    }
}
