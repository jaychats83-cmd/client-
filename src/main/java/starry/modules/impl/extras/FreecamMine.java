package starry.modules.impl.extras;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.impl.render.Freecam;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.util.Instance;

public class FreecamMine extends ModuleStructure {
    private final BooleanSetting requireFreecam = new BooleanSetting("Require Freecam", "Only operate while Freecam is enabled").setValue(true);
    private final BooleanSetting swing = new BooleanSetting("Swing", "Swing the real player's hand").setValue(true);
    private boolean held;
    private int sequence;

    public FreecamMine() {
        super("Freecam Mine", "Allows block mining from the freecam crosshair using packets", ModuleCategory.EXTRAS);
        settings(requireFreecam, swing);
    }

    @Override
    public void deactivate() { held = false; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.currentScreen != null) return;
        Freecam freecam = Instance.get(Freecam.class);
        if (requireFreecam.isValue() && (freecam == null || !freecam.isState())) return;
        if (!mc.options.attackKey.isPressed() || held || !(mc.crosshairTarget instanceof BlockHitResult hit)) {
            if (!mc.options.attackKey.isPressed()) held = false;
            return;
        }
        held = true;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                hit.getBlockPos(), hit.getSide(), sequence++));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                hit.getBlockPos(), hit.getSide(), sequence++));
        if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
    }
}
