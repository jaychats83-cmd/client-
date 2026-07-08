package starry.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import starry.screens.clickgui.ClickGui;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void disableBackgroundBlurAndDimming(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof ClickGui) {
            ci.cancel();
        }
    }

}