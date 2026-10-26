package proxyapp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class AlertMonitor {

    private static final int MAX_KEYS = 1000;
    private static final long STRANGER_PAUSE = 3600000L;

    private final AlertLog alerts;
    private final int threshold;
    private final long windowMillis;
    private final Map<String, LinkedList<Long>> blockedTimes = new HashMap<String, LinkedList<Long>>();
    private final Map<String, Long> lastBlockedAlert = new HashMap<String, Long>();
    private final Map<String, Long> lastStrangerAlert = new HashMap<String, Long>();

    public AlertMonitor(AlertLog alerts, int threshold, long windowMillis) {
        this.alerts = alerts;
        this.threshold = threshold;
        this.windowMillis = windowMillis;
    }

    long now() {
        return System.currentTimeMillis();
    }

    public synchronized void blocked(String client, String user) {
        if (threshold <= 0 || windowMillis <= 0) {
            return;
        }
        String who = user == null || user.isEmpty() ? client : user + " (" + client + ")";
        long t = now();
        if (blockedTimes.size() > MAX_KEYS) {
            blockedTimes.clear();
            lastBlockedAlert.clear();
        }
        LinkedList<Long> times = blockedTimes.get(who);
        if (times == null) {
            times = new LinkedList<Long>();
            blockedTimes.put(who, times);
        }
        times.addLast(t);
        while (!times.isEmpty() && t - times.getFirst() > windowMillis) {
            times.removeFirst();
        }
        Long last = lastBlockedAlert.get(who);
        if (times.size() >= threshold && (last == null || t - last > windowMillis)) {
            lastBlockedAlert.put(who, t);
            alerts.add(who + " bị chặn " + times.size() + " lần trong " + (windowMillis / 60000) + " phút (có thể đang thử vào nhiều trang bị cấm)");
            times.clear();
        }
    }

    public void locked(String client, int minutes) {
        alerts.add("Địa chỉ " + client + " bị khóa " + minutes + " phút vì nhập sai mật khẩu quá nhiều lần (có thể có người đang đoán mật khẩu)");
    }

    public synchronized void refusedClient(String client) {
        long t = now();
        if (lastStrangerAlert.size() > MAX_KEYS) {
            lastStrangerAlert.clear();
        }
        Long last = lastStrangerAlert.get(client);
        if (last == null || t - last > STRANGER_PAUSE) {
            lastStrangerAlert.put(client, t);
            alerts.add("Máy lạ " + client + " cố dùng proxy nhưng không nằm trong danh sách được phép");
        }
    }
}
