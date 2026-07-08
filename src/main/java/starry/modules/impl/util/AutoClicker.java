package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoClicker extends ModuleStructure {
    BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", "").setValue(true);
    BooleanSetting onlyBlocks = new BooleanSetting("Only Blocks", "").setValue(true);
    BooleanSetting onClick = new BooleanSetting("On Click", "").setValue(true);
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0f, 1000f);
    SliderSettings chance = new SliderSettings("Chance", "").setValue(100f).range(0f, 100f);
    SelectSetting mode = new SelectSetting("Actions", "").value("All", "Left", "Right").selected("All");

    private long lastClick;

    public AutoClicker() {
        super("Auto Clicker", ModuleCategory.MISC);
        settings(onlyWeapon, onlyBlocks, onClick, delay, chance, mode);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.currentScreen != null || mc.crosshairTarget == null) return;
        if (System.currentTimeMillis() - lastClick < delay.getValue() || Math.random() * 100 > chance.getValue()) return;

        if (mode.isSelected("Left") || mode.isSelected("All")) performLeftClick();
        if (mode.isSelected("Right") || mode.isSelected("All")) performRightClick();
    }

    private void performLeftClick() {
        if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) return;
        if (mc.player.isUsingItem()) return;
        if (onlyWeapon.isValue() && !isWeapon(mc.player.getMainHandStack())) return;
        if (onClick.isValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) return;
        if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit) {
            mc.interactionManager.attackEntity(mc.player, hit.getEntity());
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            lastClick = System.currentTimeMillis();
        }
    }

    private void performRightClick() {
        var mainhand = mc.player.getMainHandStack();
        var offhand = mc.player.getOffHandStack();
        if (mainhand.getComponents().contains(DataComponentTypes.FOOD) || offhand.getComponents().contains(DataComponentTypes.FOOD)) return;
        if (isRangedWeapon(mainhand) || isRangedWeapon(offhand)) return;
        if (onClick.isValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) return;
        mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
        lastClick = System.currentTimeMillis();
    }

    private boolean isWeapon(ItemStack stack) {
        return isSwordOrAxe(stack);
    }

    private boolean isSword(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_sword");
    }

    private boolean isAxe(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_axe");
    }

    private boolean isSwordOrAxe(ItemStack stack) {
        return isSword(stack) || isAxe(stack);
    }

    private boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() == net.minecraft.item.Items.BOW || stack.getItem() == net.minecraft.item.Items.CROSSBOW;
    }
}
