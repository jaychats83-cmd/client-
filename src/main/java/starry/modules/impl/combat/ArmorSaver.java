package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArmorSaver extends ModuleStructure {
    SliderSettings durability = new SliderSettings("Durability %", "").setValue(12f).range(1f, 60f);
    SliderSettings delay = new SliderSettings("Delay MS", "").setValue(250f).range(50f, 1000f);
    BooleanSetting requireInventorySpace = new BooleanSetting("Require Space", "").setValue(true);

    private long lastCheckTime;

    public ArmorSaver() {
        super("ArmourSaver", ModuleCategory.COMBAT);
        settings(durability, delay, requireInventorySpace);
    }

    @Override
    public void activate() {
        lastCheckTime = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastCheckTime < delay.getValue()) return;
        if (requireInventorySpace.isValue() && mc.player.getInventory().getEmptySlot() == -1) return;

        for (int slot = 5; slot <= 8; slot++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(slot).getStack();
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            int remaining = stack.getMaxDamage() - stack.getDamage();
            double percent = (remaining * 100.0D) / stack.getMaxDamage();
            if (percent <= durability.getValue()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
                lastCheckTime = System.currentTimeMillis();
                return;
            }
        }
    }
}
