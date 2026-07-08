package starry.modules.module.category;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ModuleCategory {
    COMBAT("Combat"),
    CPVP("CPVP"),
    ESP("ESP"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    VISUALS("Visuals"),
    MISC("Misc"),
    AUTOBUY("AutoBuy");

    final String readableName;
}
