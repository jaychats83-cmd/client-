package starry.util.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import starry.util.config.impl.UserConfigPath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Theme currentTheme = Theme.DARK;

    public static void load() {
        try {
            Path configPath = getConfigPath();
            if (Files.exists(configPath)) {
                try (BufferedReader r = Files.newBufferedReader(configPath)) {
                    JsonObject obj = GSON.fromJson(r, JsonObject.class);
                    String name = obj.get("theme").getAsString();
                    for (Theme t : Theme.PRESETS) {
                        if (t.name.equalsIgnoreCase(name)) {
                            currentTheme = t;
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentTheme = Theme.DARK;
    }

    public static void save() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(configPath)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("theme", currentTheme.name);
                GSON.toJson(obj, w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Theme getTheme() {
        return currentTheme;
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;
        save();
    }

    public static int getThemeIndex() {
        for (int i = 0; i < Theme.PRESETS.length; i++) {
            if (Theme.PRESETS[i].name.equals(currentTheme.name)) return i;
        }
        return 0;
    }

    public static void setThemeIndex(int index) {
        if (index >= 0 && index < Theme.PRESETS.length) {
            setTheme(Theme.PRESETS[index]);
        }
    }

    private static Path getConfigPath() {
        return UserConfigPath.resolve("Theme.json");
    }
}
