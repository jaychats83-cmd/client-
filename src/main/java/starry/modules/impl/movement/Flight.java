package starry.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Flight extends ModuleStructure {
    SliderSettings speed = new SliderSettings("Speed", "").setValue(1f).range(0.1f, 5f);
    SliderSettings verticalSpeed = new SliderSettings("Vertical Speed", "").setValue(0.8f).range(0.1f, 5f);
    SliderSettings antiKickTicks = new SliderSettings("Anti Kick Ticks", "").setValue(20f).range(5f, 80f);
    BooleanSetting sprintBoost = new BooleanSetting("Sprint Boost", "").setValue(true);
    BooleanSetting antiKick = new BooleanSetting("Anti Kick", "").setValue(true);
    BooleanSetting stopOnDisable = new BooleanSetting("Stop On Disable", "").setValue(true);

    private int ticks;

    public Flight() {
        super("Flight", ModuleCategory.MOVEMENT);
        settings(speed, verticalSpeed, sprintBoost, antiKick, antiKickTicks, stopOnDisable);
    }

    @Override
    public void activate() { ticks = 0; }

    @Override
    public void deactivate() {
        if (mc.player != null) {
            mc.player.fallDistance = 0;
            if (stopOnDisable.isValue()) mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        ticks++;
        mc.player.fallDistance = 0;

        Vec3d horizontal = getHorizontalVelocity();
        double y = 0.0;
        if (mc.options.jumpKey.isPressed()) y += verticalSpeed.getValue();
        if (mc.options.sneakKey.isPressed()) y -= verticalSpeed.getValue();
        if (antiKick.isValue() && y == 0.0 && ticks % antiKickTicks.getInt() == 0) y = -0.04;

        mc.player.setVelocity(horizontal.x, y, horizontal.z);
    }

    private Vec3d getHorizontalVelocity() {
        double forward = 0, strafe = 0;
        if (mc.options.forwardKey.isPressed()) forward += 1;
        if (mc.options.backKey.isPressed()) forward -= 1;
        if (mc.options.leftKey.isPressed()) strafe += 1;
        if (mc.options.rightKey.isPressed()) strafe -= 1;
        if (forward == 0 && strafe == 0) return Vec3d.ZERO;

        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = MathHelper.sin((float) yaw);
        double cos = MathHelper.cos((float) yaw);
        double boost = sprintBoost.isValue() && mc.options.sprintKey.isPressed() ? 1.6 : 1.0;
        double speedValue = speed.getValue() * boost;
        double len = Math.sqrt(forward * forward + strafe * strafe);
        forward /= len; strafe /= len;

        return new Vec3d((strafe * cos - forward * sin) * speedValue, 0, (forward * cos + strafe * sin) * speedValue);
    }
}
