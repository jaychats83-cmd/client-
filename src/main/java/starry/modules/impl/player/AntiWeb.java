package starry.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AntiWeb extends ModuleStructure {
    public AntiWeb() {
        super("Anti Web", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        mc.player.setVelocity(mc.player.getVelocity().x * 4, mc.player.getVelocity().y * 4, mc.player.getVelocity().z * 4);
    }
}
