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
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoDTAP extends ModuleStructure {
    SliderSettings placeDelay = new SliderSettings("Place Delay", "").setValue(3f).range(0, 20);
    SliderSettings breakDelay = new SliderSettings("Break Delay", "").setValue(3f).range(0, 20);
    SliderSettings placeChance = new SliderSettings("Place Chance", "").setValue(100f).range(0f, 100f);
    SliderSettings breakChance = new SliderSettings("Break Chance", "").setValue(100f).range(0f, 100f);
    SliderSettings perBlockLimit = new SliderSettings("Per Block Limit", "").setValue(3f).range(1, 8);
    BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "").setValue(true);
    BooleanSetting breakCrystals = new BooleanSetting("Break Crystals", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);

    private final Map<BlockPos, Integer> placed = new HashMap<>();
    private int placeTicks, breakTicks;

    public AutoDTAP() {
        super("Auto DTAP", ModuleCategory.CPVP);
        settings(placeDelay, breakDelay, placeChance, breakChance, perBlockLimit, autoSwitch, breakCrystals, swing);
    }

    @Override
    public void activate() { placed.clear(); placeTicks = 0; breakTicks = 0; }
    @Override
    public void deactivate() { placed.clear(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) { placeTicks = 0; breakTicks = 0; return; }
        placeTicks++;
        breakTicks++;
        if (breakCrystals.isValue() && breakTicks >= breakDelay.getInt()) breakCrystal();
        if (placeTicks >= placeDelay.getInt()) placeCrystal();
    }

    private void placeCrystal() {
        if (ThreadLocalRandom.current().nextInt(100) >= placeChance.getValue()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) && !mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) return;
        if (!canPlaceCrystal(pos)) return;
        if (placed.getOrDefault(pos, 0) >= perBlockLimit.getInt()) return;
        if (!holdingCrystal()) { if (!autoSwitch.isValue() || !selectItem(Items.END_CRYSTAL)) return; }
        mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hit);
        if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        placed.put(pos.toImmutable(), placed.getOrDefault(pos, 0) + 1);
        placeTicks = 0;
    }

    private void breakCrystal() {
        if (ThreadLocalRandom.current().nextInt(100) >= breakChance.getValue()) return;
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof EndCrystalEntity) {
            mc.interactionManager.attackEntity(mc.player, hit.getEntity());
            if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            breakTicks = 0;
            return;
        }
        EndCrystalEntity crystal = mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(5), e -> e.isAlive())
                .stream().min((a, b) -> Double.compare(a.squaredDistanceTo(mc.player), b.squaredDistanceTo(mc.player))).orElse(null);
        if (crystal != null) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            breakTicks = 0;
        }
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        net.minecraft.util.math.BlockPos up = pos.up();
        return mc.world.isAir(up) && mc.world.getOtherEntities(null, new net.minecraft.util.math.Box(up)).stream().noneMatch(e -> e instanceof EndCrystalEntity);
    }

    private boolean holdingCrystal() {
        return mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) || mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }
}
