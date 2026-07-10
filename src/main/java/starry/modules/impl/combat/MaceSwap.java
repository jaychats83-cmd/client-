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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Comparator;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaceSwap extends ModuleStructure {
    SelectSetting mode = new SelectSetting("Mode", "Which item to swap to").value("Mace", "Axe", "Sword").selected("Mace");
    SelectSetting targetMode = new SelectSetting("Target", "").value("Crosshair", "Nearest").selected("Crosshair");
    SliderSettings range = new SliderSettings("Range", "").setValue(4f).range(1f, 8f);
    SliderSettings fallDistance = new SliderSettings("Fall Distance", "").setValue(3f).range(0f, 10f);
    SliderSettings delayMs = new SliderSettings("Delay MS", "").setValue(0f).range(0f, 1000f);
    SliderSettings switchBackDelay = new SliderSettings("Switch Back Delay MS", "").setValue(0f).range(0f, 1000f);
    BooleanSetting requireFall = new BooleanSetting("Require Fall", "").setValue(true);
    BooleanSetting requireTarget = new BooleanSetting("Require Target", "").setValue(true);
    BooleanSetting autoSwitchBack = new BooleanSetting("Switch Back", "").setValue(true);

    private int prevSlot = -1;
    private long lastSwapTime;
    private long restoreAt;

    public MaceSwap() {
        super("Mace Swap", ModuleCategory.COMBAT);
        settings(mode, targetMode, range, fallDistance, delayMs, switchBackDelay, requireFall, requireTarget, autoSwitchBack);
    }

    @Override
    public void activate() { prevSlot = -1; lastSwapTime = 0; restoreAt = 0; }
    @Override
    public void deactivate() { restore(); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (restoreAt > 0 && System.currentTimeMillis() >= restoreAt) restore();
        if (!shouldHoldSwapItem()) {
            if (autoSwitchBack.isValue() && prevSlot != -1 && restoreAt == 0) scheduleRestore();
            return;
        }
        if (System.currentTimeMillis() - lastSwapTime < delayMs.getValue()) return;
        int maceSlot = findSwapSlot();
        if (maceSlot == -1 || maceSlot == mc.player.getInventory().getSelectedSlot()) return;
        prevSlot = mc.player.getInventory().getSelectedSlot();
        selectSlot(maceSlot);
        lastSwapTime = System.currentTimeMillis();
    }

    private boolean shouldHoldSwapItem() {
        if (requireFall.isValue() && mc.player.fallDistance < fallDistance.getValue()) return false;
        return !requireTarget.isValue() || findTarget() != null;
    }

    private PlayerEntity findTarget() {
        if (targetMode.isSelected("Crosshair")
                && mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity player
                && player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range.getValue()) {
            return player;
        }
        return mc.world == null ? null : mc.world.getPlayers().stream()
                .filter(player -> player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range.getValue())
                .min(Comparator.comparingDouble(player -> mc.player.distanceTo(player)))
                .orElse(null);
    }

    private int findSwapSlot() {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (mode.isSelected("Mace") && inv.getStack(i).isOf(Items.MACE)) return i;
            String path = net.minecraft.registry.Registries.ITEM.getId(inv.getStack(i).getItem()).getPath();
            if (mode.isSelected("Axe") && path.contains("_axe")) return i;
            if (mode.isSelected("Sword") && path.contains("_sword")) return i;
        }
        return -1;
    }

    private void scheduleRestore() {
        if (switchBackDelay.getValue() <= 0) restore();
        else restoreAt = System.currentTimeMillis() + (long) switchBackDelay.getValue();
    }

    private void restore() {
        if (mc.player != null && prevSlot != -1) selectSlot(prevSlot);
        prevSlot = -1;
        restoreAt = 0;
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
}
