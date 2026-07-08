package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaceSwap extends ModuleStructure {
    SliderSettings fallDistance = new SliderSettings("Fall Distance", "").setValue(3f).range(0f, 10f);
    BooleanSetting autoSwitchBack = new BooleanSetting("Switch Back", "").setValue(true);

    private int prevSlot = -1;

    public MaceSwap() {
        super("Mace Swap", ModuleCategory.COMBAT);
        settings(fallDistance, autoSwitchBack);
    }

    @Override
    public void activate() { prevSlot = -1; }
    @Override
    public void deactivate() { if (prevSlot != -1) { mc.player.getInventory().setSelectedSlot(prevSlot); prevSlot = -1; } }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player.fallDistance <= fallDistance.getValue()) return;
        int maceSlot = findMace();
        if (maceSlot == -1) return;
        prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(maceSlot);
    }

    private int findMace() {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) if (inv.getStack(i).isOf(Items.MACE)) return i;
        return -1;
    }
}
