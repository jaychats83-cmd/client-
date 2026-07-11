package starry.modules.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.input.MouseInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
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
public class LungeMacro extends ModuleStructure {
    static final int IDLE = 0;
    static final int WAIT_JUMP = 1;
    static final int WAIT_SPEAR = 2;
    static final int WAIT_APEX = 3;
    static final int WAIT_ATTACK = 4;
    static final int WAIT_FINISH_SLOT = 5;
    static final int RESTING = 6;

    BindSetting activateKey = new BindSetting("Activation Key", "Hold or press to run the lunge movement macro")
            .setKey(GLFW.GLFW_MOUSE_BUTTON_5).setType(0);
    BooleanSetting repeatWhileHeld = new BooleanSetting("Repeat While Held", "Start another cycle after resting while the activation key remains held").setValue(true);
    BooleanSetting cancelOnRelease = new BooleanSetting("Cancel On Release", "Stop the active cycle when the activation key is released").setValue(false);
    BooleanSetting requireLunge = new BooleanSetting("Require Lunge", "Only select a spear or trident carrying a Lunge enchantment").setValue(true);
    SliderSettings finishSlot = new SliderSettings("Finish Slot", "Hotbar slot selected after the apex click").setValue(1).range(1, 9);

    SliderSettings jumpDelay = new SliderSettings("Jump Delay", "Ticks from activation to the jump input").setValue(0).range(0, 20);
    SliderSettings spearDelay = new SliderSettings("Spear Switch Delay", "Ticks after jumping before selecting the spear").setValue(0).range(0, 20);
    SliderSettings apexAttackDelay = new SliderSettings("Apex Click Delay", "Extra ticks to wait after detecting the jump apex").setValue(0).range(0, 10);
    SliderSettings finishDelay = new SliderSettings("Finish Slot Delay", "Ticks after the left click before selecting Finish Slot").setValue(1).range(0, 20);
    SliderSettings restDelay = new SliderSettings("Rest Delay", "Ticks after finishing before another cycle may start").setValue(10).range(0, 100);
    SliderSettings apexTimeout = new SliderSettings("Apex Timeout", "Maximum ticks to wait for a real jump apex before cancelling").setValue(20).range(5, 60);
    SliderSettings apexThreshold = new SliderSettings("Apex Threshold", "Vertical speed at or below which the top of the jump is detected").setValue(0.03f).range(-0.1f, 0.15f);

    BooleanSetting randomization = new BooleanSetting("Random Delays", "Add a newly sampled random delay to each timed stage").setValue(false);
    MinMaxSetting randomDelay = new MinMaxSetting("Random Extra Delay", "Extra delay range in ticks").defaultValue(0, 0).range(0, 10)
            .visible(() -> randomization.isValue());

    int stage = IDLE;
    int ticks;
    int stageDelay;
    int apexTicks;
    boolean wasPressed;
    boolean jumpPressed;
    boolean leftGround;

    public LungeMacro() {
        super("Auto Lunge", "Repeatable legit movement macro: jump, find spear, apex left-click, finish slot, rest; no direct packet sending", ModuleCategory.MOVEMENT);
        settings(activateKey, repeatWhileHeld, cancelOnRelease, requireLunge, finishSlot,
                jumpDelay, spearDelay, apexAttackDelay, finishDelay, restDelay, apexTimeout, apexThreshold,
                randomization, randomDelay);
    }

    @Override
    public void activate() {
        resetMacro();
        wasPressed = false;
    }

    @Override
    public void deactivate() {
        releaseJump();
        resetMacro();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        releaseJump();

        boolean pressed = isActivationPressed();
        if (mc.currentScreen != null) {
            resetMacro();
            wasPressed = pressed;
            return;
        }

        if (stage != IDLE && stage != RESTING && cancelOnRelease.isValue() && !pressed) {
            resetMacro();
        }

        switch (stage) {
            case IDLE -> {
                if (pressed && !wasPressed) enterStage(WAIT_JUMP, jumpDelay.getInt());
            }
            case WAIT_JUMP -> {
                if (!delayComplete()) break;
                // A repeat waits for landing instead of throwing away the next cycle.
                if (!mc.player.isOnGround()) break;
                pulseJump();
                leftGround = false;
                enterStage(WAIT_SPEAR, spearDelay.getInt());
            }
            case WAIT_SPEAR -> {
                if (!delayComplete()) break;
                int spear = findLungeSpear();
                if (spear < 0) {
                    resetMacro();
                    break;
                }
                selectSlot(spear);
                stage = WAIT_APEX;
                apexTicks = 0;
            }
            case WAIT_APEX -> {
                apexTicks++;
                if (!mc.player.isOnGround()) leftGround = true;
                if (leftGround && mc.player.getVelocity().y <= apexThreshold.getValue()) {
                    enterStage(WAIT_ATTACK, apexAttackDelay.getInt());
                } else if (apexTicks >= apexTimeout.getInt()) {
                    resetMacro();
                }
            }
            case WAIT_ATTACK -> {
                if (!delayComplete()) break;
                simulateAttackClick();
                enterStage(WAIT_FINISH_SLOT, finishDelay.getInt());
            }
            case WAIT_FINISH_SLOT -> {
                if (!delayComplete()) break;
                selectSlot(finishSlot.getInt() - 1);
                enterStage(RESTING, restDelay.getInt());
            }
            case RESTING -> {
                if (!delayComplete()) break;
                if (repeatWhileHeld.isValue() && pressed) enterStage(WAIT_JUMP, jumpDelay.getInt());
                else stage = IDLE;
            }
            default -> resetMacro();
        }

        wasPressed = pressed;
    }

    private void enterStage(int nextStage, int configuredDelay) {
        stage = nextStage;
        ticks = 0;
        stageDelay = Math.max(0, configuredDelay + sampleRandomDelay());
    }

    private boolean delayComplete() {
        return ticks++ >= stageDelay;
    }

    private int sampleRandomDelay() {
        if (!randomization.isValue()) return 0;
        return Math.max(0, randomDelay.getRandomValueInt());
    }

    private void pulseJump() {
        mc.options.jumpKey.setPressed(true);
        jumpPressed = true;
    }

    private void releaseJump() {
        if (!jumpPressed) return;
        mc.options.jumpKey.setPressed(false);
        jumpPressed = false;
    }

    private void resetMacro() {
        releaseJump();
        stage = IDLE;
        ticks = 0;
        stageDelay = 0;
        apexTicks = 0;
        leftGround = false;
    }

    private void selectSlot(int slot) {
        if (slot >= 0 && slot < 9) mc.player.getInventory().setSelectedSlot(slot);
    }

    private boolean isActivationPressed() {
        int key = activateKey.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return activateKey.getType() == 0
                ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS
                : GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    private void simulateAttackClick() {
        MouseInput input = new MouseInput(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0);
        MouseAccessor mouse = (MouseAccessor) mc.mouse;
        mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_PRESS);
        mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_RELEASE);
    }

    private int findLungeSpear() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (isSpear(stack) && (!requireLunge.isValue() || hasLungeEnchantment(stack))) return slot;
        }
        return -1;
    }

    private boolean isSpear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase();
        String translationKey = stack.getItem().getTranslationKey().toLowerCase();
        return stack.isOf(Items.TRIDENT) || name.contains("spear") || translationKey.contains("spear");
    }

    private boolean hasLungeEnchantment(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) enchantments = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            String id = enchantment.getKey().map(key -> key.getValue().toString()).orElse("").toLowerCase();
            String name = enchantment.value().description().getString().toLowerCase();
            if (id.contains("lunge") || name.contains("lunge")) return true;
        }
        return false;
    }
}
