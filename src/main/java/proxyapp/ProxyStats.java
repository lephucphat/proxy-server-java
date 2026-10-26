package proxyapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProxyStats {

    public static class Row {
        public final String name;
        public final long requests;
        public final long blocked;
        public final long authFailures;
        public final long down;
        public final long up;

        Row(String name, Counter c) {
            this.name = name;
            this.requests = c.requests.get();
            this.blocked = c.blocked.get();
            this.authFailures = c.authFailures.get();
            this.down = c.down.get();
            this.up = c.up.get();
        }
    }

    private static class Counter {
        final AtomicLong requests = new AtomicLong();
        final AtomicLong blocked = new AtomicLong();
        final AtomicLong authFailures = new AtomicLong();
        final AtomicLong down = new AtomicLong();
        final AtomicLong up = new AtomicLong();
    }

    private static final int MAX_KEYS = 5000;

    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong blocked = new AtomicLong();
    private final AtomicLong authFailures = new AtomicLong();
    private final AtomicLong bytesFromClients = new AtomicLong();
    private final AtomicLong bytesToClients = new AtomicLong();
    private final AtomicInteger activeConnections = new AtomicInteger();
    private final ConcurrentHashMap<String, AtomicLong> hosts = new ConcurrentHashMap<String, AtomicLong>();
    private final ConcurrentHashMap<String, Counter> clients = new ConcurrentHashMap<String, Counter>();
    private final ConcurrentHashMap<String, Counter> users = new ConcurrentHashMap<String, Counter>();
    private volatile long startedAt = System.currentTimeMillis();

    public void request(String host) {
        requests.incrementAndGet();
        if (host != null && !host.isEmpty()) {
            AtomicLong c = hosts.get(host);
            if (c == null) {
                AtomicLong fresh = new AtomicLong();
                c = hosts.putIfAbsent(host, fresh);
                if (c == null) {
                    c = fresh;
                }
            }
            c.incrementAndGet();
        }
    }

    private static Counter counter(Map<String, Counter> map, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Counter c = map.get(key);
        if (c == null) {
            if (map.size() >= MAX_KEYS) {
                return null;
            }
            Counter fresh = new Counter();
            c = ((ConcurrentHashMap<String, Counter>) map).putIfAbsent(key, fresh);
            if (c == null) {
                c = fresh;
            }
        }
        return c;
    }

    public void requestBy(String client, String user) {
        Counter c = counter(clients, client);
        if (c != null) {
            c.requests.incrementAndGet();
        }
        Counter u = counter(users, user);
        if (u != null) {
            u.requests.incrementAndGet();
        }
    }

    public void blockedBy(String client, String user) {
        Counter c = counter(clients, client);
        if (c != null) {
            c.blocked.incrementAndGet();
        }
        Counter u = counter(users, user);
        if (u != null) {
            u.blocked.incrementAndGet();
        }
    }

    public void authFailureBy(String client, String user) {
        Counter c = counter(clients, client);
        if (c != null) {
            c.authFailures.incrementAndGet();
        }
        Counter u = counter(users, user);
        if (u != null) {
            u.authFailures.incrementAndGet();
        }
    }

    public void bytesBy(String client, String user, long downBytes, long upBytes) {
        Counter c = counter(clients, client);
        if (c != null) {
            c.down.addAndGet(downBytes);
            c.up.addAndGet(upBytes);
        }
        Counter u = counter(users, user);
        if (u != null) {
            u.down.addAndGet(downBytes);
            u.up.addAndGet(upBytes);
        }
    }

    public void blocked() {
        blocked.incrementAndGet();
    }

    public void authFailure() {
        authFailures.incrementAndGet();
    }

    public void fromClient(long n) {
        bytesFromClients.addAndGet(n);
    }

    public void toClient(long n) {
        bytesToClients.addAndGet(n);
    }

    public void connectionOpened() {
        activeConnections.incrementAndGet();
    }

    public void connectionClosed() {
        activeConnections.decrementAndGet();
    }

    public long getRequests() {
        return requests.get();
    }

    public long getBlocked() {
        return blocked.get();
    }

    public long getAuthFailures() {
        return authFailures.get();
    }

    public long getBytesFromClients() {
        return bytesFromClients.get();
    }

    public long getBytesToClients() {
        return bytesToClients.get();
    }

    public int getActiveConnections() {
        return Math.max(0, activeConnections.get());
    }

    public long getStartedAt() {
        return startedAt;
    }

    public List<Row> clientRows() {
        return rows(clients);
    }

    public List<Row> userRows() {
        return rows(users);
    }

    private static List<Row> rows(Map<String, Counter> map) {
        List<Row> list = new ArrayList<Row>();
        for (Map.Entry<String, Counter> e : map.entrySet()) {
            list.add(new Row(e.getKey(), e.getValue()));
        }
        Collections.sort(list, new Comparator<Row>() {
            public int compare(Row a, Row b) {
                int byBytes = Long.compare(b.down + b.up, a.down + a.up);
                if (byBytes != 0) {
                    return byBytes;
                }
                int byRequests = Long.compare(b.requests, a.requests);
                return byRequests != 0 ? byRequests : a.name.compareTo(b.name);
            }
        });
        return list;
    }

    public List<Map.Entry<String, Long>> topHosts(int n) {
        List<Map.Entry<String, Long>> list = new ArrayList<Map.Entry<String, Long>>();
        for (Map.Entry<String, AtomicLong> e : hosts.entrySet()) {
            list.add(new java.util.AbstractMap.SimpleEntry<String, Long>(e.getKey(), e.getValue().get()));
        }
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return Long.compare(b.getValue(), a.getValue());
            }
        });
        return list.size() > n ? new ArrayList<Map.Entry<String, Long>>(list.subList(0, n)) : list;
    }

    public void reset() {
        requests.set(0);
        blocked.set(0);
        authFailures.set(0);
        bytesFromClients.set(0);
        bytesToClients.set(0);
        hosts.clear();
        clients.clear();
        users.clear();
        startedAt = System.currentTimeMillis();
    }
}
