package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TotemKB extends ModuleStructure {
    public TotemKB() {
        super("Totem KB", ModuleCategory.CPVP);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) return;
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket velPacket && velPacket.getEntityId() == mc.player.getId()) {
            Vec3d vel = velPacket.getVelocity();
            if (vel.length() > 3.0) {
                event.setCancelled(true);
                mc.player.setVelocity(vel.multiply(0.5));
            }
        }
    }
}
