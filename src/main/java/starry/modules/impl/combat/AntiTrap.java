package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AntiTrap extends ModuleStructure {
    SliderSettings delay = new SliderSettings("Delay MS", "").setValue(150f).range(50f, 1000f);
    BooleanSetting selectPickaxe = new BooleanSetting("Select Pickaxe", "").setValue(true);
    BooleanSetting includeWebs = new BooleanSetting("Break Webs", "").setValue(true);

    private long lastBreakTime;

    public AntiTrap() {
        super("AntiTrap", ModuleCategory.CPVP);
        settings(delay, selectPickaxe, includeWebs);
    }

    @Override
    public void activate() {
        lastBreakTime = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastBreakTime < delay.getValue()) return;

        BlockPos target = trapBlock();
        if (target == null) return;

        if (selectPickaxe.isValue()) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isIn(ItemTags.PICKAXES)) {
                    mc.player.getInventory().setSelectedSlot(i);
                    break;
                }
            }
        }

        Direction side = Direction.getFacing(
                mc.player.getX() - (target.getX() + 0.5D),
                mc.player.getY() - (target.getY() + 0.5D),
                mc.player.getZ() - (target.getZ() + 0.5D));
        mc.interactionManager.attackBlock(target, side);
        mc.interactionManager.updateBlockBreakingProgress(target, side);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        lastBreakTime = System.currentTimeMillis();
    }

    private BlockPos trapBlock() {
        BlockPos feet = mc.player.getBlockPos();
        BlockPos[] checks = {
                feet, feet.up(),
                feet.north(), feet.south(), feet.east(), feet.west(),
                feet.up().north(), feet.up().south(), feet.up().east(), feet.up().west(), feet.up(2)
        };
        for (BlockPos pos : checks) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) continue;
            if (!includeWebs.isValue() && state.getBlock().getTranslationKey().contains("cobweb")) continue;
            if (state.getHardness(mc.world, pos) >= 0) return pos;
        }
        return null;
    }
}
