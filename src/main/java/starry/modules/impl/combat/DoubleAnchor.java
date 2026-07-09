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
import starry.modules.module.setting.implement.SliderSettings;

import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoubleAnchor extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that starts double anchoring").setKey(GLFW.GLFW_KEY_G);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "").setValue(0f).range(0f, 20f);
    SliderSettings totemSlot = new SliderSettings("Totem Slot", "").setValue(1f).range(1f, 9f);
    SliderSettings randomDelayMin = new SliderSettings("Random Delay Min", "").setValue(0f).range(0f, 50f);
    SliderSettings randomDelayMax = new SliderSettings("Random Delay Max", "").setValue(0f).range(0f, 100f);
    BooleanSetting randomInteractions = new BooleanSetting("Random Interactions", "").setValue(false);
    SliderSettings randomHitsMin = new SliderSettings("Random Hits Min", "").setValue(1f).range(1f, 4f);
    SliderSettings randomHitsMax = new SliderSettings("Random Hits Max", "").setValue(4f).range(1f, 4f);

    int delayCounter;
    int randomDelay;
    int step;
    boolean anchoring;

    public DoubleAnchor() {
        super("Double Anchor", ModuleCategory.CPVP);
        settings(activateKey, switchDelay, totemSlot, randomDelayMin, randomDelayMax, randomInteractions, randomHitsMin, randomHitsMax);
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

        int delay = fastDelay(switchDelay) + currentRandomDelay();
        if (delayCounter < delay) {
            delayCounter++;
            return;
        }
        randomDelay = 0;

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
        delayCounter = 0;
        randomDelay = 0;
    }

    public boolean isAnchoringActive() {
        return anchoring;
    }

    private int currentRandomDelay() {
        if (randomDelay == 0 && randomDelayMax.getValue() > randomDelayMin.getValue()) {
            randomDelay = Math.max(0, Math.round(randomInt(randomDelayMin.getInt(), randomDelayMax.getInt()) * 0.5F));
        }
        return randomDelay;
    }

    private void interactRandom(BlockHitResult hit) {
        int count = 1;
        if (randomInteractions.isValue()) {
            int min = Math.max(1, Math.min(randomHitsMin.getInt(), 4));
            int max = Math.max(min, Math.min(randomHitsMax.getInt(), 4));
            count = randomInt(min, max);
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

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode < 0) return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode + 100) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private int fastDelay(SliderSettings setting) {
        return Math.max(0, Math.round(setting.getValue() * 0.5F));
    }
}
