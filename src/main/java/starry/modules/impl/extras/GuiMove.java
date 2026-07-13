package starry.modules.impl.extras;

import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;

public class GuiMove extends ModuleStructure {
    private final BooleanSetting jump = new BooleanSetting("Jump", "Allow jumping in GUIs").setValue(true);
    private final BooleanSetting sneak = new BooleanSetting("Sneak", "Allow sneaking in GUIs").setValue(false);
    private final BooleanSetting sprint = new BooleanSetting("Sprint", "Allow sprinting in GUIs").setValue(true);

    public GuiMove() {
        super("GUI Move", "Keeps movement controls active while inventory-style screens are open", ModuleCategory.EXTRAS);
        settings(jump, sneak, sprint);
    }

    @Override
    public void deactivate() {
        if (mc.currentScreen == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) return;
        mc.options.forwardKey.setPressed(down(GLFW.GLFW_KEY_W));
        mc.options.backKey.setPressed(down(GLFW.GLFW_KEY_S));
        mc.options.leftKey.setPressed(down(GLFW.GLFW_KEY_A));
        mc.options.rightKey.setPressed(down(GLFW.GLFW_KEY_D));
        if (jump.isValue()) mc.options.jumpKey.setPressed(down(GLFW.GLFW_KEY_SPACE));
        if (sneak.isValue()) mc.options.sneakKey.setPressed(down(GLFW.GLFW_KEY_LEFT_SHIFT));
        if (sprint.isValue()) mc.options.sprintKey.setPressed(down(GLFW.GLFW_KEY_LEFT_CONTROL));
    }

    private boolean down(int key) { return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS; }
}
