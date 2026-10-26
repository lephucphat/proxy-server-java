package proxyapp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockSchedule {

    public static final String DEFAULT_TEXT =
            "# Mỗi dòng: tên miền, ngày, khung giờ (cách nhau bằng dấu cách hoặc Tab)\n"
            + "# Ngày: * (mọi ngày), T2-T6, T2,T4,T7, CN\n"
            + "# Giờ: 07:00-17:00 (có thể qua nửa đêm như 22:00-06:00; 24:00 là hết ngày)\n"
            + "# Tên miền * = chặn mọi trang trong khung giờ đó, trừ các trang trong danh sách cho phép\n"
            + "# Ví dụ:\n"
            + "# facebook.com T2-T6 07:00-17:00\n"
            + "# * * 23:00-05:00\n";

    private static final String[] DAY_NAMES = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})");

    public static class Rule {
        public final String domain;
        public final String daysText;
        public final String timeText;
        private final boolean[] days;
        private final int start;
        private final int end;

        Rule(String domain, String daysText, String timeText, boolean[] days, int start, int end) {
            this.domain = domain;
            this.daysText = daysText;
            this.timeText = timeText;
            this.days = days;
            this.start = start;
            this.end = end;
        }

        public boolean allHosts() {
            return domain.equals("*");
        }

        public boolean matchesHost(String host) {
            if (allHosts()) {
                return true;
            }
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

        public boolean isActive(LocalDateTime now) {
            int day = now.getDayOfWeek().getValue() - 1;
            int minute = now.getHour() * 60 + now.getMinute();
            if (start < end) {
                return days[day] && minute >= start && minute < end;
            }
            if (days[day] && minute >= start) {
                return true;
            }
            return days[(day + 6) % 7] && minute < end;
        }

        public String describe() {
            return daysText + " " + timeText;
        }
    }

    private final File file;
    private volatile List<Rule> rules = new ArrayList<Rule>();
    private volatile String text = "";

    public BlockSchedule(File file) {
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

    public Rule blockingRule(String host, LocalDateTime now, BlockList allowList) {
        if (host == null || host.isEmpty()) {
            return null;
        }
        String h = host.trim().toLowerCase();
        for (Rule r : rules) {
            if (r.matchesHost(h) && r.isActive(now)) {
                if (r.allHosts() && allowList != null && allowList.matches(h)) {
                    continue;
                }
                return r;
            }
        }
        return null;
    }

    static List<Rule> parse(String text, boolean strict) {
        List<Rule> list = new ArrayList<Rule>();
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.isEmpty()) {
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
        String[] f = line.split("\\s+");
        if (f.length != 3) {
            throw new IllegalArgumentException("cần đúng 3 phần: tên miền, ngày, giờ");
        }
        String domain = f[0].equals("*") ? "*" : BlockList.normalize(f[0]);
        if (domain == null) {
            throw new IllegalArgumentException("tên miền không hợp lệ");
        }
        boolean[] days = parseDays(f[1]);
        Matcher m = TIME.matcher(f[2]);
        if (!m.matches()) {
            throw new IllegalArgumentException("giờ phải có dạng 07:00-17:00");
        }
        int start = minutes(m.group(1), m.group(2), false);
        int end = minutes(m.group(3), m.group(4), true);
        if (start == end) {
            throw new IllegalArgumentException("giờ bắt đầu và kết thúc không được giống nhau");
        }
        return new Rule(domain, f[1].toUpperCase(), f[2], days, start, end);
    }

    private static int minutes(String hh, String mm, boolean allow24) {
        int h = Integer.parseInt(hh);
        int m = Integer.parseInt(mm);
        if (m > 59 || h > 24 || (h == 24 && (m != 0 || !allow24))) {
            throw new IllegalArgumentException("giờ không hợp lệ: " + hh + ":" + mm);
        }
        return h * 60 + m;
    }

    private static boolean[] parseDays(String text) {
        boolean[] days = new boolean[7];
        String t = text.toUpperCase();
        if (t.equals("*")) {
            java.util.Arrays.fill(days, true);
            return days;
        }
        for (String part : t.split(",", -1)) {
            int dash = part.indexOf('-');
            if (dash < 0) {
                days[dayIndex(part)] = true;
            } else {
                int from = dayIndex(part.substring(0, dash));
                int to = dayIndex(part.substring(dash + 1));
                int d = from;
                days[d] = true;
                while (d != to) {
                    d = (d + 1) % 7;
                    days[d] = true;
                }
            }
        }
        return days;
    }

    private static int dayIndex(String name) {
        for (int i = 0; i < DAY_NAMES.length; i++) {
            if (DAY_NAMES[i].equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("ngày không hợp lệ: " + name + " (dùng T2..T7, CN hoặc *)");
    }
}
