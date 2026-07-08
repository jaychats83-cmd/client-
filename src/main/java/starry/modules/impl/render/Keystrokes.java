package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Keystrokes extends ModuleStructure {
    SliderSettings x = new SliderSettings("X", "").setValue(8f).range(0f, 1000f);
    SliderSettings y = new SliderSettings("Y", "").setValue(92f).range(0f, 1000f);
    BooleanSetting mouseButtons = new BooleanSetting("Mouse Buttons", "").setValue(true);

    public Keystrokes() {
        super("Keystrokes", ModuleCategory.RENDER);
        settings(x, y, mouseButtons);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null) return;
        var ctx = event.getDrawContext();
        int left = x.getInt();
        int top = y.getInt();
        renderKey(ctx, "W", GLFW.GLFW_KEY_W, left + 34, top, 30, 28);
        renderKey(ctx, "A", GLFW.GLFW_KEY_A, left, top + 32, 30, 28);
        renderKey(ctx, "S", GLFW.GLFW_KEY_S, left + 34, top + 32, 30, 28);
        renderKey(ctx, "D", GLFW.GLFW_KEY_D, left + 68, top + 32, 30, 28);
        renderKey(ctx, "SPACE", GLFW.GLFW_KEY_SPACE, left, top + 64, 98, 24);
        if (mouseButtons.isValue()) {
            renderKey(ctx, "LMB", GLFW.GLFW_MOUSE_BUTTON_LEFT, left, top + 92, 47, 24);
            renderKey(ctx, "RMB", GLFW.GLFW_MOUSE_BUTTON_RIGHT, left + 51, top + 92, 47, 24);
        }
    }

    private void renderKey(net.minecraft.client.gui.DrawContext ctx, String label, int key, int rx, int ry, int rw, int rh) {
        boolean pressed = isKeyPressed(key);
        Color bg = pressed ? new Color(77, 166, 255, 122) : new Color(7, 11, 17, 172);
        ctx.fill(rx, ry, rx + rw, ry + rh, bg.getRGB());
        ctx.drawText(mc.textRenderer, label, rx + rw / 2 - mc.textRenderer.getWidth(label) / 2, ry + rh / 2 - 4, Color.WHITE.getRGB(), true);
    }

    private boolean isKeyPressed(int key) {
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    private boolean isMousePressed(int btn) {
        return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), btn) == GLFW.GLFW_PRESS;
    }
}
