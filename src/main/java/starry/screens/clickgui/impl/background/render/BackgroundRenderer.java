package starry.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import starry.util.ColorUtil;
import starry.util.render.Render2D;
import starry.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    private static final String[] TOOLTIPS = {"Self Destruct", "Client", "Config"};
    private static final String[] ICONS = {"X", "Y", "Z"};

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int baseAlpha = (int) (255 * alphaMultiplier);
        int[] gradientColors = {
                new Color(26, 26, 26, baseAlpha).getRGB(),
                new Color(0, 0, 0, baseAlpha).getRGB(),
                new Color(26, 26, 26, baseAlpha).getRGB(),
                new Color(0, 0, 0, baseAlpha).getRGB(),
                new Color(26, 26, 20, baseAlpha).getRGB()
        };

        Render2D.gradientRect(bgX, bgY, 400, 250, gradientColors, 15);
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier, float mouseX, float mouseY) {
        int panelAlpha = (int) (25 * alphaMultiplier);
        int outlineAlpha = (int) (255 * alphaMultiplier);
        int blurAlpha = (int) (155 * alphaMultiplier);

        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, new Color(128, 128, 128, panelAlpha).getRGB(), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 10);

        float btnY = bgY + 220.5f;
        Render2D.outline(bgX + 12.5f, btnY, 70, 17, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 5);
        Render2D.blur(bgX + 12.5f, btnY, 70, 17, 4, 5, new Color(25, 25, 25, blurAlpha).getRGB());

        float iconSize = 19;
        float[] iconXs = {bgX + 21.15f, bgX + 40f, bgX + 60f};
        float iconY = bgY + 217.5f;

        float textSize = 5f;
        int normalColor = new Color(58, 58, 58, outlineAlpha).getRGB();
        int hoverTextColor = ColorUtil.getText((int) (255 * alphaMultiplier));

        int hovered = getIconAt(mouseX, mouseY, bgX, bgY);

        for (int i = 0; i < 3; i++) {
            int color = (i == hovered) ? hoverTextColor : normalColor;
            Fonts.GUI_ICONS.draw(ICONS[i], iconXs[i], iconY, iconSize, color);
        }

        if (hovered >= 0) {
            String tip = TOOLTIPS[hovered];
            float tipSize = 5f;
            float tipW = Fonts.BOLD.getWidth(tip, tipSize);
            float tipX = bgX + 12.5f + (70 - tipW) / 2f;
            float tipY = btnY - 10;
            Fonts.BOLD.draw(tip, tipX, tipY, tipSize, hoverTextColor);
        }
    }

    public int getIconAt(double mouseX, double mouseY, float bgX, float bgY) {
        float btnY = bgY + 220.5f;
        if (mouseX >= bgX + 12.5f && mouseX <= bgX + 82.5f && mouseY >= btnY && mouseY <= btnY + 17) {
            float relX = (float) (mouseX - (bgX + 12.5f));
            if (relX < 22) return 0;
            if (relX < 44) return 1;
            return 2;
        }
        return -1;
    }
}