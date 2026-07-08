package starry.screens.hud;

import net.minecraft.client.gui.DrawContext;
import starry.Initialization;
import starry.client.draggables.AbstractHudElement;
import starry.modules.module.ModuleStructure;
import starry.util.render.Render2D;
import starry.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import starry.modules.impl.render.*;
import starry.util.string.StringHelper;

public class Arraylist extends AbstractHudElement {

    private List<ModuleStructure> sortedModules = new ArrayList<>();
    private long lastUpdateTime = System.currentTimeMillis();
    private float animatedWidth = 80;
    private float animatedHeight = 23;

    public Arraylist() {
        super("ArrayList", 300, 400, 80, 23, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        if (Initialization.getInstance() == null ||
                Initialization.getInstance().getManager() == null ||
                Initialization.getInstance().getManager().getModuleProvider() == null) {
            return;
        }

        k9rp40 hud = k9rp40.getInstance();
        if (hud == null) return;

        List<ModuleStructure> enabled = Initialization.getInstance().getManager().getModuleProvider().getModuleStructures().stream()
                .filter(ModuleStructure::isState)
                .toList();

        boolean alphabetical = hud.arraylistOrder.isSelected("Alphabetical");
        boolean reverse = hud.arraylistReverse.isValue();

        Comparator<ModuleStructure> cmp;
        if (alphabetical) {
            cmp = Comparator.comparing(ModuleStructure::getName, String.CASE_INSENSITIVE_ORDER);
        } else {
            cmp = Comparator.comparingInt(m -> m.getName().length());
        }
        if (reverse) cmp = cmp.reversed();

        sortedModules = new ArrayList<>(enabled);
        sortedModules.sort(cmp);

        if (!enabled.isEmpty() || isChat(mc.currentScreen)) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    public boolean visible() {
        return true;
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * 8.0f));
        return current + (target - current) * factor;
    }

    private float getModuleNameWidth(ModuleStructure module) {
        return Fonts.BOLD.getWidth(module.getName(), 6);
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        float targetWidth = 80;
        float targetHeight = 23;

        if (isChat(mc.currentScreen) && sortedModules.isEmpty()) {
            targetHeight = 34;
            String example = "ArrayList";
            float ew = Fonts.BOLD.getWidth(example, 6);
            targetWidth = Math.max(ew + 30, targetWidth);
        } else {
            targetHeight = 23 + sortedModules.size() * 11f;
            for (ModuleStructure module : sortedModules) {
                targetWidth = Math.max(getModuleNameWidth(module) + 30, targetWidth);
            }
        }

        animatedWidth = lerp(animatedWidth, targetWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, targetHeight, deltaTime);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) animatedWidth = targetWidth;
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) animatedHeight = targetHeight;

        setWidth((int) Math.ceil(animatedWidth));
        setHeight((int) Math.ceil(animatedHeight));

        int bgAlpha = (int) (255 * alphaFactor);

        Render2D.gradientRect(x, y, getWidth(), animatedHeight,
                new int[]{
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(32, 32, 32, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(32, 32, 32, bgAlpha).getRGB()
                },
                5);
        Render2D.outline(x, y, getWidth(), animatedHeight, 0.35f, new Color(90, 90, 90, bgAlpha).getRGB(), 5);

        Fonts.BOLD.draw(StringHelper.decrypt(new byte[]{11, (byte)-79, 45, (byte)-119, 107, (byte)-41, 94, (byte)-91, 62}), x + 8, y + 6.5f, 6, new Color(255, 255, 255, bgAlpha).getRGB());

        int offset = 23;
        for (ModuleStructure module : sortedModules) {
            float nameW = getModuleNameWidth(module);
            Render2D.rect(x + 8, y + offset - 1, 1f, 7,
                    new Color(155, 155, 155, (int) (128 * alphaFactor)).getRGB(), 1);
            Fonts.BOLD.draw(module.getName(), x + 13, y + offset - 1.5f, 6,
                    new Color(255, 255, 255, bgAlpha).getRGB());
            offset += 11;
        }

        if (sortedModules.isEmpty() && isChat(mc.currentScreen)) {
            Fonts.BOLD.draw(StringHelper.decrypt(new byte[]{4, (byte)-84, 127, (byte)-123, 125, (byte)-1, 66, (byte)-70, 47, (byte)-80, 127, (byte)-115, 124, (byte)-6, 85, (byte)-70, 47, (byte)-89}), x + 13, y + 23, 5,
                    new Color(140, 140, 140, bgAlpha).getRGB());
        }
    }
}