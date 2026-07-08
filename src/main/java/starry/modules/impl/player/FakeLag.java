package starry.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FakeLag extends ModuleStructure {
    MinMaxSetting lagDelay = new MinMaxSetting("Lag Delay", "").range(0, 1000).defaultValue(100, 200);
    BooleanSetting cancelOnElytra = new BooleanSetting("Cancel on Elytra", "").setValue(false);

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private Vec3d pos = Vec3d.ZERO;
    private int delay;
    private long lastFlushTime;
    private boolean flushing;

    public FakeLag() {
        super("Fake Lag", ModuleCategory.PLAYER);
        settings(lagDelay, cancelOnElytra);
    }

    @Override
    public void activate() {
        packetQueue.clear();
        lastFlushTime = System.currentTimeMillis();
        if (mc.player != null) pos = mc.player.getEntityPos();
        delay = (int) lagDelay.getRandomValue();
    }

    @Override
    public void deactivate() { flush(); }

    @EventHandler
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) return;
        if (event.getPacket() instanceof ExplosionS2CPacket) flush();
    }

    @EventHandler
    public void onPacketSend(PacketEvent event) {
        if (!event.isSend()) return;
        if (mc.world == null || mc.player == null || mc.player.isUsingItem() || mc.player.isDead()) return;
        Packet<?> p = event.getPacket();
        if (p instanceof PlayerInteractEntityC2SPacket || p instanceof HandSwingC2SPacket || p instanceof PlayerInteractBlockC2SPacket || p instanceof ClickSlotC2SPacket) { flush(); return; }
        if (cancelOnElytra.isValue() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) { flush(); return; }
        if (!flushing) { packetQueue.add(p); event.setCancelled(true); }
        if (System.currentTimeMillis() - lastFlushTime >= delay) flush();
    }

    private void flush() {
        if (mc.getNetworkHandler() == null) return;
        flushing = true;
        Packet<?> p;
        while ((p = packetQueue.poll()) != null) mc.getNetworkHandler().sendPacket(p);
        flushing = false;
        lastFlushTime = System.currentTimeMillis();
        if (mc.player != null) pos = mc.player.getEntityPos();
        delay = (int) lagDelay.getRandomValue();
    }
}
