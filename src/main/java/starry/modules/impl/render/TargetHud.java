package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.render.Render2D;
import starry.util.render.font.Fonts;
import starry.util.render.item.ItemRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetHud extends ModuleStructure {
    static final Identifier STEVE_SKIN = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    static final long TIMEOUT = 10000;

    SelectSetting style = new SelectSetting("Style", "Changes the full Target HUD layout")
            .value("Full", "Clean", "Minimal")
            .selected("Full");
    SliderSettings xCoord = new SliderSettings("X", "Horizontal location").setValue(0f).range(0f, 1920f);
    SliderSettings yCoord = new SliderSettings("Y", "Vertical location").setValue(35.1f).range(0f, 1080f);
    SliderSettings size = new SliderSettings("Size", "HUD scale").setValue(0.8f).range(0.65f, 1.8f);
    SliderSettings width = new SliderSettings("Width", "HUD width").setValue(224f).range(140f, 320f);
    SliderSettings height = new SliderSettings("Height", "HUD height").setValue(68f).range(46f, 112f);
    ColorSetting accentColor = new ColorSetting("Accent Color", "Outline and detail color")
            .setColor(new Color(40, 255, 105, 230).getRGB());
    ColorSetting secondaryColor = new ColorSetting("Second Color", "Animated accent and sparkle color")
            .setColor(new Color(80, 185, 255, 220).getRGB());
    ColorSetting backgroundColor = new ColorSetting("Background Color", "Panel background color")
            .setColor(new Color(12, 18, 16, 192).getRGB());
    ColorSetting healthColor = new ColorSetting("Health Color", "Health bar color")
            .setColor(new Color(55, 255, 80, 255).getRGB());
    ColorSetting textColor = new ColorSetting("Text Color", "Name and health text color")
            .setColor(new Color(255, 255, 255, 255).getRGB());
    BooleanSetting hudTimeout = new BooleanSetting("Timeout", "Hide after not attacking for a while").setValue(true);
    BooleanSetting dynamicHealthColor = new BooleanSetting("Dynamic Health", "Use green/yellow/red health colors").setValue(false);
    BooleanSetting showWinTag = new BooleanSetting("Win Tag", "Show the WIN pill like the reference").setValue(true);
    BooleanSetting showArmor = new BooleanSetting("Show Armor", "Render armor and held item icons").setValue(true);
    BooleanSetting showHeldItem = new BooleanSetting("Held Item", "Render the target's main hand item before armor").setValue(true);
    BooleanSetting heartIcon = new BooleanSetting("Heart Icon", "Render a heart beside health").setValue(true);
    BooleanSetting animatedBar = new BooleanSetting("Animated Bar", "Smoothly animate health changes").setValue(true);
    BooleanSetting pulse = new BooleanSetting("Pulse", "Subtle breathing animation around the HUD").setValue(true);
    BooleanSetting sparkles = new BooleanSetting("Sparkles", "Animated sparkle particles around the panel").setValue(true);
    BooleanSetting glow = new BooleanSetting("Glow", "Render a soft colored outline glow").setValue(true);
    BooleanSetting decorations = new BooleanSetting("Decorations", "Render animated rails and corner accents").setValue(true);
    SliderSettings sparkleCount = new SliderSettings("Sparkle Count", "How many sparkles render around the HUD").setValue(10f).range(0f, 24f);
    SliderSettings animationSpeed = new SliderSettings("Anim Speed", "Speed for pulse, sparkles, and health smoothing").setValue(1f).range(0.25f, 3f);
    SliderSettings glowStrength = new SliderSettings("Glow Strength", "Size of the animated outline glow").setValue(3f).range(0f, 8f);

    long lastAttackTime;
    PlayerEntity lastTarget;
    PlayerEntity displayedTarget;
    float displayedHealthPct = 1f;

    public TargetHud() {
        super("Target HUD", ModuleCategory.RENDER);
        settings(style, xCoord, yCoord, size, width, height, accentColor, secondaryColor, backgroundColor, healthColor, textColor,
                dynamicHealthColor, showArmor, showHeldItem, heartIcon, animatedBar, pulse, sparkles, sparkleCount, glow, glowStrength,
                decorations, animationSpeed, showWinTag, hudTimeout);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.world == null) return;
        PlayerEntity player = findHudTarget();
        if (player == null) return;
        if (hudTimeout.isValue() && System.currentTimeMillis() - lastAttackTime > TIMEOUT) return;

        float scale = size.getValue();
        float x = xCoord.getValue();
        float y = yCoord.getValue();
        float w = width.getValue() * scale;
        float h = height.getValue() * scale;
        float healthPct = animatedHealthPct(player);
        float pulseOffset = pulse.isValue() ? pulseAmount() * scale : 0f;
        x -= pulseOffset;
        y -= pulseOffset;
        w += pulseOffset * 2f;
        h += pulseOffset * 2f;

        if (style.isSelected("Clean")) {
            drawClean(event.getDrawContext(), player, x, y, w, h, scale, healthPct);
        } else if (style.isSelected("Minimal")) {
            drawMinimal(player, x, y, w, h, scale, healthPct);
        } else {
            drawReference(event.getDrawContext(), player, x, y, w, h, scale, healthPct);
        }
    }

    private PlayerEntity findHudTarget() {
        if (mc.player.getAttacking() instanceof PlayerEntity attacked && isValidHudTarget(attacked)) {
            lastTarget = attacked;
            lastAttackTime = System.currentTimeMillis();
            return attacked;
        }

        if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity crosshairPlayer
                && isValidHudTarget(crosshairPlayer)) {
            lastTarget = crosshairPlayer;
            lastAttackTime = System.currentTimeMillis();
            return crosshairPlayer;
        }

        if (isValidHudTarget(lastTarget)) {
            return lastTarget;
        }

        lastTarget = null;
        return null;
    }

    private boolean isValidHudTarget(PlayerEntity player) {
        return player != null && player != mc.player && player.isAlive() && !player.isSpectator();
    }

    private void drawReference(DrawContext context, PlayerEntity player, float x, float y, float w, float h, float scale, float healthPct) {
        float radius = 10f * scale;
        int bg = backgroundColor.getColor();
        int accent = accentColor.getColor();
        int second = secondaryColor.getColor();
        int text = textColor.getColor();
        float pad = 9f * scale;
        float faceSize = Math.min(h - pad * 2f, 44f * scale);
        float barH = Math.max(6f, 7.5f * scale);
        float health = player.getHealth() + player.getAbsorptionAmount();

        if (glow.isValue()) {
            float glowSize = glowStrength.getValue() * scale + pulseAmount() * scale;
            Render2D.glowOutline(x - glowSize, y - glowSize, w + glowSize * 2f, h + glowSize * 2f,
                    Math.max(0.5f, glowSize), ColorUtil.multAlpha(animatedAccent(accent, second), 0.46f), radius + glowSize, 1f, 0.85f);
        }
        Render2D.blur(x, y, w, h, 7f * scale, radius, ColorUtil.multAlpha(bg, 0.72f));
        Render2D.gradientRect(x, y, w, h, new int[]{
                ColorUtil.multBright(bg, 1.18f), bg, ColorUtil.multDark(bg, 0.8f), ColorUtil.multDark(bg, 0.7f)
        }, radius);
        Render2D.outline(x, y, w, h, Math.max(0.6f, 1.15f * scale), animatedAccent(accent, second), radius);
        if (decorations.isValue()) drawDecorations(x, y, w, h, scale);
        if (sparkles.isValue()) drawSparkles(x, y, w, h, scale);

        if (showWinTag.isValue()) {
            float tagW = 38f * scale;
            float tagH = 14f * scale;
            Render2D.rect(x + pad, y - tagH - 5f * scale, tagW, tagH, new Color(0, 40, 18, 215).getRGB(), 7f * scale);
            Fonts.BOLD.draw("WIN", x + pad + 8f * scale, y - tagH - 2.5f * scale, 6f * scale, new Color(70, 255, 120, 255).getRGB());
        }

        drawFace(player, x + pad, y + pad, faceSize, 8f * scale);

        float infoX = x + pad + faceSize + 12f * scale;
        float infoW = Math.max(28f * scale, w - (infoX - x) - pad);
        float nameY = y + 10f * scale;
        String hp = (heartIcon.isValue() ? "\u2764 " : "") + String.format("%.1f", health);
        float hpW = Fonts.BOLD.getWidth(hp, 7.6f * scale);
        float hpX = x + w - pad - hpW - 5f * scale;
        float nameW = Math.max(42f * scale, hpX - infoX - 10f * scale);
        Render2D.rect(infoX - 5f * scale, nameY - 3f * scale, nameW + 10f * scale, 16f * scale, new Color(10, 10, 12, 142).getRGB(), 5f * scale);
        Fonts.BOLD.draw(trimName(player.getName().getString(), nameW, 7.2f * scale), infoX, nameY, 7.2f * scale, text);

        Render2D.rect(hpX - 4f * scale, nameY - 3f * scale, hpW + 8f * scale, 16f * scale,
                ColorUtil.multAlpha(selectedHealthColor(healthPct), 0.16f), 5f * scale);
        Fonts.BOLD.draw(hp, hpX, nameY, 7.6f * scale, heartIcon.isValue() ? selectedHealthColor(healthPct) : text);

        float barX = infoX;
        float barY = y + 31f * scale;
        float barW = Math.max(25f * scale, w - (barX - x) - pad);
        Render2D.rect(barX, barY, barW, barH, new Color(12, 16, 16, 210).getRGB(), barH / 2f);
        Render2D.rect(barX, barY, barW * healthPct, barH, selectedHealthColor(healthPct), barH / 2f);
        Render2D.rect(barX + Math.max(0f, barW * healthPct - 2f * scale), barY - 1f * scale, 2.2f * scale, barH + 2f * scale,
                ColorUtil.multAlpha(text, healthPct > 0.04f ? 0.5f : 0f), 2f * scale);

        if (showArmor.isValue()) {
            drawEquipment(context, player, barX, y + h - pad - 14f * scale, scale);
        }
    }

    private void drawClean(DrawContext context, PlayerEntity player, float x, float y, float w, float h, float scale, float healthPct) {
        int bg = backgroundColor.getColor();
        int accent = accentColor.getColor();
        int text = textColor.getColor();
        float pad = 7f * scale;
        float face = Math.max(24f * scale, h - pad * 2f);

        Render2D.gradientRect(x, y, w, h, new int[]{
                ColorUtil.multBright(bg, 1.15f), bg, bg, ColorUtil.multDark(bg, 0.75f)
        }, 8f * scale);
        if (sparkles.isValue()) drawSparkles(x, y, w, h, scale);
        Render2D.outline(x, y, w, h, Math.max(0.5f, scale), accent, 8f * scale);
        drawFace(player, x + pad, y + pad, face, 5f * scale);

        float textX = x + pad + face + 8f * scale;
        Fonts.BOLD.draw(trimName(player.getName().getString(), w - (textX - x) - pad, 8f * scale), textX, y + 8f * scale, 8f * scale, text);
        Fonts.REGULAR.draw((heartIcon.isValue() ? "\u2764 " : "") + String.format("%.1f HP", player.getHealth() + player.getAbsorptionAmount()), textX, y + 22f * scale, 6f * scale, ColorUtil.multAlpha(text, 0.82f));

        float barX = textX;
        float barY = y + h - pad - 6f * scale;
        float barW = w - (barX - x) - pad;
        Render2D.rect(barX, barY, barW, 5f * scale, ColorUtil.multAlpha(bg, 0.55f), 3f * scale);
        Render2D.rect(barX, barY, barW * healthPct, 5f * scale, selectedHealthColor(healthPct), 3f * scale);
        if (showArmor.isValue()) drawEquipment(context, player, textX, y + h - 18f * scale, scale * 0.82f);
    }

    private void drawMinimal(PlayerEntity player, float x, float y, float w, float h, float scale, float healthPct) {
        int bg = backgroundColor.getColor();
        int text = textColor.getColor();
        float barH = Math.max(5f, h * 0.22f);

        Render2D.rect(x, y, w, h, bg, 5f * scale);
        Render2D.outline(x, y, w, h, Math.max(0.45f, 0.8f * scale), accentColor.getColor(), 5f * scale);
        Fonts.BOLD.draw(trimName(player.getName().getString(), w - 18f * scale, 7f * scale), x + 7f * scale, y + 5f * scale, 7f * scale, text);
        Render2D.rect(x + 7f * scale, y + h - barH - 6f * scale, w - 14f * scale, barH, ColorUtil.multAlpha(bg, 0.55f), barH / 2f);
        Render2D.rect(x + 7f * scale, y + h - barH - 6f * scale, (w - 14f * scale) * healthPct, barH, selectedHealthColor(healthPct), barH / 2f);
    }

    private void drawEquipment(DrawContext context, PlayerEntity player, float x, float y, float scale) {
        List<ItemStack> items = new ArrayList<>();
        if (showHeldItem.isValue() && !player.getMainHandStack().isEmpty()) items.add(player.getMainHandStack());

        addArmorItem(items, player, EquipmentSlot.HEAD);
        addArmorItem(items, player, EquipmentSlot.CHEST);
        addArmorItem(items, player, EquipmentSlot.LEGS);
        addArmorItem(items, player, EquipmentSlot.FEET);

        float itemScale = Math.max(0.56f, 0.72f * scale);
        float slot = 18f * scale;
        for (int i = 0; i < items.size(); i++) {
            float itemX = x + i * slot;
            Render2D.rect(itemX - 2f * scale, y - 2f * scale, 15f * scale, 15f * scale,
                    new Color(7, 10, 10, 130).getRGB(), 3.5f * scale);
            Render2D.outline(itemX - 2f * scale, y - 2f * scale, 15f * scale, 15f * scale,
                    0.45f * scale, ColorUtil.multAlpha(animatedAccent(accentColor.getColor(), secondaryColor.getColor()), 0.32f), 3.5f * scale);
            ItemRender.drawItemWithContext(context, items.get(i), itemX - 0.8f * scale, y - 0.8f * scale, itemScale, 1f);
        }
    }

    private void addArmorItem(List<ItemStack> items, PlayerEntity player, EquipmentSlot slot) {
        ItemStack stack = player.getEquippedStack(slot);
        if (!stack.isEmpty()) items.add(stack);
    }

    private void drawSparkles(float x, float y, float w, float h, float scale) {
        int count = Math.round(sparkleCount.getValue());
        if (count <= 0) return;

        long now = System.currentTimeMillis();
        int accent = accentColor.getColor();
        int second = secondaryColor.getColor();
        float speed = animationSpeed.getValue();
        for (int i = 0; i < count; i++) {
            float t = ((now * 0.00022f * speed) + i * 0.137f) % 1f;
            int edge = i % 4;
            float sx;
            float sy;
            float drift = (float) Math.sin(now * 0.0017f * speed + i * 1.9f) * 3.5f * scale;
            if (edge == 0) {
                sx = x + 12f * scale + (w - 24f * scale) * t;
                sy = y - 4f * scale + drift;
            } else if (edge == 1) {
                sx = x + w + 4f * scale + drift;
                sy = y + 10f * scale + (h - 20f * scale) * t;
            } else if (edge == 2) {
                sx = x + 12f * scale + (w - 24f * scale) * (1f - t);
                sy = y + h + 4f * scale + drift;
            } else {
                sx = x - 4f * scale + drift;
                sy = y + 10f * scale + (h - 20f * scale) * (1f - t);
            }
            float twinkle = 0.25f + 0.75f * (float) Math.pow(Math.abs(Math.sin((now * 0.0048f * speed) + i * 0.91f)), 1.8);
            int color = ColorUtil.multAlpha(i % 2 == 0 ? accent : second, twinkle);
            float s = (1.7f + (i % 3) * 0.7f) * scale;
            Render2D.rect(sx - s * 0.5f, sy - s * 0.5f, s, s, ColorUtil.multAlpha(color, 0.45f), s * 0.35f);
            Render2D.rect(sx - s, sy - 0.35f * scale, s * 2f, 0.7f * scale, color, s);
            Render2D.rect(sx - 0.35f * scale, sy - s, 0.7f * scale, s * 2f, color, s);
        }
    }

    private void drawDecorations(float x, float y, float w, float h, float scale) {
        long now = System.currentTimeMillis();
        int accent = accentColor.getColor();
        int second = secondaryColor.getColor();
        int animated = animatedAccent(accent, second);
        float speed = animationSpeed.getValue();
        float sweep = ((now * 0.00018f * speed) % 1f) * w;
        float railH = Math.max(1f, 1.4f * scale);
        float corner = 18f * scale;

        Render2D.rect(x + 10f * scale, y + 4f * scale, w - 20f * scale, railH, ColorUtil.multAlpha(accent, 0.15f), railH);
        Render2D.rect(x + 10f * scale + sweep * 0.55f, y + 4f * scale, Math.min(36f * scale, w - 20f * scale), railH,
                ColorUtil.multAlpha(animated, 0.75f), railH);
        Render2D.rect(x + 10f * scale, y + h - 5.4f * scale, w - 20f * scale, railH, ColorUtil.multAlpha(second, 0.12f), railH);

        Render2D.rect(x + 5f * scale, y + 5f * scale, corner, 1.2f * scale, ColorUtil.multAlpha(accent, 0.7f), 1f * scale);
        Render2D.rect(x + 5f * scale, y + 5f * scale, 1.2f * scale, corner, ColorUtil.multAlpha(accent, 0.7f), 1f * scale);
        Render2D.rect(x + w - 5f * scale - corner, y + h - 6.2f * scale, corner, 1.2f * scale, ColorUtil.multAlpha(second, 0.7f), 1f * scale);
        Render2D.rect(x + w - 6.2f * scale, y + h - 5f * scale - corner, 1.2f * scale, corner, ColorUtil.multAlpha(second, 0.7f), 1f * scale);

        float chevronX = x + w - 34f * scale;
        float chevronY = y + h - 16f * scale;
        for (int i = 0; i < 3; i++) {
            float alpha = 0.2f + 0.25f * (float) Math.sin(now * 0.004f * speed + i * 0.9f);
            Render2D.rect(chevronX + i * 6f * scale, chevronY, 4f * scale, 1f * scale, ColorUtil.multAlpha(animated, 0.35f + alpha), 1f * scale);
            Render2D.rect(chevronX + i * 6f * scale + 3f * scale, chevronY + 3f * scale, 4f * scale, 1f * scale, ColorUtil.multAlpha(animated, 0.35f + alpha), 1f * scale);
        }
    }

    private float animatedHealthPct(PlayerEntity player) {
        float target = healthPct(player);
        if (displayedTarget != player) {
            displayedTarget = player;
            displayedHealthPct = target;
            return target;
        }
        if (!animatedBar.isValue()) {
            displayedHealthPct = target;
            return target;
        }
        float step = MathHelper.clamp(0.08f * animationSpeed.getValue(), 0.02f, 0.35f);
        displayedHealthPct += (target - displayedHealthPct) * step;
        return MathHelper.clamp(displayedHealthPct, 0f, 1f);
    }

    private float pulseAmount() {
        return (float) ((Math.sin(System.currentTimeMillis() * 0.0045f * animationSpeed.getValue()) + 1.0) * 0.75f);
    }

    private int animatedAccent(int first, int second) {
        float t = (float) ((Math.sin(System.currentTimeMillis() * 0.0035f * animationSpeed.getValue()) + 1.0) * 0.5);
        return ColorUtil.lerpColor(first, second, t);
    }

    private void drawFace(PlayerEntity player, float x, float y, float size, float radius) {
        Identifier skin = getSkin(player);
        int color = new Color(255, 255, 255, 255).getRGB();
        Render2D.texture(skin, x, y, size, size, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f, color, 0f, radius);

        float hatScale = 1.14f;
        float hatSize = size * hatScale;
        float hatOffset = (hatSize - size) / 2f;
        Render2D.texture(skin, x - hatOffset, y - hatOffset, hatSize, hatSize,
                40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f, color, 0f, radius);
    }

    private Identifier getSkin(PlayerEntity player) {
        if (mc.getNetworkHandler() == null) return STEVE_SKIN;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null || entry.getSkinTextures() == null || entry.getSkinTextures().body() == null) return STEVE_SKIN;
        return entry.getSkinTextures().body().texturePath();
    }

    private float healthPct(PlayerEntity player) {
        float hp = Math.max(0, player.getHealth() + player.getAbsorptionAmount());
        float max = Math.max(1, player.getMaxHealth() + player.getAbsorptionAmount());
        return MathHelper.clamp(hp / max, 0f, 1f);
    }

    private int selectedHealthColor(float pct) {
        if (!dynamicHealthColor.isValue()) return healthColor.getColor();
        if (pct > 0.6f) return new Color(55, 255, 80, 255).getRGB();
        if (pct > 0.3f) return new Color(255, 205, 70, 255).getRGB();
        return new Color(255, 80, 80, 255).getRGB();
    }

    private String trimName(String name, float maxWidth, float fontSize) {
        if (Fonts.BOLD.getWidth(name, fontSize) <= maxWidth) return name;
        String suffix = "...";
        while (name.length() > 1 && Fonts.BOLD.getWidth(name + suffix, fontSize) > maxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        return name + suffix;
    }

    @EventHandler
    public void onPacketSend(PacketEvent event) {
        if (!event.isSend()) return;
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override public void interact(Hand hand) {}
                @Override public void interactAt(Hand hand, Vec3d pos) {}
                @Override public void attack() {
                    if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit
                            && hit.getEntity() instanceof PlayerEntity player
                            && isValidHudTarget(player)) {
                        lastTarget = player;
                        lastAttackTime = System.currentTimeMillis();
                    }
                }
            });
        }
    }
}
