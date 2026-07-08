package starry.util.subscription;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SubscriptionManager {

    private static SubscriptionManager instance;

    private final String githubOwner;
    private final String githubRepo;
    private final String githubToken;
    private final String licenseKey;
    private final String hwid;

    private LicenseData.Entry matchedEntry;
    private boolean verified = false;
    private int licenseIndex = -1;
    private int totalLicenses = 0;

    private SubscriptionManager(String githubOwner, String githubRepo, String githubToken, String licenseKey) {
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
        this.githubToken = githubToken;
        this.licenseKey = licenseKey;
        this.hwid = HwidUtil.generate();
    }

    public static synchronized void init(String githubOwner, String githubRepo, String githubToken, String licenseKey) {
        instance = new SubscriptionManager(githubOwner, githubRepo, githubToken, licenseKey);
    }

    public static SubscriptionManager getInstance() {
        return instance;
    }

    public String getHwid() {
        return hwid;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public boolean isVerified() {
        return verified;
    }

    public LicenseData.Entry getMatchedEntry() {
        return matchedEntry;
    }

    public int getLicenseIndex() {
        return licenseIndex;
    }

    public int getTotalLicenses() {
        return totalLicenses;
    }

    public void verifyAndBind() {
        new Thread(() -> {
            try {
                LicenseData data = fetchLicenses();
                if (data == null) {
                    halt("Failed to fetch license data.");
                    return;
                }

                LicenseData.Entry entry = null;
                totalLicenses = data.getLicenses().size();
                for (int i = 0; i < totalLicenses; i++) {
                    LicenseData.Entry e = data.getLicenses().get(i);
                    if (e.key.equals(licenseKey)) {
                        entry = e;
                        licenseIndex = i;
                        break;
                    }
                }

                if (entry == null) {
                    halt("License key not found. Access denied.");
                    return;
                }

                if (entry.revoked) {
                    halt("License has been revoked. Access denied.");
                    return;
                }

                long expiry = parseDate(entry.expiry);
                if (expiry > 0 && System.currentTimeMillis() > expiry) {
                    halt("License has expired. Access denied.");
                    return;
                }

                if (entry.hwid == null || entry.hwid.isEmpty()) {
                    bindHwid(data);
                    return;
                }

                if (!entry.hwid.equalsIgnoreCase(hwid)) {
                    halt("HWID mismatch. Access denied.");
                    return;
                }

                verified = true;
                matchedEntry = entry;
            } catch (Exception e) {
                halt("Verification error: " + e.getMessage());
            }
        }, "Subscription-Verify").start();
    }

    private void bindHwid(LicenseData data) {
        try {
            FileInfo fileInfo = getFileInfo();
            if (fileInfo == null) {
                halt("Failed to get license file info for HWID binding.");
                return;
            }

            for (LicenseData.Entry e : data.getLicenses()) {
                if (e.key.equals(licenseKey)) {
                    e.hwid = hwid;
                    break;
                }
            }

            String updatedJson = data.serialize();
            boolean success = updateFile(fileInfo.sha, updatedJson);

            if (success) {
                verified = true;
                for (LicenseData.Entry e : data.getLicenses()) {
                    if (e.key.equals(licenseKey)) {
                        matchedEntry = e;
                        break;
                    }
                }
            } else {
                halt("Failed to bind HWID. Contact support.");
            }
        } catch (Exception e) {
            halt("HWID binding error: " + e.getMessage());
        }
    }

    private LicenseData fetchLicenses() {
        try {
            String url = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo + "/contents/licenses.json";
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                String body = readBody(conn);
                JsonContent jsonContent = parseContentResponse(body);
                if (jsonContent != null) {
                    String decoded = new String(Base64.getDecoder().decode(jsonContent.content), StandardCharsets.UTF_8);
                    return LicenseData.parse(decoded);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private FileInfo getFileInfo() {
        try {
            String url = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo + "/contents/licenses.json";
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                String body = readBody(conn);
                JsonContent jsonContent = parseContentResponse(body);
                if (jsonContent != null) {
                    return new FileInfo(jsonContent.sha);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean updateFile(String sha, String newContent) {
        try {
            String url = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo + "/contents/licenses.json";
            String encoded = Base64.getEncoder().encodeToString(newContent.getBytes(StandardCharsets.UTF_8));
            String jsonPayload = "{\"message\":\"HWID auto-bind\",\"content\":\"" + encoded + "\",\"sha\":\"" + sha + "\"}";

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            return code == 200 || code == 201;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void halt(String reason) {
        try {
            System.err.println("[Subscription] " + reason);
        } catch (Exception ignored) {}
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}
        Runtime.getRuntime().halt(1);
    }

    private static long parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return -1;
        try {
            return Instant.parse(dateStr).toEpochMilli();
        } catch (Exception e) {
            return -1;
        }
    }

    private static String readBody(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonContent parseContentResponse(String json) {
        try {
            com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(json, com.google.gson.JsonObject.class);
            String content = obj.has("content") ? obj.get("content").getAsString() : null;
            String sha = obj.has("sha") ? obj.get("sha").getAsString() : null;
            if (content != null && sha != null) {
                return new JsonContent(content.replaceAll("\\s", ""), sha);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private record FileInfo(String sha) {}
    private record JsonContent(String content, String sha) {}
}
