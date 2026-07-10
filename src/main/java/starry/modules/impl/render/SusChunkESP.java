package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.render.Render3D;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SusChunkESP extends ModuleStructure {
    SliderSettings minScore = new SliderSettings("Min Score", "").setValue(12f).range(1f, 120f);
    SliderSettings rangeChunks = new SliderSettings("Range Chunks", "").setValue(12f).range(1f, 32f);
    SliderSettings alpha = new SliderSettings("Alpha", "").setValue(62f).range(15f, 180f);
    SliderSettings minStorage = new SliderSettings("Min Storage", "Minimum storage blocks needed before marking a chunk").setValue(2f).range(0f, 24f);
    SliderSettings clusterBonus = new SliderSettings("Cluster Bonus", "Extra score per storage block in a cluster").setValue(3f).range(0f, 12f);
    SliderSettings surfaceOffset = new SliderSettings("Surface Offset", "Vertical offset above the surface").setValue(0.04f).range(-2f, 4f);
    SliderSettings thickness = new SliderSettings("Thickness", "Render plate thickness").setValue(0.08f).range(0.02f, 2f);
    BooleanSetting fill = new BooleanSetting("Fill", "").setValue(true);
    BooleanSetting outline = new BooleanSetting("Outline", "").setValue(true);
    BooleanSetting tracers = new BooleanSetting("Tracers", "").setValue(false);
    BooleanSetting markStorageOnly = new BooleanSetting("Storage Only", "").setValue(false);
    BooleanSetting requireStorage = new BooleanSetting("Require Storage", "").setValue(true);
    BooleanSetting includeUtility = new BooleanSetting("Utility Blocks", "Count hoppers, furnaces, and enchanting tables").setValue(false);
    BooleanSetting surfaceHeight = new BooleanSetting("Surface Height", "Render on chunk surface instead of player Y").setValue(true);
    BooleanSetting loadedOnly = new BooleanSetting("Loaded Only", "Only scan loaded chunks").setValue(true);

    public SusChunkESP() {
        super("Chunk ESP [Sus Chunk]", ModuleCategory.ESP);
        settings(minScore, rangeChunks, minStorage, clusterBonus, alpha, surfaceOffset, thickness,
                fill, outline, tracers, markStorageOnly, requireStorage, includeUtility, surfaceHeight, loadedOnly);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) return;

        ChunkPos playerChunk = mc.player.getChunkPos();
        int range = rangeChunks.getInt();
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int chunkX = playerChunk.x + dx;
                int chunkZ = playerChunk.z + dz;
                if (loadedOnly.isValue() && !mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;
                var chunk = mc.world.getChunkManager().getChunk(chunkX, chunkZ);
                if (!(chunk instanceof WorldChunk worldChunk)) continue;
                if (chunk == null) continue;
                ScoreData score = scoreChunk(worldChunk);
                if (requireStorage.isValue() && score.storageCount < minStorage.getInt() && score.spawnerCount == 0) continue;
                if (score.total < minScore.getInt()) continue;
                renderChunk(worldChunk.getPos(), score.total);
            }
        }
    }

    private ScoreData scoreChunk(WorldChunk chunk) {
        ScoreData data = new ScoreData();
        for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
            BlockEntity entity = mc.world.getBlockEntity(blockPos);
            if (entity == null) continue;
            scoreEntity(entity, data);
        }
        if (data.storageCount >= minStorage.getInt()) {
            data.total += Math.max(0, data.storageCount - Math.max(0, minStorage.getInt()) + 1) * clusterBonus.getInt();
        }
        return data;
    }

    private void scoreEntity(BlockEntity entity, ScoreData data) {
        if (entity instanceof ShulkerBoxBlockEntity) {
            data.storageCount++;
            data.total += 10;
            return;
        }
        if (entity instanceof TrappedChestBlockEntity) {
            data.storageCount++;
            data.total += 8;
            return;
        }
        if (entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity) {
            data.storageCount++;
            data.total += 5;
            return;
        }
        if (entity instanceof MobSpawnerBlockEntity) {
            data.spawnerCount++;
            data.total += 12;
            return;
        }
        if (markStorageOnly.isValue() || !includeUtility.isValue()) return;
        if (entity instanceof HopperBlockEntity) data.total += 3;
        if (entity instanceof FurnaceBlockEntity) data.total += 1;
        if (entity instanceof EnchantingTableBlockEntity) data.total += 2;
    }

    private void renderChunk(ChunkPos chunkPos, int score) {
        int x1 = chunkPos.getStartX();
        int z1 = chunkPos.getStartZ();
        int x2 = x1 + 16;
        int z2 = z1 + 16;
        float y = surfaceHeight.isValue() ? surfaceY(chunkPos) : (float) Math.floor(mc.player.getY()) - 0.02F;
        Box box = new Box(x1, y, z1, x2, y + thickness.getValue(), z2);

        if (fill.isValue()) {
            Render3D.drawBox(box, colorForScore(score, alpha.getInt()).getRGB(), 1.4F, false, true, false);
        }

        if (outline.isValue()) {
            Render3D.drawBox(box, colorForScore(score, 210).getRGB(), 1.4F, true, false, false);
        }

        if (tracers.isValue()) {
            Vec3d center = new Vec3d(x1 + 8.0, y + 0.1, z1 + 8.0);
            Render3D.drawLine(mc.player.getEyePos(), center, colorForScore(score, 230).getRGB(), 1.4F, false);
        }
    }

    private float surfaceY(ChunkPos chunkPos) {
        int x1 = chunkPos.getStartX();
        int z1 = chunkPos.getStartZ();
        int x2 = x1 + 15;
        int z2 = z1 + 15;
        int centerX = x1 + 8;
        int centerZ = z1 + 8;
        int top = Math.max(
                Math.max(mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x1, z1), mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x2, z1)),
                Math.max(mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x1, z2), mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x2, z2))
        );
        top = Math.max(top, mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, centerX, centerZ));
        return top + surfaceOffset.getValue();
    }

    private Color colorForScore(int score, int a) {
        if (score >= minScore.getInt() * 3) return new Color(255, 72, 92, a);
        if (score >= minScore.getInt() * 2) return new Color(255, 168, 44, a);
        return new Color(63, 190, 255, a);
    }

    private static class ScoreData {
        int total;
        int storageCount;
        int spawnerCount;
    }
}
