package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.CameraEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Freecam extends ModuleStructure {
    public static Freecam getInstance() {
        return Instance.get(Freecam.class);
    }

    public SliderSettings speed = new SliderSettings("Speed", "").setValue(1f).range(1f, 10f);
    public Vec3d oldPos = Vec3d.ZERO;
    public Vec3d pos = Vec3d.ZERO;

    public Freecam() {
        super("Freecam", ModuleCategory.RENDER);
        settings(speed);
    }

    @Override
    public void activate() {
        if (mc.world != null) oldPos = pos = mc.player.getEyePos();
    }

    @Override
    public void deactivate() {
        if (mc.world != null) { mc.player.setVelocity(Vec3d.ZERO); mc.worldRenderer.reload(); }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);

        float f = (float) Math.PI / 180;
        Vec3d forward = new Vec3d(-MathHelper.sin(-mc.player.getYaw() * f - (float) Math.PI), 0, -MathHelper.cos(-mc.player.getYaw() * f - (float) Math.PI));
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d left = up.crossProduct(forward);
        Vec3d movement = Vec3d.ZERO;

        if (isKeyPressed(GLFW.GLFW_KEY_W)) movement = movement.add(forward);
        if (isKeyPressed(GLFW.GLFW_KEY_S)) movement = movement.subtract(forward);
        if (isKeyPressed(GLFW.GLFW_KEY_A)) movement = movement.add(left);
        if (isKeyPressed(GLFW.GLFW_KEY_D)) movement = movement.subtract(left);
        if (isKeyPressed(GLFW.GLFW_KEY_SPACE)) movement = movement.add(0, speed.getValue(), 0);
        if (isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) movement = movement.add(0, -speed.getValue(), 0);

        double mult = isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) ? 2 : 1;
        movement = movement.normalize().multiply(speed.getValue() * mult);

        oldPos = pos;
        pos = pos.add(movement);
    }

    @EventHandler
    public void onCameraUpdate(CameraEvent event) {
        event.setCameraClip(true);
    }

    private boolean isKeyPressed(int key) {
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}
