package starry.modules.impl.extras;

import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.util.Instance;

public class NoRotation extends ModuleStructure {
    private final BooleanSetting yaw = new BooleanSetting("Yaw", "Ignore server yaw changes").setValue(true);
    private final BooleanSetting pitch = new BooleanSetting("Pitch", "Ignore server pitch changes").setValue(true);
    private float savedYaw, savedPitch;

    public NoRotation() {
        super("No Rotation", "Keeps your view direction when the server corrects your position", ModuleCategory.EXTRAS);
        settings(yaw, pitch);
    }

    public static void beforeServerRotation() {
        NoRotation module = Instance.get(NoRotation.class);
        if (module == null || !module.isState() || mc.player == null) return;
        module.saveRotation();
    }

    public static void afterServerRotation() {
        NoRotation module = Instance.get(NoRotation.class);
        if (module == null || !module.isState() || mc.player == null) return;
        module.restoreRotation();
    }

    private void saveRotation() {
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
    }

    private void restoreRotation() {
        if (yaw.isValue()) mc.player.setYaw(savedYaw);
        if (pitch.isValue()) mc.player.setPitch(savedPitch);
    }
}
