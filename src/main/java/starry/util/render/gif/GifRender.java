package starry.util.render.gif;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import starry.util.render.Render2D;

import java.util.ArrayList;
import java.util.List;

public class GifRender {

    private static final List<Identifier> backgroundFrames = new ArrayList<>();

    private static long lastBackgroundTime = 0;

    private static int backgroundFrameIndex = 0;

    private static final long BACKGROUND_DELAY = 50;

    private static boolean initialized = false;
    private static boolean useDiscordAvatar = false;
    private static long lastDiscordCheck = 0;

    public static void init() {
        if (initialized) return;

        backgroundFrames.clear();

        for (int i = 0; i <= 16; i++) {
            String frameName = String.format("frame_%02d_delay-0.05s", i);
            Identifier id = Identifier.of("starry", "images/gifs/back/" + frameName + ".png");
            backgroundFrames.add(id);
        }

        lastBackgroundTime = System.currentTimeMillis();

        initialized = true;
    }

    public static void tick() {
        if (!initialized) return;

        long currentTime = System.currentTimeMillis();

        if (!backgroundFrames.isEmpty() && currentTime - lastBackgroundTime >= BACKGROUND_DELAY) {
            backgroundFrameIndex = (backgroundFrameIndex + 1) % backgroundFrames.size();
            lastBackgroundTime = currentTime;
        }
    }

    public static void drawAvatar(float x, float y, float width, float height, int color) {
        if (!initialized) init();
        if (useDiscordAvatar()) {
            Render2D.texture(Identifier.of("starry", "images/gifs/avatar/discord_avatar.png"), x, y, width, height, 1, 15, color);
        }
    }

    public static void drawAvatar(float x, float y, float width, float height, float radius, int color) {
        if (!initialized) init();
        if (useDiscordAvatar()) {
            Render2D.texture(Identifier.of("starry", "images/gifs/avatar/discord_avatar.png"), x, y, width, height, 1f, radius, color);
        }
    }

    public static void drawBackground(float x, float y, float width, float height, int color) {
        if (!initialized) init();
        if (backgroundFrames.isEmpty()) return;

        Identifier frame = backgroundFrames.get(backgroundFrameIndex);
        Render2D.texture(frame, x, y, width, height, color);
    }

    public static void drawBackground(float x, float y, float width, float height, float radius, int color) {
        if (!initialized) init();
        if (backgroundFrames.isEmpty()) return;

        Identifier frame = backgroundFrames.get(backgroundFrameIndex);
        Render2D.texture(frame, x, y, width, height, 1f, radius, color);
    }

    public static void resetBackground() {
        backgroundFrameIndex = 0;
        lastBackgroundTime = System.currentTimeMillis();
    }

    public static void reset() {
        resetBackground();
    }

    private static boolean useDiscordAvatar() {
        if (useDiscordAvatar) return true;
        long now = System.currentTimeMillis();
        if (now - lastDiscordCheck < 5000) return false;
        lastDiscordCheck = now;
        useDiscordAvatar = MinecraftClient.getInstance().getResourceManager()
                .getResource(Identifier.of("starry", "images/gifs/avatar/discord_avatar.png")).isPresent();
        return useDiscordAvatar;
    }
}
