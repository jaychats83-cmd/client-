package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoubleAnchor extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that starts double anchoring").setKey(GLFW.GLFW_KEY_G);
    SliderSettings switchDelay = new SliderSettings("Switch Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings totemSlot = new SliderSettings("Totem Slot", "").setValue(1f).range(1f, 9f);
    MinMaxSetting randomDelay = new MinMaxSetting("Random Delay MS", "Additional random delay").defaultValue(0, 0).range(0, 5000);
    BooleanSetting randomInteractions = new BooleanSetting("Random Interactions", "").setValue(false);
    MinMaxSetting randomHits = new MinMaxSetting("Random Hits", "Interaction count per step").defaultValue(1, 4).range(1, 4);

    int currentDelay;
    int step;
    long lastStepTime;
    boolean anchoring;

    public DoubleAnchor() {
        super("Double Anchor", ModuleCategory.CPVP);
        settings(activateKey, switchDelay, totemSlot, randomDelay, randomInteractions, randomHits);
    }

    @Override
    public void activate() {
        resetAll();
    }

    @Override
    public void deactivate() {
        resetAll();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (!hasRequiredItems()) return;

        if (!anchoring && !checkActivationKey()) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            anchoring = false;
            resetDelay();
            return;
        }

        if (step == 0 && mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            step = 2;
            return;
        }

        int delay = Math.round(switchDelay.getValue()) + currentRandomDelay();
        if (!delayPassed(delay)) {
            return;
        }
        currentDelay = 0;

        switch (step) {
            case 0 -> selectItem(Items.RESPAWN_ANCHOR);
            case 1 -> placeBlock(hit);
            case 2 -> selectItem(Items.GLOWSTONE);
            case 3 -> interactRandom(hit);
            case 4 -> selectItem(Items.RESPAWN_ANCHOR);
            case 5 -> {
                placeBlock(hit);
                placeBlock(hit);
            }
            case 6 -> selectItem(Items.GLOWSTONE);
            case 7 -> interactRandom(hit);
            case 8 -> mc.player.getInventory().setSelectedSlot(totemSlot.getInt() - 1);
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
        lastStepTime = System.currentTimeMillis();
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
        if (activateKey.getKey() == GLFW.GLFW_KEY_UNKNOWN || !isKeyPressed(activateKey.getKey())) {
            resetAll();
            return false;
        }

        anchoring = true;
        return true;
    }

    private void resetAll() {
        step = 0;
        anchoring = false;
        resetDelay();
    }

    private void resetDelay() {
        currentDelay = 0;
        lastStepTime = System.currentTimeMillis();
    }

    public boolean isAnchoringActive() {
        return anchoring;
    }

    private int currentRandomDelay() {
        if (currentDelay == 0 && randomDelay.getIntMax() > randomDelay.getIntMin()) {
            currentDelay = Math.max(0, randomDelay.getRandomValueInt());
        }
        return currentDelay;
    }

    private void interactRandom(BlockHitResult hit) {
        int count = 1;
        if (randomInteractions.isValue()) {
            count = Math.max(1, Math.min(randomHits.getRandomValueInt(), 4));
        }

        for (int i = 0; i < count; i++) {
            placeBlock(hit);
        }
    }

    private void selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    private void placeBlock(BlockHitResult hit) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode < 0) return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode + 100) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private boolean delayPassed(int delay) {
        return System.currentTimeMillis() - lastStepTime >= delay;
    }
}
