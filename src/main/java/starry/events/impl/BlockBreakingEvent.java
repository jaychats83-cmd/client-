package starry.events.impl;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import starry.events.api.events.callables.EventCancellable;

@Getter
@RequiredArgsConstructor
public class BlockBreakingEvent extends EventCancellable {
    private final BlockPos blockPos;
    private final Direction direction;
}
