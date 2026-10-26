package proxyapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AlertLog {

    public static class Alert {
        public final long time;
        public final String text;

        Alert(long time, String text) {
            this.time = time;
            this.text = text;
        }

        public String timeText() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
        }
    }

    public interface Listener {
        void onAlert(Alert alert);
    }

    private final int max;
    private final LinkedList<Alert> alerts = new LinkedList<Alert>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<Listener>();

    public AlertLog(int max) {
        this.max = max;
    }

    long now() {
        return System.currentTimeMillis();
    }

    public void add(String text) {
        Alert a = new Alert(now(), text);
        synchronized (this) {
            alerts.addLast(a);
            while (alerts.size() > max) {
                alerts.removeFirst();
            }
        }
        for (Listener l : listeners) {
            l.onAlert(a);
        }
    }

    public synchronized List<Alert> snapshot() {
        return new ArrayList<Alert>(alerts);
    }

    public synchronized int size() {
        return alerts.size();
    }

    public synchronized void clear() {
        alerts.clear();
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }
}
