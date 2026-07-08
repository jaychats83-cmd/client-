package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.BlockBreakingEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTool extends ModuleStructure {
    public AutoTool() {
        super("Auto Tool", ModuleCategory.MISC);
    }

    @EventHandler
    public void onBlockBreaking(BlockBreakingEvent event) {
        if (mc.player == null || mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        var state = mc.world.getBlockState(hit.getBlockPos());
        int bestSlot = mc.player.getInventory().getSelectedSlot();
        float bestSpeed = mc.player.getInventory().getStack(bestSlot).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) { bestSpeed = speed; bestSlot = slot; }
        }
        if (bestSlot != mc.player.getInventory().getSelectedSlot()) mc.player.getInventory().setSelectedSlot(bestSlot);
    }
}
