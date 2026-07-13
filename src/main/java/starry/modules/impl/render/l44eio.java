package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.ColorUtil;
import starry.util.Instance;
import starry.util.render.Render3D;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class l44eio extends ModuleStructure {
    SelectSetting style = new SelectSetting("Style", "Block overlay style")
            .value("Shape", "Box", "Outline")
            .selected("Shape");
    ColorSetting color = new ColorSetting("Color", "Overlay color")
            .value(ColorUtil.getColor(109, 252, 255, 230));
    BooleanSetting fill = new BooleanSetting("Fill", "Render fill").setValue(true);
    SliderSettings width = new SliderSettings("Width", "Outline width").setValue(1.5f).range(0.5f, 5f);

    public static l44eio getInstance() {
        return Instance.get(l44eio.class);
    }

    public l44eio() {
        super(StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-44, 65, (byte)-77, 56, (byte)-81, 62, (byte)-111}), StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-69, 120, (byte)-96, 47, (byte)-79, 51, (byte)-119, 107}), ModuleCategory.RENDER);
        settings(style, color, fill, width);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(HitResult.Type.BLOCK)) {
            BlockPos pos = result.getBlockPos();
            int overlayColor = color.getColor();
            if (style.isSelected("Box")) {
                Render3D.drawBox(new net.minecraft.util.math.Box(pos), overlayColor, width.getValue(), true, fill.isValue(), true);
            } else if (style.isSelected("Outline")) {
                Render3D.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), overlayColor, width.getValue(), false, true);
            } else {
                Render3D.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), overlayColor, width.getValue(), fill.isValue(), true);
            }
        }
    }
}