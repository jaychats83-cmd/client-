package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeyPearl extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that throws the pearl").setKey(GLFW.GLFW_KEY_R);
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0f, 20f);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "").setValue(true);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "").setValue(0f).range(0f, 20f);

    private int clock, previousSlot, switchClock;
    private boolean active, hasActivated;

    public KeyPearl() {
        super("Key Pearl", ModuleCategory.MISC);
        settings(activateKey, delay, switchBack, switchDelay);
    }

    @Override
    public void activate() { reset(); }

    private void reset() { previousSlot = -1; clock = 0; switchClock = 0; active = false; hasActivated = false; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null) return;

        if (isKeyPressed(activateKey.getKey())) active = true;

        if (!active) return;

        if (previousSlot == -1) previousSlot = mc.player.getInventory().getSelectedSlot();
        if (!selectItem(Items.ENDER_PEARL)) return;

        if (clock < delay.getInt()) { clock++; return; }

        if (!hasActivated) {
            ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
            hasActivated = true;
        }

        if (switchBack.isValue()) {
            if (switchClock < switchDelay.getInt()) { switchClock++; return; }
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        reset();
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) { mc.player.getInventory().setSelectedSlot(i); return true; }
        return false;
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode <= 8) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }
}