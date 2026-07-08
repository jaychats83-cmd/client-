package starry.util.subscription;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

public class HwidUtil {

    public static String generate() {
        try {
            StringBuilder sb = new StringBuilder();

            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder macSb = new StringBuilder();
                    for (byte b : mac) macSb.append(String.format("%02X", b));
                    sb.append(macSb);
                    break;
                }
            }

            sb.append(System.getProperty("user.name"));
            sb.append(System.getenv("COMPUTERNAME"));

            String raw = sb.toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "UNKNOWN-" + System.currentTimeMillis();
        }
    }
}
