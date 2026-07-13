package starry.screens.clickgui.impl.client;

import net.minecraft.client.gui.DrawContext;
import starry.IMinecraft;
import starry.screens.clickgui.ClickGui;
import starry.util.ColorUtil;
import starry.util.render.Render2D;
import starry.util.render.font.Fonts;
import starry.util.theme.Theme;
import starry.util.theme.ThemeManager;

import java.awt.*;
import starry.util.string.StringHelper;

public class ClientSettingsRenderer implements IMinecraft {
    public static final float PANEL_X = 92f;
    public static final float PANEL_Y = 38f;
    public static final float PANEL_W = 298f;
    public static final float PANEL_H = 204f;

    private boolean active = false;
    private boolean settingKeybind = false;

    public boolean isActive() { return active; }
    public void toggle() { active = !active; }
    public boolean isSettingKeybind() { return settingKeybind; }

    public void render(DrawContext context, float bgX, float bgY, float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        if (!active) return;

        float x = bgX + PANEL_X;
        float y = bgY + PANEL_Y;

        int panelAlpha = (int) (15 * alphaMultiplier);
        int outlineAlpha = (int) (215 * alphaMultiplier);

        Render2D.rect(x, y, PANEL_W, PANEL_H, new Color(64, 64, 64, panelAlpha).getRGB(), 6);
        Render2D.outline(x, y, PANEL_W, PANEL_H, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 6);

        int titleColor = ColorUtil.multAlpha(ColorUtil.getText(), alphaMultiplier);
        Fonts.BOLD.draw(StringHelper.decrypt(new byte[]{9, (byte)-81, 54, (byte)-115, 124, (byte)-17, 23, (byte)-123, 47, (byte)-73, 43, (byte)-127, 124, (byte)-4, 68}), x + 10, y + 8, 7, titleColor);

        float sy = y + 30;
        int textColor = ColorUtil.multAlpha(ColorUtil.getText2(), alphaMultiplier);

        Fonts.BOLD.draw(StringHelper.decrypt(new byte[]{9, (byte)-81, 54, (byte)-117, 121, (byte)-36, 98, 6, (byte)-52, (byte)-29, 20, (byte)-115, 107, (byte)-7, 94, (byte)-72, 46}), x + 10, sy, 5.5f, textColor);
        String keyName = getKeyName(ClickGui.getClickGuiKey());
        float keyX = x + PANEL_W - 10 - Fonts.BOLD.getWidth(keyName, 5.5f);
        Fonts.REGULAR.draw(keyName, keyX, sy, 5.5f, settingKeybind ? 0xFFFFAA00 : textColor);

        sy += 22;
        Fonts.BOLD.draw(StringHelper.decrypt(new byte[]{30, (byte)-85, 58, (byte)-123, 119}), x + 10, sy, 5.5f, textColor);
        Theme current = ThemeManager.getTheme();
        String themeName = current.name;
        float themeX = x + PANEL_W - 10 - Fonts.BOLD.getWidth("> " + themeName + " <", 5.5f);
        Fonts.REGULAR.draw(StringHelper.decrypt(new byte[]{116, (byte)-29}) + themeName + " <", themeX, sy, 5.5f, textColor);

        float previewY = sy + 18;
        float swatchSize = 12;
        float gap = 4;
        float totalSwatches = Theme.PRESETS.length * (swatchSize + gap) - gap;
        float swatchStartX = x + PANEL_W - 10 - totalSwatches;

        for (int i = 0; i < Theme.PRESETS.length; i++) {
            Theme t = Theme.PRESETS[i];
            float sx = swatchStartX + i * (swatchSize + gap);
            Render2D.rect(sx, previewY, swatchSize, swatchSize, t.accent, 2);
            if (t == current) {
                Render2D.outline(sx - 1, previewY - 1, swatchSize + 2, swatchSize + 2, 1, -1, 3);
            }
        }

        sy = previewY + swatchSize + 12;
        Fonts.BOLD.draw(StringHelper.decrypt(new byte[]{11, (byte)-96, 60, (byte)-115, 124, (byte)-17}), x + 10, sy, 5.5f, textColor);
        java.util.List<Integer> accentColors = java.util.List.of(
            0xFF5B3FD4, 0xFF4A90D9, 0xFF58A6FF, 0xFF7C4DFF, 0xFFE91E63, 0xFF4CAF50, 0xFFFF5722, 0xFF9C27B0
        );
        float accentStartX = x + PANEL_W - 10 - accentColors.size() * (swatchSize + gap);
        for (int i = 0; i < accentColors.size(); i++) {
            float sx = accentStartX + i * (swatchSize + gap);
            Render2D.rect(sx, sy, swatchSize, swatchSize, accentColors.get(i), 2);
        }

        sy += 28;
        Render2D.rect(x + 8, sy - 8, PANEL_W - 16, 1, ColorUtil.multAlpha(ColorUtil.getOutline(), alphaMultiplier), 0);
        Fonts.BOLD.draw("Self Destruct Dialog", x + 10, sy, 6, titleColor);

        sy += 20;
        drawOption("Style", ThemeManager.getSelfDestructStyle(), x, sy, textColor);
        sy += 18;
        drawOption("Backdrop", ThemeManager.getSelfDestructDimName(), x, sy, textColor);
        sy += 18;
        drawOption("Detail Text", ThemeManager.isSelfDestructDetails() ? "Shown" : "Hidden", x, sy, textColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float bgX, float bgY) {
        if (!active) return false;
        float x = bgX + PANEL_X;
        float y = bgY + PANEL_Y;

        float keyX = x + PANEL_W - 10 - 70;
        if (mouseX >= keyX && mouseX <= keyX + 70 && mouseY >= y + 30 && mouseY <= y + 48) {
            settingKeybind = !settingKeybind;
            return true;
        }

        String themeName = ThemeManager.getTheme().name;
        float themeTextW = Fonts.BOLD.getWidth("> " + themeName + " <", 5.5f);
        float themeTextX = x + PANEL_W - 10 - themeTextW;
        if (mouseX >= themeTextX && mouseX <= themeTextX + themeTextW && mouseY >= y + 52 && mouseY <= y + 68) {
            int next = (ThemeManager.getThemeIndex() + 1) % Theme.PRESETS.length;
            ThemeManager.setThemeIndex(next);
            return true;
        }

        float swatchSize = 12;
        float gap = 4;
        float previewY = y + 68;
        float totalSwatches = Theme.PRESETS.length * (swatchSize + gap) - gap;
        float swatchStartX = x + PANEL_W - 10 - totalSwatches;
        for (int i = 0; i < Theme.PRESETS.length; i++) {
            float sx = swatchStartX + i * (swatchSize + gap);
            if (mouseX >= sx && mouseX <= sx + swatchSize && mouseY >= previewY && mouseY <= previewY + swatchSize) {
                ThemeManager.setThemeIndex(i);
                return true;
            }
        }


        if (mouseX >= x + 8 && mouseX <= x + PANEL_W - 8 && mouseY >= y + 136 && mouseY <= y + 154) {
            ThemeManager.cycleSelfDestructStyle();
            return true;
        }
        if (mouseX >= x + 8 && mouseX <= x + PANEL_W - 8 && mouseY >= y + 154 && mouseY <= y + 172) {
            ThemeManager.cycleSelfDestructDim();
            return true;
        }
        if (mouseX >= x + 8 && mouseX <= x + PANEL_W - 8 && mouseY >= y + 172 && mouseY <= y + 194) {
            ThemeManager.toggleSelfDestructDetails();
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (settingKeybind) {
            ClickGui.setClickGuiKey(keyCode);
            settingKeybind = false;
            return true;
        }
        return false;
    }

    private String getKeyName(int key) {
        if (key == -1 || key == 0) return "NONE";
        try {
            var field = org.lwjgl.glfw.GLFW.class.getDeclaredField("GLFW_KEY_" + java.lang.Character.toString(key).toUpperCase());
            return field.getName().replace("GLFW_KEY_", "");
        } catch (Exception e) {
            try {
                for (var f : org.lwjgl.glfw.GLFW.class.getFields()) {
                    if (f.getName().startsWith("GLFW_KEY_") && f.getInt(null) == key) {
                        return f.getName().replace("GLFW_KEY_", "");
                    }
                }
            } catch (Exception e2) {}
            return "KEY_" + key;
        }
    }

    private void drawOption(String label, String value, float x, float y, int color) {
        Fonts.REGULAR.draw(label, x + 10, y, 5.5f, color);
        float valueX = x + PANEL_W - 10 - Fonts.BOLD.getWidth(value, 5.5f);
        Fonts.BOLD.draw(value, valueX, y, 5.5f, ThemeManager.getTheme().accent);
    }
}
