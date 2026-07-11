package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaceSwap extends ModuleStructure {
    SliderSettings minFall = new SliderSettings("Min Fall", "Minimum descending fall distance before swapping").setValue(1.5f).range(0f, 32f);
    SliderSettings switchBackTicks = new SliderSettings("Switch Back Ticks", "Ticks to keep the mace selected after the attack").setValue(1f).range(0, 10);
    BooleanSetting requireFall = new BooleanSetting("Require Fall", "Only swap for falling mace attacks").setValue(true);
    BooleanSetting switchBack = new BooleanSetting("Switch Back", "Restore the original weapon after the hit").setValue(true);
    int previousSlot = -1;
    int restoreTicks;

    public MaceSwap() {
        super("Mace Swap", "Swaps to a mace immediately before the attack packet, then restores your weapon", ModuleCategory.COMBAT);
        settings(minFall, switchBackTicks, requireFall, switchBack);
    }
    @Override public void activate() { previousSlot=-1; restoreTicks=0; }
    @Override public void deactivate() { restore(); }

    @EventHandler public void onAttack(AttackEvent event) {
        if (mc.player==null || mc.currentScreen!=null || previousSlot!=-1 || !validTarget(event.getTarget())) return;
        boolean descending=!mc.player.isOnGround() && mc.player.getVelocity().y < -0.01;
        if (requireFall.isValue() && (!descending || mc.player.fallDistance < minFall.getValue())) return;
        int mace=findMace();
        if (mace==-1 || mace==mc.player.getInventory().getSelectedSlot()) return;
        previousSlot=mc.player.getInventory().getSelectedSlot();
        selectSlot(mace);
        restoreTicks=Math.max(1,switchBackTicks.getInt());
    }

    @EventHandler public void onTick(TickEvent event) {
        if (previousSlot==-1 || !switchBack.isValue()) return;
        if (--restoreTicks<=0) restore();
    }
    private boolean validTarget(Entity target){return target instanceof LivingEntity living && target!=mc.player && living.isAlive();}
    private int findMace(){for(int i=0;i<9;i++)if(mc.player.getInventory().getStack(i).isOf(Items.MACE))return i;return -1;}
    private void restore(){if(mc.player!=null&&previousSlot>=0&&previousSlot<9)selectSlot(previousSlot);previousSlot=-1;restoreTicks=0;}
    private void selectSlot(int slot){mc.player.getInventory().setSelectedSlot(slot);if(mc.getNetworkHandler()!=null)mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));}
}
