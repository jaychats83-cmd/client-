package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AntiAFK extends ModuleStructure {
    SliderSettings delaySeconds = new SliderSettings("Delay Seconds", "").setValue(20f).range(5f, 120f);
    private long lastAction;
    private boolean flip;

    public AntiAFK() {
        super("Anti AFK", ModuleCategory.MISC);
        settings(delaySeconds);
    }

    @Override
    public void activate() { lastAction = 0; flip = false; }
    @Override
    public void deactivate() {
        if (mc.player != null) { mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false); }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        if (System.currentTimeMillis() - lastAction < delaySeconds.getValue() * 1000) return;
        flip = !flip;
        mc.options.forwardKey.setPressed(flip);
        mc.options.backKey.setPressed(!flip);
        mc.player.setYaw(mc.player.getYaw() + 7.5F);
        lastAction = System.currentTimeMillis();
    }
}
