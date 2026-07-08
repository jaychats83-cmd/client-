package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TriggerBot extends ModuleStructure {
    SliderSettings cps = new SliderSettings("CPS", "").setValue(10f).range(1f, 20f);
    BooleanSetting playersOnly = new BooleanSetting("Players Only", "").setValue(true);
    BooleanSetting walls = new BooleanSetting("Through Walls", "").setValue(false);
    SliderSettings fov = new SliderSettings("FOV", "").setValue(180f).range(0f, 360f);
    BooleanSetting silentSwing = new BooleanSetting("Silent Swing", "").setValue(false);

    private int tickCounter;

    public TriggerBot() {
        super("Trigger Bot", ModuleCategory.COMBAT);
        settings(cps, playersOnly, walls, fov, silentSwing);
    }

    @Override
    public void activate() { tickCounter = 0; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.crosshairTarget == null) return;
        tickCounter++;
        int delay = Math.max(1, 20 / Math.max(1, cps.getInt()));
        if (tickCounter % delay != 0) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) return;
        if (target.isDead() || !target.isAlive()) return;
        if (playersOnly.isValue() && !(target instanceof net.minecraft.entity.player.PlayerEntity)) return;
        if (!walls.isValue() && mc.player.squaredDistanceTo(target) > 16) return;
        if (mc.player.getAttackCooldownProgress(0) < 0.9F) return;

        mc.interactionManager.attackEntity(mc.player, target);
        if (silentSwing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
    }
}
