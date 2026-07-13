package starry.screens.clickgui.impl.configs.handler;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import starry.util.config.cloud.CloudConfigEntry;
import starry.util.config.cloud.CloudConfigManager;
import starry.util.config.impl.ConfigSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class ConfigDataHandler {

    @Getter(AccessLevel.NONE)
    private volatile List<CloudConfigEntry> entries = new ArrayList<>();
    private final ConfigAnimationHandler animationHandler;

    private CloudConfigEntry selectedEntry = null;
    private boolean isCreating = false;
    private String newConfigName = "";
    private boolean isLoading = false;
    private boolean isImporting = false;
    private String importCode = "";

    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private float scrollTopFade = 0f;
    private float scrollBottomFade = 0f;

    private final CloudConfigManager cloud = new CloudConfigManager();
    private String cachedLaunchConfigId;

    public ConfigDataHandler(ConfigAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public List<CloudConfigEntry> getEntries() {
        return entries;
    }

    public void refreshConfigs() {
        List<CloudConfigEntry> oldEntries = entries;
        List<CloudConfigEntry> newEntries = cloud.fetchAll();

        for (CloudConfigEntry e : newEntries) {
            boolean existed = false;
            for (CloudConfigEntry o : oldEntries) {
                if (o.id.equals(e.id)) { existed = true; break; }
            }
            if (!existed) {
                animationHandler.getItemAppearAnimations().put(e.id, 0f);
            }
        }

        if (selectedEntry != null) {
            boolean stillExists = false;
            for (CloudConfigEntry e : newEntries) {
                if (e.id.equals(selectedEntry.id)) { stillExists = true; break; }
            }
            if (!stillExists) selectedEntry = null;
        }

        entries = newEntries;
        cachedLaunchConfigId = cloud.getLaunchConfigId();
    }

    public void updateScroll(float deltaTime) {
        scrollOffset += (targetScrollOffset - scrollOffset) * 12f * deltaTime;
    }

    public void updateScrollFades(float visibleHeight) {
        float contentHeight = entries.size() * 27f;

        if (contentHeight <= visibleHeight) {
            scrollTopFade = 0f;
            scrollBottomFade = 0f;
            return;
        }

        float maxScroll = contentHeight - visibleHeight;
        scrollTopFade = (float) Math.min(1f, -scrollOffset / 20f);
        scrollBottomFade = (float) Math.min(1f, (maxScroll + scrollOffset) / 20f);
    }

    public void handleScroll(double vertical, float visibleHeight) {
        float contentHeight = entries.size() * 27f;
        float maxScroll = Math.max(0, contentHeight - visibleHeight);
        targetScrollOffset += vertical * 25;
        targetScrollOffset = Math.max(-maxScroll, Math.min(0, targetScrollOffset));
    }

    public void saveNewConfig(String name) {
        if (name.equalsIgnoreCase("autoconfig")) return;

        isLoading = true;
        CompletableFuture.runAsync(() -> {
            String data = new ConfigSerializer().serialize();
            CloudConfigEntry created = cloud.create(name, data);
            if (created != null) {
                refreshConfigs();
            }
            isLoading = false;
        });
    }

    public void loadConfig(String id) {
        isLoading = true;
        CompletableFuture.runAsync(() -> {
            CloudConfigEntry entry = cloud.findById(id);
            if (entry != null && entry.data != null) {
                new ConfigSerializer().deserialize(entry.data);
            }
            isLoading = false;
        });
    }

    public void updateConfig(String id) {
        isLoading = true;
        CompletableFuture.runAsync(() -> {
            String data = new ConfigSerializer().serialize();
            cloud.update(id, data);
            isLoading = false;
        });
    }

    public void deleteConfig(String id) {
        isLoading = true;
        CompletableFuture.runAsync(() -> {
            cloud.delete(id);
            if (selectedEntry != null && selectedEntry.id.equals(id)) {
                selectedEntry = null;
            }
            refreshConfigs();
            isLoading = false;
        });
    }

    public void importByCode(String code) {
        isLoading = true;
        CompletableFuture.runAsync(() -> {
            CloudConfigEntry entry = cloud.importFile(code.trim());
            if (entry != null && entry.data != null) {
                new ConfigSerializer().deserialize(entry.data);
                refreshConfigs();
            }
            isLoading = false;
        });
    }

    public String getShareCode(String id) {
        for (CloudConfigEntry e : entries) {
            if (e.id.equals(id)) return e.id;
        }
        return null;
    }

    public String getLaunchConfigId() {
        return cachedLaunchConfigId;
    }

    public boolean isLaunchConfig(String id) {
        return cachedLaunchConfigId != null && cachedLaunchConfigId.equals(id);
    }

    public void setLaunchConfig(String id) {
        cachedLaunchConfigId = id;
        isLoading = true;
        CompletableFuture.runAsync(() -> {
            cloud.setLaunchConfig(id);
            isLoading = false;
        });
    }

    public void clearLaunchConfig() {
        cachedLaunchConfigId = null;
        isLoading = true;
        CompletableFuture.runAsync(() -> {
            cloud.clearLaunchConfig();
            isLoading = false;
        });
    }

    public void loadLaunchConfig() {
        String id = cachedLaunchConfigId;
        if (id != null) {
            CompletableFuture.runAsync(() -> {
                CloudConfigEntry entry = cloud.findById(id);
                if (entry != null && entry.data != null) {
                    new ConfigSerializer().deserialize(entry.data);
                }
            });
        }
    }

    public void toggleCreating() {
        isCreating = !isCreating;
        if (!isCreating) {
            newConfigName = "";
        }
    }

    public void toggleImporting() {
        isImporting = !isImporting;
        if (!isImporting) {
            importCode = "";
        }
    }

    public void appendChar(char chr) {
        if (isCreating && newConfigName.length() < 32 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-')) {
            newConfigName += chr;
        }
        if (isImporting && importCode.length() < 260 && chr >= 32 && chr != '\n' && chr != '\r') {
            importCode += chr;
        }
    }

    public void removeLastChar() {
        if (isCreating && !newConfigName.isEmpty()) {
            newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
        }
        if (isImporting && !importCode.isEmpty()) {
            importCode = importCode.substring(0, importCode.length() - 1);
        }
    }

    public void clearNewConfigName() {
        newConfigName = "";
    }
}