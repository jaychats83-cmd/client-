package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.TextSetting;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommandModule extends ModuleStructure {
    TextSetting command = new TextSetting("Command", "");
    BooleanSetting disableAfterSend = new BooleanSetting("Disable After Send", "").setValue(true);

    public CommandModule() {
        super("Command", ModuleCategory.MISC);
        settings(command, disableAfterSend);
    }

    @Override
    public void activate() {
        if (mc.player != null && mc.getNetworkHandler() != null && command.getText() != null && !command.getText().isBlank()) {
            String value = command.getText().startsWith("/") ? command.getText().substring(1) : command.getText();
            mc.getNetworkHandler().sendChatCommand(value);
        }
        if (disableAfterSend.isValue()) setState(false);
    }
}
