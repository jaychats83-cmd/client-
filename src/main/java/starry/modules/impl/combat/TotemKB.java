package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TotemKB extends ModuleStructure {
    SliderSettings switchBackTicks = new SliderSettings("Switch Back Ticks", "").setValue(1f).range(0f, 8f);
    BooleanSetting onlyPlayers = new BooleanSetting("Only Players", "").setValue(true);
    BooleanSetting requireTotem = new BooleanSetting("Require Totem", "").setValue(true);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "").setValue(true);

    int totemSlot = -1;
    int restoreClock;

    public TotemKB() {
        super("Totem KB", "Swaps from totem to sword for a hit, then holds the totem again", ModuleCategory.CPVP);
        settings(switchBackTicks, onlyPlayers, requireTotem, switchBack);
    }

    @Override
    public void activate() {
        reset();
    }

    @Override
    public void deactivate() {
        restoreTotem();
        reset();
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;

        Entity target = hit.getEntity();
        if (target == mc.player || !target.isAlive()) return;
        if (onlyPlayers.isValue() && !(target instanceof PlayerEntity)) return;

        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        if (requireTotem.isValue() && !mc.player.getInventory().getStack(selectedSlot).isOf(Items.TOTEM_OF_UNDYING)) return;

        int swordSlot = getSwordSlot();
        if (swordSlot == -1 || swordSlot == selectedSlot) return;

        totemSlot = selectedSlot;
        restoreClock = switchBackTicks.getInt();
        mc.player.getInventory().setSelectedSlot(swordSlot);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || totemSlot < 0 || !switchBack.isValue()) return;
        if (restoreClock > 0) {
            restoreClock--;
            return;
        }
        restoreTotem();
        reset();
    }

    private int getSwordSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.NETHERITE_SWORD)
                    || mc.player.getInventory().getStack(i).isOf(Items.DIAMOND_SWORD)
                    || mc.player.getInventory().getStack(i).isOf(Items.IRON_SWORD)
                    || mc.player.getInventory().getStack(i).isOf(Items.STONE_SWORD)
                    || mc.player.getInventory().getStack(i).isOf(Items.GOLDEN_SWORD)
                    || mc.player.getInventory().getStack(i).isOf(Items.WOODEN_SWORD)) {
                return i;
            }
        }
        return -1;
    }

    private void restoreTotem() {
        if (mc.player == null || totemSlot < 0 || totemSlot > 8) return;
        if (mc.player.getInventory().getStack(totemSlot).isOf(Items.TOTEM_OF_UNDYING)) {
            mc.player.getInventory().setSelectedSlot(totemSlot);
            return;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    private void reset() {
        totemSlot = -1;
        restoreClock = 0;
    }
}
