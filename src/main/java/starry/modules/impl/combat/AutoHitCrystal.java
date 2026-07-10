package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.BlockInteractionEvent;
import starry.events.impl.InteractEntityEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoHitCrystal extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Асtivate Key", "Кеy that does hit crystalling").setKey(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    BooleanSetting checkPlace = new BooleanSetting("Check Place", "Checks if you can place the obsidian on that block").setValue(false);
    SliderSettings switchDelay = new SliderSettings("Switch Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings switchChance = new SliderSettings("Switch Chance", "").setValue(100f).range(0f, 100f);
    SliderSettings placeDelay = new SliderSettings("Place Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings placeChance = new SliderSettings("Place Chance", "Randomization").setValue(100f).range(0f, 100f);
    BooleanSetting workWithTotem = new BooleanSetting("Work With Totem", "").setValue(false);
    BooleanSetting workWithCrystal = new BooleanSetting("Work With Crystal", "").setValue(false);
    BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", "Makes the CPS hud think you're legit").setValue(false);
    BooleanSetting swordSwap = new BooleanSetting("Sword Swap", "").setValue(true);

    private long lastPlaceTime, lastSwitchTime;
    private boolean active, crystalling, crystalSelected;

    public AutoHitCrystal() {
        super("Auto Hit Crystal", ModuleCategory.CPVP);
        settings(activateKey, checkPlace, switchDelay, switchChance, placeDelay, placeChance, workWithTotem, workWithCrystal, clickSimulation, swordSwap);
    }

    @Override
    public void activate() { reset(); }
    @Override
    public void deactivate() { reset(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null) return;

        if (isKeyPressed(activateKey.getKey())) {
            if (mc.crosshairTarget instanceof BlockHitResult hit && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
                if (!active && !canPlaceBlockClient(hit.getBlockPos()) && checkPlace.isValue()) return;

            ItemStack stack = mc.player.getMainHandStack();
            if (!(isSword(stack) || (workWithTotem.isValue() && stack.isOf(Items.TOTEM_OF_UNDYING)) || (workWithCrystal.isValue() && stack.isOf(Items.END_CRYSTAL))) && !active)
                return;

            active = true;
            if (!crystalling) {
                if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() != HitResult.Type.MISS) {
                    if (mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.OBSIDIAN)) { crystalling = true; return; }
                    if (mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR) && isAnchorCharged(hit.getBlockPos())) return;

                    mc.options.useKey.setPressed(false);
                    if (!mc.player.isHolding(Items.OBSIDIAN)) {
                        if (!delayPassed(lastSwitchTime, switchDelay)) return;
                        if (ThreadLocalRandom.current().nextInt(1, 101) <= switchChance.getValue()) {
                            lastSwitchTime = System.currentTimeMillis();
                            selectItem(Items.OBSIDIAN);
                        }
                    }
                    if (mc.player.isHolding(Items.OBSIDIAN)) {
                        if (!delayPassed(lastPlaceTime, placeDelay)) return;
                        if (ThreadLocalRandom.current().nextInt(1, 101) <= placeChance.getValue()) {
                            mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hit);
                            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                            lastPlaceTime = System.currentTimeMillis();
                            crystalling = true;
                        }
                    }
                }
            }

            if (crystalling) {
                if (!mc.player.isHolding(Items.END_CRYSTAL) && !crystalSelected) {
                    if (!delayPassed(lastSwitchTime, switchDelay)) return;
                    if (ThreadLocalRandom.current().nextInt(1, 101) <= switchChance.getValue()) {
                        crystalSelected = selectItem(Items.END_CRYSTAL);
                        lastSwitchTime = System.currentTimeMillis();
                    }
                }
                if (mc.player.isHolding(Items.END_CRYSTAL)) {
                    if (mc.crosshairTarget instanceof BlockHitResult hit) {
                        mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hit);
                        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    }
                }
            }
        } else reset();
    }

    @EventHandler
    public void onBlockInteract(BlockInteractionEvent event) {
        ItemStack stack = mc.player.getMainHandStack();
        if ((stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.OBSIDIAN)) && !isKeyPressed(activateKey.getKey()))
            event.cancel();
    }

    @EventHandler
    public void onInteractEntity(InteractEntityEvent event) {
        if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) && !mc.options.attackKey.isPressed())
            event.cancel();
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode <= 8)
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private void reset() {
        lastPlaceTime = 0; lastSwitchTime = 0;
        active = false; crystalling = false; crystalSelected = false;
    }

    private boolean isSword(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_sword");
    }

    private boolean canPlaceBlockClient(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).isReplaceable();
    }

    private boolean isAnchorCharged(BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR) && mc.world.getBlockState(pos).get(net.minecraft.block.RespawnAnchorBlock.CHARGES) > 0;
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

    private boolean delayPassed(long lastActionTime, SliderSettings setting) {
        return lastActionTime == 0 || System.currentTimeMillis() - lastActionTime >= setting.getValue();
    }
}
