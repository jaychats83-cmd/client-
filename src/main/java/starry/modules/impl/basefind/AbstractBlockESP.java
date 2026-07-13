package starry.modules.impl.basefind;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.render.Render3D;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractBlockESP extends ModuleStructure {
    protected final SliderSettings horizontalRange = new SliderSettings("Horizontal Range", "Search radius in blocks").setValue(32).range(8, 96);
    protected final SliderSettings verticalRange = new SliderSettings("Vertical Range", "Search height above and below you").setValue(24).range(4, 64);
    protected final SliderSettings scanDelay = new SliderSettings("Scan Delay", "Ticks between world scans").setValue(20).range(5, 100);
    protected final ColorSetting color;
    protected final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw a line to each result").setValue(false);
    private final List<BlockPos> results = new ArrayList<>();
    private int ticks;

    protected AbstractBlockESP(String name, String description, int defaultColor) {
        super(name, description, ModuleCategory.ESP);
        color = new ColorSetting("Color", "Highlight color").value(defaultColor);
        settings(horizontalRange, verticalRange, scanDelay, color, tracers);
    }

    protected abstract boolean matches(BlockState state);

    @Override
    public void activate() { ticks = scanDelay.getInt(); }

    @Override
    public void deactivate() { results.clear(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || ++ticks < scanDelay.getInt()) return;
        ticks = 0;
        int horizontal = horizontalRange.getInt();
        int vertical = verticalRange.getInt();
        BlockPos center = mc.player.getBlockPos();
        List<BlockPos> found = new ArrayList<>();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int x = -horizontal; x <= horizontal; x++) {
            for (int z = -horizontal; z <= horizontal; z++) {
                int chunkX = (center.getX() + x) >> 4;
                int chunkZ = (center.getZ() + z) >> 4;
                if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;
                for (int y = -vertical; y <= vertical; y++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (mc.world.isInBuildLimit(cursor) && matches(mc.world.getBlockState(cursor))) found.add(cursor.toImmutable());
                }
            }
        }
        results.clear();
        results.addAll(found);
    }

    @EventHandler
    public void onRender(WorldRenderEvent event) {
        if (mc.player == null) return;
        int argb = color.getColor();
        for (BlockPos pos : results) {
            Render3D.drawBox(new Box(pos), argb, 1.5F, true, true, false);
            if (tracers.isValue()) Render3D.drawLine(mc.player.getEyePos(), pos.toCenterPos(), argb, 1.25F, false);
        }
    }
}
