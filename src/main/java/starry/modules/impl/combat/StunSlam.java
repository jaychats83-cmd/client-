package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Comparator;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StunSlam extends ModuleStructure {
    SliderSettings range = new SliderSettings("Range", "").setValue(4f).range(1f, 8f);
    SliderSettings minFall = new SliderSettings("Min Fall", "").setValue(0.2f).range(0f, 5f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(300f).range(0f, 1500f);
    BooleanSetting jumpFirst = new BooleanSetting("Jump First", "").setValue(true);
    BooleanSetting rotate = new BooleanSetting("Rotate", "").setValue(true);

    private long lastActionTime;

    public StunSlam() {
        super("Stun Slam", ModuleCategory.COMBAT);
        settings(range, minFall, delayMs, jumpFirst, rotate);
    }

    @Override
    public void activate() { lastActionTime = 0; }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastActionTime < delayMs.getValue()) return;

        PlayerEntity target = findNearestPlayer((float) range.getValue());
        if (target == null || !target.isAlive()) return;
        if (!selectItem(Items.MACE)) return;

        if (jumpFirst.isValue() && mc.player.isOnGround()) {
            mc.player.jump();
            lastActionTime = System.currentTimeMillis();
            return;
        }

        if (!mc.player.isOnGround() && mc.player.fallDistance >= minFall.getValue() && mc.player.getAttackCooldownProgress(0.5F) >= 0.9F) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastActionTime = System.currentTimeMillis();
        }
    }

    private PlayerEntity findNearestPlayer(float range) {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= range)
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
    }

    private boolean selectItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) { mc.player.getInventory().setSelectedSlot(i); return true; }
        return false;
    }
}
