package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.ModuleStructure;
import starry.modules.module.setting.implement.*;
import starry.util.Instance;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class k9rp40 extends ModuleStructure {
    public static k9rp40 getInstance() {
        return Instance.get(k9rp40.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Elements", "Інterface elements settings")
            .value("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "Info",
                    "Notifications",
                    "ArrayList")

            .selected("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "Info",
                    "Notifications");

    public BooleanSetting showBps = new BooleanSetting(StringHelper.decrypt(new byte[]{25, (byte)-85, 48, (byte)-97, 50, (byte)-39, 103, (byte)-123}), StringHelper.decrypt(new byte[]{25, (byte)-85, 48, (byte)-97, 50, (byte)-7, 91, (byte)-71, 41, (byte)-88, 44, (byte)-56, 98, (byte)-2, 69, (byte)-10, 57, (byte)-90, 60, (byte)-121, 124, (byte)-1}))
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Info"));

    public BooleanSetting showTps = new BooleanSetting(StringHelper.decrypt(new byte[]{25, (byte)-85, 48, (byte)-97, 50, (byte)-49, 103, (byte)-123}), StringHelper.decrypt(new byte[]{25, (byte)-85, 48, (byte)-97, 50, (byte)-49, 103, (byte)-123, 106, (byte)-86, 49, (byte)-56, 69, (byte)-6, 67, (byte)-77, 56, (byte)-82, 62, (byte)-102, 121}))
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Watermark"));

    public SelectSetting arraylistOrder = new SelectSetting(StringHelper.decrypt(new byte[]{5, (byte)-79, 59, (byte)-115, 96}), StringHelper.decrypt(new byte[]{25, (byte)-84, 45, (byte)-100, 50, (byte)-12, 69, (byte)-78, 47, (byte)-79, 127, (byte)-114, 125, (byte)-23, 23, (byte)-105, 56, (byte)-79, 62, (byte)-111, 94, (byte)-14, 68, (byte)-94}))
            .value(StringHelper.decrypt(new byte[]{11, (byte)-81, 47, (byte)-128, 115, (byte)-7, 82, (byte)-94, 35, (byte)-96, 62, (byte)-124}), StringHelper.decrypt(new byte[]{6, (byte)-90, 49, (byte)-113, 102, (byte)-13}))
            .selected(StringHelper.decrypt(new byte[]{11, (byte)-81, 47, (byte)-128, 115, (byte)-7, 82, (byte)-94, 35, (byte)-96, 62, (byte)-124}))
            .visible(() -> interfaceSettings.isSelected("ArrayList"));

    public BooleanSetting arraylistReverse = new BooleanSetting(StringHelper.decrypt(new byte[]{24, (byte)-90, 41, (byte)-115, 96, (byte)-24, 82}), StringHelper.decrypt(new byte[]{24, (byte)-90, 41, (byte)-115, 96, (byte)-24, 82, (byte)-91, 106, (byte)-73, 55, (byte)-115, 50, (byte)-38, 69, (byte)-92, 43, (byte)-70, 19, (byte)-127, 97, (byte)-17, 23, (byte)-71, 56, (byte)-89, 58, (byte)-102}))
            .setValue(false)
            .visible(() -> interfaceSettings.isSelected("ArrayList"));

    public k9rp40() {
        super("Hud", ModuleCategory.RENDER);
        settings(interfaceSettings, showBps, showTps, arraylistOrder, arraylistReverse);
    }
}