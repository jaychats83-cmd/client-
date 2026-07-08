package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemAction extends ModuleStructure {
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(250f).range(0f, 5000f);
    BooleanSetting useItem = new BooleanSetting("Use Item", "").setValue(true);
    BooleanSetting disableAfterUse = new BooleanSetting("Disable After Use", "").setValue(false);

    private long lastAction;

    public ItemAction() {
        super("Item Action", ModuleCategory.MISC);
        settings(delayMs, useItem, disableAfterUse);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (System.currentTimeMillis() - lastAction < delayMs.getValue()) return;

        if (useItem.isValue()) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        lastAction = System.currentTimeMillis();
        if (disableAfterUse.isValue()) setState(false);
    }
}
