package starry.modules.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.modules.module.setting.implement.SliderSettings;

import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LungeMacro extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Macro Key", "Runs the spear-lunge movement macro").setKey(GLFW.GLFW_MOUSE_BUTTON_5);
    BooleanSetting swapBack = new BooleanSetting("Swap Back", "Switch back to previous slot after lunging").setValue(true);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "Ticks to wait before switching back").setValue(4f).range(0, 20);
    BooleanSetting randomization = new BooleanSetting("Randomization", "Add random delay").setValue(false);
    MinMaxSetting randomDelay = new MinMaxSetting("Random Delay", "Random delay range").defaultValue(0, 0).range(0, 10).visible(() -> randomization.isValue());

    SliderSettings spearSlot = new SliderSettings("Spear Slot", "Hotbar slot used for the lunge spear").setValue(3f).range(1, 9);
    SliderSettings returnSlot = new SliderSettings("Return Slot", "Hotbar slot selected after lunging").setValue(1f).range(1, 9);
    BooleanSetting autoFindSpear = new BooleanSetting("Auto Find Spear", "Find a spear or trident automatically").setValue(true);
    int prevSlot, targetTicks, stage;
    boolean wasPressed;

    public LungeMacro() {
        super("Auto Lunge", "Timed spear-lunge movement macro; it does not target players", ModuleCategory.MOVEMENT);
        settings(activateKey, spearSlot, returnSlot, autoFindSpear, swapBack, switchDelay, randomization, randomDelay);
    }

    @Override
    public void activate() {
        prevSlot = -1;
        targetTicks = -1;
        stage = 0;
        wasPressed = false;
    }

    @Override
    public void deactivate() {
        prevSlot = -1;
        targetTicks = -1;
        stage = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        boolean pressed = isActivationPressed();
        if (stage == 0) {
            if (pressed && !wasPressed) {
                int slot = autoFindSpear.isValue() ? findLungeSpear() : spearSlot.getInt() - 1;
                if (slot != -1) {
                    prevSlot = mc.player.getInventory().getSelectedSlot();
                    selectSlot(slot);
                    mc.options.attackKey.setPressed(true);

                    int delay = switchDelay.getInt();
                    if (randomization.isValue()) {
                        delay += randomDelay.getRandomValueInt();
                    }
                    targetTicks = delay;
                    stage = 1;
                }
            }
        } else if (stage == 1) {
            if (targetTicks > 0) {
                targetTicks--;
                return;
            }

            mc.options.attackKey.setPressed(false);
            if (swapBack.isValue()) selectSlot(prevSlot);
            else selectSlot(returnSlot.getInt() - 1);

            switchState();
        }
        wasPressed = pressed;
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private boolean isActivationPressed() {
        int key = activateKey.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return true;
        if (key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    private int findLungeSpear() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isLungeSpear(stack)) return i;
        }
        return -1;
    }

    private boolean isLungeSpear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean isSpear = stack.isOf(Items.TRIDENT)
                || stack.getName().getString().toLowerCase().contains("spear")
                || stack.getItem().getTranslationKey().toLowerCase().contains("spear");
        if (!isSpear) return false;
        return hasLungeEnchantment(stack);
    }

    private boolean hasLungeEnchantment(ItemStack stack) {
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench == null || ench.isEmpty()) {
            ench = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
        }
        if (ench == null || ench.isEmpty()) return false;
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : ench.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            try {
                String id = enchantment.getIdAsString().toLowerCase();
                String name = enchantment.value().description().getString().toLowerCase();
                if (id.contains("lunge") || name.contains("lunge")) return true;
            } catch (Exception ignored) {
                String id = enchantment.getKey().map(k -> k.getValue().toString()).orElse("").toLowerCase();
                String name = enchantment.value().description().getString().toLowerCase();
                if (id.contains("lunge") || name.contains("lunge")) return true;
            }
        }
        return false;
    }
}
