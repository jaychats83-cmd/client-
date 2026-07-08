package starry.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Sprint extends ModuleStructure {
    public Sprint() {
        super("Sprint", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player != null) mc.player.setSprinting(mc.player.input.hasForwardMovement());
    }
}
