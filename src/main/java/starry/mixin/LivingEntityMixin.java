package starry.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import starry.events.api.EventManager;
import starry.events.impl.JumpEvent;
import starry.events.impl.SwingDurationEvent;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo info) {
        if ((Object) this instanceof ClientPlayerEntity player
                && player == MinecraftClient.getInstance().player) {
            JumpEvent event = new JumpEvent(player);
            EventManager.callEvent(event);
            if (event.isCancelled()) info.cancel();
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        SwingDurationEvent event = new SwingDurationEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            float animation = event.getAnimation();
            cir.setReturnValue((int) animation);
        }
    }
}
