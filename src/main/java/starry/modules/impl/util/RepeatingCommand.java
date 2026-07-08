package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import starry.modules.module.setting.implement.TextSetting;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepeatingCommand extends ModuleStructure {
    TextSetting command = new TextSetting("Command", "");
    SliderSettings delaySeconds = new SliderSettings("Delay Seconds", "").setValue(30f).range(1f, 600f);

    private long lastAction;

    public RepeatingCommand() {
        super("Repeating Command", ModuleCategory.MISC);
        settings(command, delaySeconds);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (System.currentTimeMillis() - lastAction < delaySeconds.getValue() * 1000) return;

        String value = command.getText();
        if (value != null && !value.isBlank()) {
            mc.getNetworkHandler().sendChatCommand(value.startsWith("/") ? value.substring(1) : value);
        }
        lastAction = System.currentTimeMillis();
    }
}
