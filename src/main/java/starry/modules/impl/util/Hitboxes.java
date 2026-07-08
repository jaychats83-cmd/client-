package starry.modules.impl.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Hitboxes extends ModuleStructure {
    public static Hitboxes getInstance() {
        return Instance.get(Hitboxes.class);
    }

    public SliderSettings expansion = new SliderSettings("Expansion", "").setValue(0.35f).range(0.05f, 2f);

    public Hitboxes() {
        super("Hitboxes", ModuleCategory.MISC);
        settings(expansion);
    }
}
