package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.modules.module.setting.implement.SliderSettings;

/** Port of the Double Anchor registered in Argon's Enhanced Modules category. */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoubleAnchorV3 extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that starts double anchoring").setKey(GLFW.GLFW_KEY_G);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "Delay between donor sequence steps in ticks").setValue(0).range(0, 20);
    SliderSettings totemSlot = new SliderSettings("Totem Slot", "Hotbar slot used for the final anchor interaction").setValue(1).range(1, 9);
    MinMaxSetting randomDelay = new MinMaxSetting("Random Delay", "Random extra donor delay in ticks").defaultValue(0, 0).range(0, 100);
    BooleanSetting randomInteractions = new BooleanSetting("Random Interactions", "Randomize glowstone interaction count").setValue(false);
    MinMaxSetting randomHits = new MinMaxSetting("Random Hits", "Glowstone interactions per charge step").defaultValue(1, 4).range(1, 4);

    int delayCounter;
    int sampledRandomDelay;
    int step;
    boolean anchoring;

    public DoubleAnchorV3() {
        super("Double Anchor V3", "Argon donor double-anchor sequence; does not use AirPlace", ModuleCategory.CPVP);
        settings(activateKey, switchDelay, totemSlot, randomDelay, randomInteractions, randomHits);
    }

    @Override
    public void activate() {
        anchoring = false;
        step = 0;
        resetDelay();
    }

    @Override
    public void deactivate() {
        anchoring = false;
        step = 0;
        resetDelay();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (!hasRequiredItems()) return;
        if (!anchoring && !checkActivationKey()) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            anchoring = false;
            step = 0;
            resetDelay();
            return;
        }

        if (step == 0 && mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            step = 2;
            return;
        }

        int delay = switchDelay.getInt() + currentRandomDelay();
        if (delayCounter < delay) {
            delayCounter++;
            return;
        }
        sampledRandomDelay = 0;

        switch (step) {
            case 0 -> selectItemFromHotbar(Items.RESPAWN_ANCHOR);
            case 1 -> placeBlock(hit);
            case 2 -> selectItemFromHotbar(Items.GLOWSTONE);
            case 3 -> interactRandom(hit);
            case 4 -> selectItemFromHotbar(Items.RESPAWN_ANCHOR);
            case 5 -> {
                placeBlock(hit);
                placeBlock(hit);
            }
            case 6 -> selectItemFromHotbar(Items.GLOWSTONE);
            case 7 -> interactRandom(hit);
            case 8 -> setInventorySlot(totemSlot.getInt() - 1);
            case 9 -> placeBlock(hit);
            case 10 -> {
                anchoring = false;
                step = 0;
                resetDelay();
                return;
            }
            default -> {
            }
        }

        step++;
        delayCounter = 0;
    }

    private boolean hasRequiredItems() {
        boolean hasAnchor = false;
        boolean hasGlowstone = false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isOf(Items.RESPAWN_ANCHOR)) hasAnchor = true;
            if (stack.isOf(Items.GLOWSTONE)) hasGlowstone = true;
        }
        return hasAnchor && hasGlowstone;
    }

    private boolean checkActivationKey() {
        int key = activateKey.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN || !isKeyPressed(key)) {
            resetDelay();
            return false;
        }
        anchoring = true;
        return true;
    }

    private boolean isKeyPressed(int key) {
        if (key < 0) return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key + 100) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    private int currentRandomDelay() {
        if (sampledRandomDelay == 0 && randomDelay.getIntMax() > randomDelay.getIntMin()) {
            sampledRandomDelay = randomDelay.getRandomValueInt();
        }
        return sampledRandomDelay;
    }

    private void interactRandom(BlockHitResult hit) {
        int count = randomInteractions.isValue()
                ? Math.max(1, Math.min(4, randomHits.getRandomValueInt()))
                : 1;
        for (int i = 0; i < count; i++) placeBlock(hit);
    }

    private void placeBlock(BlockHitResult hit) {
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean selectItemFromHotbar(Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(item)) {
                setInventorySlot(slot);
                return true;
            }
        }
        return false;
    }

    private void setInventorySlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void resetDelay() {
        delayCounter = 0;
        sampledRandomDelay = 0;
    }
}
