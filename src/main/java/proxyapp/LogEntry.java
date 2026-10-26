package proxyapp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogEntry {

    public final long time;
    public final String client;
    public final String user;
    public final String method;
    public final String host;
    public final String target;
    public final int status;
    public final long bytes;
    public final long durationMs;
    public final boolean blocked;

    public LogEntry(long time, String client, String user, String method, String host, String target,
            int status, long bytes, long durationMs, boolean blocked) {
        this.time = time;
        this.client = client;
        this.user = user == null ? "" : user;
        this.method = method;
        this.host = host;
        this.target = target;
        this.status = status;
        this.bytes = bytes;
        this.durationMs = durationMs;
        this.blocked = blocked;
    }

    public String timeText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
    }

    public String toCsv() {
        return csv(timeText()) + "," + csv(client) + "," + csv(user) + "," + csv(method) + "," + csv(host) + ","
                + csv(target) + "," + status + "," + bytes + "," + durationMs + "," + blocked;
    }

    public static String csvHeader() {
        return "time,client,user,method,host,target,status,bytes,duration_ms,blocked";
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
