package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AimAssist extends ModuleStructure {
    BooleanSetting stickyAim = new BooleanSetting("Sticky Aim", "Aims at the last attacked player").setValue(false);
    BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", "").setValue(true);
    BooleanSetting onLeftClick = new BooleanSetting("On Left Click", "Only gets triggered if holding down left click").setValue(false);
    SelectSetting aimAt = new SelectSetting("Aim At", "").value("Head", "Chest", "Legs").selected("Head");
    BooleanSetting stopAtTargetVertical = new BooleanSetting("Stop at Target Vert", "Stops vertically assisting if already aiming at the entity").setValue(true);
    BooleanSetting stopAtTargetHorizontal = new BooleanSetting("Stop at Target Horiz", "Stops horizontally assisting if already aiming at the entity").setValue(false);
    SliderSettings radius = new SliderSettings("Radius", "").setValue(5f).range(0.1f, 6f);
    BooleanSetting seeOnly = new BooleanSetting("See Only", "").setValue(true);
    BooleanSetting lookAtNearest = new BooleanSetting("Look at Nearest", "").setValue(false);
    SliderSettings fov = new SliderSettings("FOV", "").setValue(180f).range(5f, 360f);
    MinMaxSetting pitchSpeed = new MinMaxSetting("Vertical Speed", "").range(0f, 10f).defaultValue(2f, 4f);
    MinMaxSetting yawSpeed = new MinMaxSetting("Horizontal Speed", "").range(0f, 10f).defaultValue(2f, 4f);
    SliderSettings speedChange = new SliderSettings("Speed Delay", "Time in milliseconds to wait after resetting random speed").setValue(250f).range(0f, 1000f);
    SliderSettings randomization = new SliderSettings("Chance", "").setValue(50f).range(0f, 100f);
    BooleanSetting yawAssist = new BooleanSetting("Horizontal", "").setValue(true);
    BooleanSetting pitchAssist = new BooleanSetting("Vertical", "").setValue(true);
    SliderSettings waitFor = new SliderSettings("Wait on Move", "After you move your mouse aim assist will stop working for the selected amount of time").setValue(0f).range(0f, 1000f);
    SelectSetting lerp = new SelectSetting("Lerp", "Linear interpolation to use to rotate").value("Normal", "Smoothstep", "EaseOut").selected("Normal");
    SelectSetting posMode = new SelectSetting("Pos mode", "Precision of the target position").value("Normal", "Lerped").selected("Normal");

    private boolean move;
    private float pitch, yaw;
    private long lastMoveTime;

    public AimAssist() {
        super("Aim Assist", ModuleCategory.COMBAT);
        settings(stickyAim, onlyWeapon, onLeftClick, aimAt, stopAtTargetVertical, stopAtTargetHorizontal, radius, seeOnly, lookAtNearest, fov, pitchSpeed, yawSpeed, speedChange, randomization, yawAssist, pitchAssist, waitFor, lerp, posMode);
    }

    @Override
    public void activate() {
        move = true;
        pitch = pitchSpeed.getRandomValue();
        yaw = yawSpeed.getRandomValue();
        lastMoveTime = System.currentTimeMillis();
    }

    private PlayerEntity findTarget() {
        PlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && player != mc.player && player.isAlive()) {
                double dist = mc.player.distanceTo(player);
                if (dist <= radius.getValue() && dist < nearestDist) {
                    if (seeOnly.isValue() && !mc.player.canSee(player)) continue;
                    nearest = player;
                    nearestDist = dist;
                }
            }
        }
        if (stickyAim.isValue() && mc.player.getAttacking() instanceof PlayerEntity player && player.distanceTo(mc.player) < radius.getValue())
            nearest = player;
        return nearest;
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        if (waitFor.getValue() > 0 && System.currentTimeMillis() - lastMoveTime < waitFor.getValue()) return;

        if (onlyWeapon.isValue()) {
            net.minecraft.item.ItemStack stack = mc.player.getMainHandStack();
            if (!isSwordOrAxe(stack))
                return;
        }

        if (onLeftClick.isValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        if ((System.currentTimeMillis() - lastMoveTime) >= speedChange.getValue()) {
            pitch = pitchSpeed.getRandomValue();
            yaw = yawSpeed.getRandomValue();
            lastMoveTime = System.currentTimeMillis();
        }

        Vec3d targetPos = posMode.isSelected("Normal") ? target.getEntityPos() : target.getLerpedPos(event.getPartialTicks());
        if (aimAt.isSelected("Chest")) targetPos = targetPos.add(0, -0.5, 0);
        else if (aimAt.isSelected("Legs")) targetPos = targetPos.add(0, -1.2, 0);

        if (lookAtNearest.isValue()) {
            double offsetX = mc.player.getX() - target.getX() > 0 ? 0.29 : -0.29;
            double offsetZ = mc.player.getZ() - target.getZ() > 0 ? 0.29 : -0.29;
            targetPos = targetPos.add(offsetX, 0, offsetZ);
        }

        double deltaX = targetPos.x - mc.player.getX();
        double deltaZ = targetPos.z - mc.player.getZ();
        double deltaY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(deltaY, distance));

        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDiff = MathHelper.wrapDegrees(targetPitch - mc.player.getPitch());
        double angleToTarget = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        if (angleToTarget > fov.getValue() / 2) return;

        float yawStrength = yaw / 50f;
        float pitchStrengthVal = pitch / 50f;

        float newYaw = mc.player.getYaw();
        float newPitch = mc.player.getPitch();

        if (lerp.isSelected("Normal")) {
            newYaw += yawDiff * yawStrength;
            newPitch += pitchDiff * pitchStrengthVal;
        } else if (lerp.isSelected("Smoothstep")) {
            float t = MathHelper.clamp(yawStrength, 0, 1);
            float smooth = t * t * (3 - 2 * t);
            newYaw += yawDiff * smooth;
            t = MathHelper.clamp(pitchStrengthVal, 0, 1);
            smooth = t * t * (3 - 2 * t);
            newPitch += pitchDiff * smooth;
        } else if (lerp.isSelected("EaseOut")) {
            float t = MathHelper.clamp(yawStrength * event.getPartialTicks(), 0, 1);
            float back = 1 + 2.70158f * (float) Math.pow(t - 1, 3) + 1.70158f * (float) Math.pow(t - 1, 2);
            newYaw += yawDiff * back;
            t = MathHelper.clamp(pitchStrengthVal * event.getPartialTicks(), 0, 1);
            back = 1 + 2.70158f * (float) Math.pow(t - 1, 3) + 1.70158f * (float) Math.pow(t - 1, 2);
            newPitch += pitchDiff * back;
        }

        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 101) <= randomization.getValue()) {
            if (move && yawAssist.isValue()) {
                if (stopAtTargetHorizontal.isValue() && mc.crosshairTarget instanceof EntityHitResult hr && hr.getEntity() == target) return;
                mc.player.setYaw(newYaw);
            }
            if (move && pitchAssist.isValue()) {
                if (stopAtTargetVertical.isValue() && mc.crosshairTarget instanceof EntityHitResult hr && hr.getEntity() == target) return;
                mc.player.setPitch(newPitch);
            }
        }
    }

    private boolean isSword(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_sword");
    }

    private boolean isAxe(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_axe");
    }

    private boolean isSwordOrAxe(ItemStack stack) {
        return isSword(stack) || isAxe(stack);
    }
}
