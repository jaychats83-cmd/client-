package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
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
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class k5g3vf extends ModuleStructure {

    public static k5g3vf getInstance() {
        return Instance.get(k5g3vf.class);
    }

    final List<Particle3D> particles = new ArrayList<>();
    final List<TotemEmitter> totemEmitters = new ArrayList<>();

    public SelectSetting mode = new SelectSetting(StringHelper.decrypt(new byte[]{7, (byte)-84, 59, (byte)-115}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-73, 38, (byte)-104, 119}))
            .value(StringHelper.decrypt(new byte[]{9, (byte)-74, 61, (byte)-115, 97}), StringHelper.decrypt(new byte[]{9, (byte)-79, 48, (byte)-97, 124}), StringHelper.decrypt(new byte[]{9, (byte)-74, 61, (byte)-115}), StringHelper.decrypt(new byte[]{14, (byte)-84, 51, (byte)-124, 115, (byte)-23}), StringHelper.decrypt(new byte[]{2, (byte)-90, 62, (byte)-102, 102}), StringHelper.decrypt(new byte[]{6, (byte)-86, 56, (byte)-128, 102, (byte)-11, 94, (byte)-72, 45}), StringHelper.decrypt(new byte[]{6, (byte)-86, 49, (byte)-115}), StringHelper.decrypt(new byte[]{14, (byte)-86, 62, (byte)-123, 125, (byte)-11, 83}), StringHelper.decrypt(new byte[]{25, (byte)-83, 48, (byte)-97, 116, (byte)-9, 86, (byte)-67, 47}), StringHelper.decrypt(new byte[]{25, (byte)-73, 62, (byte)-102}), StringHelper.decrypt(new byte[]{25, (byte)-73, 62, (byte)-102, 50, (byte)-87}), StringHelper.decrypt(new byte[]{30, (byte)-79, 54, (byte)-119, 124, (byte)-4, 91, (byte)-77}), StringHelper.decrypt(new byte[]{24, (byte)-94, 49, (byte)-116, 125, (byte)-10}))
            .selected(StringHelper.decrypt(new byte[]{25, (byte)-73, 62, (byte)-102}));

    public SelectSetting glowMode = new SelectSetting(StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97}), StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97, 50, (byte)-2, 81, (byte)-80, 47, (byte)-96, 43, (byte)-56, 102, (byte)-30, 71, (byte)-77}))
            .value(StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-121, 127}), StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-121, 127, (byte)-69, 100, (byte)-73, 39, (byte)-77, 51, (byte)-115}), StringHelper.decrypt(new byte[]{8, (byte)-84, 43, (byte)-128}))
            .selected(StringHelper.decrypt(new byte[]{8, (byte)-81, 48, (byte)-121, 127, (byte)-69, 100, (byte)-73, 39, (byte)-77, 51, (byte)-115}));

    public MultiSelectSetting triggers = new MultiSelectSetting("Triggers", "Wheн to spawn particles")
            .value(StringHelper.decrypt(new byte[]{11, (byte)-73, 43, (byte)-119, 113, (byte)-16}), StringHelper.decrypt(new byte[]{30, (byte)-84, 43, (byte)-115, 127}), StringHelper.decrypt(new byte[]{29, (byte)-94, 51, (byte)-125}), StringHelper.decrypt(new byte[]{30, (byte)-85, 45, (byte)-121, 101, (byte)-6, 85, (byte)-70, 47}))
            .selected("Attack", "Totem", "Walk", "Throwable");

    public SliderSettings amount = new SliderSettings(StringHelper.decrypt(new byte[]{11, (byte)-82, 48, (byte)-99, 124, (byte)-17}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-96, 48, (byte)-99, 124, (byte)-17, 23, (byte)-71, 36, (byte)-29, 55, (byte)-127, 102}))
            .range(10, 40).setValue(40);

    public SliderSettings walkAmount = new SliderSettings(StringHelper.decrypt(new byte[]{29, (byte)-94, 51, (byte)-125, 50, (byte)-38, 90, (byte)-71, 63, (byte)-83, 43}), StringHelper.decrypt(new byte[]{33, (byte)-10, 56, (byte)-37, 100, (byte)-3, 23, (byte)-90, 47, (byte)-79, 127, (byte)-101, 119, (byte)-8, 88, (byte)-72, 46, (byte)-29, 40, (byte)-128, 123, (byte)-9, 82, (byte)-10, 61, (byte)-94, 51, (byte)-125, 123, (byte)-11, 80}))
            .range(10, 30).setValue(30).visible(() -> triggers.isSelected("Walk"));

    public SliderSettings spread = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-77, 45, (byte)-115, 115, (byte)-1}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-80, 47, (byte)-102, 119, (byte)-6, 83, (byte)-10, 57, (byte)-73, 45, (byte)-115, 124, (byte)-4, 67, (byte)-66}))
            .range(0.5f, 3.0f).setValue(1.0f);

    public SliderSettings speed = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-77, 58, (byte)-115, 118}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-82, 48, (byte)-98, 119, (byte)-10, 82, (byte)-72, 62, (byte)-29, 44, (byte)-104, 119, (byte)-2, 83}))
            .range(0.1f, 3.0f).setValue(2.0f);

    public SliderSettings lifeTime = new SliderSettings(StringHelper.decrypt(new byte[]{6, (byte)-86, 57, (byte)-115, 50, (byte)-49, 94, (byte)-69, 47}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-81, 54, (byte)-114, 119, (byte)-17, 94, (byte)-69, 47, (byte)-29, 54, (byte)-122, 50, (byte)-24, 82, (byte)-75, 37, (byte)-83, 59, (byte)-101}))
            .range(0.5f, 10f).setValue(2.5f);

    public SliderSettings size = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-86, 37, (byte)-115}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-80, 54, (byte)-110, 119}))
            .range(0.1f, 1.0f).setValue(1f);

    public BooleanSetting randomColor = new BooleanSetting(StringHelper.decrypt(new byte[]{24, (byte)-94, (byte)-113, 85, 118, (byte)-12, 90, (byte)-10, 9, (byte)-84, 51, (byte)-121, 96}), StringHelper.decrypt(new byte[]{15, (byte)-94, 60, (byte)-128, 50, (byte)-21, 86, (byte)-92, 62, (byte)-86, 60, (byte)-124, 119, (byte)-69, 80, (byte)-77, 62, (byte)-80, 127, (byte)-119, 50, (byte)-23, 86, (byte)-72, 46, (byte)-84, 50, (byte)-56, 113, (byte)-12, 91, (byte)-71, 56}))
            .setValue(false);

    public ColorSetting color = new ColorSetting("Color", "Particle color")
            .value(0xFF896148)
            .visible(() -> !randomColor.isValue());

    private static final float GLOW_SIZE = 7.5f;
    private static final int TOTEM_DURATION = 20;
    private static final float GRAVITY_STRENGTH = 0.04f;

    private static final int[] RANDOM_COLORS = {
            0xFFFF0000,
            0xFFFF7F00,
            0xFFFFFF00,
            0xFF00FF00,
            0xFF00FFFF,
            0xFF0000FF,
            0xFF8B00FF,
            0xFFFF00FF,
            0xFFFF1493,
            0xFFFFFFFF,
            0xFF00FF7F,
            0xFFFF6347
    };

    private float walkParticleAccumulator = 0;

    public k5g3vf() {
        super(StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 57}), StringHelper.decrypt(new byte[]{9, (byte)-74, 44, (byte)-100, 125, (byte)-10, 23, (byte)-90, 43, (byte)-79, 43, (byte)-127, 113, (byte)-9, 82, (byte)-91, 106, (byte)-80, 38, (byte)-101, 102, (byte)-2, 90}), ModuleCategory.VISUALS);
        settings(mode, glowMode, triggers, amount, walkAmount, spread, speed, lifeTime, size, randomColor, color);
    }

    @Override
    public void deactivate() {
        particles.clear();
        totemEmitters.clear();
        walkParticleAccumulator = 0;
    }

    private int getParticleColor() {
        if (randomColor.isValue()) {
            return RANDOM_COLORS[ThreadLocalRandom.current().nextInt(RANDOM_COLORS.length)];
        }
        return color.getColor();
    }

    private float getGravity() {
        return (1.0f - 0.9f) * GRAVITY_STRENGTH;
    }

    private float getSpeedMultiplier() {
        return speed.getValue();
    }

    private Particle3D.ParticleMode getParticleMode() {
        String selected = mode.getSelected();
        return switch (selected) {
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
            default -> Particle3D.ParticleMode.CUBES;
        };
    }

    private Particle3D.GlowMode getGlowMode() {
        String selected = glowMode.getSelected();
        return switch (selected) {
            case "Bloom" -> Particle3D.GlowMode.BLOOM;
            case "Bloom Sample" -> Particle3D.GlowMode.BLOOM_SAMPLE;
            case "Both" -> Particle3D.GlowMode.BOTH;
            default -> Particle3D.GlowMode.BOTH;
        };
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (triggers.isSelected("Walk")) {
            handleWalkParticles();
        }

        if (triggers.isSelected("Throwable")) {
            handleProjectileParticles();
        }

        Iterator<TotemEmitter> emitterIterator = totemEmitters.iterator();
        while (emitterIterator.hasNext()) {
            TotemEmitter emitter = emitterIterator.next();
            emitter.tick();

            if (emitter.isAlive()) {
                spawnTotemParticlesBurst(emitter.getEntity(), emitter.getProgress());
            } else {
                emitterIterator.remove();
            }
        }

        Iterator<Particle3D> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle3D p = iterator.next();
            p.update();
            if (p.isDead()) {
                iterator.remove();
            }
        }
    }

    private void handleWalkParticles() {
        double velocitySq = mc.player.getVelocity().lengthSquared();
        boolean isMoving = velocitySq > 0.0001 && !mc.player.isSneaking();

        if (!isMoving) {
            walkParticleAccumulator = 0;
            return;
        }

        float particlesPerSecond = walkAmount.getValue();
        float particlesPerTick = particlesPerSecond / 20f;

        walkParticleAccumulator += particlesPerTick;

        int particlesToSpawn = (int) walkParticleAccumulator;
        walkParticleAccumulator -= particlesToSpawn;

        if (particlesToSpawn <= 0) return;

        float yaw = mc.player.getYaw();
        double radian = Math.toRadians(yaw + 90);
        double offsetX = Math.cos(radian) * 0.5;
        double offsetZ = Math.sin(radian) * 0.5;

        float spreadValue = spread.getValue() * 0.05f;
        float speedMult = getSpeedMultiplier();

        for (int i = 0; i < particlesToSpawn; i++) {
            double px = mc.player.getX() - offsetX + (Math.random() - 0.5) * 0.3;
            double py = mc.player.getY() + 0.3 + Math.random() * (mc.player.getHeight() - 0.3);
            double pz = mc.player.getZ() - offsetZ + (Math.random() - 0.5) * 0.3;

            Vec3d pos = new Vec3d(px, py, pz);

            double velX = (Math.random() - 0.5) * spreadValue * speedMult;
            double velY = (Math.random() - 0.5) * spreadValue * 0.5 * speedMult;
            double velZ = (Math.random() - 0.5) * spreadValue * speedMult;

            Vec3d velocity = new Vec3d(velX, velY, velZ);

            particles.add(new Particle3D(
                    pos,
                    velocity,
                    getParticleColor(),
                    size.getValue() * 0.6f,
                    lifeTime.getValue() * 0.5f
            ).setGravity(getGravity()).setVelocityMultiplier(0.99f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
        }
    }

    private void handleProjectileParticles() {
        float spreadValue = spread.getValue() * 0.03f;
        float speedMult = getSpeedMultiplier();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ThrownEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity) {
                ProjectileEntity projectile = (ProjectileEntity) entity;

                double prevX = projectile.lastX;
                double prevY = projectile.lastY;
                double prevZ = projectile.lastZ;

                double currentX = projectile.getX();
                double currentY = projectile.getY();
                double currentZ = projectile.getZ();

                boolean isMoving = Math.abs(currentX - prevX) > 0.01 || Math.abs(currentY - prevY) > 0.01 || Math.abs(currentZ - prevZ) > 0.01;

                if (isMoving || projectile.getVelocity().lengthSquared() > 0.01) {
                    for (int i = 0; i < 2; i++) {
                        double px = projectile.getX() + (Math.random() - 0.5) * 0.5;
                        double py = projectile.getY() + Math.random() * projectile.getHeight();
                        double pz = projectile.getZ() + (Math.random() - 0.5) * 0.5;

                        Vec3d pos = new Vec3d(px, py, pz);

                        double velX = (Math.random() - 0.5) * 2 * spreadValue * speedMult;
                        double velY = (Math.random() - 0.5) * 2 * spreadValue * speedMult;
                        double velZ = (Math.random() - 0.5) * 2 * spreadValue * speedMult;

                        Vec3d velocity = new Vec3d(velX, velY, velZ);

                        particles.add(new Particle3D(
                                pos,
                                velocity,
                                getParticleColor(),
                                size.getValue() * 0.5f,
                                lifeTime.getValue() * 0.3f
                        ).setGravity(getGravity()).setVelocityMultiplier(0.99f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!triggers.isSelected("Attack") || e.getTarget() == null) return;

        Entity target = e.getTarget();

        float spreadValue = spread.getValue() * 0.15f;
        float speedMult = getSpeedMultiplier();

        int count = amount.getInt();
        for (int i = 0; i < count; i++) {
            double px = target.getX();
            double py = target.getY() + (Math.random() * target.getHeight());
            double pz = target.getZ();

            Vec3d pos = new Vec3d(px, py, pz);

            Vec3d velocity = new Vec3d(
                    (Math.random() - 0.5) * 2 * spreadValue * speedMult,
                    (Math.random() - 0.5) * 2 * spreadValue * speedMult,
                    (Math.random() - 0.5) * 2 * spreadValue * speedMult
            );

            particles.add(new Particle3D(
                    pos,
                    velocity,
                    getParticleColor(),
                    size.getValue(),
                    lifeTime.getValue()
            ).setGravity(getGravity()).setVelocityMultiplier(0.99f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
        }
    }

    public void onTotemPop(Entity entity) {
        if (!triggers.isSelected("Totem")) return;

        totemEmitters.add(new TotemEmitter(entity, TOTEM_DURATION));
    }

    private void spawnTotemParticlesBurst(Entity entity, float progress) {
        if (entity == null || entity.isRemoved()) return;

        float spreadMultiplier = 1.0f - (progress * 0.5f);
        float spreadValue = spread.getValue();
        float speedMult = getSpeedMultiplier();

        for (int i = 0; i < 4; i++) {
            double d = Math.random() * 2.0 - 1.0;
            double e = Math.random() * 2.0 - 1.0;
            double f = Math.random() * 2.0 - 1.0;

            if (d * d + e * e + f * f <= 1.0) {
                double px = entity.getX() + d * entity.getWidth() * 0.5;
                double py = entity.getBodyY(0.5) + e * entity.getHeight() * 0.5;
                double pz = entity.getZ() + f * entity.getWidth() * 0.5;

                Vec3d pos = new Vec3d(px, py, pz);

                double velocityScale = spreadValue * 0.18 * spreadMultiplier * speedMult;

                double initialUpward;
                if (Math.random() < 0.4) {
                    initialUpward = (0.15 + Math.random() * 0.2) * speedMult;
                } else {
                    initialUpward = (0.03 + Math.random() * 0.07) * speedMult;
                }

                Vec3d velocity = new Vec3d(
                        d * velocityScale,
                        initialUpward,
                        f * velocityScale
                );

                int[] totemColors = {
                        0xFF7CFC00,
                        0xFFFFD700,
                        0xFF32CD32,
                        0xFFFFA500,
                        0xFF00FF00,
                        0xFFADFF2F
                };
                int particleColor = totemColors[(int) (Math.random() * totemColors.length)];

                particles.add(new Particle3D(
                        pos,
                        velocity,
                        particleColor,
                        size.getValue() * 0.8f,
                        lifeTime.getValue() * 0.8f
                ).setGravity(getGravity()).setVelocityMultiplier(0.98f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
            }
        }
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent e) {
        if (particles.isEmpty()) return;

        MatrixStack stack = e.getStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        float partialTicks = e.getPartialTicks();

        for (Particle3D p : particles) {
            p.render(stack, immediate, GLOW_SIZE, partialTicks);
        }

        immediate.draw();
    }
}
