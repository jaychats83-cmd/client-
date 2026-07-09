package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.TickEvent;
import starry.events.impl.WorldRenderEvent;
import starry.modules.impl.render.particles.Particle3D;
import starry.modules.impl.render.particles.TotemEmitter;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.MultiSelectSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class k5g3vf extends ModuleStructure {
    static final float GLOW_SIZE = 7.5f;
    static final int TOTEM_DURATION = 20;
    static final int[] RANDOM_COLORS = {
            0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00,
            0xFF00FFFF, 0xFF0000FF, 0xFF8B00FF, 0xFFFF00FF,
            0xFFFF1493, 0xFFFFFFFF, 0xFF00FF7F, 0xFFFF6347
    };

    final List<Particle3D> particles = new ArrayList<>();
    final List<TotemEmitter> totemEmitters = new ArrayList<>();

    SelectSetting style = new SelectSetting("Style", "Particle visual style")
            .value("Cubes", "Crown", "Cube", "Dollar", "Heart", "Lightning", "Line", "Diamond", "Snowflake", "Star", "Star 2", "Triangle", "Random")
            .selected("Heart");
    SelectSetting glow = new SelectSetting("Glow", "Particle glow style")
            .value("Bloom", "Bloom Sample", "Both")
            .selected("Both");
    SelectSetting physics = new SelectSetting("Physics", "How particles move")
            .value("Realistic", "No Collision", "No Physics")
            .selected("Realistic");
    SelectSetting targets = new SelectSetting("Targets", "Which attacked targets spawn particles")
            .value("Players", "Mobs", "All")
            .selected("Players");
    public MultiSelectSetting triggers = new MultiSelectSetting("Triggers", "When to spawn particles")
            .value("Attack", "Totem", "Walk", "Throwable")
            .selected("Attack", "Totem", "Walk", "Throwable");
    SliderSettings amount = new SliderSettings("Amount", "Particle count per hit")
            .range(1, 200).setValue(24);
    SliderSettings walkAmount = new SliderSettings("Walk Amount", "Particles per second while moving")
            .range(1, 80).setValue(20).visible(() -> triggers.isSelected("Walk"));
    SliderSettings spread = new SliderSettings("Spread", "Particle burst spread")
            .range(0.05f, 1.5f).setValue(0.25f);
    SliderSettings speed = new SliderSettings("Speed", "Particle movement speed")
            .range(0.1f, 4.0f).setValue(1.4f);
    SliderSettings lifetime = new SliderSettings("Lifetime", "How long particles live")
            .range(0.2f, 8.0f).setValue(1.5f);
    SliderSettings size = new SliderSettings("Size", "Particle size")
            .range(0.1f, 4.0f).setValue(0.5f);
    BooleanSetting randomColor = new BooleanSetting("Random Color", "Use random RGB per particle").setValue(false);
    ColorSetting color = new ColorSetting("Color", "Particle RGB")
            .value(0xFFFF5FB1)
            .visible(() -> !randomColor.isValue());
    BooleanSetting handBursts = new BooleanSetting("Hand Bursts", "Spawn qcloud bursts by player hands").setValue(true);

    float walkParticleAccumulator;

    public k5g3vf() {
        super("Particle", "qcloud hit particles with full style and RGB control", ModuleCategory.VISUALS);
        settings(style, glow, physics, targets, triggers, amount, walkAmount, spread, speed, lifetime, size, randomColor, color, handBursts);
    }

    public static k5g3vf getInstance() {
        return Instance.get(k5g3vf.class);
    }

    @Override
    public void deactivate() {
        particles.clear();
        totemEmitters.clear();
        walkParticleAccumulator = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (triggers.isSelected("Walk")) handleWalkParticles();
        if (triggers.isSelected("Throwable")) handleProjectileParticles();

        Iterator<TotemEmitter> emitters = totemEmitters.iterator();
        while (emitters.hasNext()) {
            TotemEmitter emitter = emitters.next();
            emitter.tick();
            if (emitter.isAlive()) spawnTotemBurst(emitter.getEntity(), emitter.getProgress());
            else emitters.remove();
        }

        Iterator<Particle3D> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle3D particle = iterator.next();
            particle.update();
            if (particle.isDead()) iterator.remove();
        }
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        Entity target = event.getTarget();
        if (!triggers.isSelected("Attack") || target == null || !isTargetAllowed(target)) return;

        spawnBurst(hitPosition(target), amount.getInt(), spread.getValue(), size.getValue(), lifetime.getValue());

        if (handBursts.isValue() && target instanceof PlayerEntity player) {
            int handAmount = Math.max(1, Math.round(amount.getValue() * 0.4f));
            spawnBurst(handPosition(player, true), handAmount, spread.getValue(), size.getValue() * 0.8f, lifetime.getValue() * 0.8f);
            spawnBurst(handPosition(player, false), handAmount, spread.getValue(), size.getValue() * 0.8f, lifetime.getValue() * 0.8f);
        }
    }

    public void onTotemPop(Entity entity) {
        if (triggers.isSelected("Totem")) totemEmitters.add(new TotemEmitter(entity, TOTEM_DURATION));
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (particles.isEmpty()) return;
        var immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        for (Particle3D particle : particles) {
            particle.render(event.getStack(), immediate, GLOW_SIZE, event.getPartialTicks());
        }
        immediate.draw();
    }

    private void handleWalkParticles() {
        if (mc.player.getVelocity().lengthSquared() <= 0.0001 || mc.player.isSneaking()) {
            walkParticleAccumulator = 0;
            return;
        }

        walkParticleAccumulator += walkAmount.getValue() / 20f;
        int spawn = (int) walkParticleAccumulator;
        walkParticleAccumulator -= spawn;

        for (int i = 0; i < spawn; i++) {
            Vec3d pos = mc.player.getEntityPos().add(
                    (Math.random() - 0.5) * 0.6,
                    0.25 + Math.random() * Math.max(0.25, mc.player.getHeight() - 0.25),
                    (Math.random() - 0.5) * 0.6
            );
            spawnOne(pos, randomVelocity(spread.getValue() * 0.25), size.getValue() * 0.6f, lifetime.getValue() * 0.5f);
        }
    }

    private void handleProjectileParticles() {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ThrownEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity)) continue;
            ProjectileEntity projectile = (ProjectileEntity) entity;
            if (projectile.getVelocity().lengthSquared() <= 0.01) continue;
            for (int i = 0; i < 2; i++) {
                Vec3d pos = projectile.getEntityPos().add((Math.random() - 0.5) * 0.5, Math.random() * projectile.getHeight(), (Math.random() - 0.5) * 0.5);
                spawnOne(pos, randomVelocity(spread.getValue() * 0.18), size.getValue() * 0.5f, lifetime.getValue() * 0.3f);
            }
        }
    }

    private void spawnTotemBurst(Entity entity, float progress) {
        if (entity == null || entity.isRemoved()) return;

        for (int i = 0; i < 4; i++) {
            double dx = Math.random() * 2.0 - 1.0;
            double dy = Math.random() * 2.0 - 1.0;
            double dz = Math.random() * 2.0 - 1.0;
            if (dx * dx + dy * dy + dz * dz > 1.0) continue;

            Vec3d pos = new Vec3d(
                    entity.getX() + dx * entity.getWidth() * 0.5,
                    entity.getBodyY(0.5) + dy * entity.getHeight() * 0.5,
                    entity.getZ() + dz * entity.getWidth() * 0.5
            );
            Vec3d velocity = new Vec3d(dx, Math.max(0.08, dy * 0.12), dz)
                    .multiply(spread.getValue() * 0.18 * speed.getValue() * (1.0f - progress * 0.5f));
            spawnOne(pos, velocity, size.getValue() * 0.8f, lifetime.getValue() * 0.8f);
        }
    }

    private void spawnBurst(Vec3d origin, int count, float power, float particleSize, float life) {
        if (origin == null) return;
        for (int i = 0; i < Math.max(1, count); i++) {
            Vec3d pos = origin.add((Math.random() - 0.5) * 0.4, (Math.random() - 0.25) * 0.3, (Math.random() - 0.5) * 0.4);
            spawnOne(pos, randomVelocity(power), particleSize, life);
        }
    }

    private void spawnOne(Vec3d pos, Vec3d velocity, float particleSize, float life) {
        particles.add(new Particle3D(pos, velocity, particleColor(), particleSize, life)
                .setGravity(gravity())
                .setVelocityMultiplier(velocityMultiplier())
                .setCollision(collision())
                .setMode(particleMode())
                .setGlowMode(glowMode()));
    }

    private Vec3d randomVelocity(double power) {
        double scaled = power * speed.getValue();
        double y = physics.isSelected("No Physics")
                ? random(scaled * 0.6, scaled * 1.2)
                : random(scaled * 0.2, scaled);
        return new Vec3d(random(-scaled, scaled), y, random(-scaled, scaled));
    }

    private Vec3d hitPosition(Entity target) {
        if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit) return hit.getPos();
        return target.getEntityPos().add(0, target.getHeight() * 0.5, 0);
    }

    private Vec3d handPosition(PlayerEntity player, boolean leftHand) {
        float yawRad = (float) Math.toRadians(player.getYaw());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        Vec3d base = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.65, player.getZ());
        Vec3d forward = new Vec3d(-sin, 0.0, cos).multiply(0.15);
        Vec3d side = new Vec3d(cos, 0.0, sin).multiply(leftHand ? -0.35 : 0.35);
        return base.add(forward).add(side);
    }

    private boolean isTargetAllowed(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (isNakedInvisiblePlayer(player)) return false;
            return targets.isSelected("Players") || targets.isSelected("All");
        }
        return entity instanceof LivingEntity && (targets.isSelected("Mobs") || targets.isSelected("All"));
    }

    private boolean isNakedInvisiblePlayer(PlayerEntity player) {
        if (!player.isInvisible()) return false;
        return player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                && player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                && player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                && player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private int particleColor() {
        return randomColor.isValue() ? RANDOM_COLORS[ThreadLocalRandom.current().nextInt(RANDOM_COLORS.length)] : color.getColor();
    }

    private float gravity() {
        return physics.isSelected("No Physics") ? 0f : 0.03f;
    }

    private float velocityMultiplier() {
        return physics.isSelected("No Physics") ? 0.99f : 0.98f;
    }

    private boolean collision() {
        return physics.isSelected("Realistic");
    }

    private Particle3D.ParticleMode particleMode() {
        return switch (style.getSelected()) {
            case "Cubes" -> Particle3D.ParticleMode.CUBES;
            case "Crown" -> Particle3D.ParticleMode.CROWN;
            case "Cube" -> Particle3D.ParticleMode.CUBE_BLAST;
            case "Dollar" -> Particle3D.ParticleMode.DOLLAR;
            case "Heart" -> Particle3D.ParticleMode.HEART;
            case "Lightning" -> Particle3D.ParticleMode.LIGHTNING;
            case "Line" -> Particle3D.ParticleMode.LINE;
            case "Diamond" -> Particle3D.ParticleMode.RHOMBUS;
            case "Snowflake" -> Particle3D.ParticleMode.SNOWFLAKE;
            case "Star" -> Particle3D.ParticleMode.STAR;
            case "Star 2" -> Particle3D.ParticleMode.STAR_ALT;
            case "Triangle" -> Particle3D.ParticleMode.TRIANGLE;
            case "Random" -> Particle3D.ParticleMode.RANDOM;
            default -> Particle3D.ParticleMode.HEART;
        };
    }

    private Particle3D.GlowMode glowMode() {
        return switch (glow.getSelected()) {
            case "Bloom" -> Particle3D.GlowMode.BLOOM;
            case "Bloom Sample" -> Particle3D.GlowMode.BLOOM_SAMPLE;
            default -> Particle3D.GlowMode.BOTH;
        };
    }

    private double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(Math.min(min, max), Math.max(min, max));
    }
}
