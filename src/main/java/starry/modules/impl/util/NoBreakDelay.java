package starry.modules.impl.util;

import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.util.Instance;

public class NoBreakDelay extends ModuleStructure {
    public static NoBreakDelay getInstance() {
        return Instance.get(NoBreakDelay.class);
    }

    public NoBreakDelay() {
        super("No Break Delay", ModuleCategory.MISC);
    }
}
