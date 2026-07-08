package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.MinMaxSetting;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PingSpoof extends ModuleStructure {
    MinMaxSetting ping = new MinMaxSetting("Ping", "").range(0, 1000).defaultValue(0, 600);
    private int delay;

    public PingSpoof() {
        super("Ping Spoof", ModuleCategory.MISC);
        settings(ping);
    }

    @Override
    public void activate() { delay = (int) ping.getRandomValue(); }

    @EventHandler
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE || !(event.getPacket() instanceof KeepAliveS2CPacket packet) || mc.getNetworkHandler() == null) return;
        event.setCancelled(true);
        final int currentDelay = delay;
        new Thread(() -> {
            try { Thread.sleep(currentDelay); } catch (InterruptedException ignored) { return; }
            mc.getNetworkHandler().getConnection().send(new KeepAliveC2SPacket(packet.getId()));
            delay = (int) ping.getRandomValue();
        }).start();
    }
}
