package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
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

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetHud extends ModuleStructure {
    static final Identifier STEVE_SKIN = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    static final long TIMEOUT = 10000;

    SelectSetting style = new SelectSetting("Style", "Changes the full Target HUD layout")
            .value("Reference", "Clean", "Minimal")
            .selected("Clean");
    SliderSettings xCoord = new SliderSettings("X", "Horizontal location").setValue(12.5f).range(0f, 1920f);
    SliderSettings yCoord = new SliderSettings("Y", "Vertical location").setValue(35.1f).range(0f, 1080f);
    SliderSettings size = new SliderSettings("Size", "HUD scale").setValue(0.7f).range(0.65f, 1.8f);
    SliderSettings width = new SliderSettings("Width", "HUD width").setValue(184f).range(110f, 280f);
    SliderSettings height = new SliderSettings("Height", "HUD height").setValue(44.1f).range(34f, 96f);
    ColorSetting accentColor = new ColorSetting("Accent Color", "Outline and detail color")
            .setColor(new Color(120, 45, 105, 220).getRGB());
    ColorSetting backgroundColor = new ColorSetting("Background Color", "Panel background color")
            .setColor(new Color(10, 10, 12, 172).getRGB());
    ColorSetting healthColor = new ColorSetting("Health Color", "Health bar color")
            .setColor(new Color(55, 255, 80, 255).getRGB());
    ColorSetting textColor = new ColorSetting("Text Color", "Name and health text color")
            .setColor(new Color(255, 255, 255, 255).getRGB());
    BooleanSetting hudTimeout = new BooleanSetting("Timeout", "Hide after not attacking for a while").setValue(true);
    BooleanSetting dynamicHealthColor = new BooleanSetting("Dynamic Health", "Use green/yellow/red health colors").setValue(false);
    BooleanSetting showWinTag = new BooleanSetting("Win Tag", "Show the WIN pill like the reference").setValue(true);

    long lastAttackTime;

    public TargetHud() {
        super("Target HUD", ModuleCategory.RENDER);
        settings(style, xCoord, yCoord, size, width, height, accentColor, backgroundColor, healthColor, textColor, dynamicHealthColor, showWinTag, hudTimeout);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (hudTimeout.isValue() && System.currentTimeMillis() - lastAttackTime > TIMEOUT) return;
        if (!(mc.player.getAttacking() instanceof PlayerEntity player) || !player.isAlive()) return;

        float scale = size.getValue();
        float x = xCoord.getValue();
        float y = yCoord.getValue();
        float w = width.getValue() * scale;
        float h = height.getValue() * scale;

        if (style.isSelected("Clean")) {
            drawClean(player, x, y, w, h, scale);
        } else if (style.isSelected("Minimal")) {
            drawMinimal(player, x, y, w, h, scale);
        } else {
            drawReference(player, x, y, w, h, scale);
        }
    }

    private void drawReference(PlayerEntity player, float x, float y, float w, float h, float scale) {
        float radius = 12f * scale;
        int bg = backgroundColor.getColor();
        int accent = accentColor.getColor();
        int text = textColor.getColor();
        float pad = 8f * scale;
        float faceSize = Math.max(28f, h - pad * 2f);
        float barH = Math.max(6f, 7f * scale);
        float healthPct = healthPct(player);
        float health = player.getHealth() + player.getAbsorptionAmount();

        Render2D.blur(x, y, w, h, 7f * scale, radius, ColorUtil.multAlpha(bg, 0.72f));
        Render2D.rect(x, y, w, h, bg, radius);
        Render2D.outline(x, y, w, h, Math.max(0.6f, 1.2f * scale), accent, radius);

        if (showWinTag.isValue()) {
            float tagW = 38f * scale;
            float tagH = 14f * scale;
            Render2D.rect(x + pad, y - tagH - 5f * scale, tagW, tagH, new Color(0, 40, 18, 215).getRGB(), 7f * scale);
            Fonts.BOLD.draw("WIN", x + pad + 8f * scale, y - tagH - 2.5f * scale, 6f * scale, new Color(70, 255, 120, 255).getRGB());
        }

        drawFace(player, x + pad, y + pad, faceSize, 8f * scale);

        float infoX = x + pad + faceSize + 9f * scale;
        float infoW = Math.max(28f * scale, w - (infoX - x) - pad);
        float nameY = y + 9f * scale;
        Render2D.rect(infoX - 4f * scale, nameY - 2f * scale, Math.min(infoW * 0.68f, 92f * scale), 15f * scale, new Color(10, 10, 12, 150).getRGB(), 4f * scale);
        Fonts.BOLD.draw(trimName(player.getName().getString(), infoW * 0.68f, 7f * scale), infoX, nameY, 7f * scale, text);

        String hp = Math.round(health) + "";
        float hpW = Fonts.BOLD.getWidth(hp, 8f * scale);
        Fonts.BOLD.draw(hp, x + w - pad - hpW, y + h - pad - 18f * scale, 8f * scale, text);

        float barX = infoX;
        float barY = y + h - pad - barH - 5f * scale;
        float barW = Math.max(25f * scale, w - (barX - x) - pad - 28f * scale);
        Render2D.rect(barX, barY, barW, barH, new Color(12, 16, 16, 210).getRGB(), barH / 2f);
        Render2D.rect(barX, barY, barW * healthPct, barH, selectedHealthColor(healthPct), barH / 2f);
    }

    private void drawClean(PlayerEntity player, float x, float y, float w, float h, float scale) {
        int bg = backgroundColor.getColor();
        int accent = accentColor.getColor();
        int text = textColor.getColor();
        float pad = 7f * scale;
        float face = Math.max(24f * scale, h - pad * 2f);
        float healthPct = healthPct(player);

        Render2D.gradientRect(x, y, w, h, new int[]{
                ColorUtil.multBright(bg, 1.15f), bg, bg, ColorUtil.multDark(bg, 0.75f)
        }, 8f * scale);
        Render2D.outline(x, y, w, h, Math.max(0.5f, scale), accent, 8f * scale);
        drawFace(player, x + pad, y + pad, face, 5f * scale);

        float textX = x + pad + face + 8f * scale;
        Fonts.BOLD.draw(trimName(player.getName().getString(), w - (textX - x) - pad, 8f * scale), textX, y + 8f * scale, 8f * scale, text);
        Fonts.REGULAR.draw(String.format("%.1f HP", player.getHealth() + player.getAbsorptionAmount()), textX, y + 22f * scale, 6f * scale, ColorUtil.multAlpha(text, 0.72f));

        float barX = textX;
        float barY = y + h - pad - 6f * scale;
        float barW = w - (barX - x) - pad;
        Render2D.rect(barX, barY, barW, 5f * scale, ColorUtil.multAlpha(bg, 0.55f), 3f * scale);
        Render2D.rect(barX, barY, barW * healthPct, 5f * scale, selectedHealthColor(healthPct), 3f * scale);
    }

    private void drawMinimal(PlayerEntity player, float x, float y, float w, float h, float scale) {
        int bg = backgroundColor.getColor();
        int text = textColor.getColor();
        float healthPct = healthPct(player);
        float barH = Math.max(5f, h * 0.22f);

        Render2D.rect(x, y, w, h, bg, 5f * scale);
        Render2D.outline(x, y, w, h, Math.max(0.45f, 0.8f * scale), accentColor.getColor(), 5f * scale);
        Fonts.BOLD.draw(trimName(player.getName().getString(), w - 18f * scale, 7f * scale), x + 7f * scale, y + 5f * scale, 7f * scale, text);
        Render2D.rect(x + 7f * scale, y + h - barH - 6f * scale, w - 14f * scale, barH, ColorUtil.multAlpha(bg, 0.55f), barH / 2f);
        Render2D.rect(x + 7f * scale, y + h - barH - 6f * scale, (w - 14f * scale) * healthPct, barH, selectedHealthColor(healthPct), barH / 2f);
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
                    if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) lastAttackTime = System.currentTimeMillis();
                }
            });
        }
    }
}
