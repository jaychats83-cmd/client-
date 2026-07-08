package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.client.input.MouseInput;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.BlockInteractionEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.mixin.MouseAccessor;

import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoCrystal extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that does the crystalling").setKey(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    SliderSettings placeDelay = new SliderSettings("Place Delay", "").setValue(0f).range(0, 20);
    SliderSettings breakDelay = new SliderSettings("Break Delay", "").setValue(0f).range(0, 20);
    SliderSettings placeChance = new SliderSettings("Place Chance", "Randomization").setValue(100f).range(0f, 100f);
    SliderSettings breakChance = new SliderSettings("Break Chance", "Randomization").setValue(100f).range(0f, 100f);
    BooleanSetting stopOnKill = new BooleanSetting("Stop on Kill", "Won't crystal if a dead player is nearby").setValue(false);
    BooleanSetting fakePunch = new BooleanSetting("Fake Punch", "Will hit every entity and block if you miss a hitcrystal").setValue(false);
    BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", "Makes the CPS hud think you're legit").setValue(false);
    BooleanSetting damageTick = new BooleanSetting("Damage Tick", "Times your crystals for a perfect d-tap").setValue(false);
    BooleanSetting antiWeakness = new BooleanSetting("Anti-Weakness", "Silently switches to a sword and then hits the crystal if you have weakness").setValue(false);
    SliderSettings particleChance = new SliderSettings("Particle Chance", "Adds block breaking particles to make it seem more legit from your POV").setValue(20f).range(0f, 100f);

    int placeClock;
    int breakClock;
    boolean placing;
    public boolean crystalling;

    public AutoCrystal() {
        super("Auto Crystal", ModuleCategory.CPVP);
        settings(activateKey, placeDelay, breakDelay, placeChance, breakChance, stopOnKill, fakePunch, clickSimulation, damageTick, antiWeakness, particleChance);
    }

    @Override
    public void activate() {
        placeClock = 0;
        breakClock = 0;
        placing = false;
        crystalling = false;
    }

    @Override
    public void deactivate() {
        crystalling = false;
        placing = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null) return;

        boolean dontPlace = placeClock != 0;
        boolean dontBreak = breakClock != 0;

        if (stopOnKill.isValue() && isDeadBodyNearby()) return;

        int randomInt = randomInt(1, 100);

        if (dontPlace) placeClock--;
        if (dontBreak) breakClock--;

        if (mc.player.isUsingItem()) return;
        if (damageTick.isValue() && damageTickCheck()) return;

        if (activateKey.getKey() != GLFW.GLFW_KEY_UNKNOWN && !isKeyPressed(activateKey.getKey())) {
            placeClock = 0;
            breakClock = 0;
            crystalling = false;
            return;
        }
        crystalling = true;

        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            handleBlockTarget(hit, dontPlace, dontBreak, randomInt);
        }

        randomInt = randomInt(1, 100);

        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            handleEntityTarget(hit, dontBreak, randomInt);
        } else if (mc.crosshairTarget instanceof BlockHitResult hit) {
            handlePlacedCrystalTarget(hit, dontBreak, randomInt);
        }
    }

    @EventHandler
    public void onBlockInteract(BlockInteractionEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (placing) return;
        if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                && event.getHitResult() instanceof BlockHitResult hit
                && (isBlock(hit.getBlockPos(), Blocks.OBSIDIAN) || isBlock(hit.getBlockPos(), Blocks.BEDROCK))) {
            event.cancel();
        }
    }

    private void handleBlockTarget(BlockHitResult hit, boolean dontPlace, boolean dontBreak, int randomInt) {
        if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            if (!dontPlace && randomInt <= placeChance.getInt()) {
                if ((isBlock(hit.getBlockPos(), Blocks.OBSIDIAN) || isBlock(hit.getBlockPos(), Blocks.BEDROCK))
                        && canPlaceCrystalClientAssumeObsidian(hit.getBlockPos())) {
                    simulateClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                    placeBlock(hit);

                    if (fakePunch.isValue()
                            && randomInt <= particleChance.getInt()
                            && canPlaceCrystalClientAssumeObsidian(hit.getBlockPos())
                            && hit.getSide() == Direction.UP) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }

                    placeClock = placeDelay.getInt();
                }
            }

            if (fakePunch.isValue()) {
                if (!dontBreak && randomInt <= breakChance.getInt()) {
                    if (isBlock(hit.getBlockPos(), Blocks.OBSIDIAN) || isBlock(hit.getBlockPos(), Blocks.BEDROCK)) return;

                    simulateClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
                    mc.interactionManager.attackBlock(hit.getBlockPos(), hit.getSide());
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.interactionManager.updateBlockBreakingProgress(hit.getBlockPos(), hit.getSide());
                    breakClock = breakDelay.getInt();
                }

                if (!dontPlace && randomInt <= placeChance.getInt() && dontBreak) {
                    simulateClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                }
            }
        }

        if (mc.crosshairTarget.getType() == HitResult.Type.MISS && fakePunch.isValue()) {
            if (!dontBreak && randomInt <= breakChance.getInt()) {
                simulateClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
                mc.player.swingHand(Hand.MAIN_HAND);
                breakClock = breakDelay.getInt();
            }

            if (!dontPlace && randomInt <= placeChance.getInt() && dontBreak) {
                simulateClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            }
        }
    }

    private void handleEntityTarget(EntityHitResult hit, boolean dontBreak, int randomInt) {
        if (dontBreak || randomInt > breakChance.getInt()) return;

        Entity entity = hit.getEntity();
        if (!fakePunch.isValue() && !(entity instanceof EndCrystalEntity || entity instanceof SlimeEntity)) return;

        int previousSlot = mc.player.getInventory().getSelectedSlot();

        if ((entity instanceof EndCrystalEntity || entity instanceof SlimeEntity) && antiWeakness.isValue() && cantBreakCrystal()) {
            selectSword();
        }

        simulateClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        attackEntity(entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        breakClock = breakDelay.getInt();

        if (antiWeakness.isValue()) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
    }

    private void handlePlacedCrystalTarget(BlockHitResult hit, boolean dontBreak, int randomInt) {
        if (dontBreak || randomInt > breakChance.getInt()) return;
        if (!(isBlock(hit.getBlockPos(), Blocks.OBSIDIAN) || isBlock(hit.getBlockPos(), Blocks.BEDROCK))) return;

        EndCrystalEntity crystal = findCrystalOn(hit.getBlockPos());
        if (crystal == null) return;

        int previousSlot = mc.player.getInventory().getSelectedSlot();

        if (antiWeakness.isValue() && cantBreakCrystal()) {
            selectSword();
        }

        simulateClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        attackEntity(crystal);
        mc.player.swingHand(Hand.MAIN_HAND);
        breakClock = breakDelay.getInt();

        if (antiWeakness.isValue()) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
    }

    private EndCrystalEntity findCrystalOn(BlockPos pos) {
        Box box = new Box(pos.up()).expand(0.5, 1.0, 0.5);
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, box, Entity::isAlive)
                .stream()
                .filter(crystal -> crystal.squaredDistanceTo(mc.player) <= 36)
                .findFirst()
                .orElse(null);
    }

    private void attackEntity(Entity entity) {
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
    }

    private void placeBlock(BlockHitResult hit) {
        placing = true;
        try {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
        } finally {
            placing = false;
        }
    }

    private void simulateClick(int button) {
        if (clickSimulation.isValue()) {
            MouseInput input = new MouseInput(button, 0);
            MouseAccessor mouse = (MouseAccessor) mc.mouse;
            mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_PRESS);
            mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_RELEASE);
        }
    }

    private boolean canPlaceCrystalClientAssumeObsidian(BlockPos pos) {
        BlockPos up = pos.up();
        if (!mc.world.isAir(up) && !mc.world.getBlockState(up).isReplaceable()) return false;
        return mc.world.getOtherEntities(null, new Box(up)).stream().noneMatch(e -> e instanceof EndCrystalEntity);
    }

    private boolean cantBreakCrystal() {
        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        return !(weakness == null
                || strength != null && strength.getAmplifier() > weakness.getAmplifier()
                || isTool(mc.player.getMainHandStack()));
    }

    private boolean damageTickCheck() {
        return mc.world.getPlayers().parallelStream()
                .filter(e -> e != mc.player)
                .filter(e -> e.squaredDistanceTo(mc.player) < 36)
                .filter(e -> e.getLastAttacker() == null)
                .filter(e -> !e.isOnGround())
                .anyMatch(e -> e.hurtTime >= 2)
                && !(mc.player.getAttacking() instanceof PlayerEntity);
    }

    private boolean isDeadBodyNearby() {
        return mc.world.getPlayers().stream()
                .anyMatch(e -> e != mc.player && !e.isAlive() && e.distanceTo(mc.player) < 8);
    }

    private void selectSword() {
        for (int i = 0; i < 9; i++) {
            if (isSword(mc.player.getInventory().getStack(i))) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    private boolean isTool(ItemStack stack) {
        return isSword(stack) || isMiningTool(stack);
    }

    private boolean isSword(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().contains("_sword");
    }

    private boolean isMiningTool(ItemStack stack) {
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.contains("_pickaxe") || path.contains("_axe") || path.contains("_shovel") || path.contains("_hoe");
    }

    private boolean isBlock(BlockPos pos, net.minecraft.block.Block block) {
        return mc.world.getBlockState(pos).isOf(block);
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode <= 8) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
