package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SusChunkESP extends ModuleStructure {
    SliderSettings minScore = new SliderSettings("Min Score", "").setValue(8f).range(1f, 80f);
    SliderSettings rangeChunks = new SliderSettings("Range Chunks", "").setValue(12f).range(1f, 32f);
    SliderSettings alpha = new SliderSettings("Alpha", "").setValue(62f).range(15f, 180f);
    BooleanSetting fill = new BooleanSetting("Fill", "").setValue(true);
    BooleanSetting outline = new BooleanSetting("Outline", "").setValue(true);
    BooleanSetting tracers = new BooleanSetting("Tracers", "").setValue(false);
    BooleanSetting markStorageOnly = new BooleanSetting("Storage Only", "").setValue(false);

    public SusChunkESP() {
        super("Chunk ESP [Sus Chunk]", ModuleCategory.ESP);
        settings(minScore, rangeChunks, alpha, fill, outline, tracers, markStorageOnly);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) return;

        ChunkPos playerChunk = mc.player.getChunkPos();
        int range = rangeChunks.getInt();
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                var chunk = mc.world.getChunkManager().getChunk(playerChunk.x + dx, playerChunk.z + dz);
                if (!(chunk instanceof WorldChunk worldChunk)) continue;
                if (chunk == null) continue;
                int score = scoreChunk(worldChunk);
                if (score < minScore.getInt()) continue;
            }
        }
    }

    private int scoreChunk(WorldChunk chunk) {
        int score = 0;
        for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
            BlockEntity entity = mc.world.getBlockEntity(blockPos);
            if (entity == null) continue;
            score += scoreEntity(entity);
        }
        return score;
    }

    private int scoreEntity(BlockEntity entity) {
        if (entity instanceof ShulkerBoxBlockEntity) return 7;
        if (entity instanceof TrappedChestBlockEntity) return 6;
        if (entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity) return 5;
        if (entity instanceof MobSpawnerBlockEntity) return 5;
        if (markStorageOnly.isValue()) return 0;
        if (entity instanceof HopperBlockEntity) return 3;
        if (entity instanceof FurnaceBlockEntity) return 2;
        if (entity instanceof EnchantingTableBlockEntity) return 2;
        return 0;
    }
}
