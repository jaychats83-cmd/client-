package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SafeAnchor extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Асtivate Key", "Кеy that does safe anchor").setKey(GLFW.GLFW_KEY_V);
    SliderSettings switchDelay = new SliderSettings("Switch Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings totemSlot = new SliderSettings("Totem Slot", "").setValue(9f).range(1f, 9f);
    SliderSettings range = new SliderSettings("Range", "").setValue(5f).range(3f, 6f);
    BooleanSetting placeAnchor = new BooleanSetting("Place Anchor", "").setValue(true);
    BooleanSetting placeGlowstoneWall = new BooleanSetting("Place Glowstone Wall", "").setValue(true);
    BooleanSetting swing = new BooleanSetting("Swing Hand", "").setValue(true);
    BooleanSetting silentAim = new BooleanSetting("Silent Aim", "Keep your camera still while other players see placement-facing head rotations").setValue(true);

    private int step;
    private long lastStepTime;
    private boolean running, keyWasDown;
    private BlockPos anchorPos, wallPos;

    public SafeAnchor() {
        super("Safe Anchor", "Safe anchor sequence with optional server-visible placement aim", ModuleCategory.CPVP);
        settings(activateKey, switchDelay, totemSlot, range, placeAnchor, placeGlowstoneWall, swing, silentAim);
    }

    @Override
    public void activate() { reset(); keyWasDown = false; }

    private void reset() {
        lastStepTime = System.currentTimeMillis(); step = 0; running = false;
        anchorPos = null; wallPos = null;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) { reset(); return; }
        if (!hasRequiredItems()) { reset(); return; }

        if (!running) {
            boolean keyDown = isKeyPressed(activateKey.getKey());
            if (!keyDown) { keyWasDown = false; return; }
            if (keyWasDown) return;
            keyWasDown = true;
            if (!captureTarget()) return;
            running = true;
        }

        if (System.currentTimeMillis() - lastStepTime < switchDelay.getValue()) return;
        lastStepTime = System.currentTimeMillis();

        switch (step) {
            case 0 -> {
                if (mc.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR) || !placeAnchor.isValue())
                    step = mc.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR) ? 2 : 1;
            }
            case 1 -> { if (!place(anchorPos, Items.RESPAWN_ANCHOR)) { reset(); return; } }
            case 2 -> {
                if (!mc.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) { reset(); return; }
                if (!placeOn(anchorPos, Items.GLOWSTONE)) return;
            }
            case 3 -> {
                if (placeGlowstoneWall.isValue() && isReplaceable(wallPos) && !place(wallPos, Items.GLOWSTONE)) return;
            }
            case 4 -> mc.player.getInventory().setSelectedSlot(totemSlot.getInt() - 1);
            case 5 -> {
                if (hasRiskyDrop(anchorPos)) { reset(); return; }
                chargeAnchor(anchorPos);
            }
            case 6 -> { reset(); return; }
            default -> { reset(); return; }
        }
        step++;
    }

    private boolean captureTarget() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos hitPos = hit.getBlockPos();
        if (mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(hitPos)) > range.getValue()) return false;

        if (mc.world.getBlockState(hitPos).isOf(Blocks.RESPAWN_ANCHOR)) {
            anchorPos = hitPos.toImmutable();
            step = 2;
        } else if (isReplaceable(hitPos)) {
            anchorPos = hitPos.toImmutable();
            step = 1;
        } else {
            anchorPos = hitPos.offset(hit.getSide()).toImmutable();
            step = 1;
        }
        if (!mc.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR) && !isReplaceable(anchorPos)) return false;
        wallPos = calculateWallPos(anchorPos);
        return true;
    }

    private BlockPos calculateWallPos(BlockPos anchor) {
        Vec3d player = mc.player.getEntityPos();
        Vec3d anchorCenter = Vec3d.ofCenter(anchor);
        Vec3d delta = player.subtract(anchorCenter);
        Direction facing = Math.abs(delta.x) > Math.abs(delta.z)
                ? (delta.x > 0 ? Direction.EAST : Direction.WEST)
                : (delta.z > 0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos candidate = anchor.offset(facing);
        if (candidate.equals(anchor) || candidate.equals(mc.player.getBlockPos())) {
            candidate = mc.player.getBlockPos().offset(facing);
            if (candidate.equals(anchor)) candidate = mc.player.getBlockPos();
        }
        return candidate.toImmutable();
    }

    private boolean place(BlockPos pos, net.minecraft.item.Item item) {
        if (!selectItem(item)) return false;
        BlockHitResult hit = supportHit(pos);
        if (hit == null) return false;
        aimAt(hit.getPos());
        mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hit);
        if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        return true;
    }

    private boolean placeOn(BlockPos pos, net.minecraft.item.Item item) {
        if (!selectItem(item)) return false;
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        aimAt(hit.getPos());
        mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hit);
        if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        return true;
    }

    private void chargeAnchor(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        aimAt(hit.getPos());
        mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hit);
        if (swing.isValue()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    private BlockHitResult supportHit(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (isReplaceable(neighbor)) continue;
            Direction face = dir.getOpposite();
            Vec3d faceCenter = Vec3d.ofCenter(neighbor).add(Vec3d.of(face.getVector()).multiply(0.5));
            return new BlockHitResult(faceCenter, face, neighbor, false);
        }
        return null;
    }

    private void aimAt(Vec3d target) {
        if (!silentAim.isValue() || mc.getNetworkHandler() == null) return;

        double x = target.x - mc.player.getX();
        double y = target.y - mc.player.getEyeY();
        double z = target.z - mc.player.getZ();
        double horizontal = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        float pitch = (float) Math.toDegrees(-Math.atan2(y, horizontal));
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    private boolean isReplaceable(BlockPos pos) {
        return pos != null && (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).isReplaceable());
    }

    private boolean hasRequiredItems() {
        return hasItemInHotbar(Items.RESPAWN_ANCHOR) && hasItemInHotbar(Items.GLOWSTONE);
    }

    private boolean hasItemInHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(item)) return true;
        return false;
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) { mc.player.getInventory().setSelectedSlot(i); return true; }
        return false;
    }

    private boolean hasRiskyDrop(BlockPos pos) {
        Box box = new Box(pos).expand(10);
        for (ItemEntity item : mc.world.getEntitiesByClass(ItemEntity.class, box, entity -> true)) {
            if (item.getStack().isEmpty()) continue;
            String id = Registries.ITEM.getId(item.getStack().getItem()).toString();
            if (id.contains("helmet") || id.contains("chestplate") || id.contains("leggings")
                    || id.contains("boots") || id.contains("sword") || item.getStack().isOf(Items.TOTEM_OF_UNDYING))
                return true;
        }
        return false;
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode <= 8)
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

}
