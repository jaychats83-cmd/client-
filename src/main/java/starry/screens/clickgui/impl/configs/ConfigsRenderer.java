package starry.screens.clickgui.impl.configs;

import net.minecraft.client.gui.DrawContext;
import starry.modules.module.category.ModuleCategory;
import starry.screens.clickgui.impl.configs.handler.ConfigAnimationHandler;
import starry.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import starry.screens.clickgui.impl.configs.render.ConfigCreateBoxRenderer;
import starry.screens.clickgui.impl.configs.render.ConfigHeaderRenderer;
import starry.screens.clickgui.impl.configs.render.ConfigListRenderer;
import starry.screens.clickgui.impl.configs.render.ConfigNotificationRenderer;
import starry.util.render.Render2D;
import starry.util.render.font.Fonts;

import java.awt.*;
import java.util.List;
import starry.util.string.StringHelper;

public class ConfigsRenderer {

    public static final float PANEL_X_OFFSET = 92f;
    public static final float PANEL_Y_OFFSET = 38f;
    public static final float PANEL_WIDTH = 298f;
    public static final float PANEL_HEIGHT = 204f;
    public static final float CORNER_RADIUS = 6f;

    private final ConfigAnimationHandler animationHandler;
    private final ConfigDataHandler dataHandler;
    private final ConfigHeaderRenderer headerRenderer;
    private final ConfigListRenderer listRenderer;
    private final ConfigCreateBoxRenderer createBoxRenderer;
    private final ConfigNotificationRenderer notificationRenderer;

    private boolean isActive = false;
    private boolean wasActive = false;

    public ConfigsRenderer() {
        this.animationHandler = new ConfigAnimationHandler();
        this.dataHandler = new ConfigDataHandler(animationHandler);
        this.notificationRenderer = new ConfigNotificationRenderer();
        this.headerRenderer = new ConfigHeaderRenderer(dataHandler);
        this.listRenderer = new ConfigListRenderer(animationHandler, dataHandler, notificationRenderer);
        this.createBoxRenderer = new ConfigCreateBoxRenderer(dataHandler, notificationRenderer);
    }

    public void render(DrawContext context, float bgX, float bgY, float mouseX, float mouseY,
                       float delta, int guiScale, float alphaMultiplier, ModuleCategory category) {

        if (animationHandler.isFullyHidden() && !isActive) {
            return;
        }

        List<String> ids = new java.util.ArrayList<>();
        for (var e : dataHandler.getEntries()) ids.add(e.id);
        animationHandler.initItemAnimations(ids);
        animationHandler.update(isActive, ids, dataHandler.isCreating());

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        float slideOffset = (1f - animationHandler.getPanelSlide()) * 20f;
        float finalAlpha = alphaMultiplier * animationHandler.getPanelAlpha();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(slideOffset, 0);

        renderPanel(panelX, panelY, finalAlpha);
        headerRenderer.render(panelX, panelY, mouseX - slideOffset, mouseY, finalAlpha);
        listRenderer.render(context, panelX, panelY, mouseX - slideOffset, mouseY, guiScale, finalAlpha);
        createBoxRenderer.render(panelX, panelY, finalAlpha);
        renderImportBox(panelX, panelY, mouseX - slideOffset, mouseY, finalAlpha);
        notificationRenderer.render(panelX, panelY, finalAlpha);

        context.getMatrices().popMatrix();
    }

