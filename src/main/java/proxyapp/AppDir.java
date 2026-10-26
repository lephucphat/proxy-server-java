package proxyapp;

import java.io.File;

public class AppDir {

    private static volatile File base = defaultBase(System.getProperty("jpackage.app-path"),
            System.getenv("APPDATA"), System.getProperty("user.home"));

    static File defaultBase(String appPath, String appData, String userHome) {
        if (appPath == null || appPath.isEmpty()) {
            return new File(".").getAbsoluteFile();
        }
        File root = appData != null && !appData.isEmpty() ? new File(appData) : new File(userHome == null ? "." : userHome);
        return new File(root, "ProxyServer").getAbsoluteFile();
    }

    private static File ensured() {
        File dir = base;
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void set(File dir) {
        base = dir.getAbsoluteFile();
    }

    public static File get() {
        return ensured();
    }

    public static File file(String name) {
        File f = new File(name);
        return f.isAbsolute() ? f : new File(ensured(), name);
    }
}
