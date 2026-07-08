package starry.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import starry.events.api.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
