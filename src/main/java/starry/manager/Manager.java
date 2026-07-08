package starry.manager;

import lombok.Getter;
import starry.client.draggables.HudManager;
import starry.events.api.EventManager;

import starry.modules.module.*;
import starry.screens.clickgui.ClickGui;
import starry.util.config.cloud.CloudConfigEntry;
import starry.util.config.impl.ConfigSerializer;
import starry.util.config.impl.LocalConfigManager;
import starry.util.config.impl.bind.BindConfig;
import java.util.List;
import starry.util.config.impl.blockesp.BlockESPConfig;
import starry.util.config.impl.drag.DragConfig;
import starry.util.config.impl.friend.FriendConfig;
import starry.util.config.impl.staff.StaffConfig;
import starry.util.modules.ModuleProvider;
import starry.util.modules.ModuleSwitcher;
import starry.util.render.shader.RenderCore;
import starry.util.render.shader.Scissor;
import starry.util.render.font.FontInitializer;
import starry.util.repository.macro.MacroRepository;
import starry.util.repository.way.WayRepository;
import starry.util.theme.ThemeManager;
import starry.util.tps.TPSCalculate;

/**
 *  © 2026 Copyright starry Client 2.0
 *        All Rights Reserved ®
 */

@Getter
public class Manager {
    private EventManager eventManager;
    private RenderCore renderCore;
    private Scissor scissor;
    private ModuleProvider moduleProvider;
    private ModuleRepository moduleRepository;
    private ModuleSwitcher moduleSwitcher;
    private ClickGui clickgui;
    private TPSCalculate tpsCalculate;
    private HudManager hudManager = new HudManager();

    public void init() {
        MacroRepository.getInstance().init();
        WayRepository.getInstance().init();
        BlockESPConfig.getInstance().load();
        FriendConfig.getInstance().load();
        StaffConfig.getInstance().load();
        DragConfig.getInstance().load();
        BindConfig.getInstance();
        ThemeManager.load();

        FontInitializer.register();

        tpsCalculate = new TPSCalculate();

        clickgui = new ClickGui();
        eventManager = new EventManager();
        renderCore = new RenderCore();
        scissor = new Scissor();
        hudManager = new HudManager();
        hudManager.initElements();
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);

        LocalConfigManager local = new LocalConfigManager();
        List<CloudConfigEntry> allEntries = local.fetchAll();
        String launchId = local.getLaunchConfigId();
        if (launchId != null) {
            String launchIdFinal = launchId;
            List<CloudConfigEntry> cachedEntries = allEntries;
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                for (CloudConfigEntry e : cachedEntries) {
                    if (e.id.equals(launchIdFinal) && e.data != null) {
                        new ConfigSerializer().deserialize(e.data);
                        break;
                    }
                }
            }, "starry-LaunchConfig").start();
        }
    }
}
