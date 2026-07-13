package starry.modules.impl.basefind;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.Direction;

public class RotatedDeepslateESP extends AbstractBlockESP {
    public RotatedDeepslateESP() {
        super("Rotated Deepslate ESP", "Highlights deepslate placed with a horizontal axis", 0xAA9B72CF);
    }

    @Override
    protected boolean matches(BlockState state) {
        return state.isOf(Blocks.DEEPSLATE) && state.contains(PillarBlock.AXIS)
                && state.get(PillarBlock.AXIS) != Direction.Axis.Y;
    }
}
