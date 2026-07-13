package starry.modules.impl.basefind;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.render.Render3D;

public class SpawnerESP extends ModuleStructure {
    private final SliderSettings range = new SliderSettings("Chunk Range", "Loaded chunk radius").setValue(12).range(1, 32);
    private final ColorSetting color = new ColorSetting("Color", "Highlight color").value(0xAAFF3598);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to spawners").setValue(true);

    public SpawnerESP() {
        super("Spawner ESP", "Highlights loaded mob spawners", ModuleCategory.ESP);
        settings(range, color, tracers);
    }

    @EventHandler
    public void onRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        ChunkPos center = mc.player.getChunkPos();
        for (int x = -range.getInt(); x <= range.getInt(); x++) for (int z = -range.getInt(); z <= range.getInt(); z++) {
            int cx = center.x + x, cz = center.z + z;
            if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
            if (!(mc.world.getChunkManager().getChunk(cx, cz) instanceof WorldChunk chunk)) continue;
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
                BlockEntity entity = mc.world.getBlockEntity(pos);
                if (!(entity instanceof MobSpawnerBlockEntity)) continue;
                Render3D.drawBox(new Box(pos), color.getColor(), 1.5F, true, true, false);
                if (tracers.isValue()) Render3D.drawLine(mc.player.getEyePos(), pos.toCenterPos(), color.getColor(), 1.25F, false);
            }
        }
    }
}
