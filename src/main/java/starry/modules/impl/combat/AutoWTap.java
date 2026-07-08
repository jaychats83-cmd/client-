package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoWTap extends ModuleStructure {
    SliderSettings releaseTicks = new SliderSettings("Release Ticks", "How many ticks to release W after a hit").setValue(2f).range(1, 6);
    SliderSettings cooldown = new SliderSettings("Cooldown", "Minimum delay between W taps").setValue(220f).range(50f, 500f);
    BooleanSetting groundOnly = new BooleanSetting("Ground Only", "Only W tap while on the ground").setValue(true);
    BooleanSetting requireSprint = new BooleanSetting("Require Sprint", "Only W tap while sprinting").setValue(true);
    BooleanSetting playersOnly = new BooleanSetting("Players Only", "Only W tap against players").setValue(true);

    private long lastTapTime;
    private int ticksRemaining;
    private boolean restoringForward;

    public AutoWTap() {
        super("Auto WTap", ModuleCategory.COMBAT);
        settings(releaseTicks, cooldown, groundOnly, requireSprint, playersOnly);
    }

    @Override
    public void activate() { ticksRemaining = 0; restoringForward = false; lastTapTime = 0; }
    @Override
    public void deactivate() { restoreForwardIfHeld(); ticksRemaining = 0; restoringForward = false; }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null || mc.crosshairTarget == null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        if (playersOnly.isValue() && !(hit.getEntity() instanceof PlayerEntity)) return;
        if (groundOnly.isValue() && !mc.player.isOnGround()) return;
        if (requireSprint.isValue() && !mc.player.isSprinting()) return;
        if (!isForwardHeld() || System.currentTimeMillis() - lastTapTime < cooldown.getValue()) return;

        ticksRemaining = Math.max(1, releaseTicks.getInt());
        restoringForward = true;
        mc.options.forwardKey.setPressed(false);
        lastTapTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (ticksRemaining <= 0) return;
        ticksRemaining--;
        if (ticksRemaining <= 0) { restoreForwardIfHeld(); restoringForward = false; }
        else mc.options.forwardKey.setPressed(false);
    }

    private boolean isForwardHeld() {
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS || mc.options.forwardKey.isPressed();
    }

    private void restoreForwardIfHeld() {
        if (restoringForward && isForwardHeld()) mc.options.forwardKey.setPressed(true);
    }
}
