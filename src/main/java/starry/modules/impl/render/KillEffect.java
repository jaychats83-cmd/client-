package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.DrawEvent;
import starry.events.impl.TickEvent;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.render.Render2D;
import starry.util.render.Render3D;
import starry.util.render.font.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class KillEffect extends ModuleStructure {
    static final long KILL_WINDOW_MS = 3500L;
    static final long ATTACK_MEMORY_MS = 10000L;

    SelectSetting style = new SelectSetting("Style", "Kill effect style")
            .value("Cross", "Soul", "Burst", "All")
            .selected("Cross");
    SelectSetting screenStyle = new SelectSetting("Screen Style", "Full-screen kill animation")
            .value("Shockwave", "Slash", "Vignette", "All")
            .selected("All");
    SelectSetting colorMode = new SelectSetting("Color Mode", "Effect color style")
            .value("Custom", "Rainbow", "Health")
            .selected("Custom");
    ColorSetting color = new ColorSetting("Color", "Effect RGB")
            .setColor(new Color(170, 90, 255, 220).getRGB())
            .visible(() -> colorMode.isSelected("Custom"));
    SliderSettings duration = new SliderSettings("Duration", "Effect lifetime in seconds")
            .setValue(3f)
            .range(0.5f, 8f);
    SliderSettings size = new SliderSettings("Size", "Effect size")
            .setValue(1f)
            .range(0.4f, 3f);
    SliderSettings width = new SliderSettings("Line Width", "Effect line width")
            .setValue(4f)
            .range(0.5f, 8f);
    SliderSettings screenIntensity = new SliderSettings("Screen Intensity", "Full-screen effect strength")
            .setValue(1f)
            .range(0.2f, 2f);
    BooleanSetting screenEffect = new BooleanSetting("Screen Effect", "Show big full-screen kill animation").setValue(true);
    BooleanSetting throughWalls = new BooleanSetting("Through Walls", "Show effect through walls").setValue(true);
    BooleanSetting sound = new BooleanSetting("Sound", "Play a sound on kill").setValue(true);
    SliderSettings volume = new SliderSettings("Volume", "Kill sound volume")
            .setValue(0.8f)
            .range(0f, 1f)
            .visible(() -> sound.isValue());

    final List<DeathEffect> effects = new ArrayList<>();
    final Set<Integer> processedDeaths = new HashSet<>();
    final Map<Integer, Long> attackedAt = new HashMap<>();
    Object lastWorld;

    public KillEffect() {
        super("Kill Effect", "qcloud kill effects with full style and RGB control", ModuleCategory.VISUALS);
        settings(style, screenStyle, colorMode, color, duration, size, width, screenIntensity, screenEffect, throughWalls, sound, volume);
    }

    @Override
    public void deactivate() {
        clearState();
        lastWorld = null;
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null) return;
        Entity entity = event.getTarget();
        if (entity instanceof PlayerEntity target && target.isAlive() && isValidKillTarget(target)) {
            attackedAt.put(target.getId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            clearState();
            lastWorld = null;
            return;
        }
        if (lastWorld != mc.world) {
            clearState();
            lastWorld = mc.world;
        }

        long now = System.currentTimeMillis();
        attackedAt.entrySet().removeIf(entry -> now - entry.getValue() > ATTACK_MEMORY_MS);

        Set<Integer> activeIds = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isValidKillTarget(player)) continue;

            int id = player.getId();
            activeIds.add(id);
            boolean dead = player.deathTime > 0 || !player.isAlive();

            if (dead) {
                if (processedDeaths.add(id) && wasKilledByMe(player, now)) {
                    spawnEffect(player, now);
                    playKillSound(player);
                }
            } else {
                processedDeaths.remove(id);
            }
        }

        processedDeaths.removeIf(id -> !activeIds.contains(id));
        attackedAt.keySet().removeIf(id -> !activeIds.contains(id));
        effects.removeIf(effect -> now - effect.startTime > durationMs());
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null || effects.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (DeathEffect effect : effects) {
            float progress = MathHelper.clamp((now - effect.startTime) / (float) durationMs(), 0f, 1f);
            float alpha = 1f - progress;
            if (alpha <= 0.01f) continue;

            int effectColor = ColorUtil.multAlpha(effectColor(effect), alpha);
            if (effect.type == EffectType.CROSS || effect.type == EffectType.ALL) renderCross(effect, effectColor);
            if (effect.type == EffectType.SOUL || effect.type == EffectType.ALL) renderSoul(effect, effectColor, progress);
            if (effect.type == EffectType.BURST || effect.type == EffectType.ALL) renderBurst(effect, effectColor, progress);
        }
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (!screenEffect.isValue() || effects.isEmpty()) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        float cx = screenW / 2f;
        float cy = screenH / 2f;
        long now = System.currentTimeMillis();

        for (DeathEffect effect : effects) {
            float progress = MathHelper.clamp((now - effect.startTime) / (float) durationMs(), 0f, 1f);
            float alpha = 1f - progress;
            if (alpha <= 0.01f) continue;

            int base = effectColor(effect);
            if (screenStyle.isSelected("Shockwave") || screenStyle.isSelected("All")) {
                renderScreenShockwave(cx, cy, screenW, screenH, base, progress, alpha);
            }
            if (screenStyle.isSelected("Slash") || screenStyle.isSelected("All")) {
                renderScreenSlash(cx, cy, screenW, screenH, base, progress, alpha);
            }
            if (screenStyle.isSelected("Vignette") || screenStyle.isSelected("All")) {
                renderScreenVignette(screenW, screenH, base, progress, alpha);
            }
            renderKillText(cx, cy, base, progress, alpha);
        }
    }

    private void renderScreenShockwave(float cx, float cy, int screenW, int screenH, int base, float progress, float alpha) {
        float intensity = screenIntensity.getValue();
        float max = Math.max(screenW, screenH) * (0.55f + 0.35f * intensity);
        int colorA = ColorUtil.multAlpha(base, Math.min(0.95f, alpha * 0.85f * intensity));
        int colorB = ColorUtil.multAlpha(base, Math.min(0.65f, alpha * 0.45f * intensity));

        for (int i = 0; i < 4; i++) {
            float ringProgress = MathHelper.clamp(progress * 1.25f - i * 0.11f, 0f, 1f);
            if (ringProgress <= 0f) continue;
            float radius = 18f + max * ringProgress;
            float thickness = Math.max(3f, (13f - i * 2f) * (1f - ringProgress * 0.45f) * intensity);
            Render2D.arc(cx - radius / 2f, cy - radius / 2f, radius, thickness, 360f, progress * 180f + i * 35f, colorA);
            Render2D.arc(cx - radius / 2f, cy - radius / 2f, radius, Math.max(1.5f, thickness * 0.35f), 270f, -progress * 240f - i * 24f, colorB);
        }
    }

    private void renderScreenSlash(float cx, float cy, int screenW, int screenH, int base, float progress, float alpha) {
        float intensity = screenIntensity.getValue();
        int strong = ColorUtil.multAlpha(base, Math.min(0.9f, alpha * 0.8f * intensity));
        int soft = ColorUtil.multAlpha(base, Math.min(0.45f, alpha * 0.35f * intensity));
        float sweep = easeOut(progress);
        float barH = Math.max(3f, 7f * intensity * alpha);
        float left = -screenW * 0.15f + screenW * 0.3f * sweep;
        float right = screenW * 0.85f - screenW * 0.3f * sweep;

        Render2D.rect(left, cy - 28f, screenW * 0.72f, barH, strong, barH / 2f);
        Render2D.rect(right - screenW * 0.72f, cy + 28f, screenW * 0.72f, barH, strong, barH / 2f);
        Render2D.rect(cx - 2f * intensity, 0, 4f * intensity, screenH, soft, 2f * intensity);
        Render2D.rect(0, cy - 2f * intensity, screenW, 4f * intensity, soft, 2f * intensity);

        int flare = ColorUtil.multAlpha(base, Math.min(0.55f, alpha * 0.5f * intensity));
        Render2D.rect(cx - 38f * intensity, cy - 38f * intensity, 76f * intensity, 76f * intensity, flare, 38f * intensity);
    }

    private void renderScreenVignette(int screenW, int screenH, int base, float progress, float alpha) {
        float intensity = screenIntensity.getValue();
        int flash = ColorUtil.multAlpha(base, Math.min(0.28f, alpha * (1f - progress) * intensity));
        int edge = ColorUtil.multAlpha(base, Math.min(0.38f, alpha * 0.34f * intensity));
        int dark = new Color(0, 0, 0, Math.round(110 * alpha * Math.min(1.4f, intensity))).getRGB();

        Render2D.rect(0, 0, screenW, screenH, flash, 0);
        Render2D.gradientRect9(0, 0, screenW, screenH, new int[]{
                edge, edge, edge,
                edge, ColorUtil.multAlpha(dark, 0.05f), edge,
                edge, edge, edge
        }, 0f);

        float line = Math.max(2f, 5f * intensity * alpha);
        Render2D.rect(0, 0, screenW, line, edge, 0);
        Render2D.rect(0, screenH - line, screenW, line, edge, 0);
        Render2D.rect(0, 0, line, screenH, edge, 0);
        Render2D.rect(screenW - line, 0, line, screenH, edge, 0);
    }

    private void renderKillText(float cx, float cy, int base, float progress, float alpha) {
        float pop = progress < 0.2f ? progress / 0.2f : 1f;
        float fade = progress > 0.72f ? 1f - ((progress - 0.72f) / 0.28f) : 1f;
        float textAlpha = MathHelper.clamp(alpha * fade, 0f, 1f);
        float textSize = (18f + 10f * (1f - easeOut(pop))) * Math.max(0.8f, screenIntensity.getValue());
        String top = "qcloud";
        String main = "KILL";
        int glow = ColorUtil.multAlpha(base, Math.min(0.75f, textAlpha * 0.65f));
        int white = new Color(255, 255, 255, Math.round(255 * textAlpha)).getRGB();

        float mainW = Fonts.BOLD.getWidth(main, textSize);
        float topW = Fonts.BOLD.getWidth(top, 8f * Math.max(0.8f, screenIntensity.getValue()));
        Fonts.BOLD.draw(main, cx - mainW / 2f + 1.5f, cy - textSize / 2f + 1.5f, textSize, glow);
        Fonts.BOLD.draw(main, cx - mainW / 2f, cy - textSize / 2f, textSize, white);
        Fonts.BOLD.draw(top, cx - topW / 2f, cy + textSize * 0.58f, 8f * Math.max(0.8f, screenIntensity.getValue()), glow);
    }

    private void renderCross(DeathEffect effect, int effectColor) {
        Vec3d base = effect.startPos;
        float scale = size.getValue();
        float lineWidth = width.getValue();
        Render3D.drawLine(base, base.add(0, 3.0 * scale, 0), effectColor, lineWidth, throughWalls.isValue());

        float yaw = (float) Math.toRadians(effect.yaw + 95.0f);
        double arm = scale;
        double y = 2.3 * scale;
        Vec3d start = base.add(-arm * Math.sin(yaw), y, arm * Math.cos(yaw));
        Vec3d end = base.add(arm * Math.sin(yaw), y, -arm * Math.cos(yaw));
        Render3D.drawLine(start, end, effectColor, lineWidth, throughWalls.isValue());
    }

    private void renderSoul(DeathEffect effect, int effectColor, float progress) {
        Vec3d center = effect.startPos.add(0, progress * 3.0 * size.getValue(), 0);
        double radius = (0.35 + progress * 0.3) * size.getValue();
        int points = 28;
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0 * i / points + progress * Math.PI * 4.0;
            double b = Math.PI * 2.0 * (i + 1) / points + progress * Math.PI * 4.0;
            Vec3d p1 = center.add(Math.cos(a) * radius, Math.sin(a * 2.0) * 0.18 * size.getValue(), Math.sin(a) * radius);
            Vec3d p2 = center.add(Math.cos(b) * radius, Math.sin(b * 2.0) * 0.18 * size.getValue(), Math.sin(b) * radius);
            Render3D.drawLine(p1, p2, effectColor, Math.max(1f, width.getValue() * 0.45f), throughWalls.isValue());
        }
        Render3D.drawLine(center.add(-radius, 0, 0), center.add(radius, 0, 0), effectColor, Math.max(1f, width.getValue() * 0.35f), throughWalls.isValue());
        Render3D.drawLine(center.add(0, -radius, 0), center.add(0, radius, 0), effectColor, Math.max(1f, width.getValue() * 0.35f), throughWalls.isValue());
    }

    private void renderBurst(DeathEffect effect, int effectColor, float progress) {
        double radius = progress * 2.6 * size.getValue();
        int rays = 12;
        for (int i = 0; i < rays; i++) {
            double yaw = Math.PI * 2.0 * i / rays;
            double pitch = Math.sin(i * 12.9898) * 0.55;
            Vec3d dir = new Vec3d(Math.cos(yaw), pitch, Math.sin(yaw)).normalize();
            Vec3d start = effect.startPos.add(0, 1.0 * size.getValue(), 0).add(dir.multiply(radius * 0.35));
            Vec3d end = effect.startPos.add(0, 1.0 * size.getValue(), 0).add(dir.multiply(radius));
            Render3D.drawLine(start, end, effectColor, Math.max(1f, width.getValue() * 0.35f), throughWalls.isValue());
        }
    }

    private void spawnEffect(PlayerEntity player, long now) {
        EffectType type = switch (style.getSelected()) {
            case "Soul" -> EffectType.SOUL;
            case "Burst" -> EffectType.BURST;
            case "All" -> EffectType.ALL;
            default -> EffectType.CROSS;
        };
        effects.add(new DeathEffect(now, player.getYaw(), player.getEntityPos(), type, player.getHealth() / Math.max(1f, player.getMaxHealth())));
    }

    private void playKillSound(PlayerEntity player) {
        if (!sound.isValue() || mc.world == null || player == null || volume.getValue() <= 0f) return;
        mc.world.playSoundClient(player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, player.getSoundCategory(), volume.getValue(), 1.25f, false);
    }

    private boolean wasKilledByMe(LivingEntity living, long now) {
        if (living.getAttacker() == mc.player) return true;
        Long attackedTime = attackedAt.get(living.getId());
        return attackedTime != null && now - attackedTime <= KILL_WINDOW_MS;
    }

    private boolean isValidKillTarget(PlayerEntity player) {
        if (player == null || mc.player == null || mc.getNetworkHandler() == null) return false;
        if (player == mc.player || player.isSpectator()) return false;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry != null;
    }

    private int effectColor(DeathEffect effect) {
        if (colorMode.isSelected("Rainbow")) return ColorUtil.rainbow(14, (int) (effect.startTime % 360), 0.85f, 1f, 1f);
        if (colorMode.isSelected("Health")) {
            if (effect.healthPct > 0.6f) return new Color(80, 255, 105, 230).getRGB();
            if (effect.healthPct > 0.3f) return new Color(255, 205, 70, 230).getRGB();
            return new Color(255, 80, 80, 230).getRGB();
        }
        return color.getColor();
    }

    private long durationMs() {
        return Math.max(1L, Math.round(duration.getValue() * 1000f));
    }

    private float easeOut(float value) {
        float clamped = MathHelper.clamp(value, 0f, 1f);
        return 1f - (float) Math.pow(1f - clamped, 3);
    }

    private void clearState() {
        effects.clear();
        processedDeaths.clear();
        attackedAt.clear();
    }

    private enum EffectType {
        CROSS,
        SOUL,
        BURST,
        ALL
    }

    private record DeathEffect(long startTime, float yaw, Vec3d startPos, EffectType type, float healthPct) {
    }
}