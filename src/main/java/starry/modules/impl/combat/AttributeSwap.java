package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeSwap extends ModuleStructure {
    SliderSettings swapTicks = new SliderSettings("Swap Ticks", "").setValue(2f).range(1, 10);
    SliderSettings swapDelay = new SliderSettings("Swap Delay", "").setValue(1f).range(0, 5);
    SliderSettings cooldown = new SliderSettings("Cooldown Ticks", "").setValue(4f).range(0, 20);
    BooleanSetting preferMace = new BooleanSetting("Prefer Mace", "").setValue(true);
    BooleanSetting requireTarget = new BooleanSetting("Require Target", "").setValue(true);

    private int originalSlot = -1;
    private int pendingSlot = -1;
    private int pendingDelay;
    private int ticksLeft;
    private int cooldownTicks;

    public AttributeSwap() {
        super("AttributeSwap", ModuleCategory.COMBAT);
        settings(swapTicks, swapDelay, cooldown, preferMace, requireTarget);
    }

    @Override
    public void activate() { reset(); }

    @Override
    public void deactivate() { restore(); reset(); }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.currentScreen != null || cooldownTicks > 0 || ticksLeft > 0 || pendingSlot != -1) return;
        if (requireTarget.isValue() && !(mc.crosshairTarget instanceof EntityHitResult)) return;

        int swapSlot = findSwapSlot();
        if (swapSlot == -1 || swapSlot == mc.player.getInventory().getSelectedSlot()) return;

        originalSlot = mc.player.getInventory().getSelectedSlot();
        pendingSlot = swapSlot;
        pendingDelay = swapDelay.getInt();
        cooldownTicks = cooldown.getInt();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (cooldownTicks > 0) cooldownTicks--;

        if (pendingSlot != -1) {
            if (pendingDelay > 0) { pendingDelay--; return; }
            selectSlot(pendingSlot);
            pendingSlot = -1;
            ticksLeft = swapTicks.getInt();
            return;
        }

        if (ticksLeft <= 0) return;
        ticksLeft--;
        if (ticksLeft <= 0) restore();
    }

    private int findSwapSlot() {
        if (preferMace.isValue()) {
            for (int i = 0; i < 9; i++)
                if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) return i;
        }
        for (int i = 0; i < 9; i++) {
            net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(i);
            if (isSwordOrAxe(stack) && i != mc.player.getInventory().getSelectedSlot())
                return i;
        }
        return -1;
    }

    private void restore() {
        if (mc.player != null && originalSlot >= 0 && originalSlot < 9)
            selectSlot(originalSlot);
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

    private void selectSlot(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null)
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void reset() {
        originalSlot = -1; pendingSlot = -1; pendingDelay = 0; ticksLeft = 0; cooldownTicks = 0;
    }
}
