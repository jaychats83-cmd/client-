package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SelectSetting;
import starry.util.Instance;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class zos00u extends ModuleStructure {
    public static zos00u getInstance() {
        return Instance.get(zos00u.class);
    }

    public SelectSetting mode = new SelectSetting(StringHelper.decrypt(new byte[]{26, (byte)-85, 38, (byte)-101, 123, (byte)-8, 68}), StringHelper.decrypt(new byte[]{})).value(StringHelper.decrypt(new byte[]{4, (byte)-84, 45, (byte)-123, 115, (byte)-9})).selected(StringHelper.decrypt(new byte[]{4, (byte)-84, 45, (byte)-123, 115, (byte)-9}));

    public zos00u() {
        super(StringHelper.decrypt(new byte[]{3, (byte)-73, 58, (byte)-123, 66, (byte)-13, 78, (byte)-91, 35, (byte)-96}), StringHelper.decrypt(new byte[]{(byte)-102, 69, 43, (byte)-115, 127, (byte)-69, 103, (byte)-66, 51, (byte)-80, 54, (byte)-117}), ModuleCategory.VISUALS);
//        setup(mode);
    }

    @EventHandler
    public void onTick(TickEvent e) {
    }
}
