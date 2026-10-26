package proxyapp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class WindowsStartup {

    public static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    private final String key;
    private final String name;

    public WindowsStartup() {
        this(RUN_KEY, "ProxyApp");
    }

    WindowsStartup(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public boolean isEnabled() throws IOException {
        return Registry.read(key, name) != null;
    }

    public String getCommand() throws IOException {
        return Registry.read(key, name);
    }

    public void enable(String command) throws IOException {
        Registry.addText(key, name, command);
    }

    public void disable() throws IOException {
        Registry.delete(key, name);
    }

    public static String commandFor(File jar, File javaHome, File dataDir) {
        File javaw = new File(new File(javaHome, "bin"), "javaw.exe");
        return "\"" + javaw.getPath() + "\" -jar \"" + jar.getPath() + "\" --home \"" + dataDir.getPath() + "\" --minimized";
    }

    public static String commandForExe(File exe, File dataDir) {
        return "\"" + exe.getPath() + "\" --home \"" + dataDir.getPath() + "\" --minimized";
    }

    public static String commandForThisApp() throws IOException {
        String exe = System.getProperty("jpackage.app-path");
        if (exe != null && !exe.isEmpty()) {
            return commandForExe(new File(exe), AppDir.get());
        }
        File jar;
        try {
            jar = new File(WindowsStartup.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Không xác định được vị trí chương trình");
        }
        if (!jar.isFile() || !jar.getName().toLowerCase().endsWith(".jar")) {
            throw new IOException("Chỉ đăng ký chạy cùng Windows được khi chạy từ file .jar (không phải từ NetBeans)");
        }
        return commandFor(jar, new File(System.getProperty("java.home")), AppDir.get());
    }
}
