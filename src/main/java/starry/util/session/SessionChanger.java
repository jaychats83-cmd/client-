package starry.util.session;

import net.minecraft.client.session.Session;
import starry.util.config.impl.UserConfigManager;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class SessionChanger {

    private static Consumer<Session> sessionSetter;

    public static void setSessionSetter(Consumer<Session> setter) {
        sessionSetter = setter;
    }

    public static void changeUsername(String newUsername) {
        if (sessionSetter == null || newUsername == null || newUsername.isEmpty()) return;

        UserConfigManager.saveCurrentUser();

        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newUsername).getBytes());

        Session newSession = new Session(
                newUsername,
                uuid,
                "",
                Optional.empty(),
                Optional.empty()
        );

        sessionSetter.accept(newSession);
        UserConfigManager.loadCurrentUser();
    }

    public static String getCurrentUsername() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc != null && mc.getSession() != null) {
            return mc.getSession().getUsername();
        }
        return "";
    }
}
