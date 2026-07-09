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
import net.minecraft.util.math.MathHelper;
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
    private long lastAttackMs;

    public Killaura() {
        super("Killaura", ModuleCategory.COMBAT);
        settings(cps, range, fov, walls, players, animals, mobs, invisibles, priority, sortMode, silentSwing, keepSprint);
    }

    @Override
    public void activate() { tickCounter = 0; lastAttackMs = 0; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        tickCounter++;
        long minDelayMs = Math.max(1L, Math.round(1000.0 / Math.max(1.0, cps.getValue())));
        if (System.currentTimeMillis() - lastAttackMs < minDelayMs) return;
        if (mc.player.getAttackCooldownProgress(0.0F) < 0.92F) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (!keepSprint.isValue()) mc.player.setSprinting(false);
        lastAttackMs = System.currentTimeMillis();
    }

    private LivingEntity findTarget() {
        return mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(range.getValue()), e -> {
            if (e == mc.player) return false;
            if (e.isDead() || !e.isAlive()) return false;
            if (!invisibles.isValue() && e.isInvisible()) return false;
            if (!walls.isValue() && !mc.player.canSee(e)) return false;
            if (fov.getValue() < 360f && angleTo(e) > fov.getValue() * 0.5f) return false;
            if (players.isValue() && e instanceof PlayerEntity) return true;
            if (animals.isValue() && e instanceof PassiveEntity) return true;
            if (mobs.isValue() && !(e instanceof PassiveEntity) && !(e instanceof PlayerEntity)) return true;
            return false;
        }).stream().min((a, b) -> {
            return Double.compare(score(a), score(b));
        }).orElse(null);
    }

    private double score(LivingEntity entity) {
        if (priority.isSelected("Health") || sortMode.isSelected("Health")) return entity.getHealth() + entity.getAbsorptionAmount();
        if (priority.isSelected("FOV") || sortMode.isSelected("FOV")) return angleTo(entity);
        return mc.player.squaredDistanceTo(entity);
    }

    private float angleTo(LivingEntity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        float yaw = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875) - 90.0F;
        return Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
    }
}
