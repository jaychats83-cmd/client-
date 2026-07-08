package starry.util.string;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 *  © 2025 Copyright starry Client 2.0
 *        All Rights Reserved ®
 *
 *  Standalone utility to encrypt display strings so they never appear
 *  literally in source code or class files.
 *
 *  Usage:
 *    java starry.util.string.StringEncryptor "ModuleName" "Description" "SettingName"
 *
 *  The output is ready-to-paste StringHelper.decrypt() calls.
 *  The same XOR key in StringHelper is used for encryption here.
 */

public class StringEncryptor {
    private static final byte[] XOR_KEY = {0x4A, (byte)0xC3, 0x5F, (byte)0xE8, 0x12, (byte)0x9B, 0x37, (byte)0xD6};

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java StringEncryptor <plainText1> [plainText2 ...]");
            System.out.println("Example: java StringEncryptor \"KillAura\" \"Automatically attacks entities\"");
            return;
        }

        for (int i = 0; i < args.length; i++) {
            String input = args[i];
            String encrypted = encrypt(input);

            System.out.println("// \"" + escapeJavaString(input) + "\"");
            System.out.println("StringHelper.decrypt(\"" + encrypted + "\")");
            System.out.println();
        }
    }

    public static String encrypt(String plain) {
        byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = new byte[plainBytes.length];
        for (int i = 0; i < plainBytes.length; i++) {
            encrypted[i] = (byte) (plainBytes[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String escapeJavaString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
