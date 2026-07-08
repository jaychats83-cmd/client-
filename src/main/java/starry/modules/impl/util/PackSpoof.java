package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PackSpoof extends ModuleStructure {
    public PackSpoof() {
        super("Pack Spoof", ModuleCategory.MISC);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE || mc.getNetworkHandler() == null) return;
        if (event.getPacket() instanceof ResourcePackSendS2CPacket) {
            event.setCancelled(true);
            mc.getNetworkHandler().sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
            mc.getNetworkHandler().sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
        }
    }
}
