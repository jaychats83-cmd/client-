package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoPotRefill extends ModuleStructure {
    SelectSetting mode = new SelectSetting("Mode", "").value("Auto", "Hover").selected("Auto");
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0, 10);
    private int clock;

    public AutoPotRefill() {
        super("Auto Pot Refill", ModuleCategory.COMBAT);
        settings(mode, delay);
    }

    @Override
    public void activate() { clock = 0; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!(mc.currentScreen instanceof InventoryScreen inv)) return;

        if (mode.isSelected("Hover")) {
            if (clock < delay.getInt()) { clock++; return; }
            PlayerInventory inventory = mc.player.getInventory();
            int emptySlot = -1;
            for (int i = 0; i <= 8; i++) { if (inventory.getStack(i).isEmpty()) { emptySlot = i; break; } }
            if (emptySlot == -1) return;

            if (isHealthPotion(inv.getScreenHandler().getCursorStack())) { clock = 0; return; }

            var focusedSlot = inv.getScreenHandler().getCursorStack();
            if (focusedSlot == null || focusedSlot.isEmpty()) return;
                mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, 0, emptySlot, SlotActionType.SWAP, mc.player);
                clock = 0;
        }

        if (mode.isSelected("Auto")) {
            int slot = findPotionInInventory();
            if (slot != -1) {
                PlayerInventory inventory = mc.player.getInventory();
                int emptySlot = -1;
                for (int i = 0; i <= 8; i++) { if (inventory.getStack(i).isEmpty()) { emptySlot = i; break; } }
                if (emptySlot == -1) return;
                if (clock < delay.getInt()) { clock++; return; }
                mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, slot, emptySlot, SlotActionType.SWAP, mc.player);
                clock = 0;
            }
        }
    }

    private boolean isHealthPotion(net.minecraft.item.ItemStack stack) {
        return stack.isOf(Items.SPLASH_POTION) && stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS) != null
                && stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS).potion().isPresent()
                && stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS).potion().get().value().getEffects().stream()
                .anyMatch(e -> e.getEffectType().value().equals(StatusEffects.INSTANT_HEALTH));
    }

    private int findPotionInInventory() {
        for (int i = 9; i < 36; i++)
            if (isHealthPotion(mc.player.currentScreenHandler.getSlot(i).getStack())) return i;
        return -1;
    }
}
