package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.BlockInteractionEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoXP extends ModuleStructure {
    SliderSettings delay = new SliderSettings("Delay", "").setValue(0f).range(0f, 20f);
    SliderSettings chance = new SliderSettings("Chance", "").setValue(100f).range(0f, 100f);
    BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", "").setValue(false);
    int clock;

    public AutoXP() {
        super("Auto XP", ModuleCategory.MISC);
        settings(delay, chance, clickSimulation);
    }

    @Override
    public void activate() { clock = 0; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null) return;
        if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) return;
        if (clock > 0) { clock--; return; }
        if (Math.random() * 100 > chance.getValue()) return;

        ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
        clock = delay.getInt();
    }

    @EventHandler
    public void onBlockInteract(BlockInteractionEvent event) {
        if (mc.player != null && mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE)
            event.setCancelled(true);
    }
}
