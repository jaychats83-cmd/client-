package starry.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.function.Predicate;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartTrap extends ModuleStructure {
    SelectSetting targetMode = new SelectSetting("Target", "").value("Nearest", "Crosshair").selected("Nearest");
    SelectSetting railMode = new SelectSetting("Rail", "").value("Activator", "Any", "Normal", "Powered", "Detector").selected("Activator");
    SelectSetting cartMode = new SelectSetting("Cart", "").value("TNT", "Any", "Normal").selected("TNT");
    SliderSettings range = new SliderSettings("Range", "").setValue(5f).range(1f, 8f);
    SliderSettings delay = new SliderSettings("Delay MS", "").setValue(100f).range(0f, 1000f);
    SliderSettings cartAmount = new SliderSettings("Cart Amount", "").setValue(1f).range(1f, 4f);
    BooleanSetting seeOnly = new BooleanSetting("See Only", "").setValue(false);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "").setValue(true);
    BooleanSetting trapFeet = new BooleanSetting("Trap Feet", "").setValue(true);
    BooleanSetting trapHead = new BooleanSetting("Trap Head", "").setValue(true);

    private long lastActionTime;
    private int previousSlot = -1;
    private static final Predicate<net.minecraft.item.Item> RAILS = item -> item == Items.RAIL || item == Items.POWERED_RAIL || item == Items.ACTIVATOR_RAIL || item == Items.DETECTOR_RAIL;
    private static final Predicate<net.minecraft.item.Item> CARTS = item -> item == Items.TNT_MINECART || item == Items.MINECART;

    public CartTrap() {
        super("Cart Trap", ModuleCategory.CPVP);
        settings(targetMode, railMode, cartMode, range, delay, cartAmount, seeOnly, rotate, swing, switchBack, trapFeet, trapHead);
    }

    @Override
    public void activate() { previousSlot = -1; lastActionTime = 0; }
    @Override
    public void deactivate() { restoreSlot(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastActionTime < delay.getValue()) return;

        PlayerEntity target = findTarget();
        if (target == null) return;
        if (rotate.isValue()) rotateTo(target);

        BlockPos feet = target.getBlockPos();
        if (trapHead.isValue() && placeWeb(feet.up(2))) return;
        if (trapFeet.isValue() && placeWeb(feet)) return;

        BlockPos railPos = chooseRailPos(target);
        if (railPos == null) return;

        if (!isRail(railPos)) {
            if (selectRail()) placeBlock(railPos.down(), Direction.UP);
            return;
        }
        if (selectCart()) {
            boolean placed = false;
            for (int i = 0; i < cartAmount.getInt(); i++) {
                if (!placeBlock(railPos, Direction.UP)) break;
                placed = true;
            }
            if (placed) restoreSlot();
        }
    }

    private boolean placeWeb(BlockPos pos) {
        if (!mc.world.isAir(pos) || !selectItem(Items.COBWEB)) return false;
        for (Direction dir : Direction.values()) {
            BlockPos support = pos.offset(dir);
            if (mc.world.getBlockState(support).isAir()) continue;
            return placeBlock(support, dir.getOpposite());
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
        return selectItem(i -> i == item);
    }

    private boolean selectItem(Predicate<net.minecraft.item.Item> predicate) {
        for (int i = 0; i < 9; i++)
            if (predicate.test(mc.player.getInventory().getStack(i).getItem())) {
                saveSlot();
                selectSlot(i);
                return true;
            }
        return false;
    }

    private boolean selectRail() {
        if (railMode.isSelected("Activator") && selectItem(Items.ACTIVATOR_RAIL)) return true;
        if (railMode.isSelected("Normal")) return selectItem(Items.RAIL);
        if (railMode.isSelected("Powered")) return selectItem(Items.POWERED_RAIL);
        if (railMode.isSelected("Detector")) return selectItem(Items.DETECTOR_RAIL);
        return selectItem(RAILS);
    }

    private boolean selectCart() {
        if (cartMode.isSelected("TNT") && selectItem(Items.TNT_MINECART)) return true;
        if (cartMode.isSelected("Normal")) return selectItem(Items.MINECART);
        return selectItem(CARTS);
    }

    private boolean placeBlock(BlockPos pos, Direction side) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5)), side, pos, false);
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (!result.isAccepted()) return false;
        if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
        return true;
    }

    private PlayerEntity findTarget() {
        if (targetMode.isSelected("Crosshair")
                && mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity player
                && isValidTarget(player)) return player;

        return mc.world.getPlayers().stream()
                .filter(this::isValidTarget)
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p))).orElse(null);
    }

    private boolean isValidTarget(PlayerEntity player) {
        return player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range.getValue()
                && (!seeOnly.isValue() || mc.player.canSee(player));
    }

    private void rotateTo(PlayerEntity target) {
        Vec3d aim = target.getEyePos();
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(aim.z - mc.player.getZ(), aim.x - mc.player.getX())) - 90);
        mc.player.setPitch((float) Math.toDegrees(-Math.atan2(aim.y - mc.player.getEyeY(), mc.player.distanceTo(target))));
    }

    private void saveSlot() {
        if (previousSlot == -1) previousSlot = mc.player.getInventory().getSelectedSlot();
    }

    private void restoreSlot() {
        if (switchBack.isValue() && mc.player != null && previousSlot >= 0 && previousSlot < 9)
            selectSlot(previousSlot);
        previousSlot = -1;
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
}