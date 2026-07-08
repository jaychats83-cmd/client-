package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.BlockBreakingEvent;
import starry.events.impl.BlockInteractionEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Prevent extends ModuleStructure {
    BooleanSetting doubleGlowstone = new BooleanSetting("Double Glowstone", "").setValue(false);
    BooleanSetting glowstoneMisplace = new BooleanSetting("Glowstone Misplace", "").setValue(false);
    BooleanSetting anchorOnAnchor = new BooleanSetting("Anchor on Anchor", "").setValue(false);
    BooleanSetting obiPunch = new BooleanSetting("Obi Punch", "").setValue(false);
    BooleanSetting echestClick = new BooleanSetting("E-chest click", "").setValue(false);

    public Prevent() {
        super("Prevent", ModuleCategory.MISC);
        settings(doubleGlowstone, glowstoneMisplace, anchorOnAnchor, obiPunch, echestClick);
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            if (isBlock(hit, Blocks.OBSIDIAN) && obiPunch.isValue() && mc.player.isHolding(Items.END_CRYSTAL))
                mc.player.dropSelectedItem(false);
        }
    }

    @EventHandler
    public void onBlockBreaking(BlockBreakingEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            if (isBlock(hit, Blocks.OBSIDIAN) && obiPunch.isValue() && mc.player.isHolding(Items.END_CRYSTAL))
                mc.interactionManager.cancelBlockBreaking();
        }
    }

    @EventHandler
    public void onBlockInteract(BlockInteractionEvent event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        if (isAnchorCharged(hit) && doubleGlowstone.isValue() && mc.player.isHolding(Items.GLOWSTONE))
            event.setCancelled(true);
        if (!isBlock(hit, Blocks.RESPAWN_ANCHOR) && glowstoneMisplace.isValue() && mc.player.isHolding(Items.GLOWSTONE))
            event.setCancelled(true);
        if (!isAnchorCharged(hit) && isBlock(hit, Blocks.RESPAWN_ANCHOR) && anchorOnAnchor.isValue() && mc.player.isHolding(Items.RESPAWN_ANCHOR))
            event.setCancelled(true);
        if (isBlock(hit, Blocks.ENDER_CHEST) && echestClick.isValue() && isPvpItem())
            event.setCancelled(true);
    }

    private boolean isBlock(BlockHitResult hit, net.minecraft.block.Block block) {
        return mc.world != null && mc.world.getBlockState(hit.getBlockPos()).isOf(block);
    }

    private boolean isAnchorCharged(BlockHitResult hit) {
        return isBlock(hit, Blocks.RESPAWN_ANCHOR) && mc.world.getBlockState(hit.getBlockPos()).get(net.minecraft.block.RespawnAnchorBlock.CHARGES) > 0;
    }

    private boolean isPvpItem() {
        var stack = mc.player.getMainHandStack();
        return isSword(stack) || stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.OBSIDIAN)
                || stack.isOf(Items.RESPAWN_ANCHOR) || stack.isOf(Items.GLOWSTONE);
    }

    private boolean isSword(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_sword");
    }
}
