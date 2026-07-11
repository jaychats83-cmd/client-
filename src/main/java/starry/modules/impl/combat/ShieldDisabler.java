package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShieldDisabler extends ModuleStructure {
    SliderSettings hitDelay = new SliderSettings("Hit Delay", "Ticks before the axe hit").setValue(0).range(0, 20);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "Ticks before selecting the axe").setValue(0).range(0, 20);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "Restore the held slot after disabling").setValue(true);
    BooleanSetting stun = new BooleanSetting("Stun", "Send a second axe hit").setValue(false);
    BooleanSetting holdAxe = new BooleanSetting("Hold Axe", "Only run if an axe is already held").setValue(false);
    int previousSlot = -1, hitClock, switchClock;

    public ShieldDisabler() {
        super("Shield Disabler", "Automatically disables a shield facing you", ModuleCategory.COMBAT);
        settings(switchDelay, hitDelay, switchBack, stun, holdAxe);
    }

    @Override public void activate() { hitClock = hitDelay.getInt(); switchClock = switchDelay.getInt(); previousSlot = -1; }
    @Override public void deactivate() { restore(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null || mc.player.isUsingItem()) return;
        if (holdAxe.isValue() && !isAxe(mc.player.getMainHandStack())) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof PlayerEntity target)) { restore(); return; }
        if (!target.isBlocking() || !target.isHolding(Items.SHIELD) || isShieldFacingAway(target)) { restore(); return; }

        if (switchClock-- > 0) { if (previousSlot == -1) previousSlot = mc.player.getInventory().getSelectedSlot(); return; }
        int axe = findAxe();
        if (axe == -1) return;
        if (previousSlot == -1) previousSlot = mc.player.getInventory().getSelectedSlot();
        selectSlot(axe);
        if (hitClock-- > 0) return;
        attack(target);
        if (stun.isValue()) attack(target);
        hitClock = hitDelay.getInt();
        switchClock = switchDelay.getInt();
    }

    private void attack(PlayerEntity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isShieldFacingAway(PlayerEntity target) {
        Vec3d towardUs = mc.player.getEntityPos().subtract(target.getEntityPos()).normalize();
        Vec3d look = target.getRotationVec(1.0F).normalize();
        return look.dotProduct(towardUs) <= 0.0;
    }

    private int findAxe() { for (int i = 0; i < 9; i++) if (isAxe(mc.player.getInventory().getStack(i))) return i; return -1; }
    private boolean isAxe(ItemStack stack) { return Registries.ITEM.getId(stack.getItem()).getPath().contains("_axe"); }
    private void selectSlot(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
    private void restore() {
        if (mc.player != null && switchBack.isValue() && previousSlot >= 0 && previousSlot < 9) selectSlot(previousSlot);
        previousSlot = -1;
    }
}
