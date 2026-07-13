package starry.modules.impl.basefind;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import starry.modules.module.setting.implement.BooleanSetting;

public class PistonESP extends AbstractBlockESP {
    private final BooleanSetting normal = new BooleanSetting("Normal Pistons", "Highlight normal pistons").setValue(true);
    private final BooleanSetting sticky = new BooleanSetting("Sticky Pistons", "Highlight sticky pistons").setValue(true);

    public PistonESP() {
        super("Piston ESP", "Highlights loaded pistons", 0xAAF2A83B);
        settings(normal, sticky);
    }

    @Override
    protected boolean matches(BlockState state) {
        return normal.isValue() && state.isOf(Blocks.PISTON) || sticky.isValue() && state.isOf(Blocks.STICKY_PISTON);
    }
}
