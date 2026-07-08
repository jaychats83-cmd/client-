package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SelectSetting;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StateToggle extends ModuleStructure {
    SelectSetting state = new SelectSetting("State", "").value("Hide Names", "Glow").selected("Hide Names");

    public StateToggle() {
        super("State Toggle", ModuleCategory.MISC);
        settings(state);
    }

    @Override
    public void activate() { apply(true); }
    @Override
    public void deactivate() { apply(false); }

    private void apply(boolean enabled) {
        if (state.isSelected("Hide Names")) {
            // setHideNames does not exist in this version
        } else if (state.isSelected("Glow")) {
            if (mc.player != null) mc.player.setGlowing(enabled);
        }
    }
}
