package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.Instance;

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fog extends ModuleStructure {
    SelectSetting style = new SelectSetting("Style", "Fog distance preset")
            .value("qcloud", "Soft", "Dense", "Wall", "Sky")
            .selected("qcloud");
    SelectSetting colorMode = new SelectSetting("Color Mode", "Fog RGB style")
            .value("Custom", "Rainbow", "Dark", "Light")
            .selected("Custom");
    ColorSetting color = new ColorSetting("Color", "Fog RGB")
            .value(ColorUtil.getColor(120, 190, 255, 255))
            .visible(() -> colorMode.isSelected("Custom"));
    SliderSettings distance = new SliderSettings("Distance", "qcloud fog distance")
            .setValue(80f)
            .range(10f, 255f);
    SliderSettings start = new SliderSettings("Start", "Manual fog start")
            .setValue(8f)
            .range(0f, 256f)
            .visible(() -> style.isSelected("Sky"));
    SliderSettings end = new SliderSettings("End", "Manual fog end")
            .setValue(72f)
            .range(1f, 512f)
            .visible(() -> style.isSelected("Sky"));
    SliderSettings skyEnd = new SliderSettings("Sky End", "Sky fog distance")
            .setValue(96f)
            .range(1f, 512f);
    SliderSettings cloudEnd = new SliderSettings("Cloud End", "Cloud fog distance")
            .setValue(96f)
            .range(1f, 512f);
    SliderSettings alpha = new SliderSettings("Alpha", "Fog color alpha")
            .setValue(1f)
            .range(0.1f, 1f);
    BooleanSetting animated = new BooleanSetting("Animated RGB", "Slowly rotate rainbow fog").setValue(false);

    public Fog() {
        super("Fog", "qcloud custom fog with full RGB/style controls", ModuleCategory.VISUALS);
        settings(style, colorMode, color, distance, start, end, skyEnd, cloudEnd, alpha, animated);
    }

    public static Fog getInstance() {
        return Instance.get(Fog.class);
    }

    public int getColor() {
        int base;
        if (animated.isValue() || colorMode.isSelected("Rainbow")) {
            base = ColorUtil.rainbow(18, 0, 0.72f, 1f, alpha.getValue());
        } else if (colorMode.isSelected("Dark")) {
            base = new Color(28, 32, 42, Math.round(alpha.getValue() * 255)).getRGB();
        } else if (colorMode.isSelected("Light")) {
            base = new Color(205, 225, 255, Math.round(alpha.getValue() * 255)).getRGB();
        } else {
            base = color.getColor();
            base = ColorUtil.replAlpha(base, Math.round(alpha.getValue() * 255));
        }
        return base;
    }

    public float getStart() {
        return switch (style.getSelected()) {
            case "Soft" -> Math.max(0f, distance.getValue() * 0.55f);
            case "Dense" -> Math.max(0f, distance.getValue() * 0.18f);
            case "Wall" -> 0f;
            case "Sky" -> Math.min(start.getValue(), getEnd() - 0.1F);
            default -> 0f;
        };
    }

    public float getEnd() {
        return switch (style.getSelected()) {
            case "Soft" -> Math.max(1f, distance.getValue() * 1.45f);
            case "Dense" -> Math.max(1f, distance.getValue() * 0.72f);
            case "Wall" -> Math.max(1f, distance.getValue() * 0.32f);
            case "Sky" -> Math.max(end.getValue(), start.getValue() + 0.1F);
            default -> distance.getValue();
        };
    }

    public float getSkyEnd() {
        return style.isSelected("Sky") ? skyEnd.getValue() : Math.max(getEnd(), distance.getValue());
    }

    public float getCloudEnd() {
        return style.isSelected("Sky") ? cloudEnd.getValue() : Math.max(getEnd(), distance.getValue());
    }
}