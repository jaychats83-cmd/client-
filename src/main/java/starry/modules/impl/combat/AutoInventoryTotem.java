package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.mixin.HandledScreenAccessor;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoInventoryTotem extends ModuleStructure {
    SelectSetting mode = new SelectSetting("Mode", "Whether to randomize the toteming pattern or no").value("Blatant", "Random").selected("Blatant");
    SliderSettings delay = new SliderSettings("Delay MS", "").setValue(0f).range(0f, 1000f);
    BooleanSetting hotbar = new BooleanSetting("Hotbar", "Puts a totem in your hotbar as well").setValue(true);
    SliderSettings totemSlot = new SliderSettings("Totem Slot", "Your preferred totem slot").setValue(1f).range(1, 9);
    BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "Switches to totem slot when going inside the inventory").setValue(false);
    BooleanSetting forceTotem = new BooleanSetting("Force Totem", "Puts the totem in the slot, regardless if its space is taken up by something else").setValue(false);

    private long lastActionTime = -1;

    public AutoInventoryTotem() {
        super("Auto Inventory Totem", ModuleCategory.CPVP);
        settings(mode, delay, hotbar, totemSlot, autoSwitch, forceTotem);
    }

    @Override
    public void activate() { lastActionTime = -1; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            lastActionTime = -1;
            return;
        }

        if (lastActionTime == -1) lastActionTime = System.currentTimeMillis();

        if (autoSwitch.isValue()) mc.player.getInventory().setSelectedSlot(totemSlot.getInt() - 1);

        if (System.currentTimeMillis() - lastActionTime >= delay.getValue()) {
            InventoryScreen inv = (InventoryScreen) mc.currentScreen;
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                int slot = mode.isSelected("Blatant") ? findTotemSlot() : findRandomTotemSlot();
                if (slot != -1) {
                    moveCursorToSlot(inv, slot);
                    mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, slot, 40, SlotActionType.SWAP, mc.player);
                    lastActionTime = System.currentTimeMillis();
                    return;
                }
            }

            if (hotbar.isValue()) {
                ItemStack mainHand = mc.player.getMainHandStack();
                if (mainHand.isEmpty() || (forceTotem.isValue() && mainHand.getItem() != Items.TOTEM_OF_UNDYING)) {
                    int slot = mode.isSelected("Blatant") ? findTotemSlot() : findRandomTotemSlot();
                    if (slot != -1) {
                        moveCursorToSlot(inv, slot);
                        mc.interactionManager.clickSlot(inv.getScreenHandler().syncId, slot, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
                        lastActionTime = System.currentTimeMillis();
                        return;
                    }
                }
            }

        }
    }

    private int findTotemSlot() {
        for (int i = 9; i <= 44 && i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING))
                return i;
        }
        return -1;
    }

    private int findRandomTotemSlot() {
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 9; i <= 44 && i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING))
                slots.add(i);
        }
        return slots.isEmpty() ? -1 : slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
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
        for (int i = 9; i <= 35 && i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) count++;
        }
        return count;
    }

    private void moveCursorToSlot(InventoryScreen screen, int slotId) {
        if (slotId < 0 || slotId >= screen.getScreenHandler().slots.size()) return;
        var slot = screen.getScreenHandler().getSlot(slotId);
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        double scale = mc.getWindow().getScaleFactor();
        double cursorX = (accessor.getX() + slot.x + 8) * scale;
        double cursorY = (accessor.getY() + slot.y + 8) * scale;
        GLFW.glfwSetCursorPos(mc.getWindow().getHandle(), cursorX, cursorY);
    }
}
