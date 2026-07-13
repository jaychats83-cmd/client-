package starry.manager;

import lombok.Getter;
import starry.client.draggables.HudManager;
import starry.events.api.EventManager;

import starry.modules.module.*;
import starry.screens.clickgui.ClickGui;
import starry.util.config.cloud.CloudConfigEntry;
import starry.util.config.cloud.CloudConfigManager;
import starry.util.config.impl.ConfigSerializer;
import starry.util.modules.ModuleProvider;
import starry.util.modules.ModuleSwitcher;
import starry.util.render.shader.RenderCore;
import starry.util.render.shader.Scissor;
import starry.util.render.font.FontInitializer;
import starry.util.repository.macro.MacroRepository;
import starry.util.repository.way.WayRepository;
import starry.util.theme.ThemeManager;
import starry.util.tps.TPSCalculate;
import starry.util.StreamMode;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        StreamMode.initialize();

        loadLaunchConfig();
    }

    private void loadLaunchConfig() {
        CompletableFuture.runAsync(() -> {
            try {
                CloudConfigManager cloud = new CloudConfigManager();
                List<CloudConfigEntry> entries = cloud.fetchAll();
                String launchId = cloud.getLaunchConfigId();
                if (launchId == null) return;

                for (CloudConfigEntry entry : entries) {
                    if (entry.id.equals(launchId) && entry.data != null) {
                        new ConfigSerializer().deserialize(entry.data);
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }
}
