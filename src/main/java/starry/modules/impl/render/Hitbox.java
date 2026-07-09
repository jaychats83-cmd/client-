package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.math.MathUtils;
import starry.util.render.Render3D;

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Hitbox extends ModuleStructure {
    SelectSetting style = new SelectSetting("Style", "Hitbox render style")
            .value("Both", "Outline", "Fill", "Corners")
            .selected("Both");
    SelectSetting colorMode = new SelectSetting("Color Mode", "How colors are chosen")
            .value("Custom", "Health", "Rainbow")
            .selected("Custom");
    ColorSetting boxColor = new ColorSetting("Box Color", "Hitbox RGB")
            .setColor(new Color(180, 80, 255, 230).getRGB())
            .visible(() -> colorMode.isSelected("Custom"));
    ColorSetting lineColor = new ColorSetting("Look Line Color", "Eye line RGB")
            .setColor(new Color(120, 210, 255, 230).getRGB());
    SliderSettings outlineWidth = new SliderSettings("Outline Width", "Box line width")
            .setValue(1.2f).range(0.5f, 5f)
            .visible(() -> !style.isSelected("Fill"));
    SliderSettings fillAlpha = new SliderSettings("Fill Alpha", "Box fill opacity")
            .setValue(0.2f).range(0.05f, 1f)
            .visible(() -> style.isSelected("Both") || style.isSelected("Fill") || style.isSelected("Corners"));
    SliderSettings expand = new SliderSettings("Expand", "Box expansion")
            .setValue(0.001f).range(0f, 0.25f);
    SliderSettings lineLength = new SliderSettings("Look Line Length", "Eye line length")
            .setValue(2f).range(0.25f, 8f);
    BooleanSetting lookLine = new BooleanSetting("Look Line", "Draw where the player is looking").setValue(true);
    BooleanSetting throughWalls = new BooleanSetting("Through Walls", "Show boxes through walls").setValue(true);
    BooleanSetting includeSelf = new BooleanSetting("Include Self", "Show your own hitbox in third person").setValue(false);
    BooleanSetting hideInvisible = new BooleanSetting("Hide Invisible", "Skip invisible players").setValue(true);

    public Hitbox() {
        super("Hitbox", "qcloud visual hitbox customizer", ModuleCategory.VISUALS);
        settings(style, colorMode, boxColor, lineColor, outlineWidth, fillAlpha, expand, lookLine, lineLength, throughWalls, includeSelf, hideInvisible);
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!valid(player)) continue;
            Box box = interpolatedBox(player).expand(expand.getValue());
            int color = playerColor(player);
            int fill = ColorUtil.multAlpha(color, fillAlpha.getValue());

            if (style.isSelected("Both") || style.isSelected("Outline")) {
                Render3D.drawBox(box, color, outlineWidth.getValue(), true, false, throughWalls.isValue());
            } else if (style.isSelected("Corners")) {
                drawCorners(box, color);
            }

            if (style.isSelected("Both") || style.isSelected("Fill") || style.isSelected("Corners")) {
                Render3D.drawBox(box, fill, 1f, false, true, throughWalls.isValue());
            }

            if (lookLine.isValue()) {
                drawLookLine(player);
            }
        }
    }

    private boolean valid(PlayerEntity player) {
        if (player == null || !player.isAlive()) return false;
        if (player == mc.player) {
            return includeSelf.isValue() && mc.options.getPerspective() != Perspective.FIRST_PERSON;
        }
        if (hideInvisible.isValue() && (player.isInvisible() || player.hasStatusEffect(StatusEffects.INVISIBILITY))) return false;
        return throughWalls.isValue() || canSee(player);
    }

    private Box interpolatedBox(PlayerEntity player) {
        Vec3d interpolated = MathUtils.interpolate(player);
        Vec3d delta = interpolated.subtract(player.getEntityPos());
        return player.getBoundingBox().offset(delta);
    }

    private int playerColor(PlayerEntity player) {
        if (colorMode.isSelected("Rainbow")) {
            return ColorUtil.rainbow(12, player.getId() * 18, 0.85f, 1f, 0.9f);
        }
        if (colorMode.isSelected("Health")) {
            float pct = Math.max(0f, Math.min(1f, player.getHealth() / Math.max(1f, player.getMaxHealth())));
            if (pct > 0.6f) return new Color(80, 255, 105, 230).getRGB();
            if (pct > 0.3f) return new Color(255, 205, 70, 230).getRGB();
            return new Color(255, 80, 80, 230).getRGB();
        }
        return boxColor.getColor();
    }

    private void drawLookLine(PlayerEntity player) {
        Vec3d origin = MathUtils.interpolate(player).add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d direction = player.getRotationVec(1.0f);
        if (direction.lengthSquared() <= 1.0E-6) return;
        Render3D.drawLine(origin, origin.add(direction.normalize().multiply(lineLength.getValue())), lineColor.getColor(), outlineWidth.getValue(), throughWalls.isValue());
    }

    private void drawCorners(Box box, int color) {
        double sx = (box.maxX - box.minX) * 0.25;
        double sy = (box.maxY - box.minY) * 0.18;
        double sz = (box.maxZ - box.minZ) * 0.25;
        double[][] c = {
                {box.minX, box.minY, box.minZ, 1, 1, 1}, {box.maxX, box.minY, box.minZ, -1, 1, 1},
                {box.minX, box.minY, box.maxZ, 1, 1, -1}, {box.maxX, box.minY, box.maxZ, -1, 1, -1},
                {box.minX, box.maxY, box.minZ, 1, -1, 1}, {box.maxX, box.maxY, box.minZ, -1, -1, 1},
                {box.minX, box.maxY, box.maxZ, 1, -1, -1}, {box.maxX, box.maxY, box.maxZ, -1, -1, -1}
        };
        for (double[] p : c) {
            Vec3d base = new Vec3d(p[0], p[1], p[2]);
            Render3D.drawLine(base, base.add(sx * p[3], 0, 0), color, outlineWidth.getValue(), throughWalls.isValue());
            Render3D.drawLine(base, base.add(0, sy * p[4], 0), color, outlineWidth.getValue(), throughWalls.isValue());
            Render3D.drawLine(base, base.add(0, 0, sz * p[5]), color, outlineWidth.getValue(), throughWalls.isValue());
        }
    }

    private boolean canSee(PlayerEntity player) {
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d end = player.getCameraPosVec(1.0f);
        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }
}
