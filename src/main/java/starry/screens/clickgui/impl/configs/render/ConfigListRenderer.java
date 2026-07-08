package starry.screens.clickgui.impl.configs.render;

import net.minecraft.client.gui.DrawContext;
import starry.screens.clickgui.impl.configs.ConfigsRenderer;
import starry.screens.clickgui.impl.configs.handler.ConfigAnimationHandler;
import starry.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import starry.util.config.cloud.CloudConfigEntry;
import starry.util.render.Render2D;
import starry.util.render.shader.Scissor;
import starry.util.render.font.Fonts;

import java.awt.*;
import java.util.Map;
import starry.util.string.StringHelper;

public class ConfigListRenderer {

    private static final float CONFIG_ITEM_HEIGHT = 30f;
    private static final float CONFIG_ITEM_SPACING = 3f;
    private static final float HOVER_SPEED = 0.15f;

    private final ConfigAnimationHandler animationHandler;
    private final ConfigDataHandler dataHandler;
    private final ConfigNotificationRenderer notificationRenderer;

    public ConfigListRenderer(ConfigAnimationHandler animationHandler, ConfigDataHandler dataHandler,
                              ConfigNotificationRenderer notificationRenderer) {
        this.animationHandler = animationHandler;
        this.dataHandler = dataHandler;
        this.notificationRenderer = notificationRenderer;
    }

    public void render(DrawContext context, float x, float y, float mouseX, float mouseY,
                       int guiScale, float alpha) {
        float listX = x + 8;
        float listY = y + 37;
        float listW = ConfigsRenderer.PANEL_WIDTH - 16;
        float listH = ConfigsRenderer.PANEL_HEIGHT - 45;

        if (dataHandler.isCreating()) {
            listH -= 40 * animationHandler.getCreateBoxAnimation();
        }

        dataHandler.updateScroll(0.016f);
        dataHandler.updateScrollFades(listH);

        Scissor.enable(listX, listY - 8, listW, listH + 15, 2);

        float itemY = listY + (float) dataHandler.getScrollOffset();

        for (CloudConfigEntry entry : dataHandler.getEntries()) {
            float itemAlpha = animationHandler.getItemAppearAnimation(entry.id);

            if (itemAlpha < 0.01f) {
                itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
                continue;
            }

            if (itemY + CONFIG_ITEM_HEIGHT >= listY && itemY <= listY + listH) {
                float itemSlide = (1f - itemAlpha) * 15f;
                renderConfigItem(entry, listX + itemSlide, itemY, listW, mouseX, mouseY, alpha * itemAlpha);
            }
            itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
        }

        if (dataHandler.getEntries().isEmpty()) {
            renderEmptyMessage(x, y, alpha);
        }

        Scissor.disable();
    }

    private void renderConfigItem(CloudConfigEntry entry, float x, float y, float width,
                                   float mouseX, float mouseY, float alpha) {
        boolean isHovered = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + CONFIG_ITEM_HEIGHT;
        boolean isSelected = dataHandler.getSelectedEntry() != null && dataHandler.getSelectedEntry().id.equals(entry.id);
        boolean isLaunch = dataHandler.isLaunchConfig(entry.id);

        float hoverAnim = animationHandler.getHoverAnimation(entry.id);
        float target = isHovered ? 1f : 0f;
        hoverAnim += (target - hoverAnim) * HOVER_SPEED;
        animationHandler.setHoverAnimation(entry.id, hoverAnim);

        renderItemBackground(x, y, width, isSelected, isLaunch, hoverAnim, alpha);
        renderItemName(entry, x, y, isLaunch, alpha);
        Fonts.GUI_ICONS.draw("B", x + width - 16, y + 3, 9, new Color(200, 200, 200, (int) (50 * alpha)).getRGB());
        renderActionButtons(entry, x, y, width, mouseX, mouseY, alpha);
        renderActionTooltips(entry, x, y, width, mouseX, mouseY, alpha);
        renderLaunchTooltip(entry, x, y, width, mouseX, mouseY, alpha);
    }

