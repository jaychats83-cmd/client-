package starry.modules.impl.extras;

import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.access.ClientPlayerInteractionAccess;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;

public class SpeedMine extends ModuleStructure {
    private final SliderSettings multiplier = new SliderSettings("Multiplier", "Vanilla break-progress multiplier").setValue(1.8F).range(1.0F, 5.0F);

    public SpeedMine() {
        super("Speed Mine", "Accelerates normal client-side block breaking", ModuleCategory.EXTRAS);
        settings(multiplier);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.interactionManager instanceof ClientPlayerInteractionAccess access && access.starry$isBreakingBlock()) {
            access.starry$setBreakingProgress(Math.min(1.0F, access.starry$getBreakingProgress() * multiplier.getValue()));
        }
    }
}
