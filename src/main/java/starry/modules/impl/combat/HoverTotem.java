package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.mixin.HandledScreenAccessor;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HoverTotem extends ModuleStructure {
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0f, 20f);
    BooleanSetting hotbar = new BooleanSetting("Hotbar", "Puts a totem in your hotbar as well").setValue(true);
    SliderSettings slot = new SliderSettings("Totem Slot", "Your preferred totem slot").setValue(1f).range(1f, 9f);
    BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "Switches to totem slot when going inside the inventory").setValue(false);

    int clock;

    public HoverTotem() {
        super("Hover Totem", ModuleCategory.CPVP);
        settings(delay, hotbar, slot, autoSwitch);
    }

    @Override
    public void activate() {
        clock = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (mc.currentScreen instanceof InventoryScreen inv) {
            Slot hoveredSlot = ((HandledScreenAccessor) inv).getFocusedSlot();

            int preferredHotbarSlot = slot.getInt() - 1;
            if (autoSwitch.isValue()) {
                mc.player.getInventory().setSelectedSlot(preferredHotbarSlot);
            }

            if (hoveredSlot == null || !hoveredSlot.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                clock = 0;
                return;
            }

            if (hoveredSlot.getIndex() > 35) return;

            if (clock > 0) {
                clock--;
                return;
            }

            int hoveredScreenSlot = hoveredSlot.id;
            if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, hoveredScreenSlot, 40, SlotActionType.SWAP, mc.player);
                clock = fastDelay();
                return;
            }

            if (hotbar.isValue() && !mc.player.getInventory().getStack(preferredHotbarSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, hoveredScreenSlot, preferredHotbarSlot, SlotActionType.SWAP, mc.player);
                clock = fastDelay();
            }
        } else {
            clock = fastDelay();
        }
    }

    private int fastDelay() {
        return Math.max(0, Math.round(delay.getValue() * 0.5F));
    }
}