    private void renderImportBox(float x, float y, float mouseX, float mouseY, float alpha) {
        if (!dataHandler.isImporting()) return;

        float boxY = y + PANEL_HEIGHT - 40;
        float boxAlpha = alpha;

        Render2D.rect(x + 8, boxY, PANEL_WIDTH - 16, 32,
                new Color(50, 50, 55, (int) (30 * boxAlpha)).getRGB(), 5);
        Render2D.outline(x + 8, boxY, PANEL_WIDTH - 16, 32, 0.5f,
                new Color(80, 80, 85, (int) (100 * boxAlpha)).getRGB(), 5);

        float inputX = x + 15;
        float inputY = boxY + 8;
        float inputW = PANEL_WIDTH - 100;
        float inputH = 16;

        Render2D.rect(inputX, inputY, inputW, inputH,
                new Color(40, 40, 45, (int) (40 * boxAlpha)).getRGB(), 4);
        Render2D.outline(inputX, inputY, inputW, inputH, 0.5f,
                new Color(70, 70, 75, (int) (80 * boxAlpha)).getRGB(), 4);

        String path = dataHandler.getImportCode();
        if (path.isEmpty()) {
            Fonts.BOLD.draw("Paste config file path", inputX + 5, inputY + 5, 5,
                    new Color(100, 100, 105, (int) (150 * boxAlpha)).getRGB());
        } else {
            Fonts.BOLD.draw(fitPath(path, inputW - 10), inputX + 5, inputY + 5, 5,
                    new Color(210, 210, 220, (int) (255 * boxAlpha)).getRGB());
        }

        float saveX = x + PANEL_WIDTH - 75;
        float saveY = boxY + 6;
        float saveW = 60;
        float saveH = 20;

        boolean hovered = mouseX >= saveX && mouseX <= saveX + saveW &&
                mouseY >= saveY && mouseY <= saveY + saveH;

        int btnBg = (int) ((hovered ? 50 : 30) * boxAlpha);
        Render2D.rect(saveX, saveY, saveW, saveH,
                new Color(60, 100, 140, btnBg).getRGB(), 4);
        Render2D.outline(saveX, saveY, saveW, saveH, 0.5f,
                new Color(100, 180, 220, (int) (80 * boxAlpha)).getRGB(), 4);

        float tw = Fonts.BOLD.getWidth("Import", 5);
        Fonts.BOLD.draw(StringHelper.decrypt(new byte[]{3, (byte)-82, 47, (byte)-121, 96, (byte)-17}), saveX + (saveW - tw) / 2, saveY + 7, 5,
                new Color(180, 220, 255, (int) (255 * boxAlpha)).getRGB());
    }

    private void renderPanel(float x, float y, float alpha) {
        int panelAlpha = (int) (15 * alpha);
        int outlineAlpha = (int) (215 * alpha);

        Render2D.rect(x, y, PANEL_WIDTH, PANEL_HEIGHT, new Color(64, 64, 64, panelAlpha).getRGB(), CORNER_RADIUS);
        Render2D.outline(x, y, PANEL_WIDTH, PANEL_HEIGHT, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), CORNER_RADIUS);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float bgX, float bgY, ModuleCategory category) {
        if (animationHandler.getPanelAlpha() < 0.5f) return false;

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        float slideOffset = (1f - animationHandler.getPanelSlide()) * 20f;
        mouseX -= slideOffset;

        if (headerRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        if (createBoxRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        if (dataHandler.isImporting()) {
            float boxY = panelY + PANEL_HEIGHT - 40;
            float saveX = panelX + PANEL_WIDTH - 75;
            float saveY = boxY + 6;
            if (mouseX >= saveX && mouseX <= saveX + 60 &&
                    mouseY >= saveY && mouseY <= saveY + 20 && button == 0) {
                String path = dataHandler.getImportCode();
                if (!path.isEmpty()) {
                    dataHandler.importByCode(path);
                    notificationRenderer.show("Importing file...",
                            ConfigNotificationRenderer.NotificationType.INFO);
                    dataHandler.toggleImporting();
                } else {
                    notificationRenderer.show("Enter a file path",
                            ConfigNotificationRenderer.NotificationType.ERROR);
                }
                return true;
            }
        }

        if (listRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical, float bgX, float bgY, ModuleCategory category) {
        if (animationHandler.getPanelAlpha() < 0.5f) return false;

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        return listRenderer.mouseScrolled(mouseX, mouseY, vertical, panelX, panelY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (dataHandler.isImporting()) {
            if (keyCode == 259) {
                dataHandler.removeLastChar();
                return true;
            }
            if (keyCode == 257) {
                String path = dataHandler.getImportCode();
                if (!path.isEmpty()) {
                    dataHandler.importByCode(path);
                    notificationRenderer.show("Importing file...",
                            ConfigNotificationRenderer.NotificationType.INFO);
                    dataHandler.toggleImporting();
                }
                return true;
            }
            return true;
        }
        return createBoxRenderer.keyPressed(keyCode);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (dataHandler.isImporting()) {
            dataHandler.appendChar(chr);
            return true;
        }
        return createBoxRenderer.charTyped(chr);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public void toggle() {
        isActive = !isActive;
        if (isActive) {
            animationHandler.reset();
            dataHandler.refreshConfigs();
        }
    }

    public boolean isActive() { return isActive; }

    public boolean isEditing() {
        return dataHandler.isCreating();
    }

    private String fitPath(String text, float maxWidth) {
        if (Fonts.BOLD.getWidth(text, 5) <= maxWidth) return text;
        String value = text;
        while (value.length() > 3 && Fonts.BOLD.getWidth("..." + value, 5) > maxWidth) {
            value = value.substring(1);
        }
        return "..." + value;
    }
}
