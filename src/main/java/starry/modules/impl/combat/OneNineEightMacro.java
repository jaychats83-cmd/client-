package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class OneNineEightMacro extends ModuleStructure {
    SliderSettings obbyDelay = new SliderSettings("Obby Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings crystalDelay = new SliderSettings("Crystal Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings placeChance = new SliderSettings("Place Chance", "").setValue(100f).range(0f, 100f);
    SliderSettings breakChance = new SliderSettings("Break Chance", "").setValue(100f).range(0f, 100f);
    BooleanSetting silentSwitch = new BooleanSetting("Silent Switch", "").setValue(false);
    BooleanSetting breakCrystal = new BooleanSetting("Break Crystal", "").setValue(true);
    SliderSettings breakDelay = new SliderSettings("Break Delay MS", "").setValue(1000f).range(0f, 5000f);

    private enum MacroState { IDLE, PLACE_OBI, WAIT_OBI, PLACE_CRYSTAL, BREAK_CRYSTAL }
    private MacroState state = MacroState.IDLE;
    private BlockPos basePos;
    private long lastObbyTime, lastCrystalTime;

    public OneNineEightMacro() {
        super("One Nine Eight Macro", ModuleCategory.CPVP);
        settings(obbyDelay, crystalDelay, placeChance, breakChance, silentSwitch, breakCrystal, breakDelay);
    }

    @Override
    public void activate() { reset(); }

    private void reset() {
        lastObbyTime = 0; lastCrystalTime = 0;
        state = MacroState.IDLE; basePos = null;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) { reset(); return; }
        tryBreakCrystal();
        for (int i = 0; i < 4; i++) {
            MacroState before = state;
            switch (state) {
                case IDLE -> capture();
                case PLACE_OBI -> placeObsidian();
                case WAIT_OBI -> waitForObsidian();
                case PLACE_CRYSTAL -> placeCrystal();
                case BREAK_CRYSTAL -> finishOrRepeat();
            }
            if (state == before) break;
        }
    }

    private void capture() {
        HitResult result = mc.player.raycast(5.0, 1.0F, false);
        if (!(result instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || hit.getSide() != Direction.UP) return;

        BlockPos hitPos = hit.getBlockPos();
        if (isCrystalBase(hitPos)) { basePos = hitPos.toImmutable(); state = MacroState.PLACE_CRYSTAL; return; }
        basePos = hitPos.up().toImmutable();
        state = MacroState.PLACE_OBI;
    }

    private void placeObsidian() {
        if (!delayPassed(lastObbyTime, obbyDelay) || !roll(placeChance.getInt())) return;
        if (!(mc.player.raycast(5.0, 1.0F, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) { state = MacroState.IDLE; return; }
        if (!selectItem(Items.OBSIDIAN)) return;
        placeBlock(hit);
        lastObbyTime = System.currentTimeMillis();
        state = MacroState.WAIT_OBI;
    }

    private void waitForObsidian() {
        if (isCrystalBase(basePos)) { state = MacroState.PLACE_CRYSTAL; return; }
        BlockPos down = basePos.down();
        if (isCrystalBase(down)) { basePos = down; state = MacroState.PLACE_CRYSTAL; return; }
        state = MacroState.IDLE;
    }

    private void placeCrystal() {
        if (!delayPassed(lastCrystalTime, crystalDelay) || !isCrystalBase(basePos)) return;
        if (!canPlaceCrystal(basePos)) { state = MacroState.BREAK_CRYSTAL; return; }
        if (!roll(placeChance.getInt())) return;
        if (!selectItem(Items.END_CRYSTAL)) return;

        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(basePos).add(0, 0.5, 0), Direction.UP, basePos, false);
        placeBlock(hit);
        lastCrystalTime = System.currentTimeMillis();
        state = MacroState.BREAK_CRYSTAL;
    }

    private void finishOrRepeat() {
        state = isCrystalBase(basePos) ? MacroState.PLACE_CRYSTAL : MacroState.IDLE;
    }

    private void tryBreakCrystal() {
        if (!breakCrystal.isValue() || !delayPassed(lastCrystalTime, breakDelay) || !roll(breakChance.getInt())) return;

        if (mc.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof EndCrystalEntity crystal) {
            attackCrystal(crystal); lastCrystalTime = System.currentTimeMillis(); return;
        }
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal && crystal.distanceTo(mc.player) < 9.0F) {
                attackCrystal(crystal); lastCrystalTime = System.currentTimeMillis(); return;
            }
        }
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        mc.interactionManager.attackEntity(mc.player, crystal);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isCrystalBase(BlockPos pos) {
        return pos != null && (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK));
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        return mc.world.getBlockState(pos.up()).isAir() && mc.world.getOtherEntities(null, new net.minecraft.util.math.Box(pos.up())).stream()
                .noneMatch(e -> e instanceof EndCrystalEntity);
    }

    private boolean roll(int chance) { return Math.random() * 100 < chance; }

    private boolean selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) { mc.player.getInventory().setSelectedSlot(i); return true; }
        return false;
    }

    private void placeBlock(BlockHitResult hit) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean delayPassed(long lastActionTime, SliderSettings setting) {
        return lastActionTime == 0 || System.currentTimeMillis() - lastActionTime >= setting.getValue();
    }
}