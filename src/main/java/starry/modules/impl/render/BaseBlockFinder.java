package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.render.Render3D;

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseBlockFinder extends ModuleStructure {
    SliderSettings rangeChunks = new SliderSettings("Range Chunks", "Loaded chunk search radius").setValue(16).range(1, 64);
    SliderSettings alpha = new SliderSettings("Alpha", "Box fill opacity").setValue(65).range(10, 180);
    BooleanSetting storage = new BooleanSetting("Storage", "Mark chests, trapped chests, barrels, and shulker boxes").setValue(true);
    BooleanSetting spawners = new BooleanSetting("Spawners", "Mark mob spawners").setValue(true);
    BooleanSetting beehives = new BooleanSetting("Beehives", "Mark loaded beehives and bee nests").setValue(true);
    BooleanSetting utility = new BooleanSetting("Utility Blocks", "Mark hoppers, furnaces, and enchanting tables").setValue(false);
    BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to detected blocks").setValue(false);

    public BaseBlockFinder() {
        super("Base Block Finder", "Highlights exact loaded base-signal block entities", ModuleCategory.ESP);
        settings(rangeChunks, alpha, storage, spawners, beehives, utility, tracers);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        ChunkPos center = mc.player.getChunkPos();
        int range = rangeChunks.getInt();

        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int chunkX = center.x + dx;
                int chunkZ = center.z + dz;
                if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;
                if (!(mc.world.getChunkManager().getChunk(chunkX, chunkZ) instanceof WorldChunk chunk)) continue;
                for (BlockPos pos : chunk.getBlockEntityPositions()) {
                    BlockEntity entity = mc.world.getBlockEntity(pos);
                    Color color = colorFor(entity, alpha.getInt());
                    if (color == null) continue;
                    Box box = new Box(pos);
                    Render3D.drawBox(box, color.getRGB(), 1.4F, true, true, false);
                    if (tracers.isValue()) Render3D.drawLine(mc.player.getEyePos(), Vec3d.ofCenter(pos),
                            colorFor(entity, 235).getRGB(), 1.4F, false);
                }
            }
        }
    }

    private Color colorFor(BlockEntity entity, int a) {
        if (entity == null) return null;
        if (storage.isValue() && entity instanceof ShulkerBoxBlockEntity) return new Color(210, 80, 255, a);
        if (storage.isValue() && entity instanceof TrappedChestBlockEntity) return new Color(255, 80, 70, a);
        if (storage.isValue() && (entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity)) return new Color(255, 175, 45, a);
        if (spawners.isValue() && entity instanceof MobSpawnerBlockEntity) return new Color(255, 50, 150, a);
        if (beehives.isValue() && entity instanceof BeehiveBlockEntity) return new Color(255, 225, 40, a);
        if (utility.isValue() && entity instanceof HopperBlockEntity) return new Color(100, 180, 255, a);
        if (utility.isValue() && entity instanceof FurnaceBlockEntity) return new Color(170, 170, 170, a);
        if (utility.isValue() && entity instanceof EnchantingTableBlockEntity) return new Color(80, 130, 255, a);
        return null;
    }
}
