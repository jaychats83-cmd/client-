package starry.util.config.cloud;

import com.google.gson.JsonObject;

public class CloudConfigEntry {
    public String id;
    public String name;
    public long timestamp;
    public String data;

    public CloudConfigEntry() {}

    public CloudConfigEntry(String id, String name, String data) {
        this.id = id;
        this.name = name;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
    }
}
