package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoJumpDelay extends ModuleStructure {
    public NoJumpDelay() {
        super("No Jump Delay", ModuleCategory.MISC);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null) return;
        if (!mc.player.isOnGround()) return;
        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) != GLFW.GLFW_PRESS) return;
        mc.options.jumpKey.setPressed(false);
        mc.player.jump();
    }
}
