package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Velocity extends ModuleStructure {
    public static Velocity getInstance() {
        return Instance.get(Velocity.class);
    }

    public BooleanSetting knockback = new BooleanSetting("Knockback", "").setValue(true);
    public BooleanSetting explosions = new BooleanSetting("Explosions", "").setValue(true);
    SliderSettings horizontal = new SliderSettings("Horizontal", "").setValue(0f).range(0f, 100f);
    SliderSettings vertical = new SliderSettings("Vertical", "").setValue(0f).range(0f, 100f);
    SliderSettings chance = new SliderSettings("Chance", "").setValue(100f).range(0f, 100f);

    public Velocity() {
        super("Velocity", ModuleCategory.COMBAT);
        settings(knockback, explosions, horizontal, vertical, chance);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) return;
        if (Math.random() * 100 > chance.getValue()) return;

        if (knockback.isValue() && event.getPacket() instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
            event.setCancelled(true);
            mc.player.setVelocity(mc.player.getVelocity().multiply(horizontal.getValue() / 100, vertical.getValue() / 100, horizontal.getValue() / 100));
        }

        if (explosions.isValue() && event.getPacket() instanceof ExplosionS2CPacket) {
            event.setCancelled(true);
        }
    }
}
