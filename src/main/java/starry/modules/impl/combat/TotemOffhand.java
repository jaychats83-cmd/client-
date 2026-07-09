package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TotemOffhand extends ModuleStructure {
    SliderSettings health = new SliderSettings("Health Trigger", "").setValue(10f).range(1f, 20f);
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0f, 10f);
    SliderSettings totemCount = new SliderSettings("Totem Count", "").setValue(1f).range(1f, 10f);
    BooleanSetting autoSwitchBack = new BooleanSetting("Switch Back", "").setValue(true);

    private int clock;
    private int prevSlot = -1;

    public TotemOffhand() {
        super("Totem Offhand", ModuleCategory.CPVP);
        settings(health, delay, totemCount, autoSwitchBack);
    }

    @Override
    public void activate() { clock = 0; prevSlot = -1; }
    @Override
    public void deactivate() { switchBackToPrev(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null) return;

        if (mc.player.getHealth() <= health.getValue() && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            int totemSlot = findTotem();
            if (totemSlot == -1) return;
            if (clock < fastDelay()) { clock++; return; }
            if (prevSlot == -1) prevSlot = mc.player.getInventory().getSelectedSlot();
            int invIndex = totemSlot < 9 ? totemSlot + 36 : totemSlot;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, invIndex, 40, SlotActionType.SWAP, mc.player);
            clock = 0;
        } else if (prevSlot != -1 && autoSwitchBack.isValue()) {
            switchBackToPrev();
        }
    }

    private int findTotem() {
        for (int i = 0; i < 36; i++)
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        return -1;
    }

    private void switchBackToPrev() {
        if (prevSlot != -1) { mc.player.getInventory().setSelectedSlot(prevSlot); prevSlot = -1; }
    }

    private int fastDelay() {
        return Math.max(0, Math.round(delay.getValue() * 0.5F));
    }
}
