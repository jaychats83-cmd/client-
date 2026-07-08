package starry.modules.impl.render.chams;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import starry.modules.impl.render.x7m4j9;
import starry.util.render.clientpipeline.ClientPipelines;
import net.minecraft.client.render.RenderLayer;

public class ChamsFeatureRenderer<T extends EntityRenderState, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

    public ChamsFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, T state, float limbAngle, float limbDistance) {
        x7m4j9 chams = x7m4j9.getInstance();
        if (chams == null || !chams.isState()) return;
        if (!(state instanceof LivingEntityRenderState)) return;

        EntityType<?> type = state.entityType;
        boolean renderPlayers = chams.players.isValue();
        boolean renderMobs = chams.mobs.isValue();
        if ((type == EntityType.PLAYER && !renderPlayers) || (type != EntityType.PLAYER && !renderMobs))
            return;

        M model = this.getContextModel();
        model.setAngles(state);

        var client = MinecraftClient.getInstance();
        float time = 0;
        if (client.world != null)
            time = (client.world.getTime() + client.getRenderTickCounter().getTickProgress(true)) / 20.0f;

        int fillColor = chams.getColor(state, time, false);
        boolean throughWalls = chams.throughWalls.isValue();
        String mode = chams.mode.getSelected();
        boolean showFill = mode.equals("Fill") || mode.equals("Both");
        boolean showOutline = chams.outline.isValue() && (mode.equals("Outline") || mode.equals("Both"));

        if (showFill) {
            RenderLayer fillLayer = throughWalls
                    ? ClientPipelines.CHAMS_FILL_THROUGH_WALLS_LAYER
                    : ClientPipelines.CHAMS_FILL_NORMAL_LAYER;
            queue.submitModel(model, state, matrices, fillLayer, light, OverlayTexture.DEFAULT_UV, fillColor, null);
        }

        if (showOutline) {
            matrices.push();
            float width = chams.outlineWidth.getValue() * 0.005f;
            matrices.scale(1.0f + width, 1.0f + width, 1.0f + width);

            int outlineColor = chams.getColor(state, time, true);
            RenderLayer outlineLayer = throughWalls
                    ? ClientPipelines.CHAMS_OUTLINE_THROUGH_WALLS_LAYER
                    : ClientPipelines.CHAMS_OUTLINE_NORMAL_LAYER;
            queue.submitModel(model, state, matrices, outlineLayer, light, OverlayTexture.DEFAULT_UV, outlineColor, null);
            matrices.pop();
        }
    }
}
