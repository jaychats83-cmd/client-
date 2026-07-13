package starry.modules.impl.basefind;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
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

public class StorageESP extends ModuleStructure {
    private final SliderSettings range = new SliderSettings("Chunk Range", "Loaded chunk radius").setValue(12).range(1, 32);
    private final ColorSetting color = new ColorSetting("Color", "Highlight color").value(0xAAF5A934);
    private final BooleanSetting chests = new BooleanSetting("Chests", "Highlight normal and trapped chests").setValue(true);
    private final BooleanSetting barrels = new BooleanSetting("Barrels", "Highlight barrels").setValue(true);
    private final BooleanSetting shulkers = new BooleanSetting("Shulker Boxes", "Highlight shulker boxes").setValue(true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", "Highlight ender chests").setValue(true);
    private final BooleanSetting hoppers = new BooleanSetting("Hoppers", "Highlight hoppers").setValue(false);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to storage").setValue(false);

    public StorageESP() {
        super("Storage ESP", "Highlights configurable loaded storage blocks", ModuleCategory.ESP);
        settings(range, color, chests, barrels, shulkers, enderChests, hoppers, tracers);
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
                if (!matches(entity)) continue;
                Render3D.drawBox(new Box(pos), color.getColor(), 1.5F, true, true, false);
                if (tracers.isValue()) Render3D.drawLine(mc.player.getEyePos(), pos.toCenterPos(), color.getColor(), 1.25F, false);
            }
        }
    }

    private boolean matches(BlockEntity entity) {
        return chests.isValue() && entity instanceof ChestBlockEntity
                || barrels.isValue() && entity instanceof BarrelBlockEntity
                || shulkers.isValue() && entity instanceof ShulkerBoxBlockEntity
                || enderChests.isValue() && entity instanceof EnderChestBlockEntity
                || hoppers.isValue() && entity instanceof HopperBlockEntity;
    }
}
