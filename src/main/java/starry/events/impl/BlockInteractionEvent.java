package starry.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import starry.events.api.events.callables.EventCancellable;

@AllArgsConstructor
@Getter
public class BlockInteractionEvent extends EventCancellable {
    ClientPlayerEntity player;
    Hand hand;
    BlockHitResult hitResult;
}
