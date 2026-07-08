package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.component.DataComponentTypes;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoEat extends ModuleStructure {
    SliderSettings hunger = new SliderSettings("Hunger", "").setValue(14f).range(1f, 20f);

    public AutoEat() {
        super("Auto Eat", ModuleCategory.MISC);
        settings(hunger);
    }

    @Override
    public void deactivate() { if (mc.player != null) mc.options.useKey.setPressed(false); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.currentScreen != null) { if (mc.player != null) mc.options.useKey.setPressed(false); return; }
        boolean hungry = mc.player.getHungerManager().getFoodLevel() <= hunger.getInt();
        if (hungry && selectFood()) { mc.options.useKey.setPressed(true); }
        else { mc.options.useKey.setPressed(false); }
    }

    private boolean selectFood() {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).getComponents().contains(DataComponentTypes.FOOD)) {
                mc.player.getInventory().setSelectedSlot(i); return true;
            }
        return false;
    }
}
