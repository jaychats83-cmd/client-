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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnchorMacro extends ModuleStructure {
    BooleanSetting whileUse = new BooleanSetting("While Use", "If it should trigger while eating/using shield").setValue(true);
    BooleanSetting stopOnKill = new BooleanSetting("Stop on Kill", "Doesn't anchor if body nearby").setValue(false);
    BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", "Makes the CPS hud think you're legit").setValue(false);
    SliderSettings switchDelay = new SliderSettings("Switch Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings glowstoneDelay = new SliderSettings("Glowstone Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings explodeDelay = new SliderSettings("Explode Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings explodeSlot = new SliderSettings("Explode Slot", "").setValue(1f).range(1, 9);
    BooleanSetting onlyOwn = new BooleanSetting("Only Own", "").setValue(false);
    BooleanSetting onlyCharge = new BooleanSetting("Only Charge", "").setValue(false);

    long lastSwitchTime;
    long lastGlowstoneTime;
    long lastExplodeTime;
    final Set<BlockPos> ownedAnchors = new HashSet<>();

    public AnchorMacro() {
        super("Anchor Macro", ModuleCategory.CPVP);
        settings(whileUse, stopOnKill, clickSimulation, switchDelay, glowstoneDelay, explodeDelay, explodeSlot, onlyOwn, onlyCharge);
    }

    @Override
    public void activate() {
        lastSwitchTime = 0;
        lastGlowstoneTime = 0;
        lastExplodeTime = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null)
            return;

        if (isHoldingUseFoodOrShield() && whileUse.isValue())
            return;

        if (stopOnKill.isValue() && isDeadBodyNearby())
            return;

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
            if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                if (!delayPassed(lastSwitchTime, switchDelay)) {
                    return;
                }

                lastSwitchTime = System.currentTimeMillis();
                selectItemFromHotbar(Items.GLOWSTONE);
            }

            if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                if (!delayPassed(lastGlowstoneTime, glowstoneDelay)) {
                    return;
                }

                lastGlowstoneTime = System.currentTimeMillis();
                placeBlock(hit);
            }
        }

        if (isAnchorCharged(pos)) {
            int slot = intValue(explodeSlot) - 1;

            if (mc.player.getInventory().getSelectedSlot() != slot) {
                if (!delayPassed(lastSwitchTime, switchDelay)) {
                    return;
                }

                lastSwitchTime = System.currentTimeMillis();
                mc.player.getInventory().setSelectedSlot(slot);
            }

            if (mc.player.getInventory().getSelectedSlot() == slot) {
                if (!delayPassed(lastExplodeTime, explodeDelay)) {
                    return;
                }

                lastExplodeTime = System.currentTimeMillis();

                if (!onlyCharge.isValue()) {
                    placeBlock(hit);
                    ownedAnchors.remove(pos);
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

    private int intValue(SliderSettings setting) {
        return Math.round(setting.getValue());
    }

    private boolean delayPassed(long lastActionTime, SliderSettings setting) {
        return lastActionTime == 0 || System.currentTimeMillis() - lastActionTime >= setting.getValue();
    }
}
