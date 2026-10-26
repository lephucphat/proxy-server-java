package proxyapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class SystemProxy {

    public static final String REAL_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings";

    private final String key;
    private final File backupFile;
    private final boolean notify;

    public SystemProxy() {
        this(REAL_KEY, AppDir.file("system-proxy-backup.properties"), true);
    }

    SystemProxy(String key, File backupFile, boolean notify) {
        this.key = key;
        this.backupFile = backupFile;
        this.notify = notify;
    }

    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    }

    public synchronized void enable(String host, int port) throws IOException {
        if (!backupFile.isFile()) {
            saveBackup();
        }
        Registry.add(key, "ProxyServer", "REG_SZ", host + ":" + port);
        Registry.add(key, "ProxyEnable", "REG_DWORD", "1");
        notifyChange();
    }

    public synchronized void restore() throws IOException {
        if (backupFile.isFile()) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(backupFile)) {
                p.load(in);
            }
            put("ProxyServer", "REG_SZ", p.getProperty("server"));
            put("ProxyEnable", "REG_DWORD", p.getProperty("enable"));
            backupFile.delete();
            notifyChange();
        } else if (isEnabledAny("127.0.0.1:")) {
            Registry.add(key, "ProxyEnable", "REG_DWORD", "0");
            notifyChange();
        }
    }

    public synchronized void restoreLeftover() throws IOException {
        if (backupFile.isFile()) {
            restore();
        }
    }

    public synchronized boolean isEnabled(int port) throws IOException {
        return isEnabledAny("127.0.0.1:" + port);
    }

    public synchronized boolean hasBackup() {
        return backupFile.isFile();
    }

    private boolean isEnabledAny(String serverStart) throws IOException {
        String enable = read("ProxyEnable");
        String server = read("ProxyServer");
        return enable != null && parseDword(enable) == 1 && server != null && server.startsWith(serverStart);
    }

    private void saveBackup() throws IOException {
        Properties p = new Properties();
        String enable = read("ProxyEnable");
        String server = read("ProxyServer");
        if (enable != null) {
            p.setProperty("enable", String.valueOf(parseDword(enable)));
        }
        if (server != null) {
            p.setProperty("server", server);
        }
        try (OutputStream out = new FileOutputStream(backupFile)) {
            p.store(out, "Cai dat proxy he thong truoc khi bat");
        }
    }

    private void put(String name, String type, String value) throws IOException {
        if (value == null) {
            Registry.delete(key, name);
        } else {
            Registry.add(key, name, type, value);
        }
    }

    private static int parseDword(String text) {
        String t = text.trim().toLowerCase();
        try {
            if (t.startsWith("0x")) {
                return Integer.parseInt(t.substring(2), 16);
            }
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    String read(String name) throws IOException {
        return Registry.read(key, name);
    }

    public void notifyChange() {
        if (!notify) {
            return;
        }
        String script = "Add-Type -MemberDefinition '[DllImport(\"wininet.dll\")] public static extern bool "
                + "InternetSetOption(IntPtr h, int o, IntPtr b, int l);' -Name W -Namespace ProxyApp; "
                + "[ProxyApp.W]::InternetSetOption([IntPtr]::Zero, 39, [IntPtr]::Zero, 0) | Out-Null; "
                + "[ProxyApp.W]::InternetSetOption([IntPtr]::Zero, 37, [IntPtr]::Zero, 0) | Out-Null";
        try {
            Registry.run(new StringBuilder(), null, "powershell", "-NoProfile", "-NonInteractive", "-Command", script);
        } catch (IOException e) {
        }
    }
}
