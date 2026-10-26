package proxyapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrafficLog {

    public interface Listener {
        void onEntry(LogEntry entry);
    }

    private final int max;
    private final LinkedList<LogEntry> entries = new LinkedList<LogEntry>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private volatile File appendFile;

    public TrafficLog(int max) {
        this.max = max;
    }

    public void add(LogEntry e) {
        synchronized (this) {
            entries.addLast(e);
            while (entries.size() > max) {
                entries.removeFirst();
            }
        }
        File f = appendFile;
        if (f != null) {
            appendToFile(f, e);
        }
        for (Listener l : listeners) {
            l.onEntry(e);
        }
    }

    public synchronized List<LogEntry> snapshot() {
        return new ArrayList<LogEntry>(entries);
    }

    public synchronized void clear() {
        entries.clear();
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void setAppendFile(File f) {
        if (f != null) {
            moveAsideIfOldFormat(f);
        }
        this.appendFile = f;
    }

    private static synchronized void moveAsideIfOldFormat(File f) {
        if (!f.isFile() || f.length() == 0) {
            return;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String first = r.readLine();
            if (LogEntry.csvHeader().equals(first)) {
                return;
            }
        } catch (IOException e) {
            return;
        }
        File old = new File(f.getPath() + ".old");
        old.delete();
        f.renameTo(old);
    }

    public void exportCsv(File f) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(f))) {
            w.println(LogEntry.csvHeader());
            for (LogEntry e : snapshot()) {
                w.println(e.toCsv());
            }
        }
    }

    private static synchronized void appendToFile(File f, LogEntry e) {
        boolean fresh = !f.exists() || f.length() == 0;
        try (PrintWriter w = new PrintWriter(new FileWriter(f, true))) {
            if (fresh) {
                w.println(LogEntry.csvHeader());
            }
            w.println(e.toCsv());
        } catch (IOException ex) {
        }
    }
}
