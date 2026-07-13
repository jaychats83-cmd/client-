package starry.util.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Theme currentTheme = Theme.DARK;
    private static String selfDestructStyle = "Themed";
    private static int selfDestructDim = 175;
    private static boolean selfDestructDetails = true;

    public static void load() {
        try {
            Path configPath = getConfigPath();
            if (Files.exists(configPath)) {
                try (BufferedReader r = Files.newBufferedReader(configPath)) {
                    JsonObject obj = GSON.fromJson(r, JsonObject.class);
                    String name = obj.has("theme") ? obj.get("theme").getAsString() : "Dark";
                    for (Theme t : Theme.PRESETS) if (t.name.equalsIgnoreCase(name)) currentTheme = t;
                    if (obj.has("selfDestructStyle")) {
                        String style = obj.get("selfDestructStyle").getAsString();
                        if (style.equals("Themed") || style.equals("Danger") || style.equals("Minimal")) selfDestructStyle = style;
                    }
                    if (obj.has("selfDestructDim")) selfDestructDim = Math.max(0, Math.min(255, obj.get("selfDestructDim").getAsInt()));
                    if (obj.has("selfDestructDetails")) selfDestructDetails = obj.get("selfDestructDetails").getAsBoolean();
                    return;
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
                obj.addProperty("selfDestructStyle", selfDestructStyle);
                obj.addProperty("selfDestructDim", selfDestructDim);
                obj.addProperty("selfDestructDetails", selfDestructDetails);
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

    public static String getSelfDestructStyle() { return selfDestructStyle; }

    public static void cycleSelfDestructStyle() {
        selfDestructStyle = switch (selfDestructStyle) {
            case "Themed" -> "Danger";
            case "Danger" -> "Minimal";
            default -> "Themed";
        };
        save();
    }

    public static int getSelfDestructDim() { return selfDestructDim; }

    public static String getSelfDestructDimName() {
        return selfDestructDim <= 130 ? "Light" : selfDestructDim >= 210 ? "Strong" : "Medium";
    }

    public static void cycleSelfDestructDim() {
        selfDestructDim = selfDestructDim <= 130 ? 175 : selfDestructDim < 210 ? 220 : 120;
        save();
    }

    public static boolean isSelfDestructDetails() { return selfDestructDetails; }

    public static void toggleSelfDestructDetails() {
        selfDestructDetails = !selfDestructDetails;
        save();
    }

    private static Path getConfigPath() {
        Path configDir = Paths.get("starry", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (Exception ignored) {}
        return configDir.resolve("Theme.json");
    }
}
