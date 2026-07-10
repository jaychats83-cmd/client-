package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoDTAP extends ModuleStructure {
    SliderSettings stepDelay = new SliderSettings("Step Delay MS", "Delay between DTAP actions").setValue(50f).range(0f, 500f);
    SliderSettings range = new SliderSettings("Range", "").setValue(4.5f).range(2f, 6f);
    BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);
    BooleanSetting restoreSlot = new BooleanSetting("Restore Slot", "").setValue(true);

    PlayerEntity target;
    BlockPos obsidianPos;
    int stage;
    long lastStageTime;
    int originalSlot = -1;
    boolean wasPressed;

    public AutoDTAP() {
        super("Auto DTAP", "Hit, place obsidian, place crystal, and break it for a CPVP double tap", ModuleCategory.CPVP);
        settings(stepDelay, range, autoSwitch, swing, restoreSlot);
    }

    @Override
    public void activate() {
        reset();
    }

    @Override
    public void deactivate() {
        restoreOriginalSlot();
        reset();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
            reset();
            return;
        }

        boolean pressed = mc.options.attackKey.isPressed();
        if (stage == 0 && pressed && !wasPressed) {
            target = findCrosshairPlayer();
            if (target != null) {
                originalSlot = mc.player.getInventory().getSelectedSlot();
                obsidianPos = findObsidianPos(target);
                stage = 1;
                lastStageTime = 0;
            }
        }
        wasPressed = pressed;

        if (stage == 0) return;
        if (!isValidTarget(target) || obsidianPos == null) {
            restoreOriginalSlot();
            reset();
            return;
        }

        if (lastStageTime != 0 && System.currentTimeMillis() - lastStageTime < stepDelay.getValue()) return;

        switch (stage) {
            case 1 -> {
                mc.interactionManager.attackEntity(mc.player, target);
                if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
                nextStage();
            }
            case 2 -> {
                if (!mc.world.getBlockState(obsidianPos).isOf(Blocks.OBSIDIAN)) {
                    if (!selectItem(Items.OBSIDIAN) || !placeBlock(obsidianPos)) {
                        restoreOriginalSlot();
                        reset();
                        return;
                    }
                }
                nextStage();
            }
            case 3 -> {
                if (!selectItem(Items.END_CRYSTAL) || !placeCrystal(obsidianPos)) {
                    restoreOriginalSlot();
                    reset();
                    return;
                }
                nextStage();
            }
            case 4 -> {
                EndCrystalEntity crystal = findCrystal(obsidianPos);
                if (crystal != null) {
                    mc.interactionManager.attackEntity(mc.player, crystal);
                    if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
                }
                restoreOriginalSlot();
                reset();
            }
        }
    }

    private void nextStage() {
        stage++;
        lastStageTime = System.currentTimeMillis();
    }

    private PlayerEntity findCrosshairPlayer() {
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity player && isValidTarget(player)) {
            return player;
        }
        return mc.world.getPlayers().stream()
                .filter(this::isValidTarget)
                .filter(player -> mc.player.distanceTo(player) <= range.getValue())
                .min((a, b) -> Double.compare(mc.player.squaredDistanceTo(a), mc.player.squaredDistanceTo(b)))
                .orElse(null);
    }

    private boolean isValidTarget(PlayerEntity player) {
        return player != null && player != mc.player && player.isAlive() && !player.isSpectator()
                && mc.player.squaredDistanceTo(player) <= range.getValue() * range.getValue();
    }

    private BlockPos findObsidianPos(PlayerEntity player) {
        BlockPos feet = player.getBlockPos();
        BlockPos[] candidates = {
                feet.down(),
                feet.offset(player.getHorizontalFacing().getOpposite()).down(),
                feet.offset(player.getHorizontalFacing()).down(),
                feet.offset(player.getHorizontalFacing().rotateYClockwise()).down(),
                feet.offset(player.getHorizontalFacing().rotateYCounterclockwise()).down()
        };
        for (BlockPos pos : candidates) {
            if (canUseObsidianSpot(pos)) return pos;
        }
        return null;
    }

    private boolean canUseObsidianSpot(BlockPos pos) {
        return (mc.world.getBlockState(pos).isReplaceable() || mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN))
                && mc.world.getBlockState(pos.up()).isReplaceable();
    }

    private boolean placeBlock(BlockPos pos) {
        BlockHitResult hit = neighborHit(pos);
        if (hit == null) return false;
        var result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted() && swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
        return result.isAccepted();
    }

    private boolean placeCrystal(BlockPos obsidian) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(obsidian).add(0, 0.5, 0), Direction.UP, obsidian, false);
        var result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted() && swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
        return result.isAccepted();
    }

    private BlockHitResult neighborHit(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (mc.world.getBlockState(neighbor).isAir() || mc.world.getBlockState(neighbor).isReplaceable()) continue;
            Direction side = direction.getOpposite();
            return new BlockHitResult(Vec3d.ofCenter(neighbor).add(Vec3d.of(side.getVector()).multiply(0.5)), side, neighbor, false);
        }
        return null;
    }

    private EndCrystalEntity findCrystal(BlockPos obsidian) {
        Box box = new Box(obsidian.up()).expand(2.5);
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, box, EndCrystalEntity::isAlive)
                .stream()
                .min((a, b) -> Double.compare(a.squaredDistanceTo(target), b.squaredDistanceTo(target)))
                .orElse(null);
    }

    private boolean selectItem(Item item) {
        if (mc.player.getMainHandStack().isOf(item)) return true;
        if (!autoSwitch.isValue()) return false;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private void restoreOriginalSlot() {
        if (restoreSlot.isValue() && mc.player != null && originalSlot >= 0 && originalSlot <= 8) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
    }

    private void reset() {
        target = null;
        obsidianPos = null;
        stage = 0;
        lastStageTime = 0;
        originalSlot = -1;
    }
}
