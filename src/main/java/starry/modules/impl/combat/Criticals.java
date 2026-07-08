package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.EntityHitResult;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Criticals extends ModuleStructure {
    BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", "").setValue(true);

    public Criticals() {
        super("Criticals", ModuleCategory.COMBAT);
        settings(onlyWeapon);
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.currentScreen != null || !mc.player.isOnGround()) return;
        if (onlyWeapon.isValue() && !isSwordOrAxe(mc.player.getMainHandStack())) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity)) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625D, z, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
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
