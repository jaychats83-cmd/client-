package starry.util.config.impl;

import com.google.gson.*;
import starry.Initialization;
import starry.client.draggables.HudElement;
import starry.client.draggables.HudManager;
import starry.modules.module.ModuleRepository;
import starry.modules.module.ModuleStructure;
import starry.modules.module.setting.Setting;
import starry.modules.module.setting.implement.*;

import starry.util.config.impl.consolelogger.Logger;

import java.util.ArrayList;
import java.util.List;

public class ConfigSerializer {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public String serialize() {
        JsonObject root = new JsonObject();
        JsonObject modulesJson = new JsonObject();

        ModuleRepository repository = getModuleRepository();
        if (repository != null) {
            for (ModuleStructure module : repository.modules()) {
                JsonObject moduleJson = serializeModule(module);
                modulesJson.add(module.getName(), moduleJson);
            }
        }

        root.add("modules", modulesJson);
        root.add("hud", serializeHud());
        root.addProperty("version", "2.0");
        root.addProperty("timestamp", System.currentTimeMillis());
        root.addProperty("client", "qcloud");

        return GSON.toJson(root);
    }

    private JsonObject serializeModule(ModuleStructure module) {
        JsonObject moduleJson = new JsonObject();
        moduleJson.addProperty("enabled", module.isState());
        moduleJson.addProperty("key", module.getKey());
        moduleJson.addProperty("type", module.getType());
        moduleJson.addProperty("favorite", module.isFavorite());

        JsonObject settingsJson = new JsonObject();
        for (Setting setting : module.settings()) {
            JsonElement element = serializeSetting(setting);
            if (element != null) {
                settingsJson.add(setting.getName(), element);
            }
        }
        moduleJson.add("settings", settingsJson);

        return moduleJson;
    }

    private JsonElement serializeSetting(Setting setting) {
        if (setting instanceof BooleanSetting boolSetting) {
            return new JsonPrimitive(boolSetting.isValue());
        }
        if (setting instanceof SliderSettings sliderSetting) {
            return new JsonPrimitive(sliderSetting.getValue());
        }
        if (setting instanceof MinMaxSetting minMaxSetting) {
            JsonObject mmJson = new JsonObject();
            mmJson.addProperty("minValue", minMaxSetting.getMinValue());
            mmJson.addProperty("maxValue", minMaxSetting.getMaxValue());
            return mmJson;
        }
        if (setting instanceof BindSetting bindSetting) {
            JsonObject bindJson = new JsonObject();
            bindJson.addProperty("key", bindSetting.getKey());
            bindJson.addProperty("type", bindSetting.getType());
            return bindJson;
        }
        if (setting instanceof TextSetting textSetting) {
            return new JsonPrimitive(textSetting.getText() != null ? textSetting.getText() : "");
        }
        if (setting instanceof SelectSetting selectSetting) {
            return new JsonPrimitive(selectSetting.getSelected());
        }
        if (setting instanceof ColorSetting colorSetting) {
            JsonObject colorJson = new JsonObject();
            colorJson.addProperty("hue", colorSetting.getHue());
            colorJson.addProperty("saturation", colorSetting.getSaturation());
            colorJson.addProperty("brightness", colorSetting.getBrightness());
            colorJson.addProperty("alpha", colorSetting.getAlpha());
            return colorJson;
        }
        if (setting instanceof MultiSelectSetting multiSetting) {
            JsonArray array = new JsonArray();
            for (String value : multiSetting.getSelected()) {
                array.add(value);
            }
            return array;
        }
        if (setting instanceof GroupSetting groupSetting) {
            JsonObject groupJson = new JsonObject();
            groupJson.addProperty("value", groupSetting.isValue());
            JsonObject subSettingsJson = new JsonObject();
            for (Setting subSetting : groupSetting.getSubSettings()) {
                JsonElement element = serializeSetting(subSetting);
                if (element != null) {
                    subSettingsJson.add(subSetting.getName(), element);
                }
            }
            groupJson.add("subSettings", subSettingsJson);
            return groupJson;
        }
        return null;
    }

    public void deserialize(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("modules")) {
                JsonObject modulesJson = root.getAsJsonObject("modules");
                ModuleRepository repository = getModuleRepository();
                if (repository != null) {
                    for (ModuleStructure module : repository.modules()) {
                        module.setState(false);
                    }

                    for (ModuleStructure module : repository.modules()) {
                        if (modulesJson.has(module.getName())) {
                            deserializeModule(module, modulesJson.getAsJsonObject(module.getName()));
                        }
                    }
                }
            }

