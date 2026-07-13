package starry.modules.impl.extras;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends ModuleStructure {
    private final SliderSettings range = new SliderSettings("Range", "Block-breaking radius").setValue(4).range(1, 6);
    private final SliderSettings blocksPerTick = new SliderSettings("Blocks Per Tick", "Maximum packets sent each tick").setValue(2).range(1, 12);
    private final SelectSetting shape = new SelectSetting("Shape", "Area selection shape").value("Sphere", "Cube");
    private final BooleanSetting swing = new BooleanSetting("Swing", "Swing after breaking blocks").setValue(true);
    private final BooleanSetting avoidContainers = new BooleanSetting("Avoid Containers", "Do not target blocks with inventories").setValue(true);
    private int sequence;

    public Nuker() {
        super("Nuker", "Breaks nearby breakable blocks with configurable limits", ModuleCategory.EXTRAS);
        settings(range, blocksPerTick, shape, swing, avoidContainers);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.currentScreen != null) return;
        int radius = range.getInt();
        BlockPos center = mc.player.getBlockPos();
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
            if (shape.isSelected("Sphere") && center.getSquaredDistance(pos) > radius * radius) continue;
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir() || state.isOf(Blocks.BEDROCK) || state.getHardness(mc.world, pos) < 0) continue;
            if (avoidContainers.isValue() && mc.world.getBlockEntity(pos) != null) continue;
            targets.add(pos.toImmutable());
        }
        targets.sort(Comparator.comparingDouble(pos -> mc.player.squaredDistanceTo(pos.toCenterPos())));
        for (int i = 0; i < Math.min(blocksPerTick.getInt(), targets.size()); i++) {
            BlockPos pos = targets.get(i);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP, sequence++));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP, sequence++));
            if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
