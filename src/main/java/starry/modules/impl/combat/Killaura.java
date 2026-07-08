package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Killaura extends ModuleStructure {
    SliderSettings cps = new SliderSettings("CPS", "").setValue(10f).range(1f, 20f);
    SliderSettings range = new SliderSettings("Range", "").setValue(4.5f).range(1f, 6f);
    SliderSettings fov = new SliderSettings("FOV", "").setValue(360f).range(0f, 360f);
    BooleanSetting walls = new BooleanSetting("Through Walls", "").setValue(false);
    BooleanSetting players = new BooleanSetting("Players", "").setValue(true);
    BooleanSetting animals = new BooleanSetting("Animals", "").setValue(false);
    BooleanSetting mobs = new BooleanSetting("Mobs", "").setValue(false);
    BooleanSetting invisibles = new BooleanSetting("Invisibles", "").setValue(false);
    SelectSetting priority = new SelectSetting("Priority", "").value("Distance", "Health", "FOV").selected("Distance");
    SelectSetting sortMode = new SelectSetting("Sort Mode", "").value("FOV", "Distance", "Health").selected("Distance");
    BooleanSetting silentSwing = new BooleanSetting("Silent Swing", "").setValue(false);
    BooleanSetting keepSprint = new BooleanSetting("Keep Sprint", "").setValue(false);

    private int tickCounter;

    public Killaura() {
        super("Killaura", ModuleCategory.COMBAT);
        settings(cps, range, fov, walls, players, animals, mobs, invisibles, priority, sortMode, silentSwing, keepSprint);
    }

    @Override
    public void activate() { tickCounter = 0; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        tickCounter++;
        int attackDelay = Math.max(1, 20 / Math.max(1, cps.getInt()));
        if (tickCounter % attackDelay != 0) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        mc.interactionManager.attackEntity(mc.player, target);
        if (silentSwing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
        if (!keepSprint.isValue()) mc.player.setSprinting(false);
    }

    private LivingEntity findTarget() {
        return mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(range.getValue()), e -> {
            if (e == mc.player) return false;
            if (e.isDead() || !e.isAlive()) return false;
            if (!invisibles.isValue() && e.isInvisible()) return false;
            if (players.isValue() && e instanceof PlayerEntity) return true;
            if (animals.isValue() && e instanceof PassiveEntity) return true;
            if (mobs.isValue() && !(e instanceof PassiveEntity) && !(e instanceof PlayerEntity)) return true;
            return false;
        }).stream().min((a, b) -> {
            double da = mc.player.squaredDistanceTo(a);
            double db = mc.player.squaredDistanceTo(b);
            return Double.compare(da, db);
        }).orElse(null);
    }
}
