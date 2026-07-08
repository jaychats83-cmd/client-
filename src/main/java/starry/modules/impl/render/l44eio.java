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
import starry.util.Instance;
import starry.util.render.Render3D;

import java.awt.*;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class l44eio extends ModuleStructure {
    public static l44eio getInstance() {
        return Instance.get(l44eio.class);
    }

    public l44eio() {
        super(StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-44, 65, (byte)-77, 56, (byte)-81, 62, (byte)-111}), StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-69, 120, (byte)-96, 47, (byte)-79, 51, (byte)-119, 107}), ModuleCategory.RENDER);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(HitResult.Type.BLOCK)) {
            BlockPos pos = result.getBlockPos();
            Render3D.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), new Color(109, 252, 255,230).getRGB(), 1.5f, true, true);
        }
    }
}
