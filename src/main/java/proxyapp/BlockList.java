package proxyapp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class BlockList {

    public static final String DEFAULT_TEXT =
            "# Mỗi dòng một tên miền. Dòng bắt đầu bằng # là ghi chú.\n"
            + "# Tên miền bị chặn cùng mọi tên miền con của nó.\n"
            + "doubleclick.net\n"
            + "googlesyndication.com\n"
            + "googleadservices.com\n"
            + "adservice.google.com\n";

    public static final String ALLOW_DEFAULT_TEXT =
            "# Mỗi dòng một tên miền. Dòng bắt đầu bằng # là ghi chú.\n"
            + "# Chế độ chỉ cho phép: chỉ các tên miền này (và tên miền con của chúng) được truy cập.\n"
            + "# Dòng * trong lịch chặn theo giờ cũng bỏ qua các tên miền này.\n";

    public static final String BYPASS_DEFAULT_TEXT =
            "# Các trang này KHÔNG bị giải mã HTTPS (đi thẳng, proxy chỉ thấy tên miền).\n"
            + "# Mỗi dòng một tên miền, gồm cả tên miền con. Dòng bắt đầu bằng # là ghi chú.\n"
            + "# Nên thêm ngân hàng, ví điện tử, cổng dịch vụ công và các ứng dụng ghim chứng chỉ.\n"
            + "windowsupdate.com\n"
            + "update.microsoft.com\n";

    private final File file;
    private final String defaultText;
    private volatile Set<String> domains = new HashSet<String>();
    private volatile Set<String> publicDomains = new HashSet<String>();
    private volatile String text = "";

    public BlockList(File file) {
        this(file, DEFAULT_TEXT);
    }

    public BlockList(File file, String defaultText) {
        this.file = file;
        this.defaultText = defaultText;
    }

    public synchronized void load() {
        String t = defaultText;
        if (file != null && file.isFile()) {
            try {
                t = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                t = defaultText;
            }
        } else if (file != null) {
            try {
                Files.write(file.toPath(), defaultText.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
            }
        }
        apply(t);
    }

    public synchronized void setText(String newText) throws IOException {
        apply(newText);
        if (file != null) {
            Files.write(file.toPath(), newText.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String getText() {
        return text;
    }

    public int size() {
        return domains.size();
    }

    public void setPublicDomains(Set<String> set) {
        publicDomains = set;
    }

    public int publicSize() {
        return publicDomains.size();
    }

    public boolean matches(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        Set<String> d = domains;
        Set<String> p = publicDomains;
        if (d.isEmpty() && p.isEmpty()) {
            return false;
        }
        String h = host.trim().toLowerCase();
        while (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        while (true) {
            if (d.contains(h) || p.contains(h)) {
                return true;
            }
            int dot = h.indexOf('.');
            if (dot < 0) {
                return false;
            }
            h = h.substring(dot + 1);
        }
    }

    private void apply(String t) {
        Set<String> set = new HashSet<String>();
        for (String line : t.split("\\r?\\n")) {
            String rule = normalize(line);
            if (rule != null) {
                set.add(rule);
            }
        }
        this.text = t;
        this.domains = set;
    }

    static String normalize(String line) {
        String s = line.trim().toLowerCase();
        if (s.isEmpty() || s.startsWith("#")) {
            return null;
        }
        int hash = s.indexOf('#');
        if (hash > 0) {
            s = s.substring(0, hash).trim();
        }
        int scheme = s.indexOf("://");
        if (scheme >= 0) {
            s = s.substring(scheme + 3);
        }
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        int colon = s.lastIndexOf(':');
        if (colon >= 0 && s.indexOf(':') == colon) {
            s = s.substring(0, colon);
        }
        while (s.startsWith("*.") || s.startsWith(".")) {
            s = s.startsWith("*.") ? s.substring(2) : s.substring(1);
        }
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.isEmpty() ? null : s;
    }
}
