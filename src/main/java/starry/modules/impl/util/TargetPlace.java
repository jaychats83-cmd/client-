package starry.modules.impl.util;

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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetPlace extends ModuleStructure {
    SelectSetting placement = new SelectSetting("Placement", "").value("TargetFeet", "TargetHead", "Crosshair", "BelowTarget").selected("TargetFeet");
    SliderSettings range = new SliderSettings("Range", "").setValue(5f).range(1f, 8f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(250f).range(0f, 5000f);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);
    BooleanSetting disableAfterPlace = new BooleanSetting("Disable After Place", "").setValue(false);

    private long lastAction;
    private PlayerEntity lastTarget;

    public TargetPlace() {
        super("Target Place", ModuleCategory.CPVP);
        settings(placement, range, delayMs, rotate, disableAfterPlace);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastAction < delayMs.getValue()) return;

        BlockPos target = getTargetPos();
        if (target == null) return;

        if (rotate.isValue() && lastTarget != null) {
            Vec3d aim = lastTarget.getEyePos();
            mc.player.setYaw((float) Math.toDegrees(Math.atan2(aim.z - mc.player.getZ(), aim.x - mc.player.getX())) - 90);
            mc.player.setPitch((float) Math.toDegrees(-Math.atan2(aim.y - mc.player.getEyeY(), mc.player.distanceTo(lastTarget))));
        }

        if (placeAt(target)) {
            lastAction = System.currentTimeMillis();
            if (disableAfterPlace.isValue()) setState(false);
        }
    }

    private BlockPos getTargetPos() {
        lastTarget = null;
        if (placement.isSelected("Crosshair") && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK)
            return hit.getBlockPos().offset(hit.getSide());

        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= range.getValue())
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p))).orElse(null);
        if (target == null) return null;
        lastTarget = target;
        BlockPos feet = target.getBlockPos();
        if (placement.isSelected("TargetHead")) return feet.up();
        if (placement.isSelected("BelowTarget")) return feet.down();
        return feet;
    }

    private boolean placeAt(BlockPos pos) {
        if (mc.player.getMainHandStack().isOf(net.minecraft.item.Items.LAVA_BUCKET) || mc.player.getMainHandStack().isOf(net.minecraft.item.Items.WATER_BUCKET))
            return interactWithNeighbor(pos);
        if (!mc.world.isAir(pos) && !mc.world.getBlockState(pos).isReplaceable()) return false;
        return interactWithNeighbor(pos);
    }

    private boolean interactWithNeighbor(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (mc.world.getBlockState(neighbor).isAir()) continue;
            Direction side = dir.getOpposite();
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(neighbor).add(Vec3d.of(side.getVector()).multiply(0.5)), side, neighbor, false);
            ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            if (result.isAccepted()) { mc.player.swingHand(Hand.MAIN_HAND); return true; }
        }
        return false;
    }
}
