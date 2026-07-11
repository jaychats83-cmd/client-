package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;

import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoJumpReset extends ModuleStructure {
    SliderSettings chance = new SliderSettings("Chance", "").setValue(100f).range(0f, 100f);
    int lastHurtTime;
    boolean handledHit;

    public AutoJumpReset() {
        super("Auto Jump Reset", ModuleCategory.COMBAT);
        settings(chance);
    }

    @Override
    public void activate() {
        lastHurtTime = 0;
        handledHit = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isUsingItem()) return;
        if (!mc.player.isOnGround()) return;
        int ht = mc.player.hurtTime;
        if (ht == 0) { handledHit = false; lastHurtTime = 0; return; }
        if (ht > lastHurtTime) handledHit = false;
        lastHurtTime = ht;
        if (handledHit || ht != 9) return;
        handledHit = true;
        if (ThreadLocalRandom.current().nextInt(1, 101) > chance.getValue()) return;
        mc.player.jump();
    }
}
