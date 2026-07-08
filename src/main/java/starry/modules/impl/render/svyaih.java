package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import starry.events.api.EventHandler;
import starry.events.impl.PacketEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class svyaih extends ModuleStructure {

    public static svyaih getInstance() {
        return Instance.get(svyaih.class);
    }

    public SliderSettings time = new SliderSettings(StringHelper.decrypt(new byte[]{30, (byte)-86, 50, (byte)-115}), StringHelper.decrypt(new byte[]{30, (byte)-86, 50, (byte)-115, 50, (byte)-12, 81, (byte)-10, 46, (byte)-94, 38, (byte)-56, 58, (byte)-85, 26, (byte)-28, 126, (byte)-13, 111, (byte)-40, 59}))
            .range(0, 24000)
            .setValue(1000);

    private double animatedTime = 1000;

    public svyaih() {
        super(StringHelper.decrypt(new byte[]{11, (byte)-82, 61, (byte)-127, 119, (byte)-11, 84, (byte)-77}), StringHelper.decrypt(new byte[]{9, (byte)-85, 62, (byte)-122, 117, (byte)-2, 68, (byte)-10, 61, (byte)-84, 45, (byte)-124, 118, (byte)-69, 67, (byte)-65, 39, (byte)-90}), ModuleCategory.VISUALS);
        settings(time);
    }

    @Override
    public void activate() {
        animatedTime = time.getValue();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        double targetTime = time.getValue();
        double speed = 150 / 1000.0;
        double diff = targetTime - animatedTime;
        animatedTime += diff * speed;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
        }
    }

    public long getCustomTime() {
        return (long) animatedTime;
    }
}
