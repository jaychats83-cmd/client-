package starry.util;

import starry.Initialization;
import starry.manager.Manager;
import starry.modules.module.ModuleStructure;
import starry.modules.module.setting.Setting;
import starry.modules.module.setting.implement.*;
import starry.screens.clickgui.ClickGui;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;

public class SelfDestruct {

    public static void destruct() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Manager manager = Initialization.getInstance().getManager();
        if (manager == null) return;

        if (manager.getModuleRepository() != null) {
            for (ModuleStructure module : manager.getModuleRepository().modules()) {
                if (module.state) {
                    module.setState(false);
                }
                for (Setting setting : module.settings()) {
                    if (setting instanceof TextSetting t) t.setText("");
                    else if (setting instanceof SliderSettings s) s.setValue(0);
                    else if (setting instanceof MinMaxSetting m) { m.setMinValue(0); m.setMaxValue(0); }
                    else if (setting instanceof BooleanSetting b) b.setValue(false);
                    else if (setting instanceof SelectSetting s) s.setSelected("");
                    else if (setting instanceof ColorSetting c) c.setColor(0);
                }
            }
        }

        if (manager.getEventManager() != null) {
            try {
                Field listenersField = manager.getEventManager().getClass().getDeclaredField("listeners");
                listenersField.setAccessible(true);
                Object listeners = listenersField.get(manager.getEventManager());
                if (listeners instanceof java.util.Map) {
                    ((java.util.Map<?, ?>) listeners).clear();
                }
            } catch (Exception ignored) {}
        }

        try {
            Field instanceField = Initialization.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception ignored) {}

        if (mc.currentScreen instanceof ClickGui) {
            ClickGui.INSTANCE.close();
        }

        Runtime.getRuntime().gc();
    }
}
