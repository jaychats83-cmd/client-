package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoDoubleHand extends ModuleStructure {
    BooleanSetting stopOnCrystal = new BooleanSetting("Stop On Crystal", "Stops while Auto Crystal is running").setValue(false);
    BooleanSetting checkShield = new BooleanSetting("Check Shield", "Checks if you're blocking with a shield").setValue(false);
    BooleanSetting onPop = new BooleanSetting("On Pop", "Switches to a totem if you pop").setValue(false);
    BooleanSetting onHealth = new BooleanSetting("On Health", "Switches to totem if low on health").setValue(false);
    BooleanSetting predict = new BooleanSetting("Predict Damage", "").setValue(true);
    SliderSettings health = new SliderSettings("Health", "Health to trigger at").setValue(2f).range(1, 20);
    BooleanSetting onGround = new BooleanSetting("On Ground", "Whether crystal damage is checked on ground or not").setValue(true);
    BooleanSetting checkPlayers = new BooleanSetting("Check Players", "Checks for nearby players").setValue(true);
    SliderSettings distance = new SliderSettings("Distance", "Player distance").setValue(5f).range(1f, 10f);
    BooleanSetting predictCrystals = new BooleanSetting("Predict Crystals", "").setValue(false);
    BooleanSetting checkAim = new BooleanSetting("Check Aim", "Checks if the opponent is aiming at obsidian").setValue(false);
    BooleanSetting checkItems = new BooleanSetting("Check Items", "Checks if the opponent is holding crystals").setValue(false);
    SliderSettings activatesAbove = new SliderSettings("Activates Above", "Height to trigger at").setValue(0.2f).range(0f, 4f);

    private boolean belowHealth, offhandHasNoTotem;

    public AutoDoubleHand() {
        super("Auto Double Hand", ModuleCategory.CPVP);
        settings(stopOnCrystal, checkShield, onPop, onHealth, predict, health, onGround, checkPlayers, distance, predictCrystals, checkAim, checkItems, activatesAbove);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null) return;
        if (checkShield.isValue() && mc.player.isBlocking()) return;
        PlayerInventory inventory = mc.player.getInventory();

        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && onPop.isValue() && !offhandHasNoTotem) {
            offhandHasNoTotem = true;
            selectTotem();
        }
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) offhandHasNoTotem = false;
        if (mc.player.getHealth() <= health.getValue() && onHealth.isValue() && !belowHealth) {
            belowHealth = true;
            selectTotem();
        }
        if (mc.player.getHealth() > health.getValue()) belowHealth = false;
        if (!predict.isValue() || mc.player.getHealth() > 19) return;
        if (!onGround.isValue() && mc.player.isOnGround()) return;

        double squaredDist = distance.getValue() * distance.getValue();
        if (checkPlayers.isValue() && mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).noneMatch(p -> mc.player.squaredDistanceTo(p) <= squaredDist)) return;

        double above = activatesAbove.getValue();
        for (int floor = (int) Math.floor(above), i = 1; i <= floor; i++) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).isAir()) return;
        }

        Vec3d playerPos = mc.player.getEntityPos();
        BlockPos playerBlockPos = new BlockPos((int) playerPos.x, (int) playerPos.y - (int) above, (int) playerPos.z);
        if (!mc.world.getBlockState(playerBlockPos).isAir()) return;

        List<EndCrystalEntity> crystals = nearbyCrystals();
        ArrayList<Vec3d> positions = new ArrayList<>();
        crystals.forEach(e -> positions.add(e.getEntityPos()));

        if (predictCrystals.isValue()) {
            Stream<BlockPos> s = BlockPos.stream(mc.player.getBlockPos().add(-6, -8, -6), mc.player.getBlockPos().add(6, 2, 6))
                    .filter(e -> mc.world.getBlockState(e).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(e).isOf(Blocks.BEDROCK))
                    .filter(this::canPlaceCrystalClient);
            if (checkAim.isValue()) {
                s = checkItems.isValue() ? s.filter(this::arePeopleAimingAtBlockAndHoldingCrystals) : s.filter(this::arePeopleAimingAtBlock);
            }
            s.forEachOrdered(e -> positions.add(Vec3d.ofBottomCenter(e).add(0, 1, 0)));
        }

        for (Vec3d crys : positions) {
            double damage = calculateCrystalDamage(mc.player, crys);
            if (damage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                selectTotem();
                break;
            }
        }
    }

    private void selectTotem() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    private List<EndCrystalEntity> nearbyCrystals() {
        Vec3d pos = mc.player.getEntityPos();
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos.add(-6, -6, -6), pos.add(6, 6, 6)), e -> true);
    }

    private boolean canPlaceCrystalClient(BlockPos pos) {
        BlockPos up = pos.up();
        return mc.world.isAir(up) && mc.world.getOtherEntities(null, new Box(up)).stream().noneMatch(e -> e instanceof EndCrystalEntity);
    }

    private boolean arePeopleAimingAtBlock(BlockPos block) {
        return mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).anyMatch(e -> {
            Vec3d eyes = e.getCameraPosVec(1.0F);
            Vec3d lookVec = e.getRotationVec(1.0F);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(eyes, eyes.add(lookVec.multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
            return hit != null && hit.getBlockPos().equals(block);
        });
    }

    private boolean arePeopleAimingAtBlockAndHoldingCrystals(BlockPos block) {
        return mc.world.getPlayers().parallelStream().filter(e -> e != mc.player).filter(e -> e.isHolding(Items.END_CRYSTAL)).anyMatch(e -> {
            Vec3d eyes = e.getCameraPosVec(1.0F);
            Vec3d lookVec = e.getRotationVec(1.0F);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(eyes, eyes.add(lookVec.multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
            return hit != null && hit.getBlockPos().equals(block);
        });
    }

    private double calculateCrystalDamage(net.minecraft.entity.LivingEntity entity, Vec3d crystalPos) {
        double distance = entity.squaredDistanceTo(crystalPos);
        if (distance > 144) return 0;
        double exposure = 1.0;
        double damage = (12 * exposure);
        return damage;
    }
}
