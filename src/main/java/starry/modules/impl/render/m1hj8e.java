package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;
import starry.util.animations.Animation;
import starry.util.animations.Direction;
import starry.util.animations.EaseInOutQuad;
import starry.util.animations.Easings;
import starry.util.animations.SmoothAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class m1hj8e extends ModuleStructure {

    public static m1hj8e getInstance() {
        return Instance.get(m1hj8e.class);
    }

    private static final Identifier ARROW_TEXTURE = Identifier.of("starry", "textures/world/arrow.png");

    public SliderSettings arrowsDistance = new SliderSettings(StringHelper.decrypt(new byte[]{14, (byte)-86, 44, (byte)-100, 115, (byte)-11, 84, (byte)-77}), StringHelper.decrypt(new byte[]{14, (byte)-86, 44, (byte)-100, 115, (byte)-11, 84, (byte)-77, 106, (byte)-91, 45, (byte)-121, 127, (byte)-69, 84, (byte)-92, 37, (byte)-80, 44, (byte)-128, 115, (byte)-14, 69}))
            .range(1.0f, 20.0f)
            .setValue(25.0f);

    public ColorSetting arrowColor = new ColorSetting("Color", "Arrow color")
            .value(0xFF896148);

    private final SmoothAnimation animationStep = new SmoothAnimation();
    private final SmoothAnimation animatedYaw = new SmoothAnimation();
    private final SmoothAnimation animatedPitch = new SmoothAnimation();
    private final SmoothAnimation animatedCameraYaw = new SmoothAnimation();

    private final List<Arrow> playerList = new ArrayList<>();

    public m1hj8e() {
        super(StringHelper.decrypt(new byte[]{11, (byte)-79, 45, (byte)-121, 101, (byte)-24}), StringHelper.decrypt(new byte[]{25, (byte)-85, 48, (byte)-97, 97, (byte)-69, 86, (byte)-92, 56, (byte)-84, 40, (byte)-101, 50, (byte)-21, 88, (byte)-65, 36, (byte)-73, 54, (byte)-122, 117, (byte)-69, 67, (byte)-71, 106, (byte)-77, 51, (byte)-119, 107, (byte)-2, 69, (byte)-91}), ModuleCategory.ESP);
        settings(arrowsDistance, arrowColor);
    }

    @Override
    public void deactivate() {
        playerList.clear();
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;

        DrawContext context = event.getDrawContext();
        float partialTicks = event.getPartialTicks();

        animationStep.update();
        animatedYaw.update();
        animatedPitch.update();
        animatedCameraYaw.update();

        float size = 45 + arrowsDistance.getValue();

        if (mc.currentScreen instanceof InventoryScreen) {
            size += 80;
        }

        if (mc.player.isSneaking()) {
            size -= 20;
        }

        if (isMoving()) {
            size += 10;
        }

        float strafeInput = mc.player.input.getMovementInput().x;
        float forwardInput = mc.player.input.getMovementInput().y;

        animatedYaw.run(strafeInput * 5, 0.75, Easings.EXPO_OUT);
        animatedPitch.run(forwardInput * 5, 0.75, Easings.EXPO_OUT);
        animatedCameraYaw.run(mc.gameRenderer.getCamera().getYaw(), 0.75, Easings.EXPO_OUT, true);
        animationStep.run(size, 1.0, Easings.EXPO_OUT, false);

        List<Arrow> players = new ArrayList<>();

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            Optional<Arrow> arrowConsumer = playerList.stream()
                    .filter(a -> a.player == player)
                    .findFirst();

            if (arrowConsumer.isPresent() && !isValidPlayer(player)) {
                continue;
            }

            Arrow arrow = new Arrow(
                    player,
                    arrowConsumer.map(a -> a.fadeAnimation).orElse(createFadeAnimation())
            );
            players.add(arrow);
        }

        List<Arrow> arrows = new ArrayList<>(playerList);
        arrows.removeIf(p -> players.stream().anyMatch(p2 -> p.player == p2.player));

        for (Arrow arrow : arrows) {
            arrow.fadeAnimation.setDirection(Direction.BACKWARDS);
            if (!arrow.isDead()) {
                players.add(arrow);
            }
        }

        playerList.clear();
        playerList.addAll(players);

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        for (Arrow arrow : playerList) {
            PlayerEntity player = arrow.player;

            arrow.updateAlpha();

            float animValue = arrow.getAlpha();
            if (animValue <= 0.001f) continue;

            if (!isValidPlayer(player) && arrow.fadeAnimation.isDirection(Direction.FORWARDS)) {
                continue;
            }

            double playerX = player.lastRenderX + (player.getX() - player.lastRenderX) * partialTicks
                    - mc.gameRenderer.getCamera().getCameraPos().x;
            double playerZ = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * partialTicks
                    - mc.gameRenderer.getCamera().getCameraPos().z;

            double cameraYaw = animatedCameraYaw.getValue();
            double cos = MathHelper.cos((float) (cameraYaw * (Math.PI * 2 / 360)));
            double sin = MathHelper.sin((float) (cameraYaw * (Math.PI * 2 / 360)));

            double rotY = -(playerZ * cos - playerX * sin);
            double rotX = -(playerX * cos + playerZ * sin);

            float angle = (float) (Math.atan2(rotY, rotX) * 180 / Math.PI);

            double x2 = animationStep.getValue() * animValue * MathHelper.cos((float) Math.toRadians(angle)) + centerX;
            double y2 = animationStep.getValue() * animValue * MathHelper.sin((float) Math.toRadians(angle)) + centerY;

            x2 += animatedYaw.getValue();
            y2 += animatedPitch.getValue();

            int color = applyAlpha(arrowColor.getColor(), animValue);

            drawArrow(context, (float) x2, (float) y2, angle, color, 1);
        }
    }

    private Animation createFadeAnimation() {
        Animation anim = new EaseInOutQuad().setMs(200).setValue(1.0);
        anim.setDirection(Direction.FORWARDS);
        return anim;
    }

    private void drawArrow(DrawContext context, float x, float y, float angle, int color, float scale) {
        float size = 17 * scale;
        float halfSize = size / 2.0f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().rotate((float) Math.toRadians(angle));
        context.getMatrices().rotate((float) Math.toRadians(90));

        int intSize = (int) size;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ARROW_TEXTURE,
                (int) (1 - halfSize),
                (int) (-5.5f),
                0, 0,
                intSize, intSize,
                intSize, intSize,
                color
        );

        context.getMatrices().popMatrix();
    }

    private int applyAlpha(int color, float alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private boolean isValidPlayer(PlayerEntity player) {
        return player != mc.player && !player.isRemoved();
    }

    private boolean isMoving() {
        return mc.player.input.getMovementInput().y != 0 || mc.player.input.getMovementInput().x != 0;
    }

    private static class Arrow {
        final PlayerEntity player;
        final Animation fadeAnimation;
        float cachedAlpha = 0.0f;
        long lastAlphaUpdate = 0L;

        Arrow(PlayerEntity player, Animation fadeAnimation) {
            this.player = player;
            this.fadeAnimation = fadeAnimation;
        }

        void updateAlpha() {
            long now = System.currentTimeMillis();
            if (now - lastAlphaUpdate > 16L) {
                cachedAlpha = fadeAnimation.getOutput().floatValue();
                lastAlphaUpdate = now;
            }
        }

        float getAlpha() {
            return cachedAlpha;
        }

        boolean isDead() {
            return fadeAnimation.isDirection(Direction.BACKWARDS) && fadeAnimation.isDone() && cachedAlpha <= 0.0f;
        }
    }
}
