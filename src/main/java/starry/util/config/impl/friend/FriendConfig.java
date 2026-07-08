package starry.util.config.impl.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import starry.util.config.impl.UserConfigPath;
import starry.util.config.impl.consolelogger.Logger;
import starry.util.repository.friend.FriendUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FriendConfig {
    private static FriendConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private FriendConfig() {
    }

    public static FriendConfig getInstance() {
        if (instance == null) {
            instance = new FriendConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (String name : FriendUtils.getFriendNames()) {
                array.add(name);
            }
            Files.writeString(configPath(), gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("FriendConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            Path configPath = configPath();
            if (!Files.exists(configPath)) {
                FriendUtils.setFriends(List.of());
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<String> names = new ArrayList<>();
            array.forEach(element -> names.add(element.getAsString()));
            FriendUtils.setFriends(names);
            Logger.success("FriendConfig: friends.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("FriendConfig: Load failed! " + e.getMessage());
        }
    }

    private Path configPath() {
        return UserConfigPath.resolve("friends.json");
    }
}
