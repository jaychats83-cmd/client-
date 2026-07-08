package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.events.impl.WorldRenderEvent;
import starry.modules.impl.render.worldparticles.Particle;
import starry.modules.impl.render.worldparticles.ParticleSpawner;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.ColorSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;
import starry.util.timer.StopWatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class jvwsoq extends ModuleStructure {

    public static jvwsoq getInstance() {
        return Instance.get(jvwsoq.class);
    }

    final List<Particle> particles = new ArrayList<>();
    final StopWatch timer = new StopWatch();

    Vec3d lastPlayerPos = Vec3d.ZERO;
    Vec3d playerVelocity = Vec3d.ZERO;
    double playerSpeed = 0;

    public SelectSetting mode = new SelectSetting(StringHelper.decrypt(new byte[]{7, (byte)-84, 59, (byte)-115}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-73, 38, (byte)-104, 119}))
            .value(StringHelper.decrypt(new byte[]{121, (byte)-121, 127, (byte)-85, 103, (byte)-7, 82, (byte)-91}), StringHelper.decrypt(new byte[]{9, (byte)-79, 48, (byte)-97, 124}), StringHelper.decrypt(new byte[]{9, (byte)-74, 61, (byte)-115}), StringHelper.decrypt(new byte[]{14, (byte)-84, 51, (byte)-124, 115, (byte)-23}), StringHelper.decrypt(new byte[]{2, (byte)-90, 62, (byte)-102, 102}), StringHelper.decrypt(new byte[]{6, (byte)-86, 56, (byte)-128, 102, (byte)-11, 94, (byte)-72, 45}), StringHelper.decrypt(new byte[]{6, (byte)-86, 49, (byte)-115}), StringHelper.decrypt(new byte[]{14, (byte)-86, 62, (byte)-123, 125, (byte)-11, 83}), StringHelper.decrypt(new byte[]{25, (byte)-83, 48, (byte)-97, 116, (byte)-9, 86, (byte)-67, 47}), StringHelper.decrypt(new byte[]{25, (byte)-73, 62, (byte)-102}), StringHelper.decrypt(new byte[]{25, (byte)-73, 62, (byte)-102, 50, (byte)-87}), StringHelper.decrypt(new byte[]{30, (byte)-79, 54, (byte)-119, 124, (byte)-4, 91, (byte)-77}), StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97}), StringHelper.decrypt(new byte[]{24, (byte)-94, 49, (byte)-116, 125, (byte)-10}))
            .selected(StringHelper.decrypt(new byte[]{25, (byte)-73, 62, (byte)-102}));

    public SliderSettings cubeCount = new SliderSettings(StringHelper.decrypt(new byte[]{11, (byte)-82, 48, (byte)-99, 124, (byte)-17}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-96, 48, (byte)-99, 124, (byte)-17}))
            .range(10.0f, 500.0f)
            .setValue(100.0f);

    public SliderSettings lifeTime = new SliderSettings(StringHelper.decrypt(new byte[]{6, (byte)-86, 57, (byte)-115, 50, (byte)-49, 94, (byte)-69, 47}), StringHelper.decrypt(new byte[]{6, (byte)-86, 57, (byte)-115, 102, (byte)-14, 90, (byte)-77, 106, (byte)-21, 44, (byte)-115, 113, (byte)-78}))
            .range(2.0f, 60.0f)
            .setValue(10.0f);

    public SliderSettings size = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-86, 37, (byte)-115}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 106, (byte)-80, 54, (byte)-110, 119}))
            .range(0.1f, 1.5f)
            .setValue(1.5f);

    public SliderSettings glowSize = new SliderSettings(StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97, 50, (byte)-56, 94, (byte)-84, 47}), StringHelper.decrypt(new byte[]{13, (byte)-81, 48, (byte)-97, 50, (byte)-24, 94, (byte)-84, 47}))
            .range(0.1f, 5.0f)
            .setValue(3f);

    public BooleanSetting physics = new BooleanSetting(StringHelper.decrypt(new byte[]{26, (byte)-85, 38, (byte)-101, 123, (byte)-8, 68}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 57, (byte)-29, 57, (byte)-119, 126, (byte)-9, 23, (byte)-78, 37, (byte)-76, 49}))
            .setValue(false);

    public BooleanSetting randomColor = new BooleanSetting(StringHelper.decrypt(new byte[]{24, (byte)-94, (byte)-113, 85, 118, (byte)-12, 90, (byte)-10, 9, (byte)-84, 51, (byte)-121, 96}), StringHelper.decrypt(new byte[]{15, (byte)-94, 60, (byte)-128, 50, (byte)-21, 86, (byte)-92, 62, (byte)-86, 60, (byte)-124, 119, (byte)-69, 95, (byte)-73, 57, (byte)-29, 62, (byte)-56, 96, (byte)-6, 89, (byte)-78, 37, (byte)-82, 127, (byte)-117, 125, (byte)-9, 88, (byte)-92}))
            .setValue(false);

    public BooleanSetting whiteOnSpawn = new BooleanSetting(StringHelper.decrypt(new byte[]{29, (byte)-85, 54, (byte)-100, 119, (byte)-69, 88, (byte)-72, 106, (byte)-112, 47, (byte)-119, 101, (byte)-11}), StringHelper.decrypt(new byte[]{26, (byte)-94, 45, (byte)-100, 123, (byte)-8, 91, (byte)-77, 57, (byte)-29, 62, (byte)-102, 119, (byte)-69, 64, (byte)-66, 35, (byte)-73, 58, (byte)-56, 125, (byte)-11, 23, (byte)-91, 58, (byte)-94, 40, (byte)-122, 50, (byte)-6, 89, (byte)-78, 106, (byte)-80, 50, (byte)-121, 125, (byte)-17, 95, (byte)-70, 51, (byte)-29, 60, (byte)-128, 115, (byte)-11, 80, (byte)-77, 106, (byte)-96, 48, (byte)-124, 125, (byte)-23}))
            .setValue(true);

    public BooleanSetting whiteCenter = new BooleanSetting(StringHelper.decrypt(new byte[]{29, (byte)-85, 54, (byte)-100, 119, (byte)-69, 116, (byte)-77, 36, (byte)-73, 58, (byte)-102}), StringHelper.decrypt(new byte[]{29, (byte)-85, 54, (byte)-100, 119, (byte)-69, 84, (byte)-77, 36, (byte)-73, 58, (byte)-102, 50, (byte)-3, 88, (byte)-92, 106, (byte)-73, 58, (byte)-112, 102, (byte)-18, 69, (byte)-77, 46, (byte)-29, 47, (byte)-119, 96, (byte)-17, 94, (byte)-75, 38, (byte)-90, 44}))
            .setValue(false)
            .visible(() -> !mode.getSelected().equals("3D Cubes"));

    public ColorSetting cubeColor = new ColorSetting("Color", "Particle color")
            .value(0xFF896148)
            .visible(() -> !randomColor.isValue());

    public jvwsoq() {
        super(StringHelper.decrypt(new byte[]{29, (byte)-84, 45, (byte)-124, 118, (byte)-53, 86, (byte)-92, 62, (byte)-86, 60, (byte)-124, 119, (byte)-24}), StringHelper.decrypt(new byte[]{12, (byte)-81, 38, (byte)-127, 124, (byte)-4, 23, (byte)-90, 43, (byte)-79, 43, (byte)-127, 113, (byte)-9, 82, (byte)-91, 106, (byte)-86, 49, (byte)-56, 102, (byte)-13, 82, (byte)-10, 61, (byte)-84, 45, (byte)-124, 118}), ModuleCategory.VISUALS);
        settings(mode, cubeCount, lifeTime, size, glowSize, physics, randomColor, whiteOnSpawn, whiteCenter, cubeColor);
    }

    @Override
    public void deactivate() {
        particles.clear();
        lastPlayerPos = Vec3d.ZERO;
        playerVelocity = Vec3d.ZERO;
        playerSpeed = 0;
    }

    private Particle.ParticleType getParticleType() {
        String selected = mode.getSelected();
        return switch (selected) {
            case "3D Cubes" -> Particle.ParticleType.CUBE_3D;
            case "Crown" -> Particle.ParticleType.CROWN;
            case "Cube" -> Particle.ParticleType.CUBE_BLAST;
            case "Dollar" -> Particle.ParticleType.DOLLAR;
            case "Heart" -> Particle.ParticleType.HEART;
            case "Lightning" -> Particle.ParticleType.LIGHTNING;
            case "Line" -> Particle.ParticleType.LINE;
            case "Diamond" -> Particle.ParticleType.RHOMBUS;
            case "Snowflake" -> Particle.ParticleType.SNOWFLAKE;
            case "Star" -> Particle.ParticleType.STAR;
            case "Star 2" -> Particle.ParticleType.STAR_ALT;
            case "Triangle" -> Particle.ParticleType.TRIANGLE;
            case "Glow" -> Particle.ParticleType.GLOW;
            case "Random" -> Particle.ParticleType.RANDOM;
            default -> Particle.ParticleType.CUBE_3D;
        };
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        Vec3d currentPos = mc.player.getEntityPos();

        if (lastPlayerPos != Vec3d.ZERO) {
            playerVelocity = currentPos.subtract(lastPlayerPos);
            playerSpeed = playerVelocity.horizontalLength();
        }
        lastPlayerPos = currentPos;

        double despawnDistSq = ParticleSpawner.getDespawnDistanceSquared();
        for (Particle p : particles) {
            if (!p.isFadingOut()) {
                double distSq = p.getHorizontalDistanceSquaredTo(currentPos);
                if (distSq > despawnDistSq) {
                    p.startFadeOut();
                }
            }
        }

        int actualDelay = ParticleSpawner.calculateSpawnDelay(playerSpeed);

        if (particles.size() < cubeCount.getValue() && timer.finished(actualDelay)) {
            int spawnCount = ParticleSpawner.calculateSpawnCount(playerSpeed, particles.size(), (int) cubeCount.getValue());
            long lifeTimeMs = (long) (lifeTime.getValue() * 1000);
            Particle.ParticleType type = getParticleType();

            for (int i = 0; i < spawnCount && particles.size() < cubeCount.getValue(); i++) {
                Particle particle = ParticleSpawner.createParticle(currentPos, playerVelocity, playerSpeed, lifeTimeMs, type);
                particle.setPhysics(physics.isValue());
                particle.setSize(size.getValue());
                particles.add(particle);
            }

            timer.reset();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (particles.isEmpty()) return;

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrices = e.getStack();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        long now = System.currentTimeMillis();
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();
        float cameraPitch = mc.gameRenderer.getCamera().getPitch();
        float rotation = (float) (now % 9000L) / 9000.0F * 360.0F;
        int baseColor = cubeColor.getColor();
        float glow = glowSize.getValue();
        boolean useRandomColor = randomColor.isValue();
        boolean useWhiteOnSpawn = whiteOnSpawn.isValue();
        boolean useWhiteCenter = whiteCenter.isValue();

        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update(now);
            if (p.shouldRemove()) {
                iterator.remove();
            }
        }

        for (Particle p : particles) {
            double distSq = p.getDistanceSquaredTo(cameraPos);
            if (distSq < 150 * 150) {
                p.render(matrices, immediate, cameraPos, baseColor, rotation, cameraYaw, cameraPitch, glow, useRandomColor, useWhiteOnSpawn, useWhiteCenter);
            }
        }

        immediate.draw();
    }
}
