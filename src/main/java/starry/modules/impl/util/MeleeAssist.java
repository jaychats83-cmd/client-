package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeleeAssist extends ModuleStructure {
    SliderSettings range = new SliderSettings("Range", "").setValue(4f).range(1f, 6f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(350f).range(0f, 1500f);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);
    BooleanSetting useCrosshairTarget = new BooleanSetting("Crosshair First", "").setValue(true);

    private long lastAction;

    public MeleeAssist() {
        super("Melee Assist", ModuleCategory.MISC);
        settings(range, delayMs, rotate, useCrosshairTarget);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastAction < delayMs.getValue()) return;

        LivingEntity target = findTarget();
        if (target == null || !target.isAlive()) return;

        if (rotate.isValue()) {
            mc.player.setYaw((float) Math.toDegrees(Math.atan2(target.getZ() - mc.player.getZ(), target.getX() - mc.player.getX())) - 90);
            mc.player.setPitch((float) Math.toDegrees(-Math.atan2(target.getEyeY() - mc.player.getEyeY(), mc.player.distanceTo(target))));
        }

        if (mc.player.distanceTo(target) <= range.getValue() && mc.player.getAttackCooldownProgress(0.5F) >= 0.9F) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAction = System.currentTimeMillis();
        }
    }

    private LivingEntity findTarget() {
        if (useCrosshairTarget.isValue() && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living)
            return living;

        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= range.getValue())
                .min(java.util.Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
    }
}
