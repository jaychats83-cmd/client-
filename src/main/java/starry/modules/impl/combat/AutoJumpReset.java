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

    public AutoJumpReset() {
        super("Auto Jump Reset", ModuleCategory.COMBAT);
        settings(chance);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (ThreadLocalRandom.current().nextInt(1, 101) > chance.getValue()) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;
        if (mc.player.hurtTime == 0) return;
        if (mc.player.hurtTime == mc.player.maxHurtTime) return;
        if (!mc.player.isOnGround()) return;
        if (mc.player.hurtTime == 9)
            mc.player.jump();
    }
}
