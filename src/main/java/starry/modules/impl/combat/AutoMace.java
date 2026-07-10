package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoMace extends ModuleStructure {
    SelectSetting targetMode = new SelectSetting("Targets", "What Auto Mace can target").value("Players", "Living").selected("Players");
    SelectSetting aimMode = new SelectSetting("Aim", "Where rotations aim").value("Eyes", "Center", "Feet").selected("Center");
    SliderSettings range = new SliderSettings("Range", "").setValue(3.75f).range(1f, 8f);
    SliderSettings minFall = new SliderSettings("Min Fall", "").setValue(1.25f).range(0f, 8f);
    SliderSettings maxFall = new SliderSettings("Max Fall", "").setValue(12f).range(0f, 32f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(150f).range(0f, 1500f);
    SliderSettings cooldown = new SliderSettings("Cooldown", "Attack cooldown percent required").setValue(90f).range(0f, 100f);
    SliderSettings switchBackDelay = new SliderSettings("Switch Back Delay MS", "").setValue(0f).range(0f, 1000f);
    BooleanSetting requireFall = new BooleanSetting("Require Fall", "").setValue(false);
    BooleanSetting requireMace = new BooleanSetting("Require Mace", "").setValue(true);
    BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "").setValue(true);
    BooleanSetting axeShield = new BooleanSetting("Axe Shield", "").setValue(true);
    BooleanSetting crosshairFirst = new BooleanSetting("Crosshair First", "").setValue(true);
    BooleanSetting seeOnly = new BooleanSetting("See Only", "").setValue(true);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "").setValue(true);

    private int previousSlot = -1;
    private long lastActionTime;
    private long restoreAt;

    public AutoMace() {
        super("Auto Mace", ModuleCategory.COMBAT);
        settings(targetMode, aimMode, range, minFall, maxFall, delayMs, cooldown, switchBackDelay,
                requireFall, requireMace, autoSwitch, axeShield, crosshairFirst, seeOnly, rotate, swing, switchBack);
    }

    @Override
    public void activate() { lastActionTime = 0; restoreAt = 0; }
    @Override
    public void deactivate() { restoreSlot(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (restoreAt > 0 && System.currentTimeMillis() >= restoreAt) restoreSlot();
        if (System.currentTimeMillis() - lastActionTime < delayMs.getValue()) return;
        if (requireFall.isValue() && (mc.player.isOnGround() || mc.player.fallDistance < minFall.getValue())) return;
        if (maxFall.getValue() > 0 && mc.player.fallDistance > maxFall.getValue()) return;

        Entity target = findTarget();
        if (!(target instanceof LivingEntity living) || !living.isAlive() || mc.player.distanceTo(target) > range.getValue()) return;

        if (axeShield.isValue() && target instanceof PlayerEntity player && player.isBlocking()) {
            int axeSlot = getAxeSlot();
            if (axeSlot == -1) return;
            if (mc.player.getInventory().getSelectedSlot() != axeSlot && autoSwitch.isValue()) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                selectSlot(axeSlot);
            }
            if (!isAxe(mc.player.getMainHandStack())) return;
            rotateTo(target);
            attack(target);
            return;
        }

        int maceSlot = findItemInHotbar(Items.MACE);
        if (maceSlot == -1) { if (requireMace.isValue()) return; }
        else if (!mc.player.getMainHandStack().isOf(Items.MACE) && autoSwitch.isValue()) {
            previousSlot = mc.player.getInventory().getSelectedSlot();
            selectSlot(maceSlot);
        }
        if (requireMace.isValue() && !mc.player.getMainHandStack().isOf(Items.MACE)) return;

        rotateTo(target);
        attack(target);
    }

    private void attack(Entity target) {
        if (mc.player.getAttackCooldownProgress(0.5F) < cooldown.getValue() / 100f) return;
        mc.interactionManager.attackEntity(mc.player, target);
        if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        if (switchBack.isValue()) scheduleRestore();
        lastActionTime = System.currentTimeMillis();
    }

    private Entity findTarget() {
        if (crosshairFirst.isValue() && mc.crosshairTarget instanceof EntityHitResult hit && isValidTarget(hit.getEntity()))
            return hit.getEntity();

        PlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity p && isValidTarget(p)) {
                double dist = mc.player.distanceTo(p);
                if (dist <= range.getValue() && dist < nearestDist) { nearest = p; nearestDist = dist; }
            }
        }
        if (nearest != null) return nearest;

        if (targetMode.isSelected("Players")) return null;
        Entity nearestEntity = null;
        double nearestEntDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity && isValidTarget(entity)) {
                double dist = mc.player.distanceTo(entity);
                if (dist <= range.getValue() && dist < nearestEntDist) { nearestEntity = entity; nearestEntDist = dist; }
            }
        }
        return nearestEntity;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) return false;
        if (targetMode.isSelected("Players") && !(entity instanceof PlayerEntity)) return false;
        return !seeOnly.isValue() || mc.player.canSee(entity);
    }

    private void rotateTo(Entity target) {
        if (!rotate.isValue()) return;
        Vec3d targetPos = aimMode.isSelected("Eyes")
                ? target.getEyePos()
                : aimMode.isSelected("Feet") ? target.getEntityPos() : target.getBoundingBox().getCenter();
        double diffX = targetPos.x - mc.player.getX();
        double diffZ = targetPos.z - mc.player.getZ();
        double diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-diffX, diffZ)));
        mc.player.setPitch((float) Math.toDegrees(-Math.atan2(diffY, dist)));
    }

    private void restoreSlot() {
        if (mc.player != null && previousSlot >= 0 && previousSlot < 9)
            selectSlot(previousSlot);
        previousSlot = -1;
        restoreAt = 0;
    }

    private void scheduleRestore() {
        if (switchBackDelay.getValue() <= 0) restoreSlot();
        else restoreAt = System.currentTimeMillis() + (long) switchBackDelay.getValue();
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private int getAxeSlot() {
        for (int i = 0; i < 9; i++)
            if (isAxe(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    private int findItemInHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private boolean isAxe(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_axe");
    }
}
