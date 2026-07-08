package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.BlockInteractionEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnchorMacro extends ModuleStructure {
    BooleanSetting whileUse = new BooleanSetting("While Use", "If it should trigger while eating/using shield").setValue(true);
    BooleanSetting stopOnKill = new BooleanSetting("Stop on Kill", "Doesn't anchor if body nearby").setValue(false);
    BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", "Makes the CPS hud think you're legit").setValue(false);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "").setValue(0f).range(0, 20);
    SliderSettings switchChance = new SliderSettings("Switch Chance", "").setValue(100f).range(0f, 100f);
    SliderSettings placeChance = new SliderSettings("Place Chance", "Randomization").setValue(100f).range(0f, 100f);
    SliderSettings glowstoneDelay = new SliderSettings("Glowstone Delay", "").setValue(0f).range(0, 20);
    SliderSettings glowstoneChance = new SliderSettings("Glowstone Chance", "").setValue(100f).range(0f, 100f);
    SliderSettings explodeDelay = new SliderSettings("Explode Delay", "").setValue(0f).range(0, 20);
    SliderSettings explodeChance = new SliderSettings("Explode Chance", "").setValue(100f).range(0f, 100f);
    SliderSettings explodeSlot = new SliderSettings("Explode Slot", "").setValue(1f).range(1, 9);
    BooleanSetting onlyOwn = new BooleanSetting("Only Own", "").setValue(false);
    BooleanSetting onlyCharge = new BooleanSetting("Only Charge", "").setValue(false);

    private int switchClock, glowstoneClock, explodeClock;
    private final Set<BlockPos> ownedAnchors = new HashSet<>();

    public AnchorMacro() {
        super("Anchor Macro", ModuleCategory.CPVP);
        settings(whileUse, stopOnKill, clickSimulation, placeChance, switchDelay, switchChance, glowstoneDelay, glowstoneChance, explodeDelay, explodeChance, explodeSlot, onlyOwn, onlyCharge);
    }

    @Override
    public void activate() {
        switchClock = 0; glowstoneClock = 0; explodeClock = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null) return;
        if (isUsingItem() && whileUse.isValue()) return;
        if (stopOnKill.isValue() && isDeadBodyNearby()) return;

        if (mc.options.useKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos pos = hit.getBlockPos();
            if (mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
                if (onlyOwn.isValue() && !ownedAnchors.contains(pos)) return;
                mc.options.useKey.setPressed(false);

                if (!isAnchorCharged(pos)) {
                    if (randomInt(1, 100) <= placeChance.getValue()) {
                        if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                            if (switchClock < switchDelay.getValue()) { switchClock++; return; }
                            if (randomInt(1, 100) <= switchChance.getValue()) {
                                switchClock = 0;
                                selectItem(Items.GLOWSTONE);
                            }
                        }
                        if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                            if (glowstoneClock < glowstoneDelay.getValue()) { glowstoneClock++; return; }
                            if (randomInt(1, 100) <= glowstoneChance.getValue()) {
                                glowstoneClock = 0;
                                placeBlock(hit);
                            }
                        }
                    }
                } else {
                    int slot = explodeSlot.getInt() - 1;
                    if (mc.player.getInventory().getSelectedSlot() != slot) {
                        if (switchClock < switchDelay.getValue()) { switchClock++; return; }
                        if (randomInt(1, 100) <= switchChance.getValue()) {
                            switchClock = 0;
                            mc.player.getInventory().setSelectedSlot(slot);
                        }
                    }
                    if (mc.player.getInventory().getSelectedSlot() == slot) {
                        if (explodeClock < explodeDelay.getValue()) { explodeClock++; return; }
                        if (randomInt(1, 100) <= explodeChance.getValue()) {
                            explodeClock = 0;
                            if (!onlyCharge.isValue()) {
                                placeBlock(hit);
                                ownedAnchors.remove(pos);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockInteract(BlockInteractionEvent event) {
        if (event.getHitResult() instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            if (mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                Direction dir = hit.getSide();
                BlockPos pos = hit.getBlockPos();
                if (!mc.world.getBlockState(pos).isReplaceable()) {
                    ownedAnchors.add(pos.offset(dir));
                } else {
                    ownedAnchors.add(pos);
                }
            }
            if (isAnchorCharged(event.getHitResult().getBlockPos()))
                ownedAnchors.remove(event.getHitResult().getBlockPos());
        }
    }

    private boolean isAnchorCharged(BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR) && mc.world.getBlockState(pos).get(net.minecraft.block.RespawnAnchorBlock.CHARGES) > 0;
    }

    private boolean isUsingItem() {
        return (mc.player.getMainHandStack().getItem() instanceof ShieldItem || mc.player.getOffHandStack().getItem() instanceof ShieldItem)
                && mc.options.useKey.isPressed();
    }

    private boolean isDeadBodyNearby() {
        return mc.world.getPlayers().stream().anyMatch(e -> e != mc.player && !e.isAlive() && e.distanceTo(mc.player) < 8);
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    private void placeBlock(BlockHitResult hit) {
        mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hit);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }
}
