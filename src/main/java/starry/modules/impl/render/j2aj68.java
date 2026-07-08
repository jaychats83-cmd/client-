package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.EntityColorEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class j2aj68 extends ModuleStructure {

    SliderSettings alphaSetting = new SliderSettings(StringHelper.decrypt(new byte[]{30, (byte)-79, 62, (byte)-122, 97, (byte)-21, 86, (byte)-92, 47, (byte)-83, 60, (byte)-111}), StringHelper.decrypt(new byte[]{26, (byte)-81, 62, (byte)-111, 119, (byte)-23, 23, (byte)-94, 56, (byte)-94, 49, (byte)-101, 98, (byte)-6, 69, (byte)-77, 36, (byte)-96, 38})).setValue(0.5f).range(0.1F, 1);

    public j2aj68() {
        super(StringHelper.decrypt(new byte[]{(byte)-102, 70, 58, (byte)-115, 91, (byte)-11, 65, (byte)-65, 57, (byte)-86, 61, (byte)-124, 119}), StringHelper.decrypt(new byte[]{25, (byte)-90, 58, (byte)-56, 91, (byte)-11, 65, (byte)-65, 57, (byte)-86, 61, (byte)-124, 119}), ModuleCategory.ESP);
        settings(alphaSetting);
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        e.setColor(ColorUtil.multAlpha(e.getColor(), alphaSetting.getValue()));
        e.cancel();
    }

}
