package starry.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import starry.Initialization;
import starry.modules.impl.render.*;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float leet$getValue(Double instance) {
        try {
            if (Initialization.getInstance() != null
                    && Initialization.getInstance().getManager() != null
                    && Initialization.getInstance().getManager().getModuleProvider() != null) {
                qxbn5c module = Initialization.getInstance().getManager().getModuleProvider().get(qxbn5c.class);
                if (module != null && module.isState()) {
                    return 200F;
                }
            }
        } catch (Exception ignored) {}
        return instance.floatValue();
    }

    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
    private void removeDarknessEffect(CallbackInfoReturnable<Float> cir) {
        h1v5ow noRender = h1v5ow.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
            cir.setReturnValue(0.0F);
        }
    }
}
