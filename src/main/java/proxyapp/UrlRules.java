package proxyapp;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UrlRules {

    public static final String DEFAULT_TEXT =
            "# Mỗi dòng: tên miền, loại, giá trị (cách nhau bằng dấu cách hoặc Tab)\n"
            + "# Loại path: chặn khi đường dẫn bắt đầu bằng giá trị (phải bắt đầu bằng /)\n"
            + "# Loại keyword: chặn khi tên miền + đường dẫn có chứa từ khóa\n"
            + "# Tên miền * = áp dụng cho mọi trang. Không phân biệt hoa thường.\n"
            + "# Với HTTPS chỉ thấy đường dẫn khi bật giải mã; không giải mã thì chỉ so từ khóa với tên miền.\n"
            + "# Ví dụ:\n"
            + "# youtube.com path /shorts\n"
            + "# * keyword casino\n";

    public static class Rule {
        public final String domain;
        public final String kind;
        public final String value;

        Rule(String domain, String kind, String value) {
            this.domain = domain;
            this.kind = kind;
            this.value = value;
        }

        public String describe() {
            return domain + " " + kind + " " + value;
        }

        boolean matches(String host, String pathAndQuery, boolean tunnel) {
            if (!domain.equals("*") && !hostMatches(host)) {
                return false;
            }
            if (kind.equals("path")) {
                return !tunnel && startsWith(pathAndQuery);
            }
            return contains(tunnel ? host : host + pathAndQuery);
        }

        private boolean hostMatches(String host) {
            String h = host;
            while (true) {
                if (h.equals(domain)) {
                    return true;
                }
                int dot = h.indexOf('.');
                if (dot < 0) {
                    return false;
                }
                h = h.substring(dot + 1);
            }
        }

        private boolean startsWith(String text) {
            String raw = text.toLowerCase();
            if (raw.startsWith(value)) {
                return true;
            }
            String decoded = decode(raw);
            return decoded != null && decoded.startsWith(value);
        }

        private boolean contains(String text) {
            String raw = text.toLowerCase();
            if (raw.contains(value)) {
                return true;
            }
            String decoded = decode(raw);
            return decoded != null && decoded.contains(value);
        }
    }

    private final File file;
    private volatile List<Rule> rules = new ArrayList<Rule>();
    private volatile String text = "";

    public UrlRules(File file) {
        this.file = file;
    }

    public synchronized void load() {
        String t = DEFAULT_TEXT;
        if (file != null && file.isFile()) {
            try {
                t = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                t = DEFAULT_TEXT;
            }
        } else if (file != null) {
            try {
                Files.write(file.toPath(), DEFAULT_TEXT.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
            }
        }
        this.rules = parse(t, false);
        this.text = t;
    }

    public synchronized void setText(String newText) throws IOException {
        List<Rule> parsed = parse(newText, true);
        if (file != null) {
            Files.write(file.toPath(), newText.getBytes(StandardCharsets.UTF_8));
        }
        this.rules = parsed;
        this.text = newText;
    }

    public String getText() {
        return text;
    }

    public int size() {
        return rules.size();
    }

    public Rule blockingRule(String host, String pathAndQuery, boolean tunnel) {
        if (host == null) {
            return null;
        }
        String h = host.trim().toLowerCase();
        String p = pathAndQuery == null ? "" : pathAndQuery;
        for (Rule r : rules) {
            if (r.matches(h, p, tunnel)) {
                return r;
            }
        }
        return null;
    }

    static List<Rule> parse(String text, boolean strict) {
        List<Rule> list = new ArrayList<Rule>();
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                list.add(parseLine(line));
            } catch (IllegalArgumentException e) {
                if (strict) {
                    throw new IllegalArgumentException("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return list;
    }

    private static Rule parseLine(String line) {
        String[] f = line.split("\\s+", 3);
        if (f.length != 3) {
            throw new IllegalArgumentException("cần 3 phần: tên miền, loại (path hoặc keyword), giá trị");
        }
        String domain = f[0].equals("*") ? "*" : BlockList.normalize(f[0]);
        if (domain == null) {
            throw new IllegalArgumentException("tên miền không hợp lệ");
        }
        String kind = f[1].toLowerCase();
        if (kind.equals("duongdan")) {
            kind = "path";
        } else if (kind.equals("tukhoa")) {
            kind = "keyword";
        }
        if (!kind.equals("path") && !kind.equals("keyword")) {
            throw new IllegalArgumentException("loại phải là path hoặc keyword");
        }
        String value = f[2].trim().toLowerCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("thiếu giá trị");
        }
        if (kind.equals("path") && !value.startsWith("/")) {
            throw new IllegalArgumentException("đường dẫn phải bắt đầu bằng /");
        }
        return new Rule(domain, kind, value);
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8").toLowerCase();
        } catch (IllegalArgumentException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
