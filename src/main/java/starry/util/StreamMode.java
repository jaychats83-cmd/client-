package starry.util;

import net.minecraft.client.MinecraftClient;
import starry.Initialization;
import starry.events.api.events.Event;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.util.theme.ThemeManager;
import starry.util.window.WindowStyle;

import java.util.IdentityHashMap;
import java.util.Map;

/** Capture protection and client-visual suppression controlled from the Client tab. */
public final class StreamMode {
    private static final Map<ModuleStructure, Boolean> RESTORE_STATES = new IdentityHashMap<>();
    private static Boolean previousPauseOnLostFocus;
    private static int affinityRefreshTicks;

    private StreamMode() {}

    public static boolean isEnabled() {
        return ThemeManager.isStreamModeEnabled();
    }

    public static void initialize() {
        if (isEnabled()) applyEnabledState();
    }

    public static void setEnabled(boolean enabled) {
        if (enabled == isEnabled()) return;
        ThemeManager.setStreamModeEnabled(enabled);
        if (enabled) applyEnabledState();
        else restoreNormalState();
    }

    public static void tick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.pauseOnLostFocus = false;
        suppressVisualModules();
        if (++affinityRefreshTicks >= 100) {
            affinityRefreshTicks = 0;
            WindowStyle.setCaptureExcluded(mc.getWindow().getHandle(), true);
        }
    }

    public static boolean shouldSuppress(Event event) {
        if (!isEnabled()) return false;
        String name = event.getClass().getSimpleName();
        return name.contains("Render") || name.contains("Draw") || name.contains("Color")
                || name.contains("Camera") || name.contains("Fov") || name.contains("Hand")
                || name.contains("Swing") || name.contains("ChunkOcclusion")
                || name.contains("BoundingBox") || name.equals("Event3D");
    }

    private static void applyEnabledState() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (previousPauseOnLostFocus == null) previousPauseOnLostFocus = mc.options.pauseOnLostFocus;
        mc.options.pauseOnLostFocus = false;
        WindowStyle.setCaptureExcluded(mc.getWindow().getHandle(), true);
        affinityRefreshTicks = 0;
        suppressVisualModules();
    }

    private static void restoreNormalState() {
        MinecraftClient mc = MinecraftClient.getInstance();
        WindowStyle.setCaptureExcluded(mc.getWindow().getHandle(), false);
        if (previousPauseOnLostFocus != null) {
            mc.options.pauseOnLostFocus = previousPauseOnLostFocus;
            previousPauseOnLostFocus = null;
        }
        for (Map.Entry<ModuleStructure, Boolean> entry : RESTORE_STATES.entrySet()) {
            if (entry.getValue()) entry.getKey().setState(true);
        }
        RESTORE_STATES.clear();
    }

    private static void suppressVisualModules() {
        Initialization initialization = Initialization.getInstance();
        if (initialization == null || initialization.getManager() == null
                || initialization.getManager().getModuleRepository() == null) return;
        for (ModuleStructure module : initialization.getManager().getModuleRepository().modules()) {
            if (!isVisualCategory(module.getCategory()) || !module.isState()) continue;
            RESTORE_STATES.put(module, true);
            module.setState(false);
        }
    }

    private static boolean isVisualCategory(ModuleCategory category) {
        return category == ModuleCategory.ESP || category == ModuleCategory.RENDER || category == ModuleCategory.VISUALS;
    }
}
