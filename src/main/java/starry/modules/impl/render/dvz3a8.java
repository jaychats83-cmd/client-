package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;
import starry.events.api.EventHandler;
import starry.events.impl.CameraEvent;
import starry.events.impl.FovEvent;
import starry.events.impl.HotBarScrollEvent;
import starry.events.impl.KeyEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;
import starry.util.math.MathUtils;
import starry.util.string.PlayerInteractionHelper;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class dvz3a8 extends ModuleStructure {

    float fov = 110;
    float smoothFov = 30;
    float lastChangedFov = 30;

    BooleanSetting clipSetting = new BooleanSetting(StringHelper.decrypt(new byte[]{9, (byte)-94, 50, (byte)-115, 96, (byte)-6, 23, (byte)-107, 38, (byte)-86, 47}), StringHelper.decrypt(new byte[]{9, (byte)-94, 50, (byte)-115, 96, (byte)-6, 23, (byte)-90, 43, (byte)-80, 44, (byte)-115, 97, (byte)-69, 67, (byte)-66, 56, (byte)-84, 42, (byte)-113, 122, (byte)-69, 85, (byte)-70, 37, (byte)-96, 52, (byte)-101})).setValue(true);
    SliderSettings distanceSetting = new SliderSettings(StringHelper.decrypt(new byte[]{9, (byte)-94, 50, (byte)-115, 96, (byte)-6, 23, (byte)-110, 35, (byte)-80, 43, (byte)-119, 124, (byte)-8, 82}), StringHelper.decrypt(new byte[]{9, (byte)-94, 50, (byte)-115, 96, (byte)-6, 23, (byte)-78, 35, (byte)-80, 43, (byte)-119, 124, (byte)-8, 82, (byte)-10, 57, (byte)-90, 43, (byte)-100, 123, (byte)-11, 80}))
            .setValue(3.0F).range(2.0F, 5.0F);
    BindSetting zoomSetting = new BindSetting("Zoom", "Camera zoom key");

    public dvz3a8() {
        super(StringHelper.decrypt(new byte[]{9, (byte)-94, 50, (byte)-115, 96, (byte)-6, (byte)-25, 83, 47, (byte)-73, 43, (byte)-127, 124, (byte)-4, 68}), StringHelper.decrypt(new byte[]{9, (byte)-94, 50, (byte)-115, 96, (byte)-6, 23, (byte)-123, 47, (byte)-73, 43, (byte)-127, 124, (byte)-4, 68}), ModuleCategory.RENDER);
        settings(clipSetting, distanceSetting, zoomSetting);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(zoomSetting.getKey())) {
            fov = Math.min(lastChangedFov, mc.options.getFov().getValue() - 20);
        }
        if (e.isKeyReleased(zoomSetting.getKey(), true)) {
            lastChangedFov = fov;
            fov = mc.options.getFov().getValue();
        }
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (PlayerInteractionHelper.isKey(zoomSetting)) {
            fov = (int) MathHelper.clamp(fov - e.getVertical() * 10, 10, mc.options.getFov().getValue());
            e.cancel();
        }
    }

    @EventHandler
    public void onFov(FovEvent e) {
        e.setFov((int) MathHelper.clamp((smoothFov = MathUtils.interpolateSmooth(1.6, smoothFov, fov)) + 1, 10, mc.options.getFov().getValue()));
        e.cancel();
    }

    @EventHandler
    public void onCamera(CameraEvent e) {
        e.setCameraClip(clipSetting.isValue());
        e.setDistance(distanceSetting.getValue());
        e.setYaw(mc.player.getYaw());
        e.setPitch(mc.player.getPitch());
        e.cancel();
    }
}