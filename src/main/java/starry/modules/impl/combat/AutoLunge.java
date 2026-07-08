package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoLunge extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that triggers the lunge").setKey(GLFW.GLFW_MOUSE_BUTTON_LEFT);
    SliderSettings forwardBoost = new SliderSettings("Forward Boost", "How strongly to push toward the target").setValue(0.42f).range(0f, 1.2f);
    SliderSettings verticalBoost = new SliderSettings("Vertical Boost", "Upward movement added to the lunge").setValue(0.08f).range(0f, 0.5f);
    SliderSettings packetLift = new SliderSettings("Packet Lift", "Small server-side lift packet sent with the lunge").setValue(0.0625f).range(0f, 0.5f);
    SliderSettings maxSpeed = new SliderSettings("Max Speed", "Caps horizontal speed after lunging").setValue(0.85f).range(0.1f, 2f);
    SliderSettings range = new SliderSettings("Range", "Maximum target range").setValue(4.5f).range(1f, 8f);
    SliderSettings cooldown = new SliderSettings("Cooldown Ticks", "Ticks to wait between lunges").setValue(6f).range(0f, 20f);
    SliderSettings motionThreshold = new SliderSettings("Motion Threshold", "Maximum vertical motion allowed before lunging").setValue(0.18f).range(0f, 1f);
    BooleanSetting requireGround = new BooleanSetting("Require Ground", "Only lunges while standing on the ground").setValue(true);
    BooleanSetting playersOnly = new BooleanSetting("Players Only", "Only lunges at players").setValue(false);
    BooleanSetting preserveMomentum = new BooleanSetting("Preserve Momentum", "Adds the lunge to your current movement").setValue(true);
    BooleanSetting sendLiftPacket = new BooleanSetting("Send Lift Packet", "Keeps the old packet lift behavior").setValue(true);

    int cooldownTicks;

    public AutoLunge() {
        super("Auto Lunge", ModuleCategory.COMBAT);
        settings(activateKey, forwardBoost, verticalBoost, packetLift, maxSpeed, range, cooldown, motionThreshold,
                requireGround, playersOnly, preserveMomentum, sendLiftPacket);
    }

    @Override
    public void activate() {
        cooldownTicks = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (cooldownTicks > 0) cooldownTicks--;
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.currentScreen != null) return;
        if (cooldownTicks > 0 || !isActivationPressed()) return;

        Entity target = event.getTarget();
        if (!isValidTarget(target)) return;
        if (requireGround.isValue() && !mc.player.isOnGround()) return;
        if (Math.abs(mc.player.getVelocity().y) > motionThreshold.getValue()) return;

        lungeToward(target);
        cooldownTicks = cooldown.getInt();
    }

    private boolean isValidTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || target == mc.player || !target.isAlive()) return false;
        if (playersOnly.isValue() && !(target instanceof PlayerEntity)) return false;
        return mc.player.distanceTo(living) <= range.getValue();
    }

    private void lungeToward(Entity target) {
        Vec3d direction = target.getEntityPos().subtract(mc.player.getEntityPos()).multiply(1, 0, 1);
        if (direction.lengthSquared() < 1.0E-4) {
            direction = Vec3d.fromPolar(0, mc.player.getYaw()).multiply(1, 0, 1);
        }

        direction = direction.normalize();
        Vec3d current = mc.player.getVelocity();
        double boost = forwardBoost.getValue();
        double x = preserveMomentum.isValue() ? current.x + direction.x * boost : direction.x * boost;
        double z = preserveMomentum.isValue() ? current.z + direction.z * boost : direction.z * boost;

        Vec3d capped = capHorizontalSpeed(x, z);
        double y = Math.max(current.y, verticalBoost.getValue());
        mc.player.setVelocity(capped.x, y, capped.z);
        mc.player.velocityDirty = true;

        if (sendLiftPacket.isValue() && packetLift.getValue() > 0) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), mc.player.getY() + packetLift.getValue(), mc.player.getZ(), false, false));
        }
    }

    private Vec3d capHorizontalSpeed(double x, double z) {
        double speed = Math.sqrt(x * x + z * z);
        double cap = maxSpeed.getValue();
        if (speed <= cap || speed == 0) return new Vec3d(x, 0, z);
        double scale = cap / speed;
        return new Vec3d(x * scale, 0, z * scale);
    }

    private boolean isActivationPressed() {
        int key = activateKey.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return true;
        if (key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}
