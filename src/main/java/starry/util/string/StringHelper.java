package starry.util.string;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *  © 2025 Copyright starry Client 2.0
 *        All Rights Reserved ®
 */

@UtilityClass
public class StringHelper {
    private static final byte[] XOR_KEY = {0x4A, (byte)0xC3, 0x5F, (byte)0xE8, 0x12, (byte)0x9B, 0x37, (byte)0xD6};

    public String randomString(int length) {
        return IntStream.range(0, length)
                .mapToObj(operand -> String.valueOf((char) new Random().nextInt('a', 'z' + 1)))
                .collect(Collectors.joining());
    }

    public String getBindName(int key) {
        if (key < 0) return "N/A";
        return PlayerInteractionHelper.getKeyType(key).createFromCode(key).getTranslationKey().replace("key.keyboard.", "")
                .replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public String getUserRole() {
        return switch ("DEVELOPER") {
            case "Developer" -> "Developer";
            case "Admin" -> "Admin";
            default -> "User";
        };
    }

    public String getDuration(int time) {
        int mins = time / 60;
        String sec = String.format("%02d", time % 60);
        return mins + ":" + sec;
    }

    /**
     * Decrypt a byte array by XOR'ing with the key.
     * Supports the legacy byte-array format.
     */
    public static String decrypt(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte)(data[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * Decrypt a Base64-encoded, XOR'd string.
     * Compact format: the plaintext is XOR'd then Base64-encoded.
     * At runtime we Base64-decode then XOR back to plain text.
     */
    public static String decrypt(String base64Data) {
        byte[] data = Base64.getDecoder().decode(base64Data);
        return decrypt(data);
    }
}