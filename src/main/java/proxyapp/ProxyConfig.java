package proxyapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ProxyConfig {

    public int port = 8080;
    public boolean allowLan = false;
    public boolean requireAuth = false;
    public String username = "admin";
    public String passwordHash = "";
    public int maxKBps = 0;
    public String blocklistFile = "blocklist.txt";
    public boolean logToFile = false;
    public String logFile = "proxy-log.csv";
    public String allowedClients = "";
    public boolean blockInternal = true;
    public int maxAuthFailures = 5;
    public int lockMinutes = 5;
    public String publicListFile = "blocklist-public.txt";
    public String publicListUrls = "";
    public int publicListHours = 24;
    public boolean closeToTray = true;
    public boolean autoStartProxy = false;
    public boolean allowOnlyMode = false;
    public String allowlistFile = "allowlist.txt";
    public String scheduleFile = "schedule.txt";
    public String usersFile = "users.txt";
    public int alertBlocked = 20;
    public int alertMinutes = 5;
    public String caFile = "proxy-ca.p12";
    public String caCertFile = "proxy-ca.crt";
    public boolean mitmEnabled = false;
    public String mitmBypassFile = "mitm-bypass.txt";
    public String urlRulesFile = "url-rules.txt";

    public static ProxyConfig load(File file) {
        ProxyConfig c = new ProxyConfig();
        if (file == null || !file.isFile()) {
            return c;
        }
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            return c;
        }
        c.port = intOf(p.getProperty("port"), c.port);
        c.allowLan = Boolean.parseBoolean(p.getProperty("allowLan", "false"));
        c.requireAuth = Boolean.parseBoolean(p.getProperty("requireAuth", "false"));
        c.username = p.getProperty("username", c.username);
        c.passwordHash = p.getProperty("passwordHash", "");
        String oldPassword = p.getProperty("password", "");
        if (c.passwordHash.isEmpty() && !oldPassword.isEmpty()) {
            c.setPassword(oldPassword);
        }
        c.maxKBps = intOf(p.getProperty("maxKBps"), c.maxKBps);
        c.blocklistFile = p.getProperty("blocklistFile", c.blocklistFile);
        c.logToFile = Boolean.parseBoolean(p.getProperty("logToFile", "false"));
        c.logFile = p.getProperty("logFile", c.logFile);
        c.allowedClients = p.getProperty("allowedClients", "");
        c.blockInternal = Boolean.parseBoolean(p.getProperty("blockInternal", "true"));
        c.maxAuthFailures = intOf(p.getProperty("maxAuthFailures"), c.maxAuthFailures);
        c.lockMinutes = intOf(p.getProperty("lockMinutes"), c.lockMinutes);
        c.publicListFile = p.getProperty("publicListFile", c.publicListFile);
        c.publicListUrls = p.getProperty("publicListUrls", "");
        c.publicListHours = intOf(p.getProperty("publicListHours"), c.publicListHours);
        c.closeToTray = Boolean.parseBoolean(p.getProperty("closeToTray", "true"));
        c.autoStartProxy = Boolean.parseBoolean(p.getProperty("autoStartProxy", "false"));
        c.allowOnlyMode = Boolean.parseBoolean(p.getProperty("allowOnlyMode", "false"));
        c.allowlistFile = p.getProperty("allowlistFile", c.allowlistFile);
        c.scheduleFile = p.getProperty("scheduleFile", c.scheduleFile);
        c.usersFile = p.getProperty("usersFile", c.usersFile);
        c.alertBlocked = intOf(p.getProperty("alertBlocked"), c.alertBlocked);
        c.alertMinutes = intOf(p.getProperty("alertMinutes"), c.alertMinutes);
        c.caFile = p.getProperty("caFile", c.caFile);
        c.caCertFile = p.getProperty("caCertFile", c.caCertFile);
        c.mitmEnabled = Boolean.parseBoolean(p.getProperty("mitmEnabled", "false"));
        c.mitmBypassFile = p.getProperty("mitmBypassFile", c.mitmBypassFile);
        c.urlRulesFile = p.getProperty("urlRulesFile", c.urlRulesFile);
        return c;
    }

    public void save(File file) throws IOException {
        Properties p = new Properties();
        p.setProperty("port", String.valueOf(port));
        p.setProperty("allowLan", String.valueOf(allowLan));
        p.setProperty("requireAuth", String.valueOf(requireAuth));
        p.setProperty("username", username);
        p.setProperty("passwordHash", passwordHash);
        p.setProperty("maxKBps", String.valueOf(maxKBps));
        p.setProperty("blocklistFile", blocklistFile);
        p.setProperty("logToFile", String.valueOf(logToFile));
        p.setProperty("logFile", logFile);
        p.setProperty("allowedClients", allowedClients);
        p.setProperty("blockInternal", String.valueOf(blockInternal));
        p.setProperty("maxAuthFailures", String.valueOf(maxAuthFailures));
        p.setProperty("lockMinutes", String.valueOf(lockMinutes));
        p.setProperty("publicListFile", publicListFile);
        p.setProperty("publicListUrls", publicListUrls);
        p.setProperty("publicListHours", String.valueOf(publicListHours));
        p.setProperty("closeToTray", String.valueOf(closeToTray));
        p.setProperty("autoStartProxy", String.valueOf(autoStartProxy));
        p.setProperty("allowOnlyMode", String.valueOf(allowOnlyMode));
        p.setProperty("allowlistFile", allowlistFile);
        p.setProperty("scheduleFile", scheduleFile);
        p.setProperty("usersFile", usersFile);
        p.setProperty("alertBlocked", String.valueOf(alertBlocked));
        p.setProperty("alertMinutes", String.valueOf(alertMinutes));
        p.setProperty("caFile", caFile);
        p.setProperty("caCertFile", caCertFile);
        p.setProperty("mitmEnabled", String.valueOf(mitmEnabled));
        p.setProperty("mitmBypassFile", mitmBypassFile);
        p.setProperty("urlRulesFile", urlRulesFile);
        try (OutputStream out = new FileOutputStream(file)) {
            p.store(out, "Proxy configuration");
        }
    }

    public void setPassword(String plain) {
        passwordHash = plain == null || plain.isEmpty() ? "" : PasswordHasher.hash(plain);
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    public boolean checkPassword(String plain) {
        return hasPassword() && PasswordHasher.matches(plain, passwordHash);
    }

    public void validate() {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Cổng phải nằm trong khoảng 1-65535");
        }
        if (maxKBps < 0) {
            throw new IllegalArgumentException("Giới hạn tốc độ không được âm");
        }
        if (requireAuth && (username == null || username.trim().isEmpty())) {
            throw new IllegalArgumentException("Cần nhập tên đăng nhập khi bật xác thực");
        }
        if (requireAuth && !hasPassword()) {
            throw new IllegalArgumentException("Cần đặt mật khẩu khi bật xác thực");
        }
        if (allowLan && !requireAuth) {
            throw new IllegalArgumentException("Bật cho phép LAN thì phải bật yêu cầu đăng nhập");
        }
        if (maxAuthFailures < 0 || lockMinutes < 0) {
            throw new IllegalArgumentException("Số lần sai và thời gian khóa không được âm");
        }
        if (alertBlocked < 0 || alertMinutes < 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo không được âm");
        }
        if (publicListHours < 0) {
            throw new IllegalArgumentException("Số giờ tự cập nhật không được âm");
        }
        ClientAccess.parse(allowedClients);
    }

    private static int intOf(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
