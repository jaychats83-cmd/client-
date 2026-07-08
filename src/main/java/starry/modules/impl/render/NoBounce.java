package starry.modules.impl.render;

import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.util.Instance;

public class NoBounce extends ModuleStructure {
    public static NoBounce getInstance() {
        return Instance.get(NoBounce.class);
    }

    public NoBounce() {
        super("No Bounce", ModuleCategory.RENDER);
    }
}
