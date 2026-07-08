package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FastPlace extends ModuleStructure {
    public FastPlace() {
        super("Fast Place", ModuleCategory.MISC);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.itemUseCooldown > 0) mc.itemUseCooldown = 0;
    }
}
