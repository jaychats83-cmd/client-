package starry.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.util.interfaces.AbstractSettingComponent;
import starry.util.render.Render2D;
import starry.util.render.font.Fonts;

import java.awt.*;

public class MinMaxComponent extends AbstractSettingComponent {
    private final MinMaxSetting minMaxSetting;
    private boolean draggingMin = false;
    private boolean draggingMax = false;
    private float animatedPercentageMin = 0f;
    private float animatedPercentageMax = 1f;
    private float knobAnimation = 0f;
    private float hoverAnimationMin = 0f;
    private float hoverAnimationMax = 0f;
    private float backgroundFade = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float KNOB_SIZE = 5f;
    private static final float SLIDER_HEIGHT = 2.5f;

    public MinMaxComponent(MinMaxSetting setting) {
        super(setting);
        this.minMaxSetting = setting;
        float range = setting.getMax() - setting.getMin();
        if (range > 0) {
            animatedPercentageMin = (setting.getMinValue() - setting.getMin()) / range;
            animatedPercentageMax = (setting.getMaxValue() - setting.getMin()) / range;
        }
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;
        return deltaTime;
    }

    private float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * Math.min(speed, 1f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (draggingMin) updateMinValue(mouseX);
        if (draggingMax) updateMaxValue(mouseX);

        float deltaTime = getDeltaTime();

        float range = minMaxSetting.getMax() - minMaxSetting.getMin();
        float targetMinPct = range > 0 ? (minMaxSetting.getMinValue() - minMaxSetting.getMin()) / range : 0f;
        float targetMaxPct = range > 0 ? (minMaxSetting.getMaxValue() - minMaxSetting.getMin()) / range : 1f;

        animatedPercentageMin += (targetMinPct - animatedPercentageMin) * 0.25f;
        animatedPercentageMax += (targetMaxPct - animatedPercentageMax) * 0.25f;

        float knobTarget = (draggingMin || draggingMax) ? 1f : 0f;
        knobAnimation += (knobTarget - knobAnimation) * 0.25f;
        knobAnimation = Math.max(0f, Math.min(1f, knobAnimation));

        boolean hoverMin = isKnobHover(mouseX, mouseY, true);
        boolean hoverMax = isKnobHover(mouseX, mouseY, false);
        hoverAnimationMin = lerp(hoverAnimationMin, hoverMin ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        hoverAnimationMax = lerp(hoverAnimationMax, hoverMax ? 1f : 0f, deltaTime * ANIMATION_SPEED);

        boolean hovered = isHover(mouseX, mouseY);
        backgroundFade = lerp(backgroundFade, hovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);

        int iconAlpha = (int)(200 * alphaMultiplier);
        Fonts.GUI_ICONS.draw("H", x - 0.5f, y + 0.5f, 9, new Color(210, 210, 210, iconAlpha).getRGB());

        Fonts.BOLD.draw(minMaxSetting.getName(), x + 9.5f, y + 1f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        renderValueText();
        renderSlider();
    }

    private void renderValueText() {
        String minStr = minMaxSetting.isInteger()
                ? String.valueOf(minMaxSetting.getIntMin())
                : String.format("%.1f", minMaxSetting.getMinValue());
        String maxStr = minMaxSetting.isInteger()
                ? String.valueOf(minMaxSetting.getIntMax())
                : String.format("%.1f", minMaxSetting.getMaxValue());
        String displayText = minStr + " - " + maxStr;

        float textWidth = Fonts.BOLD.getWidth(displayText, 5);
        float textX = x + width - textWidth - 4;
        float textY = y + 2f;

        int textAlpha = clampAlpha(160 * alphaMultiplier);
        Fonts.BOLD.draw(displayText, textX, textY, 5, new Color(100, 100, 105, textAlpha).getRGB());
    }

    private void renderSlider() {
        float sliderY = y + 11;
        float sliderPadding = 1f;
        float sliderTrackWidth = width - 2;

        Render2D.rect(x + sliderPadding, sliderY, sliderTrackWidth, SLIDER_HEIGHT,
                applyAlpha(new Color(60, 60, 65, 220)).getRGB(), 2f);

        float fillStart = x + sliderPadding + sliderTrackWidth * animatedPercentageMin;
        float fillEnd = x + sliderPadding + sliderTrackWidth * animatedPercentageMax;
        float fillWidth = fillEnd - fillStart;

        if (fillWidth > 0) {
            Render2D.rect(fillStart, sliderY, fillWidth, SLIDER_HEIGHT,
                    applyAlpha(new Color(130, 130, 135, 230)).getRGB(), 2f);
        }

        float knobSize = KNOB_SIZE + (knobAnimation * 1f);
        drawKnob(sliderY, sliderPadding, sliderTrackWidth, animatedPercentageMin, knobSize, hoverAnimationMin);
        drawKnob(sliderY, sliderPadding, sliderTrackWidth, animatedPercentageMax, knobSize, hoverAnimationMax);
    }

    private void drawKnob(float sliderY, float sliderPadding, float trackWidth, float pct, float size, float hoverPct) {
        float knobX = x + sliderPadding + (trackWidth * pct) - (size / 2f);
        float knobY = sliderY + (SLIDER_HEIGHT / 2f) - (size / 2f);

        knobX = Math.max(x + sliderPadding - (size / 2f),
                Math.min(knobX, x + sliderPadding + trackWidth - (size / 2f)));

        int r = (int)(180 + hoverPct * 40);
        int g = (int)(180 + hoverPct * 40);
        int b = (int)(185 + hoverPct * 40);
        Render2D.rect(knobX, knobY, size, size,
                applyAlpha(new Color(r, g, b, 255)).getRGB(), size / 2f);
    }

    private boolean isKnobHover(double mouseX, double mouseY, boolean isMin) {
        float pct = isMin ? animatedPercentageMin : animatedPercentageMax;
        float sliderPadding = 1f;
        float trackWidth = width - 2;
        float size = KNOB_SIZE + (knobAnimation * 1f);
        float knobX = x + sliderPadding + (trackWidth * pct) - (size / 2f);
        float knobY = y + 11 + (SLIDER_HEIGHT / 2f) - (size / 2f);
        return mouseX >= knobX && mouseX <= knobX + size && mouseY >= knobY && mouseY <= knobY + size + 4;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isKnobHover(mouseX, mouseY, true)) {
                draggingMin = true;
                updateMinValue(mouseX);
                return true;
            }
            if (isKnobHover(mouseX, mouseY, false)) {
                draggingMax = true;
                updateMaxValue(mouseX);
                return true;
            }
            if (isSliderHover(mouseX, mouseY)) {
                float mid = (animatedPercentageMin + animatedPercentageMax) / 2f;
                float clickPct = (float)((mouseX - x - 1) / (width - 2));
                if (clickPct < mid) {
                    draggingMin = true;
                    updateMinValue(mouseX);
                } else {
                    draggingMax = true;
                    updateMaxValue(mouseX);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isSliderHover(double mouseX, double mouseY) {
        float sliderY = y + 6;
        float sliderHeight = 12f;
        return mouseX >= x && mouseX <= x + width && mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasDragging = draggingMin || draggingMax;
            draggingMin = false;
            draggingMax = false;
            return wasDragging;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            if (draggingMin) { updateMinValue(mouseX); return true; }
            if (draggingMax) { updateMaxValue(mouseX); return true; }
        }
        return false;
    }

    private void updateMinValue(double mouseX) {
        float sliderPadding = 1f;
        float sliderTrackWidth = width - 2;
        float percentage = (float) ((mouseX - x - sliderPadding) / sliderTrackWidth);
        percentage = Math.max(0, Math.min(1, percentage));

        float range = minMaxSetting.getMax() - minMaxSetting.getMin();
        float newValue = minMaxSetting.getMin() + (range * percentage);

        if (minMaxSetting.isInteger()) newValue = Math.round(newValue);

        float maxVal = minMaxSetting.getMaxValue();
        if (newValue > maxVal) newValue = maxVal;

        minMaxSetting.setMinValue(newValue);
    }

    private void updateMaxValue(double mouseX) {
        float sliderPadding = 1f;
        float sliderTrackWidth = width - 2;
        float percentage = (float) ((mouseX - x - sliderPadding) / sliderTrackWidth);
        percentage = Math.max(0, Math.min(1, percentage));

        float range = minMaxSetting.getMax() - minMaxSetting.getMin();
        float newValue = minMaxSetting.getMin() + (range * percentage);

        if (minMaxSetting.isInteger()) newValue = Math.round(newValue);

        float minVal = minMaxSetting.getMinValue();
        if (newValue < minVal) newValue = minVal;

        minMaxSetting.setMaxValue(newValue);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) alpha));
    }
}
