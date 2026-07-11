package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.util.math.Box;
import starry.events.api.EventHandler;
import starry.events.impl.WorldRenderEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.render.Render3D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseEntityFinder extends ModuleStructure {
    SliderSettings range = new SliderSettings("Range", "Entity search radius in blocks").setValue(128).range(16, 256);
    SliderSettings alpha = new SliderSettings("Alpha", "Box fill opacity").setValue(55).range(10, 180);
    BooleanSetting villagers = new BooleanSetting("Villagers", "Mark villagers and zombie-cured trading activity").setValue(true);
    BooleanSetting llamas = new BooleanSetting("Llamas", "Mark llamas that may indicate leads, caravans, or bases").setValue(true);
    BooleanSetting wanderingTraders = new BooleanSetting("Wandering Traders", "Mark wandering traders").setValue(true);
    BooleanSetting pillagers = new BooleanSetting("Pillagers", "Mark pillagers that may indicate outposts or raids").setValue(true);
    BooleanSetting tracers = new BooleanSetting("Tracers", "Draw a line to every detected entity").setValue(true);

    public BaseEntityFinder() {
        super("Base Entity Finder", "Highlights unusual loaded entities commonly used as base-finding signals", ModuleCategory.ESP);
        settings(range, alpha, villagers, llamas, wanderingTraders, pillagers, tracers);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        Box search = mc.player.getBoundingBox().expand(range.getValue());
        List<Entity> found = new ArrayList<>();
        if (villagers.isValue()) found.addAll(mc.world.getEntitiesByClass(VillagerEntity.class, search, Entity::isAlive));
        if (llamas.isValue()) found.addAll(mc.world.getEntitiesByClass(LlamaEntity.class, search, Entity::isAlive));
        if (wanderingTraders.isValue()) found.addAll(mc.world.getEntitiesByClass(WanderingTraderEntity.class, search, Entity::isAlive));
        if (pillagers.isValue()) found.addAll(mc.world.getEntitiesByClass(PillagerEntity.class, search, Entity::isAlive));

        for (Entity entity : found) {
            Color color = colorFor(entity, alpha.getInt());
            Render3D.drawBox(entity.getBoundingBox(), color.getRGB(), 1.5F, true, true, false);
            if (tracers.isValue()) Render3D.drawLine(mc.player.getEyePos(), entity.getBoundingBox().getCenter(),
                    colorFor(entity, 230).getRGB(), 1.4F, false);
        }
    }

    private Color colorFor(Entity entity, int a) {
        if (entity instanceof VillagerEntity) return new Color(50, 220, 110, a);
        if (entity instanceof LlamaEntity) return new Color(255, 190, 70, a);
        if (entity instanceof WanderingTraderEntity) return new Color(70, 190, 255, a);
        return new Color(255, 75, 90, a);
    }
}
