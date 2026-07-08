package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.PotionItem;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HoldUseItem extends ModuleStructure {
    BooleanSetting selectItem = new BooleanSetting("Select Item", "").setValue(true);

    public HoldUseItem() {
        super("Hold Use Item", ModuleCategory.MISC);
        settings(selectItem);
    }

    @Override
    public void deactivate() { if (mc.player != null) mc.options.useKey.setPressed(false); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.currentScreen != null) { if (mc.player != null) mc.options.useKey.setPressed(false); return; }
        if (selectItem.isValue()) {
            for (int i = 0; i < 9; i++)
                if (mc.player.getInventory().getStack(i).getComponents().contains(DataComponentTypes.FOOD)
                        || mc.player.getInventory().getStack(i).getItem() instanceof PotionItem) {
                    mc.player.getInventory().setSelectedSlot(i); break;
                }
        }
        mc.options.useKey.setPressed(true);
    }
}
