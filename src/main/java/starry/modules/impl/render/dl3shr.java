package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.ColorSetting;
import starry.util.ColorUtil;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class dl3shr extends ModuleStructure {

    private static dl3shr instance;

    public static dl3shr getInstance() {
        return instance;
    }

    public final ColorSetting color1 = new ColorSetting("Color 1", "First gradient color")
            .value(ColorUtil.getColor(255, 50, 100, 255));

    public final ColorSetting color2 = new ColorSetting("Color 2", "Second gradient color")
            .value(ColorUtil.getColor(100, 50, 255, 255));

    public dl3shr() {
        super(StringHelper.decrypt(new byte[]{9, (byte)-85, 54, (byte)-122, 115, (byte)-45, 86, (byte)-94}), StringHelper.decrypt(new byte[]{9, (byte)-85, 54, (byte)-122, 115, (byte)-69, 127, (byte)-73, 62}), ModuleCategory.VISUALS);
        instance = this;
        settings(color1, color2);
    }
}
