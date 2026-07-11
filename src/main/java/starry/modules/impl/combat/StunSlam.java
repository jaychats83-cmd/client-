package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StunSlam extends ModuleStructure {
    SelectSetting targetMode = new SelectSetting("Target", "").value("Nearest", "Crosshair").selected("Nearest");
    SelectSetting aimMode = new SelectSetting("Aim", "").value("Eyes", "Center", "Feet").selected("Center");
    SliderSettings range = new SliderSettings("Range", "").setValue(4f).range(1f, 8f);
    MinMaxSetting fallDistance = new MinMaxSetting("Fall Distance", "Fall distance range").defaultValue(0.2f, 10f).range(0f, 32f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(300f).range(0f, 1500f);
    SliderSettings cooldown = new SliderSettings("Cooldown", "Attack cooldown percent required").setValue(90f).range(0f, 100f);
    BooleanSetting requireFall = new BooleanSetting("Require Fall", "").setValue(false);
    BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "").setValue(true);
    BooleanSetting jumpFirst = new BooleanSetting("Jump First", "").setValue(true);
    BooleanSetting seeOnly = new BooleanSetting("See Only", "").setValue(true);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);
    BooleanSetting silentAim = new BooleanSetting("Silent Aim", "Aim server-side without moving your camera").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "").setValue(true);

    private long lastActionTime;
    private int previousSlot = -1;

    public StunSlam() {
        super("Stun Slam", ModuleCategory.COMBAT);
        settings(targetMode, aimMode, range, fallDistance, delayMs, cooldown, requireFall,
                autoSwitch, jumpFirst, seeOnly, rotate, silentAim, swing, switchBack);
    }

    @Override
    public void activate() { lastActionTime = 0; previousSlot = -1; }
    @Override
    public void deactivate() { restoreSlot(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastActionTime < delayMs.getValue()) return;

        PlayerEntity target = findTarget((float) range.getValue());
        if (target == null || !target.isAlive()) return;
        if (!selectItem(Items.MACE)) return;

        if (jumpFirst.isValue() && mc.player.isOnGround()) {
            mc.player.jump();
            lastActionTime = System.currentTimeMillis();
            return;
        }

        if (requireFall.isValue() && (mc.player.isOnGround() || mc.player.fallDistance < fallDistance.getMinValue())) return;
        if (fallDistance.getMaxValue() > 0 && mc.player.fallDistance > fallDistance.getMaxValue()) return;
        if (rotate.isValue()) rotateTo(target);

        if (mc.player.getAttackCooldownProgress(0.5F) >= cooldown.getValue() / 100f) {
            mc.interactionManager.attackEntity(mc.player, target);
            if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
            if (switchBack.isValue()) restoreSlot();
            lastActionTime = System.currentTimeMillis();
        }
    }

    private PlayerEntity findTarget(float range) {
        if (targetMode.isSelected("Crosshair")
                && mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity player
                && isValidTarget(player, range)) return player;

        return mc.world.getPlayers().stream()
                .filter(p -> isValidTarget(p, range))
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
    }

    private boolean isValidTarget(PlayerEntity player, float range) {
        return player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range
                && (!seeOnly.isValue() || mc.player.canSee(player));
    }

    private void rotateTo(PlayerEntity target) {
        Vec3d aim = aimMode.isSelected("Eyes")
                ? target.getEyePos()
                : aimMode.isSelected("Feet") ? target.getEntityPos() : target.getBoundingBox().getCenter();
        double diffX = aim.x - mc.player.getX();
        double diffZ = aim.z - mc.player.getZ();
        double diffY = aim.y - mc.player.getEyeY();
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(-diffX, diffZ));
        float pitch = (float) Math.toDegrees(-Math.atan2(diffY, dist));
        if (silentAim.isValue() && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch,
                    mc.player.isOnGround(), mc.player.horizontalCollision));
        } else {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        if (mc.player.getMainHandStack().isOf(item)) return true;
        if (!autoSwitch.isValue()) return false;
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                if (previousSlot == -1) previousSlot = mc.player.getInventory().getSelectedSlot();
                selectSlot(i);
                return true;
            }
        return false;
    }

    private void restoreSlot() {
        if (mc.player != null && previousSlot >= 0 && previousSlot < 9)
            selectSlot(previousSlot);
        previousSlot = -1;
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
}