            if (root.has("hud")) {
                deserializeHud(root.getAsJsonObject("hud"));
            }

        } catch (JsonSyntaxException e) {
            Logger.error("AutoConfiguration: JSON syntax error!");
        }
    }

    private void deserializeModule(ModuleStructure module, JsonObject moduleJson) {
        if (moduleJson.has("enabled")) {
            boolean enabled = moduleJson.get("enabled").getAsBoolean();
            if (enabled) {
                module.setState(true);
            }
        }
        if (moduleJson.has("key")) {
            module.setKey(moduleJson.get("key").getAsInt());
        }
        if (moduleJson.has("type")) {
            module.setType(moduleJson.get("type").getAsInt());
        }
        if (moduleJson.has("favorite")) {
            module.setFavorite(moduleJson.get("favorite").getAsBoolean());
        }
        if (moduleJson.has("settings")) {
            JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
            for (Setting setting : module.settings()) {
                if (settingsJson.has(setting.getName())) {
                    deserializeSetting(setting, settingsJson.get(setting.getName()));
                }
            }
        }
    }

    private void deserializeSetting(Setting setting, JsonElement element) {
        try {
            if (setting instanceof BooleanSetting boolSetting) {
                boolSetting.setValue(element.getAsBoolean());
            } else if (setting instanceof SliderSettings sliderSetting) {
                sliderSetting.setValue((float) element.getAsDouble());
            } else if (setting instanceof MinMaxSetting minMaxSetting) {
                if (element.isJsonObject()) {
                    JsonObject mmJson = element.getAsJsonObject();
                    if (mmJson.has("minValue")) {
                        minMaxSetting.setMinValue(mmJson.get("minValue").getAsFloat());
                    }
                    if (mmJson.has("maxValue")) {
                        minMaxSetting.setMaxValue(mmJson.get("maxValue").getAsFloat());
                    }
                }
            } else if (setting instanceof BindSetting bindSetting) {
                if (element.isJsonObject()) {
                    JsonObject bindJson = element.getAsJsonObject();
                    if (bindJson.has("key")) {
                        bindSetting.setKey(bindJson.get("key").getAsInt());
                    }
                    if (bindJson.has("type")) {
                        bindSetting.setType(bindJson.get("type").getAsInt());
                    }
                } else {
                    bindSetting.setKey(element.getAsInt());
                }
            } else if (setting instanceof TextSetting textSetting) {
                textSetting.setText(element.getAsString());
            } else if (setting instanceof SelectSetting selectSetting) {
                selectSetting.setSelected(element.getAsString());
            } else if (setting instanceof ColorSetting colorSetting) {
                if (element.isJsonObject()) {
                    JsonObject colorJson = element.getAsJsonObject();
                    if (colorJson.has("hue")) {
                        colorSetting.setHue(colorJson.get("hue").getAsFloat());
                    }
                    if (colorJson.has("saturation")) {
                        colorSetting.setSaturation(colorJson.get("saturation").getAsFloat());
                    }
                    if (colorJson.has("brightness")) {
                        colorSetting.setBrightness(colorJson.get("brightness").getAsFloat());
                    }
                    if (colorJson.has("alpha")) {
                        colorSetting.setAlpha(colorJson.get("alpha").getAsFloat());
                    }
                } else {
                    colorSetting.setColor(element.getAsInt());
                }
            } else if (setting instanceof MultiSelectSetting multiSetting) {
                if (element.isJsonArray()) {
                    JsonArray array = element.getAsJsonArray();
                    List<String> selected = new ArrayList<>();
                    for (JsonElement e : array) {
                        selected.add(e.getAsString());
                    }
                    multiSetting.setSelected(selected);
                }
            } else if (setting instanceof GroupSetting groupSetting) {
                if (element.isJsonObject()) {
                    JsonObject groupJson = element.getAsJsonObject();
                    if (groupJson.has("value")) {
                        groupSetting.setValue(groupJson.get("value").getAsBoolean());
                    }
                    if (groupJson.has("subSettings")) {
                        JsonObject subSettingsJson = groupJson.getAsJsonObject("subSettings");
                        for (Setting subSetting : groupSetting.getSubSettings()) {
                            if (subSettingsJson.has(subSetting.getName())) {
                                deserializeSetting(subSetting, subSettingsJson.get(subSetting.getName()));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private JsonObject serializeHud() {
        JsonObject hudJson = new JsonObject();
        HudManager hud = getHudManager();
        if (hud == null) return hudJson;

        for (HudElement element : hud.getElements()) {
            JsonObject elJson = new JsonObject();
            elJson.addProperty("x", element.getX());
            elJson.addProperty("y", element.getY());
            elJson.addProperty("width", element.getWidth());
            elJson.addProperty("height", element.getHeight());
            elJson.addProperty("enabled", element.isEnabled());
            hudJson.add(element.getName(), elJson);
        }
        return hudJson;
    }

    private void deserializeHud(JsonObject hudJson) {
        HudManager hud = getHudManager();
        if (hud == null) return;

        for (HudElement element : hud.getElements()) {
            String name = element.getName();
            if (hudJson.has(name)) {
                JsonObject elJson = hudJson.getAsJsonObject(name);
                if (elJson.has("x")) element.setX(elJson.get("x").getAsInt());
                if (elJson.has("y")) element.setY(elJson.get("y").getAsInt());
                if (elJson.has("width")) element.setWidth(elJson.get("width").getAsInt());
                if (elJson.has("height")) element.setHeight(elJson.get("height").getAsInt());
                if (elJson.has("enabled")) element.setEnabled(elJson.get("enabled").getAsBoolean());
            }
        }
    }

    private ModuleRepository getModuleRepository() {
        Initialization instance = Initialization.getInstance();
        if (instance != null && instance.getManager() != null) {
            return instance.getManager().getModuleRepository();
        }
        return null;
    }

    private HudManager getHudManager() {
        Initialization instance = Initialization.getInstance();
        if (instance != null && instance.getManager() != null) {
            return instance.getManager().getHudManager();
        }
        return null;
    }
}
