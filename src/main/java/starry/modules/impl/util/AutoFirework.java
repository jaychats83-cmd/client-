package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoFirework extends ModuleStructure {
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(1200f).range(250f, 5000f);
    private long lastAction;

    public AutoFirework() {
        super("Auto Firework", ModuleCategory.MISC);
        settings(delayMs);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null || !mc.player.isGliding()) return;
        if (System.currentTimeMillis() - lastAction < delayMs.getValue()) return;
        if (selectItem(Items.FIREWORK_ROCKET)) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAction = System.currentTimeMillis();
        }
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) { mc.player.getInventory().setSelectedSlot(i); return true; }
        return false;
    }
}
