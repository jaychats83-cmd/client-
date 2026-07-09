package starry.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import starry.modules.impl.render.Fog;
import starry.util.ColorUtil;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @ModifyArgs(
            method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            ),
            require = 0
    )
    private void onApplyFog(Args args, Camera camera, int viewDistance, RenderTickCounter tickCounter, float skyDarkness, ClientWorld world) {
        Fog fog = Fog.getInstance();
        if (fog == null || !fog.isState()) return;

        int color = fog.getColor();
        args.set(2, new Vector4f(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), 1.0F));
        args.set(3, fog.getStart());
        args.set(4, fog.getEnd());
        args.set(5, fog.getStart());
        args.set(6, fog.getEnd());
        args.set(7, fog.getSkyEnd());
        args.set(8, fog.getCloudEnd());
    }
}
