package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoPot extends ModuleStructure {
    SliderSettings minHealth = new SliderSettings("Min Health", "").setValue(10f).range(1, 20);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "").setValue(0f).range(0, 10);
    SliderSettings throwDelay = new SliderSettings("Throw Delay", "").setValue(0f).range(0, 10);
    BooleanSetting goToPrevSlot = new BooleanSetting("Switch Back", "").setValue(true);
    BooleanSetting lookDown = new BooleanSetting("Look Down", "").setValue(true);

    private int switchClock, throwClock, prevSlot;
    private float prevPitch;
    private boolean bool;

    public AutoPot() {
        super("Auto Pot", ModuleCategory.COMBAT);
        settings(minHealth, switchDelay, throwDelay, goToPrevSlot, lookDown);
    }

    @Override
    public void activate() { reset(); }

    private void reset() { switchClock = 0; throwClock = 0; prevSlot = -1; prevPitch = -1; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;
        if ((mc.player.getHealth() <= minHealth.getValue() || bool)) {
            if (bool && mc.player.getHealth() >= mc.player.getMaxHealth()) { bool = false; return; }

            if (!isHealthPotion(mc.player.getMainHandStack())) {
                if (switchClock < switchDelay.getValue()) { switchClock++; return; }
                if (goToPrevSlot.isValue() && prevSlot == -1) prevSlot = mc.player.getInventory().getSelectedSlot();
                if (lookDown.isValue() && prevPitch == -1) prevPitch = mc.player.getPitch();

                int potSlot = findHealthPotion();
                if (potSlot != -1) {
                    setInvSlot(potSlot);
                    switchClock = 0;
                }
            }

            if (isHealthPotion(mc.player.getMainHandStack())) {
                if (throwClock < throwDelay.getValue()) { throwClock++; return; }
                if (lookDown.isValue()) mc.player.setPitch(90F);
                ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
                throwClock = 0;
            }
        } else {
            restorePlayerState();
        }
    }

    @Override
    public void deactivate() {
        if (mc.player != null) restorePlayerState();
        reset();
    }

    private void restorePlayerState() {
        if (prevSlot != -1) {
            setInvSlot(prevSlot);
            prevSlot = -1;
        }

        if (prevPitch != -1) {
            mc.player.setPitch(prevPitch);
            prevPitch = -1;
        }
    }

    private boolean isHealthPotion(ItemStack stack) {
        StatusEffectInstance potion = new StatusEffectInstance(
                Registries.STATUS_EFFECT.getEntry(StatusEffects.INSTANT_HEALTH.value()), 1, 1);
        var contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        return stack.getItem() instanceof SplashPotionItem
                && contents != null
                && contents.getEffects().toString().contains(potion.toString());
    }

    private int findHealthPotion() {
        for (int i = 0; i < 9; i++)
            if (isHealthPotion(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    private void setInvSlot(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }
}