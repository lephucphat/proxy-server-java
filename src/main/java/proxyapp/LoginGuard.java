package proxyapp;

import java.util.HashMap;
import java.util.Map;

public class LoginGuard {

    private static class Record {
        int failures;
        long lastFailure;
        long lockedUntil;
    }

    private final int maxFailures;
    private final long lockMillis;
    private final Map<String, Record> records = new HashMap<String, Record>();

    public LoginGuard(int maxFailures, long lockMillis) {
        this.maxFailures = maxFailures;
        this.lockMillis = lockMillis;
    }

    long now() {
        return System.currentTimeMillis();
    }

    public synchronized boolean isLocked(String ip) {
        Record r = records.get(ip);
        if (r == null || r.lockedUntil == 0) {
            return false;
        }
        if (now() < r.lockedUntil) {
            return true;
        }
        records.remove(ip);
        return false;
    }

    public synchronized boolean failure(String ip) {
        if (maxFailures <= 0) {
            return false;
        }
        Record r = records.get(ip);
        if (r == null) {
            r = new Record();
            records.put(ip, r);
        }
        long t = now();
        if (r.failures > 0 && t - r.lastFailure > lockMillis) {
            r.failures = 0;
        }
        r.failures++;
        r.lastFailure = t;
        if (r.failures >= maxFailures) {
            r.lockedUntil = t + lockMillis;
            return true;
        }
        return false;
    }

    public synchronized void success(String ip) {
        records.remove(ip);
    }
}
