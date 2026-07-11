package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.input.MouseInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.mixin.MouseAccessor;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoubleAnchorV3 extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that starts double anchoring").setKey(GLFW.GLFW_KEY_G);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "Base delay between sequence steps in ticks").setValue(1).range(0, 20);
    MinMaxSetting randomDelay = new MinMaxSetting("Random Delay", "Random extra delay added to each step in ticks").defaultValue(0, 0).range(0, 20);
    SliderSettings detonationSlot = new SliderSettings("Detonation Slot", "Non-anchor, non-glowstone hotbar slot used to explode each charged anchor").setValue(1).range(1, 9);
    SliderSettings finishSlot = new SliderSettings("Finish Slot", "Hotbar slot selected when the sequence finishes").setValue(1).range(1, 9);
    BooleanSetting restoreSlot = new BooleanSetting("Restore Original Slot", "Return to the slot held before the sequence instead of Finish Slot").setValue(false);
    BooleanSetting holdToRun = new BooleanSetting("Hold To Run", "Cancel the sequence when the activation key is released").setValue(true);
    BooleanSetting randomInteractions = new BooleanSetting("Random Charge Clicks", "Randomize the number of glowstone interactions per charge step").setValue(false);
    MinMaxSetting randomHits = new MinMaxSetting("Charge Clicks", "Glowstone interactions per charge step").defaultValue(1, 4).range(1, 4);

    int delayCounter;
    int sampledDelay = -1;
    int step;
    int originalSlot = -1;
    boolean anchoring;

    public DoubleAnchorV3() {
        super("Double Anchor V3", "Legit input macro using real targeted block faces; no AirPlace or direct packet sending", ModuleCategory.CPVP);
        settings(activateKey, switchDelay, randomDelay, detonationSlot, finishSlot, restoreSlot, holdToRun,
                randomInteractions, randomHits);
    }

    @Override
    public void activate() {
        resetAll(false);
    }

    @Override
    public void deactivate() {
        resetAll(true);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (!anchoring) {
            if (!activationPressed() || !hasItems()) return;
            anchoring = true;
            originalSlot = mc.player.getInventory().getSelectedSlot();
        } else if (holdToRun.isValue() && !activationPressed()) {
            resetAll(true);
            return;
        }

        BlockHitResult hit = targetedBlock();
        if (hit == null) {
            resetAll(true);
            return;
        }

        // If the player is already looking at an anchor, begin with its charge step.
        if (step == 0 && mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            step = 2;
            resetDelay();
            return;
        }

        if (!delayPassed()) return;

        boolean advance = switch (step) {
            case 0 -> select(Items.RESPAWN_ANCHOR);
            case 1 -> clickUse(1);
            case 2 -> select(Items.GLOWSTONE);
            case 3 -> clickUse(chargeClicks());
            case 4 -> selectDetonationSlot();
            case 5 -> clickUse(1);
            case 6 -> select(Items.RESPAWN_ANCHOR);
            case 7 -> clickUse(1);
            case 8 -> select(Items.GLOWSTONE);
            case 9 -> clickUse(chargeClicks());
            case 10 -> selectDetonationSlot();
            case 11 -> clickUse(1);
            case 12 -> selectSlot(restoreSlot.isValue() ? originalSlot : finishSlot.getInt() - 1);
            case 13 -> {
                resetAll(false);
                yield false;
            }
            default -> false;
        };

        if (advance) {
            step++;
            resetDelay();
        }
    }

    private BlockHitResult targetedBlock() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return hit;
    }

    private boolean hasItems() {
        int anchors = 0;
        int glowstone = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isOf(Items.RESPAWN_ANCHOR)) anchors += stack.getCount();
            if (stack.isOf(Items.GLOWSTONE)) glowstone += stack.getCount();
        }
        return anchors >= 2 && glowstone >= 2;
    }

    private boolean activationPressed() {
        int key = activateKey.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return key < 0
                ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key + 100) == GLFW.GLFW_PRESS
                : GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    private boolean delayPassed() {
        if (sampledDelay < 0) {
            sampledDelay = randomDelay.getIntMax() > randomDelay.getIntMin()
                    ? Math.max(0, randomDelay.getRandomValueInt())
                    : Math.max(0, randomDelay.getIntMin());
        }
        return delayCounter++ >= switchDelay.getInt() + sampledDelay;
    }

    private int chargeClicks() {
        return randomInteractions.isValue()
                ? Math.max(1, Math.min(4, randomHits.getRandomValueInt()))
                : 1;
    }

    private boolean clickUse(int count) {
        MouseInput input = new MouseInput(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 0);
        MouseAccessor mouse = (MouseAccessor) mc.mouse;
        for (int i = 0; i < count; i++) {
            mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_PRESS);
            mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_RELEASE);
        }
        return true;
    }

    private boolean select(Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(item)) return selectSlot(slot);
        }
        resetAll(true);
        return false;
    }

    private boolean selectDetonationSlot() {
        int slot = detonationSlot.getInt() - 1;
        if (slot < 0 || slot > 8) return false;
        ItemStack stack = mc.player.getInventory().getStack(slot);
        if (stack.isOf(Items.RESPAWN_ANCHOR) || stack.isOf(Items.GLOWSTONE)) {
            resetAll(true);
            return false;
        }
        return selectSlot(slot);
    }

    private boolean selectSlot(int slot) {
        if (slot < 0 || slot > 8) return false;
        // Vanilla observes this selection and performs its normal slot sync; this macro never sends a packet itself.
        mc.player.getInventory().setSelectedSlot(slot);
        return true;
    }

    private void resetDelay() {
        delayCounter = 0;
        sampledDelay = -1;
    }

    private void resetAll(boolean restoreOriginal) {
        if (restoreOriginal && restoreSlot.isValue() && mc.player != null && originalSlot >= 0) selectSlot(originalSlot);
        step = 0;
        anchoring = false;
        originalSlot = -1;
        resetDelay();
    }
}
