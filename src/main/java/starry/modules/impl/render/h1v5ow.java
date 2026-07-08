package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.MultiSelectSetting;
import starry.util.Instance;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class h1v5ow extends ModuleStructure {

    public static h1v5ow getInstance() {
        return Instance.get(h1v5ow.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Elements", "Select elements to ignore")
            .value(StringHelper.decrypt(new byte[]{12, (byte)-86, 45, (byte)-115}), StringHelper.decrypt(new byte[]{8, (byte)-94, 59, (byte)-56, 87, (byte)-3, 81, (byte)-77, 41, (byte)-73, 44}), StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-69, 120, (byte)-96, 47, (byte)-79, 51, (byte)-119, 107}), StringHelper.decrypt(new byte[]{14, (byte)-94, 45, (byte)-125, 124, (byte)-2, 68, (byte)-91}), StringHelper.decrypt(new byte[]{14, (byte)-94, 50, (byte)-119, 117, (byte)-2}), StringHelper.decrypt(new byte[]{4, (byte)-94, 42, (byte)-101, 119, (byte)-6}), StringHelper.decrypt(new byte[]{25, (byte)-96, 48, (byte)-102, 119, (byte)-7, 88, (byte)-73, 56, (byte)-89}), StringHelper.decrypt(new byte[]{8, (byte)-84, 44, (byte)-101, 80, (byte)-6, 69}))
            .selected("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Nausea");

    public h1v5ow() {
        super(StringHelper.decrypt(new byte[]{4, (byte)-84, 13, (byte)-115, 124, (byte)-1, 82, (byte)-92}), StringHelper.decrypt(new byte[]{4, (byte)-84, 127, (byte)-70, 119, (byte)-11, 83, (byte)-77, 56}), ModuleCategory.RENDER);
        settings(modeSetting);
    }
}