package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartPvPModule extends ModuleStructure {
    SelectSetting mode = new SelectSetting("Mode", "").value("Auto Cart", "Cart Trap", "Safe Cart").selected("Auto Cart");
    SliderSettings range = new SliderSettings("Range", "").setValue(5f).range(1f, 8f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(75f).range(0f, 1000f);
    SliderSettings safeDistance = new SliderSettings("Safe Distance", "").setValue(3f).range(0f, 8f);
    BooleanSetting preferActivatorRail = new BooleanSetting("Activator Rail", "").setValue(true);
    BooleanSetting preferTntCart = new BooleanSetting("Prefer TNT Cart", "").setValue(true);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);
    BooleanSetting trapHead = new BooleanSetting("Trap Head", "").setValue(true);

    private long lastActionTime;

    public CartPvPModule() {
        super("Cart PvP", ModuleCategory.CPVP);
        settings(mode, range, delayMs, safeDistance, preferActivatorRail, preferTntCart, rotate, trapHead);
    }

    @Override
    public void activate() { lastActionTime = 0; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastActionTime < delayMs.getValue()) return;

        PlayerEntity target = findNearestPlayer((float) range.getValue());
        if (target == null || !target.isAlive()) return;

        if (mode.isSelected("Safe Cart") && mc.player.distanceTo(target) < safeDistance.getValue()) return;

        if (rotate.isValue()) {
            Vec3d aim = target.getEyePos();
            mc.player.setYaw((float) Math.toDegrees(Math.atan2(aim.z - mc.player.getZ(), aim.x - mc.player.getX())) - 90);
            mc.player.setPitch((float) Math.toDegrees(-Math.atan2(aim.y - mc.player.getEyeY(), mc.player.distanceTo(target))));
        }

        BlockPos base = chooseRailPos(target);
        if (base == null) return;

        BlockPos targetFeet = target.getBlockPos();
        if (mode.isSelected("Cart Trap") && trapHead.isValue() && placeBlockIfAir(targetFeet.up(2), Items.COBWEB)) return;
        if (mode.isSelected("Cart Trap") && placeBlockIfAir(targetFeet, Items.COBWEB)) return;

        if (!(mc.world.getBlockState(base).getBlock() instanceof AbstractRailBlock)) {
            if (mc.world.isAir(base) && selectRail() && click(base.down(), Direction.UP)) {
                lastActionTime = System.currentTimeMillis();
            }
            return;
        }

        if (selectCart() && click(base, Direction.UP)) {
            lastActionTime = System.currentTimeMillis();
        }
    }

    private boolean placeBlockIfAir(BlockPos pos, net.minecraft.item.Item item) {
        if (!mc.world.isAir(pos) || !selectItem(item)) return false;
        for (Direction direction : Direction.values()) {
            BlockPos support = pos.offset(direction);
            if (!mc.world.getBlockState(support).isAir() && click(support, direction.getOpposite())) {
                lastActionTime = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    private boolean selectRail() {
        if (preferActivatorRail.isValue() && selectItem(Items.ACTIVATOR_RAIL)) return true;
        return selectItem(i -> i == Items.RAIL || i == Items.POWERED_RAIL || i == Items.ACTIVATOR_RAIL || i == Items.DETECTOR_RAIL);
    }

    private boolean selectCart() {
        if (preferTntCart.isValue() && selectItem(Items.TNT_MINECART)) return true;
        return selectItem(i -> i == Items.TNT_MINECART || i == Items.MINECART);
    }

    private boolean selectItem(java.util.function.Predicate<net.minecraft.item.Item> predicate) {
        for (int i = 0; i < 9; i++)
            if (predicate.test(mc.player.getInventory().getStack(i).getItem())) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        return false;
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        return selectItem(i -> i == item);
    }

    private BlockPos chooseRailPos(PlayerEntity target) {
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos feet = target.getBlockPos();
        for (Direction direction : Direction.Type.HORIZONTAL) candidates.add(feet.offset(direction));
        candidates.add(feet);
        return candidates.stream()
                .filter(pos -> mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(pos)) <= range.getValue())
                .filter(this::canUseRailPos)
                .min(Comparator.comparingDouble(pos -> Vec3d.ofCenter(pos).squaredDistanceTo(target.getEntityPos())))
                .orElse(null);
    }

    private boolean canUseRailPos(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() instanceof AbstractRailBlock) return true;
        return mc.world.isAir(pos) && !mc.world.getBlockState(pos.down()).isAir() && !hasBlockingEntity(pos);
    }

    private boolean hasBlockingEntity(BlockPos pos) {
        return mc.world.getOtherEntities(null, new net.minecraft.util.math.Box(pos)).stream()
                .anyMatch(entity -> entity instanceof PlayerEntity || entity instanceof net.minecraft.entity.vehicle.AbstractMinecartEntity);
    }

    private boolean click(BlockPos block, Direction side) {
        if (mc.world.getBlockState(block).isAir()) return false;
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(block).add(Vec3d.of(side.getVector()).multiply(0.5)), side, block, false);
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) { mc.player.swingHand(Hand.MAIN_HAND); return true; }
        return false;
    }

    private PlayerEntity findNearestPlayer(float range) {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= range)
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
    }
}
