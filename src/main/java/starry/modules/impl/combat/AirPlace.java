package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.BlockInteractionEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AirPlace extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Асtivate Key", "Кеy that does airplace").setKey(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    SliderSettings range = new SliderSettings("Range", "").setValue(5f).range(2f, 8f);
    BooleanSetting onlyMiss = new BooleanSetting("Only Miss", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);

    public AirPlace() {
        super("AirPlace", ModuleCategory.CPVP);
        settings(activateKey, range, onlyMiss, swing);
    }

    @EventHandler
    public void onBlockInteract(BlockInteractionEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (!isKeyPressed(activateKey.getKey())) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (onlyMiss.isValue() && mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.MISS) return;

        Vec3d start = mc.player.getCameraPosVec(1.0F);
        Vec3d end = start.add(new Vec3d(
                -Math.sin(Math.toRadians(mc.player.getYaw())) * Math.cos(Math.toRadians(mc.player.getPitch())),
                -Math.sin(Math.toRadians(mc.player.getPitch())),
                Math.cos(Math.toRadians(mc.player.getYaw())) * Math.cos(Math.toRadians(mc.player.getPitch()))
        ).multiply(range.getValue()));
        BlockPos pos = BlockPos.ofFloored(end);
        if (!mc.world.isAir(pos) && !mc.world.getBlockState(pos).isReplaceable()) return;

        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
            event.cancel();
        }
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode <= 8)
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }
}
