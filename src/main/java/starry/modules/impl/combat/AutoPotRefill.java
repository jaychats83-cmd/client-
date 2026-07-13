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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import starry.mixin.HandledScreenAccessor;

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
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof InventoryScreen inv)) return;

        if (mode.isSelected("Hover")) {
            Slot focusedSlot = ((HandledScreenAccessor) inv).getFocusedSlot();
            if (focusedSlot == null || !isHealthPotion(focusedSlot.getStack())) {
                clock = 0;
                return;
            }

            PlayerInventory inventory = mc.player.getInventory();
            int emptySlot = -1;
            for (int i = 0; i <= 8; i++) { if (inventory.getStack(i).isEmpty()) { emptySlot = i; break; } }
            if (emptySlot == -1) return;

            if (clock < delay.getInt()) { clock++; return; }
            mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, focusedSlot.id, emptySlot, SlotActionType.SWAP, mc.player);
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

    private boolean isHealthPotion(ItemStack stack) {
        StatusEffectInstance potion = new StatusEffectInstance(
                Registries.STATUS_EFFECT.getEntry(StatusEffects.INSTANT_HEALTH.value()), 1, 1);
        var contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        return stack.getItem() instanceof SplashPotionItem
                && contents != null
                && contents.getEffects().toString().contains(potion.toString());
    }

    private int findPotionInInventory() {
        for (int i = 9; i < 36; i++)
            if (isHealthPotion(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }
}