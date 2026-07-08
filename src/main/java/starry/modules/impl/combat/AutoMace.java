package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoMace extends ModuleStructure {
    SliderSettings range = new SliderSettings("Range", "").setValue(3.75f).range(1f, 8f);
    SliderSettings minFall = new SliderSettings("Min Fall", "").setValue(1.25f).range(0f, 8f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(150f).range(0f, 1500f);
    BooleanSetting requireFall = new BooleanSetting("Require Fall", "").setValue(true);
    BooleanSetting requireMace = new BooleanSetting("Require Mace", "").setValue(true);
    BooleanSetting axeShield = new BooleanSetting("Axe Shield", "").setValue(true);
    BooleanSetting crosshairFirst = new BooleanSetting("Crosshair First", "").setValue(true);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "").setValue(true);

    private int previousSlot = -1;
    private long lastActionTime;

    public AutoMace() {
        super("Auto Mace", ModuleCategory.COMBAT);
        settings(range, minFall, delayMs, requireFall, requireMace, axeShield, crosshairFirst, rotate, swing, switchBack);
    }

    @Override
    public void activate() { lastActionTime = 0; }
    @Override
    public void deactivate() { restoreSlot(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastActionTime < delayMs.getValue()) return;
        if (requireFall.isValue() && (mc.player.isOnGround() || mc.player.fallDistance < minFall.getValue())) return;

        Entity target = findTarget();
        if (!(target instanceof LivingEntity living) || !living.isAlive() || mc.player.distanceTo(target) > range.getValue()) return;

        if (axeShield.isValue() && target instanceof PlayerEntity player && player.isBlocking()) {
            int axeSlot = getAxeSlot();
            if (axeSlot == -1) return;
            if (mc.player.getInventory().getSelectedSlot() != axeSlot) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(axeSlot);
            }
            rotateTo(target);
            if (mc.player.getAttackCooldownProgress(0.5F) >= 0.9F) {
                mc.interactionManager.attackEntity(mc.player, target);
                if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                if (switchBack.isValue()) restoreSlot();
                lastActionTime = System.currentTimeMillis();
            }
            return;
        }

        int maceSlot = findItemInHotbar(Items.MACE);
        if (maceSlot == -1) { if (requireMace.isValue()) return; }
        else if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
            previousSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(maceSlot);
        }
        if (requireMace.isValue() && !mc.player.getMainHandStack().isOf(Items.MACE)) return;

        rotateTo(target);
        if (mc.player.getAttackCooldownProgress(0.5F) >= 0.9F) {
            mc.interactionManager.attackEntity(mc.player, target);
            if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            if (switchBack.isValue()) restoreSlot();
            lastActionTime = System.currentTimeMillis();
        }
    }

    private Entity findTarget() {
        if (crosshairFirst.isValue() && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() != mc.player)
            return hit.getEntity();

        PlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity p && p != mc.player && p.isAlive() && mc.player.canSee(p)) {
                double dist = mc.player.distanceTo(p);
                if (dist <= range.getValue() && dist < nearestDist) { nearest = p; nearestDist = dist; }
            }
        }
        if (nearest != null) return nearest;

        Entity nearestEntity = null;
        double nearestEntDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity && entity != mc.player && entity.isAlive()) {
                double dist = mc.player.distanceTo(entity);
                if (dist <= range.getValue() && dist < nearestEntDist) { nearestEntity = entity; nearestEntDist = dist; }
            }
        }
        return nearestEntity;
    }

    private void rotateTo(Entity target) {
        Vec3d targetPos = target.getBoundingBox().getCenter();
        double diffX = targetPos.x - mc.player.getX();
        double diffZ = targetPos.z - mc.player.getZ();
        double diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-diffX, diffZ)));
        mc.player.setPitch((float) Math.toDegrees(-Math.atan2(diffY, dist)));
    }

    private void restoreSlot() {
        if (mc.player != null && previousSlot >= 0 && previousSlot < 9)
            mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
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
