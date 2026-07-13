package starry.modules.impl.extras;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import starry.events.api.EventHandler;
import starry.events.impl.BlockBreakingEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;

public class PacketMine extends ModuleStructure {
    private final BooleanSetting swing = new BooleanSetting("Swing", "Swing your hand when packet mining").setValue(true);
    private final BooleanSetting instantStop = new BooleanSetting("Instant Stop", "Send the stop packet immediately after start").setValue(true);
    private int sequence;

    public PacketMine() {
        super("Packet Mine", "Mines targeted blocks using explicit start and stop packets", ModuleCategory.EXTRAS);
        settings(swing, instantStop);
    }

    @EventHandler
    public void onBreak(BlockBreakingEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        event.cancel();
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                event.getBlockPos(), event.getDirection(), sequence++));
        if (instantStop.isValue()) mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                event.getBlockPos(), event.getDirection(), sequence++));
        if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
    }
}
