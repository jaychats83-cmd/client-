package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityType;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class x7m4j9 extends ModuleStructure {

    public static x7m4j9 getInstance() {
        return Instance.get(x7m4j9.class);
    }

    public BooleanSetting players = new BooleanSetting(
            StringHelper.decrypt(new byte[]{26, (byte)0xAF, 62, (byte)0x91, 119, (byte)0xE9, 68}),
            StringHelper.decrypt(new byte[]{15, (byte)0xB0, 55, (byte)0x8C, 123, (byte)0xE2, 82, (byte)0xBE, 106, (byte)0xB5, 62, (byte)0x80, 101, (byte)0xE9, 88, (byte)0xAF, 40, (byte)0xAC, 54, (byte)0xBF, 113, (byte)0x8E, 95}))
            .setValue(true);

    public BooleanSetting mobs = new BooleanSetting(
            StringHelper.decrypt(new byte[]{7, (byte)0xAC, 61, (byte)0x9B}),
            StringHelper.decrypt(new byte[]{15, (byte)0xB0, 55, (byte)0x8C, 123, (byte)0xE2, 82, (byte)0xBE, 106, (byte)0xB5, 62, (byte)0x80, 101, (byte)0xE9, 88, (byte)0xAF, 40, (byte)0xAC, 54, (byte)0xBF, 113, (byte)0x8E, 95}))
            .setValue(true);

    public BooleanSetting throughWalls = new BooleanSetting(
            StringHelper.decrypt(new byte[]{30, (byte)0xAB, 45, (byte)0x87, 103, (byte)0xFC, 95, (byte)0xF6, 29, (byte)0xA2, 51, (byte)0x84, 97}),
            StringHelper.decrypt(new byte[]{30, (byte)0xAB, 45, (byte)0x87, 103, (byte)0xFC, 95, (byte)0xF6, 29, (byte)0xA2, 51, (byte)0x84, 97, 50, (byte)0xEB, 81, (byte)0xBE, 106, (byte)0xB5, 62, (byte)0x80, 101, (byte)0xE9, 88, (byte)0xAF, 40, (byte)0xAC, 54, (byte)0xBF, 113, (byte)0xE8, 94}))
            .setValue(true);

    public SelectSetting mode = new SelectSetting(
            StringHelper.decrypt(new byte[]{7, (byte)0xB0, 55, (byte)0x8F, 107}),
            StringHelper.decrypt(new byte[]{19, (byte)0xB2, 121, (byte)0xF1, 107, (byte)0xEB, 81, (byte)0xBE, 106, (byte)0xB5, 62, (byte)0x80, 101, (byte)0xE9, 88, (byte)0xAF, 40, (byte)0xAC, 54, (byte)0xBF, 113, (byte)0xE8, 94}))
            .value(
                    StringHelper.decrypt(new byte[]{12, (byte)0xAA, 51, (byte)0x84}),
                    StringHelper.decrypt(new byte[]{5, (byte)0xB6, 43, (byte)0x84, 123, (byte)0xF5, 82}),
                    StringHelper.decrypt(new byte[]{8, (byte)0xAC, 43, (byte)0x80}))
            .selected(StringHelper.decrypt(new byte[]{12, (byte)0xAA, 51, (byte)0x84}));

    public BooleanSetting outline = new BooleanSetting(
            StringHelper.decrypt(new byte[]{5, (byte)0xB6, 43, (byte)0x84, 123, (byte)0xF5, 82}),
            StringHelper.decrypt(new byte[]{5, (byte)0xB6, 43, (byte)0x84, 123, (byte)0xF5, 82, 50, (byte)0xEB, 81, (byte)0xBE, 106, (byte)0xB5, 62, (byte)0x80, 101, (byte)0xE9, 88, (byte)0xAF, 40, (byte)0xAC, 54, (byte)0xBF, 113, (byte)0xE8, 94}))
            .setValue(true)
            .visible(() -> mode.isSelected("Both") || mode.isSelected("Outline"));

    public SliderSettings outlineWidth = new SliderSettings(
            StringHelper.decrypt(new byte[]{5, (byte)0xB6, 43, (byte)0x84, 123, (byte)0xF5, 82, (byte)0xF6, 29, (byte)0xAA, 59, (byte)0x9C, 122}),
            StringHelper.decrypt(new byte[]{5, (byte)0xB6, 43, (byte)0x84, 123, (byte)0xF5, 82, 50, (byte)0xEB, 81, (byte)0xBE, 106, (byte)0xB5, 62, (byte)0x80, 101, (byte)0xE9, 88, (byte)0xAF, 40, (byte)0xAC, 54, (byte)0xBF, 113, (byte)0xE8, 94}))
            .range(0.0f, 20.0f).setValue(7.0f)
            .visible(() -> outline.isValue() && (mode.isSelected("Both") || mode.isSelected("Outline")));

    public BooleanSetting rainbow = new BooleanSetting(
            StringHelper.decrypt(new byte[]{24, (byte)0xA2, 54, (byte)0x86, 112, (byte)0xF4, 64}),
            StringHelper.decrypt(new byte[]{24, (byte)0xA2, 54, (byte)0x86, 112, (byte)0xF4, 64, 50, (byte)0xB0, 47, (byte)0x8C, 118, (byte)0xF6, 84}))
            .setValue(false);

    public SliderSettings intensity = new SliderSettings(
            StringHelper.decrypt(new byte[]{3, (byte)0xAD, 43, (byte)0x8D, 124, (byte)0xE8, 94, (byte)0xA2, 51}),
            StringHelper.decrypt(new byte[]{3, (byte)0xAD, 43, (byte)0x8D, 124, (byte)0xE8, 94, (byte)0xA2, 51, 50, (byte)0xB0, 47, (byte)0x8C, 118, (byte)0xF6, 84}))
            .range(0.2f, 3.0f).setValue(1.25f);

    public ColorSetting color = new ColorSetting("Color", "Chams overlay color")
            .value(0xFF1AD9FF);

    public x7m4j9() {
        super(
                StringHelper.decrypt(new byte[]{9, (byte)0xAB, 62, (byte)0x85, 97}),
                StringHelper.decrypt(new byte[]{24, (byte)0xA6, 49, (byte)0x8C, 119, (byte)0xE9, 68, (byte)0xF6, 47, (byte)0xAD, 43, (byte)0x81, 102, (byte)0xF2, 52, (byte)0xA5, 106, (byte)0xB7, 55, (byte)0x9A, 125, (byte)0xEE, 80, (byte)0xBE, 106, (byte)0xB4, 62, (byte)0x84, 126, (byte)0xE8}),
                ModuleCategory.ESP);
        settings(mode, players, mobs, throughWalls, outline, outlineWidth, rainbow, intensity, color);
    }

    public int getColor(EntityRenderState state, float time, boolean outline) {
        int baseColor = color.getColorNoAlpha();
        float r, g, b, a;

        float intensityVal = intensity.getValue();
        boolean rainbowVal = rainbow.isValue();

        if (outline) {
            r = ((baseColor >> 16) & 0xFF) / 255.0f;
            g = ((baseColor >> 8) & 0xFF) / 255.0f;
            b = (baseColor & 0xFF) / 255.0f;

            if (rainbowVal) {
                float hue = (time * 0.25f) % 1.0f;
                int i = (int)(hue * 6.0f);
                float f = hue * 6.0f - i;
                float p = 1.0f * (1.0f - 0.55f);
                float q = 1.0f * (1.0f - f * 0.55f);
                float t = 1.0f * (1.0f - (1.0f - f) * 0.55f);
                switch (i % 6) {
                    case 0 -> { r = 1.0f; g = t; b = p; }
                    case 1 -> { r = q; g = 1.0f; b = p; }
                    case 2 -> { r = p; g = 1.0f; b = t; }
                    case 3 -> { r = p; g = q; b = 1.0f; }
                    case 4 -> { r = t; g = p; b = 1.0f; }
                    default -> { r = 1.0f; g = p; b = q; }
                }
            } else {
                r = lerp(r, 1.0f, 0.35f);
                g = lerp(g, 1.0f, 0.35f);
                b = lerp(b, 1.0f, 0.35f);
            }

            r = clamp01(r * intensityVal);
            g = clamp01(g * intensityVal);
            b = clamp01(b * intensityVal);
            a = 0.9f;
        } else {
            if (rainbowVal) {
                float hue = (time * 0.15f) % 1.0f;
                int i = (int)(hue * 6.0f);
                float f = hue * 6.0f - i;
                float p = 1.0f * (1.0f - 0.65f);
                float q = 1.0f * (1.0f - f * 0.65f);
                float t = 1.0f * (1.0f - (1.0f - f) * 0.65f);
                switch (i % 6) {
                    case 0 -> { r = 1.0f; g = t; b = p; }
                    case 1 -> { r = q; g = 1.0f; b = p; }
                    case 2 -> { r = p; g = 1.0f; b = t; }
                    case 3 -> { r = p; g = q; b = 1.0f; }
                    case 4 -> { r = t; g = p; b = 1.0f; }
                    default -> { r = 1.0f; g = p; b = q; }
                }
            } else {
                r = ((baseColor >> 16) & 0xFF) / 255.0f;
                g = ((baseColor >> 8) & 0xFF) / 255.0f;
                b = (baseColor & 0xFF) / 255.0f;
            }

            r = clamp01(r * intensityVal);
            g = clamp01(g * intensityVal);
            b = clamp01(b * intensityVal);
            a = 0.55f;
        }

        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
