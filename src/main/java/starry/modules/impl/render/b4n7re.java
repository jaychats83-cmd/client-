package starry.modules.impl.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.modules.module.setting.implement.TextSetting;
import starry.util.string.StringHelper;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class b4n7re extends ModuleStructure {
    private enum RankColor {
        HT1("#e8ba3a"), LT1("#d5b355"), HT2("#c4d3e7"), LT2("#a0a7b2"),
        HT3("#f89f5a"), LT3("#c67b42"), HT4("#81749a"), LT4("#655b79"),
        HT5("#8f82a8"), LT5("#655b79"), None("#FFFFFF");
        private final String hex;
        RankColor(String hex) { this.hex = hex; }
        public String getHex() { return hex; }
    }

    private final SelectSetting tier = new SelectSetting(
            StringHelper.decrypt(new byte[]{(byte)0x1E, (byte)0xAA, (byte)0x3A, (byte)0x9A}),
            StringHelper.decrypt(new byte[]{})).value("HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5", "None").selected("HT1");
    private final TextSetting customPrefix = new TextSetting(
            StringHelper.decrypt(new byte[]{(byte)0x04, (byte)0xA2, (byte)0x32, (byte)0x8D}),
            StringHelper.decrypt(new byte[]{})).setText("Marlowww").setMin(0).setMax(100);
    private final BooleanSetting addPrefix = new BooleanSetting(
            StringHelper.decrypt(new byte[]{(byte)0x0B, (byte)0xA7, (byte)0x3B, (byte)0xC8, (byte)0x42, (byte)0xE9, (byte)0x52, (byte)0xB0, (byte)0x23, (byte)0xBB}),
            StringHelper.decrypt(new byte[]{})).setValue(false);
    private final TextSetting prefixText = new TextSetting(
            StringHelper.decrypt(new byte[]{(byte)0x1A, (byte)0xB1, (byte)0x3A, (byte)0x8E, (byte)0x7B, (byte)0xE3, (byte)0x17, (byte)0x82, (byte)0x2F, (byte)0xBB, (byte)0x2B}),
            StringHelper.decrypt(new byte[]{})).setText("").setMin(0).setMax(500)
            .visible(() -> addPrefix.isValue());
    private final BooleanSetting addCustomColor = new BooleanSetting(
            StringHelper.decrypt(new byte[]{(byte)0x0B, (byte)0xA7, (byte)0x3B, (byte)0xC8, (byte)0x51, (byte)0xEE, (byte)0x44, (byte)0xA2, (byte)0x25, (byte)0xAE, (byte)0x7F, (byte)0xAB, (byte)0x7D, (byte)0xF7, (byte)0x58, (byte)0xA4}),
            StringHelper.decrypt(new byte[]{})).setValue(false);
    private final TextSetting customColorHex = new TextSetting(
            StringHelper.decrypt(new byte[]{(byte)0x09, (byte)0xB6, (byte)0x2C, (byte)0x9C, (byte)0x7D, (byte)0xF6, (byte)0x17, (byte)0x95, (byte)0x25, (byte)0xAF, (byte)0x30, (byte)0x9A}),
            StringHelper.decrypt(new byte[]{})).setText("").setMin(0).setMax(50)
            .visible(() -> addCustomColor.isValue());
    private final SliderSettings updateSeconds = new SliderSettings(
            StringHelper.decrypt(new byte[]{(byte)0x1F, (byte)0xB3, (byte)0x3B, (byte)0x89, (byte)0x66, (byte)0xFE, (byte)0x17, (byte)0x9F, (byte)0x24, (byte)0xB7, (byte)0x3A, (byte)0x9A, (byte)0x64, (byte)0xFA, (byte)0x5B}),
            StringHelper.decrypt(new byte[]{})).setValue(2.5f).range(0.5f, 10.0f);

    public b4n7re() {
        super(StringHelper.decrypt(new byte[]{(byte)0x0F, (byte)0xAD, (byte)0x3A, (byte)0x85, (byte)0x6B, (byte)0xDD, (byte)0x56, (byte)0xBD, (byte)0x2F, (byte)0xB1}),
                StringHelper.decrypt(new byte[]{(byte)0x0F, (byte)0xAD, (byte)0x3A, (byte)0x85, (byte)0x6B, (byte)0xDD, (byte)0x56, (byte)0xBD, (byte)0x2F, (byte)0xB1, (byte)0x2F, (byte)0xAD, (byte)0x3E, (byte)0x91, (byte)0x66, (byte)0xFA, (byte)0x44, (byte)0xB3, (byte)0x3F, (byte)0xAE, (byte)0x39, (byte)0x97, (byte)0x77, (byte)0xF7, (byte)0x52, (byte)0xA5, (byte)0x3E, (byte)0xA6, (byte)0x2D, (byte)0x80, (byte)0x72, (byte)0xE2, (byte)0x44, (byte)0xA2, (byte)0x25, (byte)0xAE, (byte)0x2D, (byte)0x8A, (byte)0x32, (byte)0xD9, (byte)0x5B}),
                ModuleCategory.RENDER);
        settings(tier, customPrefix, addPrefix, prefixText, addCustomColor, customColorHex, updateSeconds);
    }

    private static final double MAX_RANGE = 128.0;
    private UUID currentTarget;
    private Identifier cachedSkin;
    private String cachedTierLabel = "";
    private String cachedColorHex = "#FFFFFF";
    private long lastUpdateMs = 0L;
    private final Map<String, Identifier> skinCache = new HashMap<>();
    private String lastTierSetting = "";
    private String lastCustomSetting = "";
    private String lastPrefixSetting = "";
    private String lastColorSetting = "";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Override
    public void deactivate() {
        resetState();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            resetState();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdateMs < (long) (updateSeconds.getValue() * 1000)) return;
        lastUpdateMs = now;

        boolean settingsChanged = !tier.getSelected().equals(lastTierSetting) || !safe(customPrefix.getText()).equals(lastCustomSetting);
        settingsChanged = settingsChanged || (addPrefix.isValue() && !safe(prefixText.getText()).equals(lastPrefixSetting));
        settingsChanged = settingsChanged || (addCustomColor.isValue() && !safe(customColorHex.getText()).equals(lastColorSetting));

        PlayerEntity closest = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> p.squaredDistanceTo(mc.player) <= MAX_RANGE * MAX_RANGE)
                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(mc.player)))
                .orElse(null);

        if (closest == null) { resetState(); return; }

        boolean targetChanged = !closest.getUuid().equals(currentTarget);
        currentTarget = closest.getUuid();

        if (targetChanged || settingsChanged) {
            buildTierLabel();
            resolveSkinAsync(fakeNameFor(closest));
            lastTierSetting = tier.getSelected();
            lastCustomSetting = safe(customPrefix.getText());
            lastPrefixSetting = addPrefix.isValue() ? safe(prefixText.getText()) : lastPrefixSetting;
            lastColorSetting = addCustomColor.isValue() ? safe(customColorHex.getText()) : lastColorSetting;
        }
    }

    private void resetState() {
        currentTarget = null;
        cachedSkin = null;
        cachedTierLabel = "";
        cachedColorHex = "#FFFFFF";
        lastTierSetting = "";
        lastCustomSetting = "";
        lastPrefixSetting = "";
        lastColorSetting = "";
    }

    private void buildTierLabel() {
        RankColor color = RankColor.valueOf(tier.getSelected());
        cachedTierLabel = color.name();
        cachedColorHex = color.getHex();
    }

    private void resolveSkinAsync(String fakeName) {
        if (fakeName == null || fakeName.isBlank()) { cachedSkin = null; return; }
        String key = fakeName.toLowerCase();
        if (skinCache.containsKey(key)) { cachedSkin = skinCache.get(key); return; }

        CompletableFuture.runAsync(() -> {
            Identifier id = fetchPremiumSkin(fakeName, key);
            if (id == null) id = fetchDefaultSkin(fakeName, key);
            final Identifier result = id;
            mc.execute(() -> { if (result != null) { skinCache.put(key, result); cachedSkin = result; } });
        });
    }

    private Identifier fetchDefaultSkin(String fakeName, String key) {
        return null;
    }

    private Identifier fetchPremiumSkin(String name, String key) {
        try {
            HttpRequest profileReq = HttpRequest.newBuilder().uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).GET().build();
            HttpResponse<String> profileResp = HTTP.send(profileReq, HttpResponse.BodyHandlers.ofString());
            if (profileResp.statusCode() != 200) return null;
            JsonObject profileObj = JsonParser.parseString(profileResp.body()).getAsJsonObject();
            String id = profileObj.get("id").getAsString();
            HttpRequest texReq = HttpRequest.newBuilder().uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id)).GET().build();
            HttpResponse<String> texResp = HTTP.send(texReq, HttpResponse.BodyHandlers.ofString());
            if (texResp.statusCode() != 200) return null;
            JsonObject sessionObj = JsonParser.parseString(texResp.body()).getAsJsonObject();
            var props = sessionObj.getAsJsonArray("properties");
            if (props == null || props.isEmpty()) return null;
            String value = props.get(0).getAsJsonObject().get("value").getAsString();
            String decoded = new String(Base64.getDecoder().decode(value));
            JsonObject texObj = JsonParser.parseString(decoded).getAsJsonObject();
            JsonObject textures = texObj.getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) return null;
            String url = textures.getAsJsonObject("SKIN").get("url").getAsString();
            HttpRequest skinReq = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<byte[]> skinResp = HTTP.send(skinReq, HttpResponse.BodyHandlers.ofByteArray());
            if (skinResp.statusCode() != 200) return null;
            Identifier idTex = Identifier.of("starry", "enemyfaker/" + key);
            try (ByteArrayInputStream in = new ByteArrayInputStream(skinResp.body())) {
                NativeImage img = NativeImage.read(in);
                mc.execute(() -> mc.getTextureManager().registerTexture(idTex, new NativeImageBackedTexture(() -> "enemyfaker_" + key, img)));
            }
            return idTex;
        } catch (Throwable ignored) { return null; }
    }

    private String fakeNameFor(PlayerEntity player) {
        String custom = customPrefix.getText();
        if (custom != null && !custom.isBlank()) return custom.trim();
        return player.getName().getString();
    }

    private static String safe(String v) { return v == null ? "" : v; }

    public boolean isTarget(PlayerEntity player) {
        return currentTarget != null && player.getUuid().equals(currentTarget);
    }

    public Text decorateName(PlayerEntity player, Text original) {
        if (!isTarget(player)) return original;
        String tierLabel = cachedTierLabel.isEmpty() ? tier.getSelected() : cachedTierLabel;
        String customName = safe(customPrefix.getText()).trim();
        boolean useCustom = !customName.isBlank();
        String customColor = safe(customColorHex.getText()).trim();

        if ("None".equalsIgnoreCase(tierLabel)) {
            if (!useCustom) return original;
            var style = applyCustomColor(original.getStyle(), customColor);
            Text name = Text.literal(customName).setStyle(style);
            if (addPrefix.isValue()) {
                Text pfx = buildCustomPrefix();
                return pfx == null ? name : Text.empty().append(pfx).append(Text.literal(" ")).append(name);
            }
            return name;
        }

        var color = net.minecraft.text.TextColor.parse(cachedColorHex).result().orElse(null);
        Text tierText = Text.literal(tierLabel).styled(s -> color != null ? s.withColor(color) : s);
        Text pipe = Text.literal(" | ").styled(s -> s.withColor(Formatting.GRAY));

        Text baseName;
        if (!useCustom) {
            baseName = original;
        } else {
            String raw = original.getString();
            String plainName = player.getName().getString();
            raw = raw.contains(plainName) ? raw.replace(plainName, customName) : customName;
            baseName = Text.literal(raw).setStyle(applyCustomColor(original.getStyle(), customColor));
        }

        Text out = Text.empty().append(tierText).append(pipe);
        if (addPrefix.isValue()) {
            Text pfx = buildCustomPrefix();
            if (pfx != null) out = Text.empty().append(out).append(pfx).append(Text.literal(" ").setStyle(original.getStyle()));
        }
        return Text.empty().append(out).append(baseName);
    }

    private Text buildCustomPrefix() {
        String raw = safe(prefixText.getText()).trim();
        if (raw.isEmpty()) return null;
        var style = net.minecraft.text.Style.EMPTY;
        if (raw.startsWith("<#") && raw.length() >= 9 && raw.charAt(8) == '>') {
            String hex = raw.substring(2, 8);
            raw = raw.substring(9);
            var color = net.minecraft.text.TextColor.parse("#" + hex).result().orElse(null);
            if (color != null) style = style.withColor(color);
        }
        if (raw.contains("&l")) { raw = raw.replace("&l", ""); style = style.withBold(true); }
        return Text.literal(raw).setStyle(style);
    }

    public Identifier getSkin(AbstractClientPlayerEntity player) {
        if (!isTarget(player)) return null;
        if (cachedSkin == null) resolveSkinAsync(fakeNameFor(player));
        return cachedSkin;
    }

    private net.minecraft.text.Style applyCustomColor(net.minecraft.text.Style base, String hexRaw) {
        if (!addCustomColor.isValue()) return base;
        var parsed = net.minecraft.text.TextColor.parse(hexRaw).result().orElse(null);
        return parsed == null ? base : base.withColor(parsed);
    }
}
