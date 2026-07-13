package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import starry.events.api.EventHandler;
import starry.events.api.types.EventType;
import starry.events.impl.UsingItemEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AirPlace extends ModuleStructure {
    SliderSettings range = new SliderSettings("Range", "").setValue(5f).range(2f, 8f);
    BooleanSetting onlyMiss = new BooleanSetting("Only Miss", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);

    public AirPlace() {
        super("AirPlace", "Attempts to place block items at your crosshair point without a block face", ModuleCategory.CPVP);
        settings(range, onlyMiss, swing);
    }

    @EventHandler
    public void onItemUse(UsingItemEvent event) {
        if (event.getType() != EventType.START) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (onlyMiss.isValue() && mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.MISS) return;

        Vec3d start = mc.player.getCameraPosVec(1.0F);
        Vec3d look = mc.player.getRotationVec(1.0F);
        BlockPos pos = BlockPos.ofFloored(start.add(look.multiply(range.getValue())));
        if (!mc.world.isAir(pos) && !mc.world.getBlockState(pos).isReplaceable()) return;

        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
            event.cancel();
        }
    }
}