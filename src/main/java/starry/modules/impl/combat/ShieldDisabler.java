package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.InteractEntityEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShieldDisabler extends ModuleStructure {
    SelectSetting mode = new SelectSetting("Mode", "").value("Sword", "Axe", "Both").selected("Axe");
    BooleanSetting onlyPlayers = new BooleanSetting("Only Players", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing", "").setValue(true);
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0f, 10f);

    public ShieldDisabler() {
        super("Shield Disabler", ModuleCategory.COMBAT);
        settings(mode, onlyPlayers, swing, delay);
    }

    @EventHandler
    public void onInteract(InteractEntityEvent event) {
        if (onlyPlayers.isValue() && !(event.getEntity() instanceof PlayerEntity)) return;
        if (!isHoldingWeapon()) return;
        event.setCancelled(true);

        if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(event.getEntity(), false));
    }

    private boolean isHoldingWeapon() {
        ItemStack stack = mc.player.getMainHandStack();
        if (mode.isSelected("Both")) return isSwordOrAxe(stack);
        if (mode.isSelected("Sword")) return isSword(stack);
        if (mode.isSelected("Axe")) return isAxe(stack);
        return false;
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
}
