package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.GlassHandsRenderEvent;
import starry.events.impl.WorldChangeEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.render.shader.GlassHandsRenderer;
import starry.util.string.StringHelper;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class hr71oe extends ModuleStructure {

    private static hr71oe instance;

    SliderSettings blurRadius = new SliderSettings(StringHelper.decrypt(new byte[]{8, (byte)-81, 42, (byte)-102, 50, (byte)-56, 67, (byte)-92, 47, (byte)-83, 56, (byte)-100, 122}), StringHelper.decrypt(new byte[]{13, (byte)-81, 62, (byte)-101, 97, (byte)-69, 85, (byte)-70, 63, (byte)-79, 127, (byte)-115, 116, (byte)-3, 82, (byte)-75, 62, (byte)-29, 44, (byte)-100, 96, (byte)-2, 89, (byte)-79, 62, (byte)-85}))
            .setValue(2.5f).range(1.0f, 5.0f);

    SliderSettings blurIterations = new SliderSettings(StringHelper.decrypt(new byte[]{27, (byte)-74, 62, (byte)-124, 123, (byte)-17, 78}), StringHelper.decrypt(new byte[]{4, (byte)-74, 50, (byte)-118, 119, (byte)-23, 23, (byte)-71, 44, (byte)-29, 61, (byte)-124, 103, (byte)-23, 23, (byte)-65, 62, (byte)-90, 45, (byte)-119, 102, (byte)-14, 88, (byte)-72, 57}))
            .setValue(3).range(1, 5);

    SliderSettings saturation = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-94, 43, (byte)-99, 96, (byte)-6, 67, (byte)-65, 37, (byte)-83}), StringHelper.decrypt(new byte[]{9, (byte)-84, 51, (byte)-121, 96, (byte)-69, 68, (byte)-73, 62, (byte)-74, 45, (byte)-119, 102, (byte)-14, 88, (byte)-72}))
            .setValue(0).range(0.0f, 2.0f);

    BooleanSetting enableTint = new BooleanSetting(StringHelper.decrypt(new byte[]{30, (byte)-86, 49, (byte)-100}), StringHelper.decrypt(new byte[]{15, 19, (byte)-30, (byte)-119, 112, (byte)-9, 82, (byte)-10, 41, (byte)-84, 51, (byte)-121, 96, (byte)-2, 83, (byte)-10, 45, (byte)-81, 62, (byte)-101, 97, (byte)-69, 67, (byte)-65, 36, (byte)-73}))
            .setValue(false);

    SliderSettings tintIntensity = new SliderSettings(StringHelper.decrypt(new byte[]{30, (byte)-86, 49, (byte)-100, 50, (byte)-56, 67, (byte)-92, 47, (byte)-83, 56, (byte)-100, 122}), StringHelper.decrypt(new byte[]{30, (byte)-86, 49, (byte)-100, 50, (byte)-14, 89, (byte)-94, 47, (byte)-83, 44, (byte)-127, 102, (byte)-30}))
            .setValue(0.2f).range(0.0f, 0.5f)
            .visible(enableTint::isValue);

    ColorSetting tintColor = new ColorSetting("Tint Color", "Glass tint color")
            .value(0xFF00FFFF)
            .visible(enableTint::isValue);

    BooleanSetting enableEdgeGlow = new BooleanSetting(StringHelper.decrypt(new byte[]{15, (byte)-89, 56, (byte)-115, 50, (byte)-36, 91, (byte)-71, 61}), StringHelper.decrypt(new byte[]{15, (byte)-89, 56, (byte)-115, 50, (byte)-4, 91, (byte)-71, 61, (byte)-29, 48, (byte)-122, 50, (byte)-4, 91, (byte)-73, 57, (byte)-80}))
            .setValue(true);

    SliderSettings edgeGlowIntensity = new SliderSettings(StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97, 50, (byte)-56, 67, (byte)-92, 47, (byte)-83, 56, (byte)-100, 122}), StringHelper.decrypt(new byte[]{15, (byte)-89, 56, (byte)-115, 50, (byte)-4, 91, (byte)-71, 61, (byte)-29, 54, (byte)-122, 102, (byte)-2, 89, (byte)-91, 35, (byte)-73, 38}))
            .setValue(0.2f).range(0.0f, 1.0f)
            .visible(enableEdgeGlow::isValue);

    public hr71oe() {
        super(StringHelper.decrypt(new byte[]{13, (byte)-81, 62, (byte)-101, 97, (byte)-45, 86, (byte)-72, 46, (byte)-80}), StringHelper.decrypt(new byte[]{7, (byte)-94, 52, (byte)-115, 97, (byte)-69, 95, (byte)-73, 36, (byte)-89, 44, (byte)-56, 115, (byte)-11, 83, (byte)-10, 35, (byte)-73, 58, (byte)-123, 97, (byte)-69, 80, (byte)-70, 43, (byte)-80, 44, (byte)-59, 126, (byte)-14, 92, (byte)-77}), ModuleCategory.VISUALS);
        settings(blurRadius, blurIterations, saturation, enableTint, tintIntensity, tintColor, enableEdgeGlow, edgeGlowIntensity);
        instance = this;
    }

    public static hr71oe getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer != null) {
            renderer.invalidate();
            renderer.setEnabled(true);
            updateRendererSettings();
        }
    }

    @Override
    public void deactivate() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer != null) {
            renderer.setEnabled(false);
        }
    }

    @EventHandler
    public void onWorldChange(WorldChangeEvent event) {
        if (!isState()) return;

        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer != null) {
            renderer.invalidate();
            renderer.setEnabled(true);
            updateRendererSettings();
        }
    }

    @EventHandler
    public void onGlassHandsRender(GlassHandsRenderEvent event) {
        if (!isState()) return;

        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer == null) return;

        updateRendererSettings();

        if (event.getPhase() == GlassHandsRenderEvent.Phase.PRE) {
            renderer.captureSceneBeforeHands();
        } else if (event.getPhase() == GlassHandsRenderEvent.Phase.POST) {
            renderer.captureSceneAfterHands();
            renderer.renderGlassEffect();
        }
    }

    private void updateRendererSettings() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer == null) return;

        renderer.setBlurRadius(blurRadius.getValue());
        renderer.setBlurIterations(blurIterations.getInt());
        renderer.setSaturation(saturation.getValue());
        renderer.setReflect(true);

        if (enableTint.isValue()) {
            renderer.setTintColor(tintColor.getColor());
            renderer.setTintIntensity(tintIntensity.getValue());
        } else {
            renderer.setTintColor(0x00000000);
            renderer.setTintIntensity(0.0f);
        }

        if (enableEdgeGlow.isValue()) {
            renderer.setEdgeGlowIntensity(edgeGlowIntensity.getValue());
        } else {
            renderer.setEdgeGlowIntensity(0.0f);
        }
    }
}
