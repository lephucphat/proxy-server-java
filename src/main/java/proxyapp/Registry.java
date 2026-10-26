package proxyapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Registry {

    public static String read(String key, String name) throws IOException {
        StringBuilder out = new StringBuilder();
        int code = run(out, null, "reg", "query", key, "/v", name);
        if (code != 0) {
            return null;
        }
        Pattern p = Pattern.compile("^\\s*" + Pattern.quote(name) + "\\s+REG_\\w+\\s*(.*)$", Pattern.CASE_INSENSITIVE);
        for (String line : out.toString().split("\\r?\\n")) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                return m.group(1).trim();
            }
        }
        return null;
    }

    public static void add(String key, String name, String type, String data) throws IOException {
        int code = run(new StringBuilder(), null, "reg", "add", key, "/v", name, "/t", type, "/d", data, "/f");
        if (code != 0) {
            throw new IOException("Không ghi được " + name + " vào registry");
        }
    }

    public static void addText(String key, String name, String text) throws IOException {
        String path = key.startsWith("HKCU\\") ? "Registry::HKEY_CURRENT_USER\\" + key.substring(5) : key;
        String script = "New-Item -Path $env:PA_KEY -Force | Out-Null; "
                + "Set-ItemProperty -Path $env:PA_KEY -Name $env:PA_NAME -Value $env:PA_VALUE";
        java.util.Map<String, String> env = new java.util.HashMap<String, String>();
        env.put("PA_KEY", path);
        env.put("PA_NAME", name);
        env.put("PA_VALUE", text);
        int code = run(new StringBuilder(), env, "powershell", "-NoProfile", "-NonInteractive", "-Command", script);
        if (code != 0) {
            throw new IOException("Không ghi được " + name + " vào registry");
        }
    }

    public static void delete(String key, String name) throws IOException {
        run(new StringBuilder(), null, "reg", "delete", key, "/v", name, "/f");
    }

    public static void deleteKey(String key) throws IOException {
        run(new StringBuilder(), null, "reg", "delete", key, "/f");
    }

    public static int run(StringBuilder out, Map<String, String> env, String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        if (env != null) {
            pb.environment().putAll(env);
        }
        Process proc = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        try {
            if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                proc.destroy();
                throw new IOException("Lệnh " + command[0] + " chạy quá lâu");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Bị ngắt khi chạy " + command[0]);
        }
        return proc.exitValue();
    }
}
