package starry.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import starry.modules.impl.util.Hitboxes;
import starry.util.Instance;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof PlayerEntity)) return;
        Hitboxes hitboxes = Instance.get(Hitboxes.class);
        if (hitboxes != null && hitboxes.isState()) {
            cir.setReturnValue(cir.getReturnValueF() + hitboxes.expansion.getValue());
        }
    }
}
