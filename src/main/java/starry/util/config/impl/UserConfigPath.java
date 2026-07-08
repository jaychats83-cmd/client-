package starry.util.config.impl;

import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class UserConfigPath {
    private static final String FALLBACK_USER = "default";

    private UserConfigPath() {
    }

    public static Path resolve(String fileName) {
        Path dir = Paths.get("qcloud", "configs", "users", currentUser());
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        Path userPath = dir.resolve(fileName);
        Path legacyPath = Paths.get("starry", "configs", fileName);
        if (!Files.exists(userPath) && Files.exists(legacyPath)) {
            try {
                Files.copy(legacyPath, userPath, StandardCopyOption.COPY_ATTRIBUTES);
            } catch (IOException ignored) {
            }
        }
        return userPath;
    }

    public static String remotePath(String fileName) {
        return "configs/users/" + currentUser() + "/" + fileName;
    }

    public static String currentUser() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String username = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : FALLBACK_USER;
        if (username == null || username.isBlank()) {
            username = FALLBACK_USER;
        }
        return username.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
