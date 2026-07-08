package starry;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import starry.manager.Manager;
import starry.util.subscription.SubscriptionManager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class Initialization implements ClientModInitializer {

    private static final String GITHUB_OWNER = "p1aegg";
    private static final String GITHUB_REPO  = "client";
    private static final String GITHUB_TOKEN = System.getenv("QCLOUD_GITHUB_TOKEN");

    private static final byte[] XOR_KEY = "JjJKLl$".getBytes(StandardCharsets.UTF_8);
    private static final String HMAC_SALT = "Mx78*^.=";

    @Getter
    private static Initialization instance;

    @Getter
    private Manager manager;

    @Override
    public void onInitializeClient() {

    }

    public void init() {
        instance = this;

        manager = new Manager();
        manager.init();
    }

    private static String loadLicenseKey() {
        try (InputStream is = Initialization.class.getResourceAsStream("/license.key")) {
            if (is != null) {
                byte[] encrypted = is.readAllBytes();
                byte[] decrypted = xor(encrypted);
                String plain = new String(decrypted, StandardCharsets.UTF_8).trim();
                int sep = plain.lastIndexOf(':');
                if (sep > 0) {
                    String key = plain.substring(0, sep);
                    String expectedHmac = plain.substring(sep + 1);
                    if (sha256(key + HMAC_SALT).equals(expectedHmac)) {
                        return key;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "PLACEHOLDER_KEY";
    }

    static byte[] xor(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return result;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
