package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoInventoryTotem extends ModuleStructure {
    SelectSetting mode = new SelectSetting("Mode", "Whether to randomize the toteming pattern or no").value("Blatant", "Random").selected("Blatant");
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0, 20);
    BooleanSetting hotbar = new BooleanSetting("Hotbar", "Puts a totem in your hotbar as well").setValue(true);
    SliderSettings totemSlot = new SliderSettings("Totem Slot", "Your preferred totem slot").setValue(1f).range(1, 9);
    BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "Switches to totem slot when going inside the inventory").setValue(false);
    BooleanSetting forceTotem = new BooleanSetting("Force Totem", "Puts the totem in the slot, regardless if its space is taken up by something else").setValue(false);
    BooleanSetting autoOpen = new BooleanSetting("Auto Open", "Automatically opens and closes the inventory for you").setValue(false);
    SliderSettings stayOpenFor = new SliderSettings("Stay Open For", "").setValue(0f).range(0, 20);

    private int clock = -1, closeClock = -1;

    public AutoInventoryTotem() {
        super("Auto Inventory Totem", ModuleCategory.CPVP);
        settings(mode, delay, hotbar, totemSlot, autoSwitch, forceTotem, autoOpen, stayOpenFor);
    }

    @Override
    public void activate() { clock = -1; closeClock = -1; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (shouldOpenScreen() && autoOpen.isValue())
            mc.setScreen(new InventoryScreen(mc.player));

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            clock = -1; closeClock = -1;
            return;
        }

        if (clock == -1) clock = delay.getInt();
        if (closeClock == -1) closeClock = stayOpenFor.getInt();
        if (clock > 0) clock--;

        if (autoSwitch.isValue()) mc.player.getInventory().setSelectedSlot(totemSlot.getInt() - 1);

        if (clock <= 0) {
            InventoryScreen inv = (InventoryScreen) mc.currentScreen;
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                int slot = mode.isSelected("Blatant") ? findTotemSlot() : findRandomTotemSlot();
                if (slot != -1) {
                    mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, toScreenSlot(slot), 40, SlotActionType.SWAP, mc.player);
                    clock = delay.getInt();
                    return;
                }
            }

            if (hotbar.isValue()) {
                ItemStack mainHand = mc.player.getMainHandStack();
                if (mainHand.isEmpty() || (forceTotem.isValue() && mainHand.getItem() != Items.TOTEM_OF_UNDYING)) {
                    int slot = mode.isSelected("Blatant") ? findTotemSlot() : findRandomTotemSlot();
                    if (slot != -1) {
                        mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, toScreenSlot(slot), mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
                        clock = delay.getInt();
                        return;
                    }
                }
            }

            if (shouldCloseScreen() && autoOpen.isValue()) {
                if (closeClock != 0) { closeClock--; return; }
                mc.currentScreen.close();
                closeClock = stayOpenFor.getInt();
            }
        }
    }

    private int findTotemSlot() {
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING))
                if (i < 36 || i >= 45) return i;
        }
        return -1;
    }

    private int findRandomTotemSlot() {
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING))
                if (i < 36 || i >= 45) slots.add(i);
        }
        return slots.isEmpty() ? -1 : slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
    }

    private int toScreenSlot(int slot) {
        if (slot < 9) return slot + 36;
        else if (slot < 18) return slot - 9;
        else if (slot < 27) return slot - 9;
        else if (slot < 36) return slot - 9;
        return slot;
    }

    public boolean shouldCloseScreen() {
        if (hotbar.isValue())
            return (mc.player.getInventory().getStack(totemSlot.getInt() - 1).isOf(Items.TOTEM_OF_UNDYING) && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))
                    && mc.currentScreen instanceof InventoryScreen;
        else return mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) && mc.currentScreen instanceof InventoryScreen;
    }

    public boolean shouldOpenScreen() {
        if (hotbar.isValue())
            return (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || mc.player.getInventory().getStack(totemSlot.getInt() - 1).getItem() != Items.TOTEM_OF_UNDYING)
                    && !(mc.currentScreen instanceof InventoryScreen) && countTotemsExceptHotbar() != 0;
        else return mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING
                && !(mc.currentScreen instanceof InventoryScreen) && countTotemsExceptHotbar() != 0;
    }

    private int countTotemsExceptHotbar() {
        int count = 0;
        for (int i = 9; i < 36; i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) count++;
        }
        return count;
    }
}
