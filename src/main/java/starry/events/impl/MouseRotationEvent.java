package starry.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import starry.events.api.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class MouseRotationEvent extends EventCancellable {
    float cursorDeltaX, cursorDeltaY;
}
