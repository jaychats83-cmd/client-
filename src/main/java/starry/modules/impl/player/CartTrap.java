package starry.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.function.Predicate;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartTrap extends ModuleStructure {
    SliderSettings range = new SliderSettings("Range", "").setValue(5f).range(1f, 8f);
    SliderSettings delay = new SliderSettings("Delay", "").setValue(100f).range(0f, 1000f);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);

    private long lastActionTime;
    private static final Predicate<net.minecraft.item.Item> RAILS = item -> item == Items.RAIL || item == Items.POWERED_RAIL || item == Items.ACTIVATOR_RAIL || item == Items.DETECTOR_RAIL;
    private static final Predicate<net.minecraft.item.Item> CARTS = item -> item == Items.TNT_MINECART || item == Items.MINECART;

    public CartTrap() {
        super("Cart Trap", ModuleCategory.CPVP);
        settings(range, delay, rotate);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastActionTime < delay.getValue()) return;

        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= range.getValue())
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p))).orElse(null);
        if (target == null) return;

        BlockPos feet = target.getBlockPos();
        if (placeWeb(feet.up(2))) return;
        if (placeWeb(feet)) return;

        BlockPos railPos = chooseRailPos(target);
        if (railPos == null) return;

        if (!isRail(railPos)) {
            if (selectItem(RAILS)) placeBlock(railPos.down(), Direction.UP);
            return;
        }
        if (selectItem(CARTS)) placeBlock(railPos, Direction.UP);
    }

    private boolean placeWeb(BlockPos pos) {
        if (!mc.world.isAir(pos) || !selectItem(Items.COBWEB)) return false;
        for (Direction dir : Direction.values()) {
            BlockPos support = pos.offset(dir);
            if (mc.world.getBlockState(support).isAir()) continue;
            placeBlock(support, dir.getOpposite());
            return true;
        }
        return false;
    }

    private BlockPos chooseRailPos(PlayerEntity target) {
        BlockPos feet = target.getBlockPos();
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos pos = feet.offset(dir);
            if (canUseRailPos(pos)) return pos;
        }
        return canUseRailPos(feet) ? feet : null;
    }

    private boolean canUseRailPos(BlockPos pos) {
        return (isRail(pos) || (mc.world.isAir(pos) && !mc.world.getBlockState(pos.down()).isAir())) && mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(pos)) <= range.getValue();
    }

    private boolean isRail(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() instanceof net.minecraft.block.AbstractRailBlock;
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) { mc.player.getInventory().setSelectedSlot(i); return true; }
        return selectItem(p -> false);
    }

    private boolean selectItem(Predicate<net.minecraft.item.Item> predicate) {
        for (int i = 0; i < 9; i++)
            if (predicate.test(mc.player.getInventory().getStack(i).getItem())) { mc.player.getInventory().setSelectedSlot(i); return true; }
        return false;
    }

    private void placeBlock(BlockPos pos, Direction side) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5)), side, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
    }
}
