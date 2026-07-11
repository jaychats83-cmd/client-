package starry.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import starry.events.api.EventManager;
import starry.events.impl.KeyEvent;
import starry.screens.clickgui.ClickGui;
import starry.util.SelfDestruct;
@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (input.key() != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {

            if (action == 0 && input.key() == ClickGui.getClickGuiKey() && canOpenClickGui()) {
                ClickGui.INSTANCE.openGui();
            }

            EventManager.callEvent(new KeyEvent(client.currentScreen, InputUtil.Type.KEYSYM, input.key(), action));
        }
    }

    private boolean canOpenClickGui() {
        if (SelfDestruct.isSessionDisabled()) return false;
        if (client.world == null || client.player == null) return false;
        if (client.currentScreen != null) return false;
        return true;
    }
}