    private void renderItemBackground(float x, float y, float width, boolean isSelected,
                                       boolean isLaunch, float hoverAnim, float alpha) {
        int bgAlpha = (int) ((20 + 15 * hoverAnim + (isSelected ? 10 : 0) + (isLaunch ? 15 : 0)) * alpha);
        int gray = (int) (60 + 20 * hoverAnim + (isLaunch ? 15 : 0));
        Render2D.rect(x, y, width, CONFIG_ITEM_HEIGHT, new Color(gray, gray, gray, bgAlpha).getRGB(), 5);

        if (isLaunch) {
            Render2D.rect(x + width - 3, y, 3, CONFIG_ITEM_HEIGHT, new Color(80, 200, 80, (int) (120 * alpha)).getRGB(), 0);
        }

        if (isSelected || hoverAnim > 0.01f) {
            int outlineAlpha = (int) ((40 + 40 * hoverAnim) * alpha);
            Render2D.outline(x, y, width, CONFIG_ITEM_HEIGHT, 0.5f,
                    new Color(100, 100, 100, outlineAlpha).getRGB(), 5);
        }
    }

    private void renderItemName(CloudConfigEntry entry, float x, float y, boolean isLaunch, float alpha) {
        float nameX = x + 8;
        if (isLaunch) {
            Fonts.GUI_ICONS.draw("B", x + 2, y + 5, 6, new Color(80, 200, 80, (int) (200 * alpha)).getRGB());
            nameX = x + 12;
        }
        Fonts.BOLD.draw(entry.name, nameX, y + 4, 6, new Color(220, 220, 220, (int) (255 * alpha)).getRGB());
        Fonts.REGULAR.draw(StringHelper.decrypt(new byte[]{3, (byte)-121, 101, (byte)-56}) + entry.id, x + 8, y + 15, 4, new Color(120, 120, 120, (int) (180 * alpha)).getRGB());
    }

    private void renderActionButtons(CloudConfigEntry entry, float x, float y, float width,
                                      float mouseX, float mouseY, float alpha) {
        float buttonSize = 16f;
        float buttonY = y + (CONFIG_ITEM_HEIGHT - buttonSize) / 2 + 1;
        float deleteButtonX = x + width - buttonSize - 6;
        float refreshButtonX = deleteButtonX - buttonSize - 4;
        float loadButtonX = refreshButtonX - buttonSize - 4;

        renderActionButton(loadButtonX, buttonY, buttonSize, "P", 13f, 3f, 2f,
                mouseX, mouseY, animationHandler.getLoadHoverAnimations(), entry.id,
                new Color(80, 180, 80), alpha);

        renderActionButton(refreshButtonX, buttonY, buttonSize, "N", 9f, 4f, 3.5f,
                mouseX, mouseY, animationHandler.getRefreshHoverAnimations(), entry.id,
                new Color(80, 140, 200), alpha);

        renderActionButton(deleteButtonX, buttonY, buttonSize, "O", 11f, 3.5f, 2f,
                mouseX, mouseY, animationHandler.getDeleteHoverAnimations(), entry.id,
                new Color(180, 80, 80), alpha);
    }

    private void renderActionButton(float x, float y, float size, String icon,
                                    float iconSize, float iconOffsetX, float iconOffsetY,
                                    float mouseX, float mouseY,
                                    Map<String, Float> animations, String id,
                                    Color hoverColor, float alpha) {
        boolean hovered = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;

        float anim = animations.getOrDefault(id, 0f);
        float target = hovered ? 1f : 0f;
        anim += (target - anim) * HOVER_SPEED;
        animations.put(id, anim);

        int bgAlpha = (int) ((25 + 20 * anim) * alpha);
        int r = (int) (60 + (hoverColor.getRed() - 60) * anim);
        int g = (int) (60 + (hoverColor.getGreen() - 60) * anim);
        int b = (int) (60 + (hoverColor.getBlue() - 60) * anim);

        Render2D.rect(x, y, size, size, new Color(r, g, b, bgAlpha).getRGB(), 4);

        int iconAlpha = (int) ((150 + 105 * anim) * alpha);
        Fonts.GUI_ICONS.draw(icon, x + iconOffsetX, y + iconOffsetY, iconSize,
                new Color(200, 200, 200, iconAlpha).getRGB());
    }

