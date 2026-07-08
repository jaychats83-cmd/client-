package starry.util.config.impl;

import starry.util.config.impl.bind.BindConfig;
import starry.util.config.impl.blockesp.BlockESPConfig;
import starry.util.config.impl.drag.DragConfig;
import starry.util.config.impl.friend.FriendConfig;
import starry.util.config.impl.macro.MacroConfig;
import starry.util.config.impl.staff.StaffConfig;
import starry.util.config.impl.way.WayConfig;
import starry.util.theme.ThemeManager;

public final class UserConfigManager {
    private UserConfigManager() {
    }

    public static void saveCurrentUser() {
        BindConfig.getInstance().save();
        BlockESPConfig.getInstance().save();
        DragConfig.getInstance().save();
        FriendConfig.getInstance().save();
        MacroConfig.getInstance().save();
        StaffConfig.getInstance().save();
        WayConfig.getInstance().save();
        ThemeManager.save();
    }

    public static void loadCurrentUser() {
        BindConfig.getInstance().load();
        BlockESPConfig.getInstance().load();
        DragConfig.getInstance().load();
        FriendConfig.getInstance().load();
        MacroConfig.getInstance().load();
        StaffConfig.getInstance().load();
        WayConfig.getInstance().load();
        ThemeManager.load();
    }
}
