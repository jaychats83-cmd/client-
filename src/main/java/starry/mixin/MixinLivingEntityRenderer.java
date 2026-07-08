package starry.mixin;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import starry.modules.impl.render.chams.ChamsFeatureRenderer;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererFactory.Context ctx, CallbackInfo ci) {
        LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
        if (!(renderer.getClass().getName().contains("Player"))) {
            renderer.addFeature(new ChamsFeatureRenderer(renderer));
        }
    }
}
