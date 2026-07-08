package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import starry.IMinecraft;
import starry.events.api.EventHandler;
import starry.events.impl.JumpEvent;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.render.clientpipeline.ClientPipelines;
import starry.util.timer.StopWatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class xnpteo extends ModuleStructure implements IMinecraft {

    private final List<Circle> circles = new ArrayList<>();

    final Identifier circleTexture = Identifier.of("starry", "images/circle/circle.png");
    final Identifier glowTexture = Identifier.of("starry", "images/particle/glow.png");

    final SliderSettings maxSize = new SliderSettings(StringHelper.decrypt(new byte[]{7, (byte)-94, 39, (byte)-56, 65, (byte)-14, 77, (byte)-77}), StringHelper.decrypt(new byte[]{7, (byte)-94, 39, (byte)-127, 127, (byte)-18, 90, (byte)-10, 41, (byte)-86, 45, (byte)-117, 126, (byte)-2, 23, (byte)-91, 35, (byte)-71, 58}))
            .setValue(2f)
            .range(1.0f, 2.0f);

    final SliderSettings speed = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-77, 58, (byte)-115, 118}), StringHelper.decrypt(new byte[]{11, 19, (byte)-30, (byte)-127, 127, (byte)-6, 67, (byte)-65, 37, (byte)-83, 127, (byte)-101, 98, (byte)-2, 82, (byte)-78}))
            .setValue(2000f)
            .range(1000f, 2000f);

    final BooleanSetting glow = new BooleanSetting(StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97}), StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97, 50, (byte)-2, 81, (byte)-80, 47, (byte)-96, 43}))
            .setValue(true);

    final ColorSetting color1 = new ColorSetting("Color 1", "First color")
            .value(ColorUtil.getColor(137, 97, 72, 255));

    final ColorSetting color2 = new ColorSetting("Color 2", "Second color")
            .value(ColorUtil.getColor(255, 255, 255, 255));

    private static final int SEGMENTS = 64;

    public xnpteo() {
        super(StringHelper.decrypt(new byte[]{0, (byte)-74, 50, (byte)-104, 81, (byte)-14, 69, (byte)-75, 38, (byte)-90}), StringHelper.decrypt(new byte[]{(byte)-102, 75, 42, (byte)-123, 98, (byte)-69, 116, (byte)-65, 56, (byte)-96, 51, (byte)-115}), ModuleCategory.VISUALS);
        settings(maxSize, speed, glow, color1, color2);
    }

    @EventHandler
    public void onJump(JumpEvent event) {
        if (mc.player == null || event.getPlayer() != mc.player) return;

        Vec3d pos = new Vec3d(
                mc.player.getX(),
                Math.floor(mc.player.getY()) + 0.001,
                mc.player.getZ()
        );
        circles.add(new Circle(pos, new StopWatch()));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        long maxTime = (long) speed.getValue();

        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();
            if (circle.timer.elapsedTime() > maxTime) {
                iterator.remove();
            }
        }

        if (circles.isEmpty()) return;

        MatrixStack matrices = e.getStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        for (Circle circle : circles) {
            renderSingleCircle(matrices, immediate, circle, cameraPos);
        }

        immediate.draw();
    }

    private void renderSingleCircle(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Circle circle, Vec3d cameraPos) {
        float lifeTime = circle.timer.elapsedTime();
        float maxTime = speed.getValue();
        float progress = Math.min(lifeTime / maxTime, 1f);

        if (progress >= 1f) return;

        float easedProgress = bounceOut(progress);
        float scale = easedProgress * maxSize.getValue();

        float fadeInDuration = 0.15f;
        float glowStart = 0.65f;
        float fadeOutStart = 0.85f;
        float alpha;

        if (progress < fadeInDuration) {
            alpha = progress / fadeInDuration;
        } else if (progress >= fadeOutStart) {
            float fadeOutProgress = (progress - fadeOutStart) / (1f - fadeOutStart);
            alpha = 1f - fadeOutProgress;

            if (progress > glowStart) {
                float glowProgress = (progress - glowStart) / (fadeOutStart - glowStart);
                float glowPulse = (float) (Math.sin(glowProgress * Math.PI * 3) * 0.3 + 0.3);
                alpha += glowPulse * (1f - fadeOutProgress);
            }
        } else if (progress > glowStart) {
            float glowProgress = (progress - glowStart) / (fadeOutStart - glowStart);
            float glowPulse = (float) (Math.sin(glowProgress * Math.PI * 3) * 0.3 + 0.3);
            alpha = 1f + glowPulse;
        } else {
            alpha = 1f;
        }

        alpha = Math.max(0f, Math.min(1f, alpha));

        float rotationOffset = (lifeTime / 1000f) * 0.5f * 360f;

        Vec3d circlePos = circle.pos();

        if (glow.isValue()) {
            renderGradientGlow(matrices, immediate, circlePos, scale, alpha * 0.1f, rotationOffset, cameraPos);
        }

        renderGradientCircle(matrices, immediate, circlePos, scale, alpha, rotationOffset, cameraPos);
    }

    private void renderGradientCircle(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                      Vec3d pos, float size, float alpha, float rotationOffset, Vec3d cameraPos) {
        VertexConsumer buffer = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(circleTexture));

        matrices.push();

        float x = (float) (pos.x - cameraPos.x);
        float y = (float) (pos.y - cameraPos.y);
        float z = (float) (pos.z - cameraPos.z);

        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float radius = size / 2f;

        int c1 = color1.getColor();
        int c2 = color2.getColor();

        for (int i = 0; i < SEGMENTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / SEGMENTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / SEGMENTS);

            float t = (float) i / SEGMENTS;
            float tNext = (float) (i + 1) / SEGMENTS;

            float adjustedT = (t + rotationOffset / 360f) % 1f;
            float adjustedTNext = (tNext + rotationOffset / 360f) % 1f;

            int currentColor = getGradientColor(c1, c2, adjustedT, alpha);
            int nextColor = getGradientColor(c1, c2, adjustedTNext, alpha);

            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);

            float u1 = (float) (0.5 + 0.5 * Math.cos(angle1));
            float v1 = (float) (0.5 + 0.5 * Math.sin(angle1));
            float u2 = (float) (0.5 + 0.5 * Math.cos(angle2));
            float v2 = (float) (0.5 + 0.5 * Math.sin(angle2));

            int centerColor = ColorUtil.lerpColor(currentColor, nextColor, 0.5f);

            buffer.vertex(matrix, 0, 0, 0).texture(0.5f, 0.5f).color(centerColor);
            buffer.vertex(matrix, x1, z1, 0).texture(u1, v1).color(currentColor);
            buffer.vertex(matrix, x2, z2, 0).texture(u2, v2).color(nextColor);
            buffer.vertex(matrix, x2, z2, 0).texture(u2, v2).color(nextColor);
        }

        matrices.pop();
    }

    private void renderGradientGlow(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                    Vec3d pos, float scale, float alpha, float rotationOffset, Vec3d cameraPos) {
        int c1 = color1.getColor();
        int c2 = color2.getColor();

        for (int layer = 0; layer < 3; layer++) {
            float layerScale = scale * (1.3f + layer * 0.4f);
            float layerAlpha = alpha * (0.35f - layer * 0.1f);

            renderGlowLayer(matrices, immediate, pos, layerScale, layerAlpha, rotationOffset, c1, c2, cameraPos);
        }

        float coreAlpha = alpha * 0.2f;
        int coreColor1 = ColorUtil.multAlpha(c1, coreAlpha);
        int coreColor2 = ColorUtil.multAlpha(c2, coreAlpha);
        int mixedCore = ColorUtil.lerpColor(coreColor1, coreColor2, 0.5f);
        renderTexturedQuad(matrices, immediate, pos, scale * 2.5f, mixedCore, glowTexture, cameraPos);
    }

    private void renderGlowLayer(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                 Vec3d pos, float size, float alpha, float rotationOffset,
                                 int c1, int c2, Vec3d cameraPos) {
        VertexConsumer buffer = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(glowTexture));

        int glowSegments = 16;
        float radius = size / 2f;

        for (int i = 0; i < glowSegments; i++) {
            float angle = (float) (2 * Math.PI * i / glowSegments);
            float t = (float) i / glowSegments;

            float adjustedT = (t + rotationOffset / 360f) % 1f;
            int glowColor = getGradientColor(c1, c2, adjustedT, alpha);

            float glowX = (float) (pos.x + Math.cos(angle) * radius * 0.8f);
            float glowZ = (float) (pos.z + Math.sin(angle) * radius * 0.8f);
            Vec3d glowPos = new Vec3d(glowX, pos.y, glowZ);

            float glowSize = size * 0.4f;
            renderTexturedQuadAtPos(matrices, buffer, glowPos, glowSize, glowColor, cameraPos);
        }
    }

    private void renderTexturedQuadAtPos(MatrixStack matrices, VertexConsumer buffer, Vec3d pos, float size, int color, Vec3d cameraPos) {
        matrices.push();

        float x = (float) (pos.x - cameraPos.x);
        float y = (float) (pos.y - cameraPos.y);
        float z = (float) (pos.z - cameraPos.z);

        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2f;

        buffer.vertex(matrix, -half, -half, 0).texture(0, 0).color(color);
        buffer.vertex(matrix, half, -half, 0).texture(1, 0).color(color);
        buffer.vertex(matrix, half, half, 0).texture(1, 1).color(color);
        buffer.vertex(matrix, -half, half, 0).texture(0, 1).color(color);

        matrices.pop();
    }

    private int getGradientColor(int c1, int c2, float t, float alpha) {
        float gradientT;
        if (t <= 0.5f) {
            gradientT = t * 2f;
        } else {
            gradientT = (1f - t) * 2f;
        }

        int color = ColorUtil.lerpColor(c1, c2, gradientT);
        return ColorUtil.multAlpha(color, alpha);
    }

    private void renderTexturedQuad(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d pos, float size, int color, Identifier texture, Vec3d cameraPos) {
        VertexConsumer buffer = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(texture));

        matrices.push();

        float x = (float) (pos.x - cameraPos.x);
        float y = (float) (pos.y - cameraPos.y);
        float z = (float) (pos.z - cameraPos.z);

        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2f;

        buffer.vertex(matrix, -half, -half, 0).texture(0, 0).color(color);
        buffer.vertex(matrix, half, -half, 0).texture(1, 0).color(color);
        buffer.vertex(matrix, half, half, 0).texture(1, 1).color(color);
        buffer.vertex(matrix, -half, half, 0).texture(0, 1).color(color);

        matrices.pop();
    }

    private float bounceOut(float value) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (value < 1.0f / d1) {
            return n1 * value * value;
        } else if (value < 2.0f / d1) {
            return n1 * (value -= 1.5f / d1) * value + 0.75f;
        } else if (value < 2.5f / d1) {
            return n1 * (value -= 2.25f / d1) * value + 0.9375f;
        } else {
            return n1 * (value -= 2.625f / d1) * value + 0.984375f;
        }
    }

    public record Circle(Vec3d pos, StopWatch timer) {}
}
