package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.animations.Animation;
import starry.util.animations.Direction;
import starry.util.animations.OutBack;
import starry.util.render.Render3D;
import starry.util.render.clientpipeline.ClientPipelines;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetESP extends ModuleStructure {
    final Animation espAnim = new OutBack().setMs(300).setValue(1);

    SelectSetting mode = new SelectSetting("Mode", "Vurst-style target effect")
            .value("Circle", "Rhomb", "Ghost", "Chain", "Crystals")
            .selected("Circle");
    ColorSetting color1 = new ColorSetting("Color 1", "Primary RGB color")
            .setColor(new Color(255, 101, 57, 255).getRGB());
    ColorSetting color2 = new ColorSetting("Color 2", "Secondary RGB color")
            .setColor(new Color(255, 50, 150, 255).getRGB());
    ColorSetting color3 = new ColorSetting("Color 3", "Ghost accent RGB color")
            .setColor(new Color(150, 50, 255, 255).getRGB())
            .visible(() -> mode.isSelected("Ghost"));
    SliderSettings range = new SliderSettings("Range", "Target search range").setValue(8f).range(2f, 16f);
    SliderSettings fadeTime = new SliderSettings("Fade Time", "How long the ESP stays after losing the target").setValue(0.45f).range(0.05f, 2f);
    SliderSettings crystalRotationSpeed = new SliderSettings("Crystal Speed", "Crystal mode rotation speed")
            .setValue(0.5f)
            .range(0.1f, 2.0f)
            .visible(() -> mode.isSelected("Crystals"));
    BooleanSetting damageRed = new BooleanSetting("Damage Red", "Flashes red when the target is hit").setValue(true);
    BooleanSetting throughWalls = new BooleanSetting("Through Walls", "Render even when the target is behind blocks").setValue(false);

    LivingEntity currentTarget;
    LivingEntity lastTarget;
    Vec3d smoothedPos;
    long lastSeenAt;
    long lastFrameTime = System.currentTimeMillis();
    float movingValue;
    float hurtProgress;
    float rotationAngle;
    Entity lastRenderedTarget;
    final List<Crystal> crystalList = new ArrayList<>();

    static final float TARGET_FPS = 60f;
    static final float TARGET_FRAME_TIME = 1000f / TARGET_FPS;

    public TargetESP() {
        super("Target ESP", "Vurst-style animated target highlight", ModuleCategory.VISUALS);
        settings(mode, color1, color2, color3, range, fadeTime, crystalRotationSpeed, damageRed, throughWalls);
    }

    @Override
    public void deactivate() {
        currentTarget = null;
        lastTarget = null;
        smoothedPos = null;
        crystalList.clear();
        Render3D.resetCircleSmoothing();
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;

        float deltaTime = getDeltaTime();
        LivingEntity target = findTarget();
        long now = System.currentTimeMillis();

        if (target != null) {
            currentTarget = target;
            lastSeenAt = now;
        } else if (currentTarget != null && now - lastSeenAt > fadeTime.getValue() * 1000L) {
            currentTarget = null;
        }

        if (currentTarget == null || !currentTarget.isAlive()) {
            espAnim.setDirection(Direction.BACKWARDS);
            smoothedPos = null;
            lastTarget = null;
            Render3D.resetCircleSmoothing();
            return;
        }

        espAnim.setDirection(Direction.FORWARDS);
        float alpha = espAnim.getOutput().floatValue();
        if (target == null) {
            alpha *= 1.0f - MathHelper.clamp((now - lastSeenAt) / Math.max(1f, fadeTime.getValue() * 1000f), 0f, 1f);
        }
        if (alpha <= 0.01f) return;

        movingValue += 2f * deltaTime;
        if (movingValue > 360000f) movingValue = 0f;

        float hurtDecay = 0.1f * deltaTime;
        hurtProgress = damageRed.isValue() && currentTarget.hurtTime > 0
                ? (float) currentTarget.hurtTime / 10f
                : Math.max(0, hurtProgress - hurtDecay);

        Render3D.updateTargetEsp(deltaTime);

        if (mode.isSelected("Circle")) {
            renderCircle(event.getStack(), currentTarget, alpha);
            return;
        }

        MatrixStack stack = event.getStack();
        VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d targetPos = currentTarget.getLerpedPos(event.getPartialTicks());

        if (lastTarget != currentTarget || smoothedPos == null) {
            smoothedPos = targetPos;
            lastTarget = currentTarget;
            crystalList.clear();
            Render3D.resetCircleSmoothing();
        } else {
            float smoothingFactor = Math.min(1.0f, event.getPartialTicks() * 1.5f);
            smoothedPos = smoothedPos.add(targetPos.subtract(smoothedPos).multiply(smoothingFactor));
        }

        stack.push();
        stack.translate(smoothedPos.x - camPos.x, smoothedPos.y - camPos.y, smoothedPos.z - camPos.z);

        if (mode.isSelected("Rhomb")) {
            renderRhomb(stack, provider, currentTarget, alpha);
        } else if (mode.isSelected("Ghost")) {
            renderGhost(stack, provider, currentTarget, alpha);
        } else if (mode.isSelected("Chain")) {
            renderChain(stack, provider, currentTarget, alpha);
        } else if (mode.isSelected("Crystals")) {
            if (crystalList.isEmpty() || lastRenderedTarget != currentTarget) {
                createCrystals();
                lastRenderedTarget = currentTarget;
            }
            renderCrystals(stack, provider, alpha, deltaTime);
        }

        provider.draw();
        stack.pop();
    }

    private LivingEntity findTarget() {
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && isValidTarget(living)) {
            return living;
        }

        return mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(range.getValue()), this::isValidTarget)
                .stream()
                .min((a, b) -> Double.compare(angleTo(a), angleTo(b)))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || entity.isDead() || !entity.isAlive()) return false;
        if (entity instanceof PlayerEntity player && player.isSpectator()) return false;
        if (mc.player.squaredDistanceTo(entity) > range.getValue() * range.getValue()) return false;
        return throughWalls.isValue() || mc.player.canSee(entity);
    }

    private double angleTo(Entity entity) {
        Vec3d look = mc.player.getRotationVec(1.0F).normalize();
        Vec3d dir = entity.getBoundingBox().getCenter().subtract(mc.player.getEyePos()).normalize();
        return 1.0 - look.dotProduct(dir);
    }

    private void renderCircle(MatrixStack stack, LivingEntity target, float alpha) {
        int baseColor1 = color1.getColor();
        int baseColor2 = color2.getColor();

        if (hurtProgress > 0) {
            baseColor1 = lerpColor(baseColor1, 0xFFFF0000, hurtProgress);
            baseColor2 = lerpColor(baseColor2, 0xFFFF0000, hurtProgress);
        }

        Render3D.drawCircle(stack, target, alpha, hurtProgress, baseColor1, baseColor2);
    }

    private void renderRhomb(MatrixStack stack, VertexConsumerProvider provider, LivingEntity target, float alpha) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(Identifier.of("starry", "images/world/cube.png")));
        Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();

        stack.translate(0, target.getHeight() / 2f, 0);
        stack.multiply(camRot);
        float timeRotation = (System.currentTimeMillis() % 6283) / 1000f;
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(timeRotation) * 360f));
        stack.scale(0.5f, 0.5f, 1f);

        int c1 = withAlpha(applyDamage(color1.getColor()), (int) (255 * alpha));
        int c2 = withAlpha(applyDamage(color2.getColor()), (int) (255 * alpha));
        MatrixStack.Entry m = stack.peek();
        consumer.vertex(m, -1, -1, 0).texture(0, 0).color(c2);
        consumer.vertex(m, -1, 1, 0).texture(0, 1).color(c1);
        consumer.vertex(m, 1, 1, 0).texture(1, 1).color(c2);
        consumer.vertex(m, 1, -1, 0).texture(1, 0).color(c1);
    }

    private void renderGhost(MatrixStack stack, VertexConsumerProvider consumers, LivingEntity target, float alpha) {
        VertexConsumer consumer = consumers.getBuffer(ClientPipelines.GHOSTS_ESP.apply(Identifier.of("starry", "images/particle/ghost-glow.png")));
        stack.translate(0, target.getHeight() * 0.5f, 0);
        particle(stack, consumer, (sin, cos) -> new Vec3d(sin, cos, -cos), alpha, 0);
        particle(stack, consumer, (sin, cos) -> new Vec3d(-sin, sin, -cos), alpha, 1);
        particle(stack, consumer, (sin, cos) -> new Vec3d(-sin, -sin, cos), alpha, 2);
    }

    private void particle(MatrixStack stack, VertexConsumer consumer, Transformation transformation, float alpha, int colorIndex) {
        double radius = 0.7f;
        double distance = 11;
        float particleSize = 0.5f;
        int alphaFactor = 15;
        long elapsed = System.currentTimeMillis();
        int baseColor = switch (colorIndex) {
            case 0 -> color1.getColor();
            case 1 -> color2.getColor();
            default -> color3.getColor();
        };

        for (int i = 0; i < 40 * alpha; i++) {
            stack.push();
            double angle = 0.15 * ((elapsed * 0.5) - (i * distance)) / 30.0;
            double sin = Math.sin(angle) * radius;
            double cos = Math.cos(angle) * radius;
            Vec3d trans = transformation.make(sin, cos);
            stack.translate(trans.x, trans.y, trans.z);
            stack.multiply(mc.gameRenderer.getCamera().getRotation());
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((elapsed * 0.1f) - (i * 10f)));
            stack.translate(particleSize / 2f, particleSize / 2f, 0);

            float x = (float) i / 40f;
            int lerpedColor = applyDamage(lerpColor(baseColor, getNextColor(colorIndex), x));
            int c1 = withAlpha(lerpedColor, (int) ((255 - i * alphaFactor) * alpha));
            int c2 = withAlpha(lerpedColor, (int) ((255 - i * alphaFactor) * alpha));
            MatrixStack.Entry m = stack.peek();
            consumer.vertex(m, 0, -particleSize, 0).texture(0, 0).color(c2);
            consumer.vertex(m, -particleSize, -particleSize, 0).texture(0, 1).color(c1);
            consumer.vertex(m, -particleSize, 0, 0).texture(1, 1).color(c2);
            consumer.vertex(m, 0, 0, 0).texture(1, 0).color(c1);
            stack.pop();
        }
    }

    private void renderChain(MatrixStack stack, VertexConsumerProvider provider, LivingEntity target, float alpha) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.CHAIN_ESP.apply(Identifier.of("starry", "images/world/chain.png")));
        float animValue = (System.currentTimeMillis() % 360000) / 1000f * 60f;
        float gradusX = (float) (20 * Math.min(1 + Math.sin(Math.toRadians(animValue)), 1));
        float gradusZ = (float) (20 * (Math.min(1 + Math.sin(Math.toRadians(animValue)), 2) - 1));
        float width = target.getWidth() * 3;
        int linksStep = 18;
        int totalAngle = 720;
        float chainSizeVal = 8;
        float down = 1.5f;
        int alphaVal = MathHelper.clamp((int) (alpha * 128), 0, 128);
        int c1 = withAlpha(applyDamage(color1.getColor()), alphaVal);
        int c2 = withAlpha(applyDamage(color2.getColor()), alphaVal);
        float rotationValue = (System.currentTimeMillis() % 720000) / 1000f * 30f;

        for (int chain = 0; chain < 2; chain++) {
            float val = 1.2f - 0.5f * (chain == 0 ? 1.0f : 0.9f);
            stack.push();
            stack.translate(0, target.getHeight() / 2.0f, 0);
            stack.scale(0.5f, 0.5f, 0.5f);
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chain == 0 ? gradusX : -gradusX));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chain == 0 ? gradusZ : -gradusZ));
            Matrix4f matrix = stack.peek().getPositionMatrix();

            int modif = linksStep / 2;
            for (int i = 0; i < totalAngle; i += modif) {
                float offsetX = (chain == 0 ? gradusX : -gradusX) / 100F;
                float offsetZ = (chain == 0 ? -gradusZ : gradusZ) / 100F;
                float prevSin = (float) (offsetX + Math.sin(Math.toRadians(i - modif + rotationValue)) * width * val);
                float prevCos = (float) (offsetZ + Math.cos(Math.toRadians(i - modif + rotationValue)) * width * val);
                float sin = (float) (offsetX + Math.sin(Math.toRadians(i + rotationValue)) * width * val);
                float cos = (float) (offsetZ + Math.cos(Math.toRadians(i + rotationValue)) * width * val);
                float u0 = 1f / 360f * (i - modif) * chainSizeVal;
                float u1 = 1f / 360f * i * chainSizeVal;
                consumer.vertex(matrix, prevSin, -0.5f, prevCos).texture(u0, 0).color(c1);
                consumer.vertex(matrix, sin, -0.5f, cos).texture(u1, 0).color(c1);
                consumer.vertex(matrix, sin, -0.5f + down, cos).texture(u1, 0.99f).color(c2);
                consumer.vertex(matrix, prevSin, -0.5f + down, prevCos).texture(u0, 0.99f).color(c2);
            }
            stack.pop();
        }
    }

    private void createCrystals() {
        crystalList.clear();
        crystalList.add(new Crystal(new Vec3d(0, 0.85, 0.8), new Vec3d(-49, 0, 40)));
        crystalList.add(new Crystal(new Vec3d(0.2, 0.85, -0.675), new Vec3d(35, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30, 0, 35)));
        crystalList.add(new Crystal(new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.74, 0.95, -0.4), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30, 0, -25)));
        crystalList.add(new Crystal(new Vec3d(0, 1.35, -0.6), new Vec3d(45, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(0.85, 0.7, 0.1), new Vec3d(-30, 0, 30)));
        crystalList.add(new Crystal(new Vec3d(-0.7, 1.35, -0.3), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.3, 1.35, 0.55), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
    }

    private void renderCrystals(MatrixStack stack, VertexConsumerProvider provider, float alpha, float deltaTime) {
        rotationAngle = (rotationAngle + crystalRotationSpeed.getValue() * deltaTime) % 360;
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
        int baseColor = applyDamage(color1.getColor());
        for (Crystal crystal : crystalList) crystal.render(stack, provider, alpha, baseColor);
        stack.pop();
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaMs = currentTime - lastFrameTime;
        lastFrameTime = currentTime;
        deltaMs = Math.max(1f, Math.min(deltaMs, 100f));
        return deltaMs / TARGET_FRAME_TIME;
    }

    private int applyDamage(int color) {
        return hurtProgress > 0 ? lerpColor(color, 0xFFFF0000, hurtProgress) : color;
    }

    private int getNextColor(int colorIndex) {
        return switch (colorIndex) {
            case 0 -> color2.getColor();
            case 1 -> color3.getColor();
            default -> color1.getColor();
        };
    }

    private int lerpColor(int c1, int c2, float t) {
        return ColorUtil.lerpColor(c1, c2, MathHelper.clamp(t, 0f, 1f));
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    @FunctionalInterface
    private interface Transformation {
        Vec3d make(double sin, double cos);
    }

    private class Crystal {
        final Vec3d position;
        final Vec3d rotation;
        final float rotationSpeed;

        Crystal(Vec3d position, Vec3d rotation) {
            this.position = position;
            this.rotation = rotation;
            this.rotationSpeed = 0.5f + (float) (Math.random() * 1.5f);
        }

        void render(MatrixStack stack, VertexConsumerProvider provider, float alpha, int baseColor) {
            stack.push();
            stack.translate(position.x, position.y, position.z);
            float timeSeconds = (System.currentTimeMillis() % 31416) / 1000f;
            float pulse = 1.0f + (float) (Math.sin(timeSeconds * 2f) * 0.1f);
            stack.scale(pulse, pulse, pulse);
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));

            drawFilledCrystal(stack, provider.getBuffer(ClientPipelines.CRYSTAL_FILLED), baseColor, 0.255f, alpha);
            stack.push();
            stack.scale(1.15f, 1.15f, 1.15f);
            drawFilledCrystal(stack, provider.getBuffer(ClientPipelines.CRYSTAL_GLOW), baseColor, 0.075f, alpha);
            stack.pop();
            drawBloomEffect(stack, provider, baseColor, alpha);
            stack.pop();
        }

        void drawFilledCrystal(MatrixStack stack, VertexConsumer consumer, int baseColor, float alphaMultiplier, float anim) {
            float s = 0.05f;
            float hPrism = s;
            float hPyramid = s * 1.5f;
            int sides = 8;
            List<Vector3f> top = new ArrayList<>();
            List<Vector3f> bottom = new ArrayList<>();
            for (int i = 0; i < sides; i++) {
                float angle = (float) (Math.PI * 2 * i / sides);
                top.add(new Vector3f((float) (s * Math.cos(angle)), hPrism / 2, (float) (s * Math.sin(angle))));
                bottom.add(new Vector3f((float) (s * Math.cos(angle)), -hPrism / 2, (float) (s * Math.sin(angle))));
            }

            Vector3f vTop = new Vector3f(0, hPrism / 2 + hPyramid, 0);
            Vector3f vBottom = new Vector3f(0, -hPrism / 2 - hPyramid, 0);
            int finalAlpha = (int) (alphaMultiplier * 255 * anim);
            int finalColor = withAlpha(baseColor, finalAlpha);
            int darkerColor = withAlpha(darkenColor(baseColor, 0.7f), finalAlpha);
            int lighterColor = withAlpha(lightenColor(baseColor, 1.2f), finalAlpha);
            Matrix4f matrix = stack.peek().getPositionMatrix();

            for (int i = 0; i < sides; i++) {
                drawQuadFilled(matrix, consumer, bottom.get(i), bottom.get((i + 1) % sides), top.get((i + 1) % sides), top.get(i), i % 2 == 0 ? finalColor : darkerColor);
                drawTriangleFilled(matrix, consumer, vTop, top.get((i + 1) % sides), top.get(i), i % 2 == 0 ? lighterColor : finalColor);
                drawTriangleFilled(matrix, consumer, vBottom, bottom.get(i), bottom.get((i + 1) % sides), i % 2 == 0 ? darkerColor : finalColor);
            }
        }

        void drawTriangleFilled(Matrix4f matrix, VertexConsumer consumer, Vector3f v1, Vector3f v2, Vector3f v3, int color) {
            consumer.vertex(matrix, v1.x, v1.y, v1.z).color(color);
            consumer.vertex(matrix, v2.x, v2.y, v2.z).color(color);
            consumer.vertex(matrix, v3.x, v3.y, v3.z).color(color);
            consumer.vertex(matrix, v3.x, v3.y, v3.z).color(color);
        }

        void drawQuadFilled(Matrix4f matrix, VertexConsumer consumer, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, int color) {
            consumer.vertex(matrix, v1.x, v1.y, v1.z).color(color);
            consumer.vertex(matrix, v2.x, v2.y, v2.z).color(color);
            consumer.vertex(matrix, v3.x, v3.y, v3.z).color(color);
            consumer.vertex(matrix, v4.x, v4.y, v4.z).color(color);
        }

        void drawBloomEffect(MatrixStack stack, VertexConsumerProvider provider, int baseColor, float anim) {
            VertexConsumer bloomConsumer = provider.getBuffer(ClientPipelines.BLOOM_ESP.apply(Identifier.of("starry", "images/particle/glow.png")));
            int bloomColor = withAlpha(baseColor, (int) (18 * anim));
            float bloomSize = 0.75f;
            Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();

            for (int i = 0; i < 6; i++) {
                stack.push();
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60f * i));
                stack.multiply(camRot);
                Matrix4f matrix = stack.peek().getPositionMatrix();
                bloomConsumer.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bloomConsumer.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bloomConsumer.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bloomConsumer.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                stack.pop();
            }
        }
    }

    private int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int lightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