    private void renderActionTooltips(CloudConfigEntry entry, float x, float y, float width,
                                       float mouseX, float mouseY, float alpha) {
        float buttonSize = 16f;
        float buttonY = y + (CONFIG_ITEM_HEIGHT - buttonSize) / 2 + 1;
        float deleteX = x + width - buttonSize - 6;
        float updateX = deleteX - buttonSize - 4;
        float loadX = updateX - buttonSize - 4;

        String tooltip = null;
        float tipX = 0;
        if (mouseX >= loadX && mouseX <= loadX + buttonSize && mouseY >= buttonY && mouseY <= buttonY + buttonSize) {
            tooltip = "Load";
            tipX = loadX + buttonSize / 2;
        } else if (mouseX >= updateX && mouseX <= updateX + buttonSize && mouseY >= buttonY && mouseY <= buttonY + buttonSize) {
            tooltip = "Save";
            tipX = updateX + buttonSize / 2;
        } else if (mouseX >= deleteX && mouseX <= deleteX + buttonSize && mouseY >= buttonY && mouseY <= buttonY + buttonSize) {
            tooltip = "Delete";
            tipX = deleteX + buttonSize / 2;
        }

        if (tooltip != null) {
            float tipSize = 4f;
            float tw = Fonts.BOLD.getWidth(tooltip, tipSize);
            float tx = tipX - tw / 2;
            float ty = buttonY - 8;
            int col = new Color(220, 220, 220, (int) (220 * alpha)).getRGB();
            Fonts.BOLD.draw(tooltip, tx, ty, tipSize, col);
        }
    }

    private void renderLaunchTooltip(CloudConfigEntry entry, float x, float y, float width,
                                      float mouseX, float mouseY, float alpha) {
        boolean hovered = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + CONFIG_ITEM_HEIGHT;

        boolean onButton = false;
        float buttonSize = 16f;
        float buttonY = y + (CONFIG_ITEM_HEIGHT - buttonSize) / 2 + 1;
        float deleteX = x + width - buttonSize - 6;
        float updateX = deleteX - buttonSize - 4;
        float loadX = updateX - buttonSize - 4;
        if ((mouseX >= loadX && mouseX <= loadX + buttonSize && mouseY >= buttonY && mouseY <= buttonY + buttonSize) ||
            (mouseX >= updateX && mouseX <= updateX + buttonSize && mouseY >= buttonY && mouseY <= buttonY + buttonSize) ||
            (mouseX >= deleteX && mouseX <= deleteX + buttonSize && mouseY >= buttonY && mouseY <= buttonY + buttonSize)) {
            onButton = true;
        }

        if (hovered && !onButton) {
            String text = dataHandler.isLaunchConfig(entry.id) ? "Right-click: remove auto-launch" : "Right-click: auto-launch on startup";
            float tipSize = 3.5f;
            float tw = Fonts.BOLD.getWidth(text, tipSize);
            float tx = x + width / 2 - tw / 2;
            float ty = y + CONFIG_ITEM_HEIGHT + 2;
            int col = new Color(160, 160, 160, (int) (200 * alpha)).getRGB();
            Fonts.BOLD.draw(text, tx, ty, tipSize, col);
        }
    }

