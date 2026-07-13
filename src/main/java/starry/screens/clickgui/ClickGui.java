package starry.screens.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import starry.IMinecraft;
import starry.Initialization;
import starry.util.SelfDestruct;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.ModuleStructure;
import starry.screens.clickgui.impl.DragHandler;

import starry.screens.clickgui.impl.background.BackgroundComponent;
import starry.screens.clickgui.impl.client.ClientSettingsRenderer;
import starry.screens.clickgui.impl.configs.ConfigsRenderer;
import starry.screens.clickgui.impl.module.ModuleComponent;
import starry.screens.clickgui.impl.settingsrender.BindComponent;
import starry.screens.clickgui.impl.settingsrender.TextComponent;
import starry.util.animations.Direction;
import starry.util.animations.GuiAnimation;
import starry.util.interfaces.AbstractSettingComponent;
import starry.util.math.FrameRateCounter;
import starry.util.render.Render2D;
import starry.util.render.shader.Scissor;
import starry.util.render.gif.GifRender;
import starry.util.render.font.Fonts;
import starry.util.ColorUtil;
import starry.util.theme.Theme;
import starry.util.theme.ThemeManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen implements IMinecraft {
    public static ClickGui INSTANCE = new ClickGui();
    private static final int FIXED_GUI_SCALE = 2;

    private final BackgroundComponent background = new BackgroundComponent();
    private final ModuleComponent moduleComponent = new ModuleComponent();
    private final ConfigsRenderer configsRenderer = new ConfigsRenderer();
    private final ClientSettingsRenderer clientSettingsRenderer = new ClientSettingsRenderer();
    private final DragHandler dragHandler = new DragHandler();
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;

    private final GuiAnimation openAnimation = new GuiAnimation();
    private boolean closing = false;
    private boolean destructChoiceOpen = false;

    private static int clickGuiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    public static int getClickGuiKey() { return clickGuiKey; }
    public static void setClickGuiKey(int key) { clickGuiKey = key; }

    private float hintAlphaAnimation = 0f;
    private long lastHintUpdateTime = System.currentTimeMillis();
    private static final float HINT_ANIM_SPEED = 6f;
    private static final float OFFSET_THRESHOLD = 5f;

    private int lastMouseX;
    private int lastMouseY;
    private float lastDelta;

    public ClickGui() {
        super(Text.of("MenuScreen"));
    }

    public boolean isClosing() {
        return closing;
    }

    @Override
    protected void init() {
        super.init();
        closing = false;
        destructChoiceOpen = false;
        openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
        hintAlphaAnimation = 0f;
        lastHintUpdateTime = System.currentTimeMillis();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        background.setSearchActive(false);
        updateModules();
    }

    private void updateModules() {
        List<ModuleStructure> modules = new ArrayList<>();
        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure m : repo.modules()) {
                    if (m.getCategory() == selectedCategory) modules.add(m);
                }
            }
        } catch (Exception ignored) {}
        moduleComponent.updateModules(modules, selectedCategory);
    }

    public void openGui() {
        if (!SelfDestruct.isSessionDisabled() && mc.currentScreen == null) {
            closing = false;
            openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
            mc.setScreen(this);
        }
    }

    @Override
    public void tick() {
        GifRender.tick();
        moduleComponent.tick();
        super.tick();
    }

    private float[] calculateBackground(float scale) {
        int vw = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int vh = mc.getWindow().getHeight() / FIXED_GUI_SCALE;
        float bgX = (vw - BackgroundComponent.BG_WIDTH) / 2f + dragHandler.getOffsetX();
        float bgY = (vh - BackgroundComponent.BG_HEIGHT) / 2f + dragHandler.getOffsetY();
        return new float[]{bgX, bgY, vw, vh};
    }

    private boolean isAnyBindListening() {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                return true;
            }
        }
        return false;
    }

    private void updateHintAnimation() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastHintUpdateTime) / 1000f, 0.1f);
        lastHintUpdateTime = currentTime;

        float offsetX = Math.abs(dragHandler.getOffsetX());
        float offsetY = Math.abs(dragHandler.getOffsetY());
        boolean shouldShow = (offsetX > OFFSET_THRESHOLD || offsetY > OFFSET_THRESHOLD);

        float target = shouldShow ? 1f : 0f;
        float diff = target - hintAlphaAnimation;

        if (Math.abs(diff) < 0.001f) {
            hintAlphaAnimation = target;
        } else {
            hintAlphaAnimation += diff * HINT_ANIM_SPEED * deltaTime;
            hintAlphaAnimation = Math.max(0f, Math.min(1f, hintAlphaAnimation));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastDelta = delta;

        FrameRateCounter.INSTANCE.recordFrame();

        if (closing && openAnimation.isFinished(Direction.BACKWARDS)) {
            closing = false;
            TextComponent.typing = false;
            moduleComponent.setBindingModule(null);
            dragHandler.stopDrag();
            mc.currentScreen = null;
        }
    }

    public void renderOverlay(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.getWindow() == null) return;

        float delta = lastDelta;
        int mouseX = lastMouseX;
        int mouseY = lastMouseY;

        float scrollSpeed = Math.min(1f, 60f / Math.max(FrameRateCounter.INSTANCE.getFps(), 1));
        float animValue = openAnimation.getOutput().floatValue();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        context.createNewRootLayer();

        int dimAlpha = (int) (125 * animValue);
        if (dimAlpha > 0) {
            Render2D.rect(0, 0, 5000, 5000, new Color(0, 0, 0, dimAlpha).getRGB(), 0);
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;

        float mx = mouseX / scale, my = mouseY / scale;

        if (!closing) {
            dragHandler.update(mx, my);
        }

        updateHintAnimation();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float[] bg = calculateBackground(scale);
        float bgX = bg[0];
        float bgY = bg[1];
        int vw = (int) bg[2];
        int vh = (int) bg[3];

        float yOffset;
        if (closing) {
            yOffset = (1f - animValue) * 30f;
        } else {
            yOffset = (1f - animValue) * -15f;
        }
        bgY += yOffset;

        float alphaMultiplier = animValue;

        context.getMatrices().pushMatrix();

        background.render(context, bgX, bgY, selectedCategory, delta, alphaMultiplier);
        background.renderCategoryPanel(bgX, bgY, alphaMultiplier, mx, my);
        background.renderHeader(bgX, bgY, selectedCategory, alphaMultiplier);
        background.renderCategoryNames(bgX, bgY, selectedCategory, alphaMultiplier);

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 46f;
        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 46f;

        boolean overlayActive = clientSettingsRenderer.isActive() || configsRenderer.isActive();
        float normalAlpha = background.getNormalPanelAlpha();
        float searchAlpha = background.getSearchPanelAlpha();

        if (normalAlpha > 0.01f) {
            if (!overlayActive) {
                moduleComponent.updateScroll(delta, scrollSpeed);
                moduleComponent.updateScrollFades(delta, scrollSpeed, mlH, spH);
                moduleComponent.renderModuleList(context, mlX, mlY, mlW, mlH, mx, my, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
                moduleComponent.renderSettingsPanel(context, spX, spY, spW, spH, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
            }
            configsRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);
        }

        if (searchAlpha > 0.01f) {
            background.renderSearchResults(context, bgX, bgY, mx, my, FIXED_GUI_SCALE, alphaMultiplier);
        }

        Scissor.reset();

        clientSettingsRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier);

        if (destructChoiceOpen) renderDestructChoices(bgX, bgY, mx, my, alphaMultiplier);

        context.getMatrices().popMatrix();

        float finalHintAlpha = hintAlphaAnimation * alphaMultiplier;
        if (finalHintAlpha > 0.01f) {
            int hintAlpha = (int) (255 * finalHintAlpha);
            float centerX = vw / 2f;
            float centerY = vh / 2f;
            float textY = centerY + BackgroundComponent.BG_HEIGHT / 2f + 10f;
//            Fonts.TEST.drawCentered("Press CTRL + ALT to reset position", centerX, textY + 65, 6, new Color(150, 150, 150, hintAlpha).getRGB());
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) return false;

        if (destructChoiceOpen) {
            int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
            float scale = (float) FIXED_GUI_SCALE / guiScale;
            double mx = click.x() / scale, my = click.y() / scale;
            float[] bg = calculateBackground(scale);
            float modalX = bg[0] + 90f, modalY = bg[1] + 71f;
            float buttonY = modalY + 70f;
            if (click.button() == 0 && my >= buttonY && my <= buttonY + 24f) {
                if (mx >= modalX + 12f && mx <= modalX + 104f) {
                    destructChoiceOpen = false;
                    SelfDestruct.destruct();
                } else if (mx >= modalX + 116f && mx <= modalX + 208f) {
                    destructChoiceOpen = false;
                    SelfDestruct.disableForSession();
                }
            }
            return true;
        }

        // If a module is in binding mode, any mouse button sets the bind
        if (moduleComponent.getBindingModule() != null) {
            ModuleStructure m = moduleComponent.getBindingModule();
            m.setKey(click.button());
            m.setType(0);
            moduleComponent.setBindingModule(null);
            return true;
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = click.x() / scale, my = click.y() / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        boolean overlayActive = clientSettingsRenderer.isActive() || configsRenderer.isActive();

        if (!overlayActive) {
            int bottomBtn = background.getBottomButtonAt(mx, my, bgX, bgY);
            if (bottomBtn >= 0 && click.button() == 0) {
                if (bottomBtn == 0) {
                    destructChoiceOpen = true;
                } else if (bottomBtn == 1) {
                    clientSettingsRenderer.toggle();
                } else if (bottomBtn == 2) {
                    configsRenderer.toggle();
                }
                return true;
            }
        }

        if (clientSettingsRenderer.mouseClicked(mx, my, click.button(), bgX, bgY)) {
            return true;
        }

        if (configsRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
            return true;
        }

        if (background.isSearchBoxHovered(mx, my, bgX, bgY) && click.button() == 0) {
            background.setSearchActive(true);
            return true;
        }

        if (background.isSearchActive()) {
            if (click.button() == 0) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    searchModule.switchState();
                    return true;
                }

                float panelX = bgX + 92f;
                float panelY = bgY + 38f;
                float panelW = BackgroundComponent.BG_WIDTH - 100f;
                float panelH = BackgroundComponent.BG_HEIGHT - 46f;

                if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
                    return true;
                }

                if (!background.isSearchBoxHovered(mx, my, bgX, bgY)) {
                    background.setSearchActive(false);
                }
            } else if (click.button() == 1) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    background.setSearchActive(false);
                    selectedCategory = searchModule.getCategory();
                    moduleComponent.selectModuleFromSearch(searchModule);
                    updateModules();
                    return true;
                }
            }
            return true;
        }

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;

        if (click.button() == 2) {
            if (isAnyBindListening()) {
                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                        bindComponent.handleMiddleMouseBind();
                        return true;
                    }
                }
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
            if (module != null) {
                moduleComponent.setBindingModule(module);
                return true;
            }

            if (dragHandler.startDrag(mx, my, bgX, bgY, BackgroundComponent.BG_WIDTH, BackgroundComponent.BG_HEIGHT)) {
                return true;
            }
        }

        ModuleCategory cat = background.getCategoryAtPosition(mx, my, bgX, bgY);
        if (cat != null) {
            if (clientSettingsRenderer.isActive()) clientSettingsRenderer.toggle();
            if (configsRenderer.isActive()) configsRenderer.toggle();
            selectedCategory = cat;
            updateModules();
            return true;
        }

        if (!overlayActive) {
            ModuleStructure starModule = moduleComponent.getModuleForStarClick(mx, my, mlX, mlY, mlW, mlH);
            if (starModule != null && click.button() == 0) {
                moduleComponent.toggleFavorite(starModule);
                return true;
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
            if (module != null) {
                if (click.button() == 0) module.switchState();
                else if (click.button() == 1) moduleComponent.selectModule(module);
                return true;
            }

            float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
            if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
                if (moduleComponent.getSelectedModule() != null
                        && moduleComponent.getSettingsRenderer().isHeaderBindBoxClicked((float) mx, (float) my)) {
                    moduleComponent.setBindingModule(moduleComponent.getSelectedModule());
                    return true;
                }

                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (c.getSetting().isVisible() && c.mouseClicked(mx, my, click.button())) return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (closing) return false;

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            configsRenderer.mouseReleased(click.x(), click.y(), click.button());
//        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.mouseReleased(click.x(), click.y(), click.button())) {
                return true;
            }
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) return false;

        if (isAnyBindListening()) {
            for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleScrollBind(vertical);
                    return true;
                }
            }
        }

        if (moduleComponent.getBindingModule() != null) {
            ModuleStructure m = moduleComponent.getBindingModule();
            if (vertical > 0) {
                m.setKey(BindComponent.SCROLL_UP_BIND);
            } else {
                m.setKey(BindComponent.SCROLL_DOWN_BIND);
            }
            m.setType(2);
            moduleComponent.setBindingModule(null);
            return true;
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = mouseX / scale, my = mouseY / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (background.isSearchActive()) {
            float panelX = bgX + 92f;
            float panelY = bgY + 38f;
            float panelW = BackgroundComponent.BG_WIDTH - 100f;
            float panelH = BackgroundComponent.BG_HEIGHT - 46f;

            if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
                background.handleSearchScroll(vertical, panelH);
                return true;
            }
        }

        if (configsRenderer.isActive()) {
            if (configsRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
                return true;
            }
        }

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= mlX && mx <= mlX + mlW && my >= mlY && my <= mlY + mlH) {
            moduleComponent.handleModuleScroll(vertical, mlH);
            return true;
        }

        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
            for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                if (c.getSetting().isVisible() && c.mouseScrolled(mx, my, vertical)) return true;
            }
            moduleComponent.handleSettingScroll(vertical, spH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (destructChoiceOpen) {
                destructChoiceOpen = false;
                return true;
            }
            if (configsRenderer.isActive()) {
                configsRenderer.toggle();
                return true;
            }
            if (clientSettingsRenderer.isActive()) {
                clientSettingsRenderer.toggle();
                return true;
            }
            if (background.isSearchActive()) {
                background.setSearchActive(false);
                return true;
            }
            if (moduleComponent.getBindingModule() != null) {
                moduleComponent.getBindingModule().setKey(GLFW.GLFW_KEY_UNKNOWN);
                moduleComponent.getBindingModule().setType(1);
                moduleComponent.setBindingModule(null);
                return true;
            }
            close();
            return true;
        }

        if (closing) return false;

        if (clientSettingsRenderer.keyPressed(input.key())) {
            return true;
        }

        if (configsRenderer.keyPressed(input.key(), input.scancode(), input.modifiers())) {
            return true;
        }

        if (background.isSearchActive()) {
            if (background.handleSearchKey(input.key())) {
                return true;
            }
        }

        if (dragHandler.isResetNeeded(input.key(), input.modifiers())) {
            dragHandler.reset();
            return true;
        }

        ModuleStructure binding = moduleComponent.getBindingModule();
        if (binding != null) {
            binding.setKey(input.key() == GLFW.GLFW_KEY_DELETE ? GLFW.GLFW_KEY_UNKNOWN : input.key());
            binding.setType(input.key() == GLFW.GLFW_KEY_DELETE ? 1 : 1);
            moduleComponent.setBindingModule(null);
            return true;
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (closing) return false;

        if (configsRenderer.charTyped((char) input.codepoint(), input.modifiers())) {
            return true;
        }

        if (background.isSearchActive()) {
            if (background.handleSearchChar((char) input.codepoint())) {
                return true;
            }
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.charTyped((char) input.codepoint(), input.modifiers())) return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void startActualClose() {
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;

        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        TextComponent.typing = false;
        moduleComponent.setBindingModule(null);
        background.setSearchActive(false);
        if (clientSettingsRenderer.isActive()) clientSettingsRenderer.toggle();
        if (configsRenderer.isActive()) configsRenderer.toggle();
        dragHandler.stopDrag();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            startActualClose();
        }
    }

    private void renderDestructChoices(float bgX, float bgY, float mouseX, float mouseY, float alphaMultiplier) {
        Theme theme = ThemeManager.getTheme();
        float x = bgX + 90f, y = bgY + 71f, width = 220f, height = 108f;
        Render2D.rect(bgX, bgY, BackgroundComponent.BG_WIDTH, BackgroundComponent.BG_HEIGHT,
                new Color(0, 0, 0, (int) (ThemeManager.getSelfDestructDim() * alphaMultiplier)).getRGB(), 15);
        Render2D.rect(x, y, width, height, ColorUtil.multAlpha(theme.panelBg, alphaMultiplier), 7);
        Render2D.outline(x, y, width, height, 0.7f, ColorUtil.multAlpha(theme.outline, alphaMultiplier), 7);
        Render2D.rect(x, y, 3f, height, ColorUtil.multAlpha(theme.accent, alphaMultiplier), 7);

        Render2D.rect(x + 13, y + 13, 22, 22, ColorUtil.multAlpha(theme.panelBg2, alphaMultiplier), 5);
        Render2D.outline(x + 13, y + 13, 22, 22, 0.7f, ColorUtil.multAlpha(theme.accent, alphaMultiplier), 5);
        Fonts.BOLD.drawCentered("!", x + 24f, y + 21f, 7, ColorUtil.multAlpha(theme.accent, alphaMultiplier));
        Fonts.BOLD.draw("Self Destruct", x + 43f, y + 13f, 7.5f, ColorUtil.multAlpha(theme.text, alphaMultiplier));
        Fonts.REGULAR.draw("Choose a shutdown mode", x + 43f, y + 26f, 5f, ColorUtil.multAlpha(theme.text2, alphaMultiplier));

        if (ThemeManager.isSelfDestructDetails()) {
            Fonts.REGULAR.draw("Full removes data  •  Session lasts until restart", x + 13f, y + 47f, 4.6f,
                    ColorUtil.multAlpha(theme.text2, alphaMultiplier));
        }

        float buttonY = y + 70f;
        boolean fullHover = mouseX >= x + 12 && mouseX <= x + 104 && mouseY >= buttonY && mouseY <= buttonY + 24;
        boolean sessionHover = mouseX >= x + 116 && mouseX <= x + 208 && mouseY >= buttonY && mouseY <= buttonY + 24;
        String style = ThemeManager.getSelfDestructStyle();
        int fullColor = style.equals("Minimal") ? theme.panelBg2 : fullHover ? 0xFFD95662 : 0xFFB63F4A;
        int sessionColor = style.equals("Danger") ? (sessionHover ? 0xFFD79A42 : 0xFFAD7630)
                : style.equals("Minimal") ? theme.panelBg2 : sessionHover ? ColorUtil.lightenColor(theme.accent, 1.15f) : theme.accent;
        Render2D.rect(x + 12f, buttonY, 92f, 24f, ColorUtil.multAlpha(fullColor, alphaMultiplier), 5);
        Render2D.rect(x + 116f, buttonY, 92f, 24f, ColorUtil.multAlpha(sessionColor, alphaMultiplier), 5);
        if (style.equals("Minimal")) {
            Render2D.outline(x + 12f, buttonY, 92f, 24f, 0.7f, ColorUtil.multAlpha(0xFFB63F4A, alphaMultiplier), 5);
            Render2D.outline(x + 116f, buttonY, 92f, 24f, 0.7f, ColorUtil.multAlpha(theme.accent, alphaMultiplier), 5);
        }
        Fonts.BOLD.drawCentered("Full Destruct", x + 58f, buttonY + 9f, 5, ColorUtil.multAlpha(theme.text, alphaMultiplier));
        Fonts.BOLD.drawCentered("Disable Session", x + 162f, buttonY + 9f, 5, ColorUtil.multAlpha(theme.text, alphaMultiplier));
    }
}
