package starry.modules.impl.extras;

import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.util.Instance;

public class AntiPacketKick extends ModuleStructure {
    public AntiPacketKick() {
        super("Anti Packet Kick", "Prevents malformed incoming packets from crashing or disconnecting the client", ModuleCategory.EXTRAS);
    }

    public static boolean isEnabled() {
        AntiPacketKick module = Instance.get(AntiPacketKick.class);
        return module != null && module.isState();
    }
}
