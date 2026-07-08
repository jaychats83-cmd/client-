package starry.screens.clickgui.impl.background.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import starry.util.render.Render2D;
import starry.util.render.shader.Scissor;
import starry.util.render.font.Fonts;
import starry.util.render.gif.GifRender;
import starry.util.subscription.SubscriptionManager;

import java.awt.*;

public class AvatarRenderer {

    private static final int FORCED_GUI_SCALE = 2;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        int alphaFon = (int) (105 * alphaMultiplier);
        int alphaText = (int) (200 * alphaMultiplier);

        SubscriptionManager sub = SubscriptionManager.getInstance();
        String username = mc.getSession().getUsername();
        int total = sub != null ? sub.getTotalLicenses() : 0;
        int index = sub != null ? sub.getLicenseIndex() : -1;

        Render2D.blur(bgX + 15f, bgY + 15f, 1, 1, 0f, 0, alphaText);
        context.getMatrices().pushMatrix();
        Render2D.blur(bgX + 15f, bgY + 15f, 1, 1, 0f, 0, alphaText);
        GifRender.drawBackground(bgX + 12.5f, bgY + 12.5f, 70, 30, 7, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 15f, bgY + 15f, 25, 25, new Color(42, 42, 42, alpha).getRGB(), 15);
        GifRender.drawAvatar(bgX + 16f, bgY + 16f, 23, 23, 15, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 33, bgY + 33, 5, 5, new Color(0, 255, 0, alpha).getRGB(), 10);
        context.getMatrices().popMatrix();

        Render2D.rect(bgX + 12.5f, bgY + 12.5f, 70, 30, new Color(0, 0, 0, alphaFon).getRGB(), 7);

        float textX = bgX + 44;
        float textY = bgY + 22;
        float maxTextWidth = 35f;
        float textHeight = 14f;

        Scissor.enable(textX, textY - 2, maxTextWidth, textHeight, FORCED_GUI_SCALE);
        Fonts.BOLD.draw(username, textX, textY, 6, new Color(255, 255, 255, alphaText).getRGB());
        String tag = (index >= 0) ? "Uid: " + (index + 1) : "subscription";
        Fonts.BOLD.draw(tag, textX, textY + 7, 5, new Color(255, 255, 255, alphaText).getRGB());
        Render2D.blur(textX, textY + 7, 1, 1, 0f, 0, alphaText);
        Scissor.disable();

        Render2D.blur(textX, textY + 7, 1, 1, 0f, 0, alphaText);
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}