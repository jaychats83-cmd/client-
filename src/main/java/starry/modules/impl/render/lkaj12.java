package starry.modules.impl.render;

import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.string.StringHelper;

public class lkaj12 extends ModuleStructure {
    private static lkaj12 instance;

    private final SliderSettings speed = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-77, 58, (byte)-115, 118}), StringHelper.decrypt(new byte[]{})).range(1, 20).setValue(10);

    public lkaj12() {
        super(StringHelper.decrypt(new byte[]{9, 19, (byte)-30, (byte)-99, 124, (byte)-16, 23, (byte)-105, 36, (byte)-86, 50, (byte)-119, 102, (byte)-12, 69}), StringHelper.decrypt(new byte[]{11, 19, (byte)-30, (byte)-127, 127, (byte)-6, 67, (byte)-77, 57, (byte)-29, 62, (byte)-104, 98, (byte)-2, 86, (byte)-92, 35, (byte)-83, 56, (byte)-56, 113, (byte)-13, 66, (byte)-72, 33, (byte)-80}), ModuleCategory.VISUALS);
        instance = this;
        settings(speed);
    }

    public static lkaj12 getInstance() {
        return instance;
    }

    public float getSpeed() {
        return speed.getValue();
    }
}
