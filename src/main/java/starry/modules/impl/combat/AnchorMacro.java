package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.BlockInteractionEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

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

    int switchClock;
    int glowstoneClock;
    int explodeClock;
    final Set<BlockPos> ownedAnchors = new HashSet<>();

    public AnchorMacro() {
        super("Anchor Macro", ModuleCategory.CPVP);
        settings(whileUse, stopOnKill, clickSimulation, placeChance, switchDelay, switchChance, glowstoneDelay, glowstoneChance, explodeDelay, explodeChance, explodeSlot, onlyOwn, onlyCharge);
    }

    @Override
    public void activate() {
        switchClock = 0;
        glowstoneClock = 0;
        explodeClock = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null)
            return;

        if (isHoldingUseFoodOrShield() && whileUse.isValue())
            return;

        if (stopOnKill.isValue() && isDeadBodyNearby())
            return;

        int randomInt = randomInt(1, 100);

        if (!isRightMouseDown())
            return;

        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return;

        BlockPos pos = hit.getBlockPos();
        if (!isBlock(pos, Blocks.RESPAWN_ANCHOR))
            return;

        if (onlyOwn.isValue() && !ownedAnchors.contains(pos))
            return;

        mc.options.useKey.setPressed(false);

        if (isAnchorNotCharged(pos)) {
            randomInt = randomInt(1, 100);

            if (randomInt <= intValue(placeChance)) {
                if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                    if (switchClock != delayValue(switchDelay)) {
                        switchClock++;
                        return;
                    }

                    randomInt = randomInt(1, 100);

                    if (randomInt <= intValue(switchChance)) {
                        switchClock = 0;
                        selectItemFromHotbar(Items.GLOWSTONE);
                    }
                }

                if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                    if (glowstoneClock != delayValue(glowstoneDelay)) {
                        glowstoneClock++;
                        return;
                    }

                    randomInt = randomInt(1, 100);

                    if (randomInt <= intValue(glowstoneChance)) {
                        glowstoneClock = 0;
                        placeBlock(hit);
                    }
                }
            }
        }

        if (isAnchorCharged(pos)) {
            int slot = intValue(explodeSlot) - 1;

            if (mc.player.getInventory().getSelectedSlot() != slot) {
                if (switchClock != delayValue(switchDelay)) {
                    switchClock++;
                    return;
                }

                if (randomInt <= intValue(switchChance)) {
                    switchClock = 0;
                    mc.player.getInventory().setSelectedSlot(slot);
                }
            }

            if (mc.player.getInventory().getSelectedSlot() == slot) {
                if (explodeClock != delayValue(explodeDelay)) {
                    explodeClock++;
                    return;
                }

                randomInt = randomInt(1, 100);

                if (randomInt <= intValue(explodeChance)) {
                    explodeClock = 0;

                    if (!onlyCharge.isValue()) {
                        placeBlock(hit);
                        ownedAnchors.remove(pos);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockInteract(BlockInteractionEvent event) {
        if (mc.player == null || mc.world == null)
            return;

        BlockHitResult hit = event.getHitResult();
        if (hit == null || hit.getType() != HitResult.Type.BLOCK)
            return;

        if (event.getHand() == Hand.MAIN_HAND && mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
            Direction dir = hit.getSide();
            BlockPos pos = hit.getBlockPos();

            if (!mc.world.getBlockState(pos).isReplaceable())
                ownedAnchors.add(pos.offset(dir).toImmutable());
            else
                ownedAnchors.add(pos.toImmutable());
        }

        BlockPos bp = hit.getBlockPos();
        if (isAnchorCharged(bp))
            ownedAnchors.remove(bp);
    }

    private boolean isHoldingUseFoodOrShield() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        boolean holdingUseItem = mainHand.getComponents().contains(DataComponentTypes.FOOD)
                || offHand.getComponents().contains(DataComponentTypes.FOOD)
                || mainHand.getItem() instanceof ShieldItem
                || offHand.getItem() instanceof ShieldItem;

        return holdingUseItem && isRightMouseDown();
    }

    private boolean isDeadBodyNearby() {
        return mc.world.getPlayers().stream().anyMatch(player -> player != mc.player && !player.isAlive() && player.distanceTo(mc.player) < 8.0F);
    }

    private boolean isAnchorCharged(BlockPos pos) {
        return isBlock(pos, Blocks.RESPAWN_ANCHOR) && mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) > 0;
    }

    private boolean isAnchorNotCharged(BlockPos pos) {
        return isBlock(pos, Blocks.RESPAWN_ANCHOR) && mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0;
    }

    private boolean isBlock(BlockPos pos, net.minecraft.block.Block block) {
        return mc.world.getBlockState(pos).isOf(block);
    }

    private boolean selectItemFromHotbar(Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(item)) {
                mc.player.getInventory().setSelectedSlot(slot);
                return true;
            }
        }
        return false;
    }

    private void placeBlock(BlockHitResult hit) {
        if (clickSimulation.isValue())
            mc.options.useKey.setPressed(true);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (clickSimulation.isValue())
            mc.options.useKey.setPressed(false);
    }

    private boolean isRightMouseDown() {
        return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private int intValue(SliderSettings setting) {
        return Math.round(setting.getValue());
    }

    private int delayValue(SliderSettings setting) {
        return Math.max(0, Math.round(setting.getValue() * 0.5F));
    }
}
