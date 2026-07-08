package starry.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import starry.modules.impl.render.Freecam;
import starry.util.Instance;

@Mixin(Camera.class)
public class CameraMixin {
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void onCameraSetPos(Args args) {
        Freecam freecam = Instance.get(Freecam.class);
        if (freecam != null && freecam.isState()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
            double x = MathHelper.lerp(tickDelta, freecam.oldPos.x, freecam.pos.x);
            double y = MathHelper.lerp(tickDelta, freecam.oldPos.y, freecam.pos.y);
            double z = MathHelper.lerp(tickDelta, freecam.oldPos.z, freecam.pos.z);
            args.set(0, x);
            args.set(1, y);
            args.set(2, z);
        }
    }
}
