package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.TextSetting;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UICommand extends ModuleStructure {
    BindSetting commandKey = new BindSetting("Command Key", "Press to send the configured command from any UI")
            .setKey(GLFW.GLFW_KEY_UNKNOWN);
    TextSetting command = new TextSetting("Command", "Command to send, with or without the leading slash").setText("/home");
    BooleanSetting onlyInUi = new BooleanSetting("Only In UI", "Only trigger while a screen or container is open").setValue(false);

    boolean wasPressed;

    public UICommand() {
        super("UI Command", "Sends a configured command by keybind even while a container or other UI is open", ModuleCategory.MISC);
        settings(commandKey, command, onlyInUi);
    }

    @Override
    public void activate() {
        wasPressed = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        boolean pressed = isCommandKeyPressed();
        if (pressed && !wasPressed && (!onlyInUi.isValue() || mc.currentScreen != null)) sendCommand();
        wasPressed = pressed;
    }

    private void sendCommand() {
        String value = command.getText();
        if (value == null || value.isBlank()) return;
        value = value.trim();
        mc.getNetworkHandler().sendChatCommand(value.startsWith("/") ? value.substring(1) : value);
    }

    private boolean isCommandKeyPressed() {
        int key = commandKey.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return commandKey.getType() == 0
                ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS
                : GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}
