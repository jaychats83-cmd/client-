package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CrystalOptimizer extends ModuleStructure {
    public CrystalOptimizer() {
        super("Crystal Optimizer", ModuleCategory.CPVP);
    }

    @EventHandler
    public void onPacketSend(PacketEvent event) {
        if (!event.isSend() || !(event.getPacket() instanceof PlayerInteractEntityC2SPacket interactPacket)) return;

        interactPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override
            public void interact(Hand hand) {}
            @Override
            public void interactAt(Hand hand, Vec3d pos) {}
            @Override
            public void attack() {
                if (mc.crosshairTarget == null) return;
                if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY && mc.crosshairTarget instanceof EntityHitResult hit) {
                    if (hit.getEntity() instanceof EndCrystalEntity) {
                        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
                        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
                        if (!(weakness == null || strength != null && strength.getAmplifier() > weakness.getAmplifier() || isTool(mc.player.getMainHandStack())))
                            return;
                        hit.getEntity().discard();
                        hit.getEntity().setRemoved(Entity.RemovalReason.KILLED);
                        hit.getEntity().onRemoved();
                    }
                }
            }
        });
    }

    private boolean isTool(ItemStack stack) {
        return isSword(stack) || isMiningTool(stack);
    }

    private boolean isSword(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_sword");
    }

    private boolean isMiningTool(ItemStack stack) {
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.contains("_pickaxe") || path.contains("_axe") || path.contains("_shovel") || path.contains("_hoe");
    }
}
