package starry.mixin;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import starry.modules.impl.render.*;

@Mixin(WorldRenderer.class)
public class BlockOutlineMixin {

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, double x, double y, double z, OutlineRenderState state, int color, float lineWidth, CallbackInfo ci) {
        l44eio blockOverlay = l44eio.getInstance();
        if (blockOverlay != null && blockOverlay.isState()) {
            ci.cancel();
        }
    }
}
