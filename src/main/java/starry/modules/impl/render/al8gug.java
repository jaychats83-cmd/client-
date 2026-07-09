package starry.modules.impl.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ButtonSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.modules.module.setting.implement.TextSetting;
import starry.util.ColorUtil;
import starry.util.config.impl.blockesp.BlockESPConfig;
import starry.util.render.Render3D;
import starry.util.string.chat.ChatMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import starry.util.string.StringHelper;

public class al8gug extends ModuleStructure {
    ColorSetting color = new ColorSetting("Color", "Block highlight color").value(ColorUtil.getColor(255, 0, 0, 255));
    TextSetting blockSearch = new TextSetting("Block Search", "Type a block id or name").setText("minecraft:diamond_ore");
    ButtonSetting addBlock = new ButtonSetting("Add Block", "Adds the best matching block").setButtonName("Add Best Match").setRunnable(this::addSearchedBlock);
    ButtonSetting removeBlock = new ButtonSetting("Remove Block", "Removes the best matching block").setButtonName("Remove Match").setRunnable(this::removeSearchedBlock);
    SliderSettings range = new SliderSettings(StringHelper.decrypt(new byte[]{24, (byte)-94, 59, (byte)-127, 103, (byte)-24}), StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-69, 68, (byte)-77, 43, (byte)-79, 60, (byte)-128, 50, (byte)-23, 86, (byte)-78, 35, (byte)-74, 44})).range(1, 128).setValue(32);
    BooleanSetting notifyInChat = new BooleanSetting(StringHelper.decrypt(new byte[]{4, (byte)-84, 43, (byte)-127, 116, (byte)-14, 84, (byte)-73, 62, (byte)-86, 48, (byte)-122, 97}), StringHelper.decrypt(new byte[]{25, (byte)-85, 48, (byte)-97, 50, (byte)-3, 88, (byte)-93, 36, (byte)-89, 127, (byte)-118, 126, (byte)-12, 84, (byte)-67, 106, (byte)-96, 48, (byte)-121, 96, (byte)-1, 94, (byte)-72, 43, (byte)-73, 58, (byte)-101, 50, (byte)-14, 89, (byte)-10, 41, (byte)-85, 62, (byte)-100})).setValue(false);

    Set<String> blocksToHighlight = new CopyOnWriteArraySet<>();
    Map<BlockPos, BlockState> renderBlocks = new HashMap<>();
    Set<BlockPos> notifiedBlocks = new CopyOnWriteArraySet<>();
    long lastScanTime = 0;
    int checkCounter = 0;

    public al8gug() {
        super(StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-34, 100, (byte)-122}), StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-117, 121, (byte)-69, 114, (byte)-123, 26}), ModuleCategory.ESP);
        settings(color, blockSearch, addBlock, removeBlock, range, notifyInChat);
    }

    public Set<String> getBlocksToHighlight() {
        return blocksToHighlight;
    }

    @Override
    public void activate() {
        blocksToHighlight.clear();
        blocksToHighlight.addAll(BlockESPConfig.getInstance().getBlocks());
        notifiedBlocks.clear();
    }

    @Override
    public void deactivate() {
        renderBlocks.clear();
        notifiedBlocks.clear();
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (!state || mc.world == null || mc.player == null) {
            renderBlocks.clear();
            return;
        }
        if (blocksToHighlight.isEmpty()) {
            renderBlocks.clear();
            return;
        }
        BlockPos playerPos = mc.player.getBlockPos();
        long currentTime = System.nanoTime() / 1_000_000;
        if (currentTime - lastScanTime >= 2000) {
            renderBlocks.clear();
            int chunkRange = 2;
            int yRange = 48;
            for (int x = -chunkRange; x <= chunkRange; x++) {
                for (int z = -chunkRange; z <= chunkRange; z++) {
                    int chunkX = (playerPos.getX() >> 4) + x;
                    int chunkZ = (playerPos.getZ() >> 4) + z;
                    if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                    if (chunk == null) continue;
                    int cx = chunk.getPos().x << 4;
                    int cz = chunk.getPos().z << 4;
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            int minY = Math.max(mc.world.getBottomY(), playerPos.getY() - yRange);
                            int maxY = Math.min(mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, cx + bx, cz + bz), playerPos.getY() + yRange);
                            for (int by = minY; by <= maxY; by++) {
                                BlockPos pos = new BlockPos(cx + bx, by, cz + bz);
                                double dist = mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                if (dist > range.getValue() * range.getValue()) continue;
                                Block block = mc.world.getBlockState(pos).getBlock();
                                String blockName = Registries.BLOCK.getId(block).toString();
                                if (blocksToHighlight.contains(blockName)) {
                                    renderBlocks.put(pos.toImmutable(), mc.world.getBlockState(pos));
                                    if (notifyInChat.isValue() && !notifiedBlocks.contains(pos)) {
                                        notifyBlockFound(pos, blockName);
                                        notifiedBlocks.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            lastScanTime = currentTime;
            checkCounter = 0;
        }
        if (checkCounter % 5 == 0) {
            int nearChunkRange = 1;
            for (int x = -nearChunkRange; x <= nearChunkRange; x++) {
                for (int z = -nearChunkRange; z <= nearChunkRange; z++) {
                    int chunkX = (playerPos.getX() >> 4) + x;
                    int chunkZ = (playerPos.getZ() >> 4) + z;
                    if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                    if (chunk == null) continue;
                    int cx = chunk.getPos().x << 4;
                    int cz = chunk.getPos().z << 4;
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            int minY = Math.max(mc.world.getBottomY(), playerPos.getY() - 24);
                            int maxY = Math.min(mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, cx + bx, cz + bz), playerPos.getY() + 24);
                            for (int by = minY; by <= maxY; by++) {
                                BlockPos pos = new BlockPos(cx + bx, by, cz + bz);
                                double dist = mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                if (dist > 4 * 4) continue;
                                Block block = mc.world.getBlockState(pos).getBlock();
                                String blockName = Registries.BLOCK.getId(block).toString();
                                if (blocksToHighlight.contains(blockName) && !renderBlocks.containsKey(pos)) {
                                    renderBlocks.put(pos.toImmutable(), mc.world.getBlockState(pos));
                                    if (notifyInChat.isValue() && !notifiedBlocks.contains(pos)) {
                                        notifyBlockFound(pos, blockName);
                                        notifiedBlocks.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (checkCounter % 60 == 0) {
            renderBlocks.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                Block block = mc.world.getBlockState(pos).getBlock();
                String blockName = Registries.BLOCK.getId(block).toString();
                boolean shouldRemove = !blocksToHighlight.contains(blockName);
                if (shouldRemove) {
                    notifiedBlocks.remove(pos);
                }
                return shouldRemove;
            });
        }
        checkCounter++;
        renderBlocks.forEach((pos, blockState) -> {
            Render3D.drawBox(new Box(pos), color.getColor(), 1);
        });
    }

    private void notifyBlockFound(BlockPos pos, String blockName) {
        if (mc.player != null) {
            ChatMessage.brandmessage("Found block " + blockName + " -> " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        }
    }

    private void addSearchedBlock() {
        String id = resolveBlockId(blockSearch.getText());
        if (id == null) {
            ChatMessage.brandmessage("No matching block found");
            return;
        }
        BlockESPConfig.getInstance().addBlockAndSave(id);
        blocksToHighlight.add(id);
        renderBlocks.clear();
        ChatMessage.brandmessage("Added BlockESP block " + id);
    }

    private void removeSearchedBlock() {
        String id = resolveBlockId(blockSearch.getText());
        if (id == null) {
            ChatMessage.brandmessage("No matching block found");
            return;
        }
        BlockESPConfig.getInstance().removeBlockAndSave(id);
        blocksToHighlight.remove(id);
        renderBlocks.clear();
        ChatMessage.brandmessage("Removed BlockESP block " + id);
    }

    private String resolveBlockId(String query) {
        if (query == null || query.isBlank()) return null;
        String normalized = query.toLowerCase().trim();
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        for (Block block : Registries.BLOCK) {
            String id = Registries.BLOCK.getId(block).toString();
            if (id.equals(normalized)) return id;
        }
        String loose = normalized.replace("minecraft:", "");
        for (Block block : Registries.BLOCK) {
            String id = Registries.BLOCK.getId(block).toString();
            if (id.contains(loose)) return id;
        }
        return null;
    }
}
