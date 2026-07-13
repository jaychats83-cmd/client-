package starry.modules.impl.basefind;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.Box;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.render.Render3D;

public class VillagerESP extends ModuleStructure {
    private final SliderSettings range = new SliderSettings("Range", "Search radius in blocks").setValue(128).range(16, 256);
    private final ColorSetting color = new ColorSetting("Color", "Highlight color").value(0xAA35E875);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to villagers").setValue(true);

    public VillagerESP() {
        super("Villager ESP", "Highlights loaded villagers", ModuleCategory.ESP);
        settings(range, color, tracers);
    }

    @EventHandler
    public void onRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        Box area = mc.player.getBoundingBox().expand(range.getValue());
        for (VillagerEntity villager : mc.world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive)) {
            Render3D.drawBox(villager.getBoundingBox(), color.getColor(), 1.5F, true, true, false);
            if (tracers.isValue()) Render3D.drawLine(mc.player.getEyePos(), villager.getBoundingBox().getCenter(), color.getColor(), 1.25F, false);
        }
    }
}
