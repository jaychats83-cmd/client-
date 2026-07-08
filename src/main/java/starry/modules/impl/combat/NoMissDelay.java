package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoMissDelay extends ModuleStructure {
    BooleanSetting cooldown = new BooleanSetting("Cooldown", "").setValue(true);
    BooleanSetting stopOnHit = new BooleanSetting("Stop on Hit", "").setValue(true);

    private int swingTicks;
    private boolean bool;

    public NoMissDelay() {
        super("No Miss Delay", ModuleCategory.COMBAT);
        settings(cooldown, stopOnHit);
    }

    @Override
    public void activate() { swingTicks = 0; bool = false; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (cooldown.isValue()) return;

        if (mc.player.getAttackCooldownProgress(0) >= 0.9F) {
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.interactionManager.attackEntity(mc.player, null);
        }
    }
}
