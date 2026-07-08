package starry.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import starry.events.api.events.Event;
import starry.modules.module.ModuleStructure;

@Getter
@AllArgsConstructor
public class ModuleToggleEvent implements Event {
    private final ModuleStructure module;
    private final boolean enabled;
}