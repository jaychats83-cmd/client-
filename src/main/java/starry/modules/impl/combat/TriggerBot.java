package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.input.MouseInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.mixin.MouseAccessor;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.timer.TimerUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TriggerBot extends ModuleStructure {
    BooleanSetting inScreen = new BooleanSetting("Work In Screen", "").setValue(false);
    BooleanSetting whileUse = new BooleanSetting("While Use", "").setValue(false);
    BooleanSetting onLeftClick = new BooleanSetting("On Left Click", "").setValue(false);
    BooleanSetting allItems = new BooleanSetting("All Items", "").setValue(false);
    MinMaxSetting swordDelay = new MinMaxSetting("Sword Delay", "").defaultValue(540, 550).range(0, 1000);
    MinMaxSetting axeDelay = new MinMaxSetting("Axe Delay", "").defaultValue(780, 800).range(0, 1000);
    BooleanSetting checkShield = new BooleanSetting("Check Shield", "").setValue(false);
    BooleanSetting onlyCritSword = new BooleanSetting("Only Crit Sword", "").setValue(false);
    BooleanSetting onlyCritAxe = new BooleanSetting("Only Crit Axe", "").setValue(false);
    BooleanSetting swing = new BooleanSetting("Swing Hand", "").setValue(true);
    BooleanSetting whileAscend = new BooleanSetting("While Ascending", "").setValue(false);
    BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", "").setValue(false);
    BooleanSetting strayBypass = new BooleanSetting("Stray Bypass", "").setValue(false);
    BooleanSetting allEntities = new BooleanSetting("All Entities", "").setValue(false);
    BooleanSetting useShield = new BooleanSetting("Use Shield", "").setValue(false);
    SliderSettings shieldTime = new SliderSettings("Shield Time", "").setValue(350f).range(100, 1000);
    BooleanSetting sticky = new BooleanSetting("Same Player", "").setValue(false);

    TimerUtil timer = new TimerUtil();
    int currentSwordDelay;
    int currentAxeDelay;
    Entity lastAttacked;
    int shieldStage;
    long shieldPressTime;

    public TriggerBot() {
        super("Trigger Bot", ModuleCategory.COMBAT);
        settings(inScreen, whileUse, onLeftClick, allItems, swordDelay, axeDelay, checkShield, whileAscend, sticky, onlyCritSword, onlyCritAxe, swing, clickSimulation, strayBypass, allEntities, useShield, shieldTime);
    }

    @Override
    public void activate() {
        currentSwordDelay = swordDelay.getRandomValueInt();
        currentAxeDelay = axeDelay.getRandomValueInt();
        lastAttacked = null;
        shieldStage = 0;
    }

    @Override
    public void deactivate() {
        lastAttacked = null;
        shieldStage = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        try {
            if (!inScreen.isValue() && mc.currentScreen != null) return;

            ItemStack mainHandStack = mc.player.getMainHandStack();
            Item item = mainHandStack.getItem();

            if (onLeftClick.isValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) return;

            if (((mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD) || mc.player.getOffHandStack().getItem() instanceof ShieldItem) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) && !whileUse.isValue()) return;

            if (!whileAscend.isValue() && ((!mc.player.isOnGround() && mc.player.getVelocity().y > 0) || (!mc.player.isOnGround() && mc.player.fallDistance <= 0.0F))) return;

            handleShieldRelease();

            if (!allItems.isValue()) {
                if (isSword(mainHandStack)) {
                    handleSwordAttack();
                } else if (isAxe(mainHandStack)) {
                    handleAxeAttack();
                }
            } else {
                handleAllItemsAttack();
            }
        } catch (Exception ignored) {}
    }

    private void handleSwordAttack() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity entity = hit.getEntity();

        if (sticky.isValue() && lastAttacked != null && entity != lastAttacked) return;

        if (!isValidTarget(entity)) return;

        if (entity instanceof PlayerEntity player && checkShield.isValue() && player.isBlocking() && !isShieldFacingAway(player)) return;

        if (onlyCritSword.isValue() && mc.player.fallDistance <= 0.0F) return;

        if (timer.isReached(currentSwordDelay)) {
            if (useShield.isValue() && mc.player.getOffHandStack().getItem() == Items.SHIELD && mc.player.isBlocking()) {
                simulateClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            }

            hitEntity(entity);
            lastAttacked = entity;

            if (clickSimulation.isValue()) simulateClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

            currentSwordDelay = swordDelay.getRandomValueInt();
            timer.reset();
        } else {
            if (useShield.isValue() && mc.player.getOffHandStack().getItem() == Items.SHIELD) {
                shieldStage = 1;
                shieldPressTime = System.currentTimeMillis();
            }
        }
    }

    private void handleAxeAttack() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity entity = hit.getEntity();

        if (!isValidTarget(entity)) return;

        if (entity instanceof PlayerEntity player && checkShield.isValue() && player.isBlocking() && !isShieldFacingAway(player)) return;

        if (onlyCritAxe.isValue() && mc.player.fallDistance <= 0.0F) return;

        if (timer.isReached(currentAxeDelay)) {
            hitEntity(entity);

            if (clickSimulation.isValue()) simulateClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

            currentAxeDelay = axeDelay.getRandomValueInt();
            timer.reset();
        } else {
            if (useShield.isValue() && mc.player.getOffHandStack().getItem() == Items.SHIELD) {
                shieldStage = 1;
                shieldPressTime = System.currentTimeMillis();
            }
        }
    }

    private void handleAllItemsAttack() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)) return;
        Entity entity = hit.getEntity();

        if (sticky.isValue() && lastAttacked != null && entity != lastAttacked) return;

        if (!isValidTarget(entity)) return;

        if (entity instanceof PlayerEntity player && checkShield.isValue() && player.isBlocking() && !isShieldFacingAway(player)) return;

        if (onlyCritSword.isValue() && mc.player.fallDistance <= 0.0F) return;

        if (timer.isReached(currentSwordDelay)) {
            hitEntity(entity);
            lastAttacked = entity;

            if (clickSimulation.isValue()) simulateClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

            currentSwordDelay = swordDelay.getRandomValueInt();
            timer.reset();
        } else {
            if (useShield.isValue() && mc.player.getOffHandStack().getItem() == Items.SHIELD) {
                shieldStage = 1;
                shieldPressTime = System.currentTimeMillis();
            }
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity instanceof PlayerEntity) return true;
        if (strayBypass.isValue() && entity instanceof ZombieEntity) return true;
        return allEntities.isValue() && entity != null;
    }

    private void handleShieldRelease() {
        if (shieldStage == 1 && System.currentTimeMillis() - shieldPressTime >= shieldTime.getValue()) {
            MouseInput input = new MouseInput(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 0);
            ((MouseAccessor) mc.mouse).qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_RELEASE);
            shieldStage = 0;
        }
    }

    private boolean isShieldFacingAway(PlayerEntity player) {
        if (mc.player != null && player != null) {
            Vec3d playerPos = mc.player.getEntityPos();
            Vec3d targetPos = player.getEntityPos();
            Vec3d directionToPlayer = playerPos.subtract(targetPos).normalize();
            float yaw = player.getYaw();
            float pitch = player.getPitch();
            Vec3d facingDirection = new Vec3d(
                    -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    -Math.sin(Math.toRadians(pitch)),
                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
            ).normalize();
            double dotProduct = facingDirection.dotProduct(directionToPlayer);
            return dotProduct < 0;
        }
        return false;
    }

    private boolean isSword(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS);
    }

    private boolean isAxe(ItemStack stack) {
        return stack.isIn(ItemTags.AXES);
    }

    private void hitEntity(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void simulateClick(int button) {
        if (clickSimulation.isValue()) {
            MouseInput input = new MouseInput(button, 0);
            MouseAccessor mouse = (MouseAccessor) mc.mouse;
            mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_PRESS);
            mouse.qcloud$onMouseButton(mc.getWindow().getHandle(), input, GLFW.GLFW_RELEASE);
        }
    }

}