    private void renderEmptyMessage(float x, float y, float alpha) {
        String text = "No configs found";
        float textWidth = Fonts.BOLD.getWidth(text, 6);
        Fonts.BOLD.draw(text, x + (ConfigsRenderer.PANEL_WIDTH - textWidth) / 2,
                y + ConfigsRenderer.PANEL_HEIGHT / 2, 6,
                new Color(100, 100, 100, (int) (150 * alpha)).getRGB());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY) {
        float listX = panelX + 8;
        float listY = panelY + 37;
        float listW = ConfigsRenderer.PANEL_WIDTH - 16;
        float listH = ConfigsRenderer.PANEL_HEIGHT - 45;

        if (dataHandler.isCreating()) {
            listH -= 40 * animationHandler.getCreateBoxAnimation();
        }

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            float itemY = listY + (float) dataHandler.getScrollOffset();

            for (CloudConfigEntry entry : dataHandler.getEntries()) {
                float itemAlpha = animationHandler.getItemAppearAnimation(entry.id);
                if (itemAlpha < 0.5f) {
                    itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
                    continue;
                }

                if (mouseY >= itemY && mouseY <= itemY + CONFIG_ITEM_HEIGHT) {
                    return handleItemClick(entry, mouseX, mouseY, button, listX, listW, itemY);
                }
                itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
            }
        }

        return false;
    }

    private boolean handleItemClick(CloudConfigEntry entry, double mouseX, double mouseY, int button,
                                    float listX, float listW, float itemY) {
        float buttonSize = 16f;
        float buttonYPos = itemY + (CONFIG_ITEM_HEIGHT - buttonSize) / 2 + 1;
        float deleteButtonX = listX + listW - buttonSize - 6;
        float refreshButtonX = deleteButtonX - buttonSize - 4;
        float loadButtonX = refreshButtonX - buttonSize - 4;

        if (mouseX >= loadButtonX && mouseX <= loadButtonX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize && button == 0) {
            dataHandler.loadConfig(entry.id);
            notificationRenderer.show("Loading: " + entry.name,
                    ConfigNotificationRenderer.NotificationType.SUCCESS);
            return true;
        }

        if (mouseX >= refreshButtonX && mouseX <= refreshButtonX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize && button == 0) {
            dataHandler.updateConfig(entry.id);
            notificationRenderer.show("Updating: " + entry.name,
                    ConfigNotificationRenderer.NotificationType.INFO);
            return true;
        }

        if (mouseX >= deleteButtonX && mouseX <= deleteButtonX + buttonSize &&
                mouseY >= buttonYPos && mouseY <= buttonYPos + buttonSize && button == 0) {
            dataHandler.deleteConfig(entry.id);
            notificationRenderer.show("Deleted: " + entry.name,
                    ConfigNotificationRenderer.NotificationType.SUCCESS);
            return true;
        }

        if (button == 1) {
            if (dataHandler.isLaunchConfig(entry.id)) {
                dataHandler.clearLaunchConfig();
                notificationRenderer.show("Removed auto-launch: " + entry.name,
                        ConfigNotificationRenderer.NotificationType.INFO);
            } else {
                dataHandler.setLaunchConfig(entry.id);
                notificationRenderer.show("Set auto-launch: " + entry.name,
                        ConfigNotificationRenderer.NotificationType.SUCCESS);
            }
            return true;
        }

        if (button == 0) {
            CloudConfigEntry current = dataHandler.getSelectedEntry();
            dataHandler.setSelectedEntry(entry.equals(current) ? null : entry);
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical,
                                 float panelX, float panelY) {
        if (mouseX >= panelX && mouseX <= panelX + ConfigsRenderer.PANEL_WIDTH &&
                mouseY >= panelY && mouseY <= panelY + ConfigsRenderer.PANEL_HEIGHT) {

            float visibleHeight = ConfigsRenderer.PANEL_HEIGHT - 45;
            if (dataHandler.isCreating()) {
                visibleHeight -= 40;
            }

            dataHandler.handleScroll(vertical, visibleHeight);
            return true;
        }

        return false;
    }
}