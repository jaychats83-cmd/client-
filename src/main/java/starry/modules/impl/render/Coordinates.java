package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coordinates extends ModuleStructure {
    SliderSettings x = new SliderSettings("X", "").setValue(8f).range(0f, 1000f);
    SliderSettings y = new SliderSettings("Y", "").setValue(46f).range(0f, 1000f);
    BooleanSetting showDimension = new BooleanSetting("Dimension", "").setValue(true);

    public Coordinates() {
        super("Coordinates", ModuleCategory.RENDER);
        settings(x, y, showDimension);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null) return;

        var pos = mc.player.getBlockPos();
        String coords = "XYZ " + pos.getX() + " / " + pos.getY() + " / " + pos.getZ();
        String dimension = mc.world == null ? "" : mc.world.getRegistryKey().getValue().getPath();

        var ctx = event.getDrawContext();
        int tw = mc.textRenderer.getWidth(coords);
        int dw = showDimension.isValue() ? mc.textRenderer.getWidth(dimension) : 0;
        int width = Math.max(tw, dw) + 24;
        int left = x.getInt();
        int top = y.getInt();
        int bottom = top + (showDimension.isValue() ? 42 : 28);

        ctx.fill(left, top, left + width, bottom, new Color(7, 11, 17, 184).getRGB());
        ctx.drawText(mc.textRenderer, coords, left + 12, top + 10, Color.WHITE.getRGB(), true);
        if (showDimension.isValue())
            ctx.drawText(mc.textRenderer, dimension, left + 12, top + 26, new Color(162, 176, 194).getRGB(), true);
    }
}
