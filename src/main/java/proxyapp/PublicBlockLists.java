package proxyapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class PublicBlockLists {

    public static class Result {
        public boolean anyOk;
        public int domains;
        public final List<String> messages = new ArrayList<String>();
    }

    private final BlockList blockList;
    private final File cacheFile;
    private final Object updateLock = new Object();
    private Timer timer;
    private volatile long updatedAt;
    long maxBytes = 64L * 1024 * 1024;

    public PublicBlockLists(BlockList blockList, File cacheFile) {
        this.blockList = blockList;
        this.cacheFile = cacheFile;
    }

    public static List<String> splitSources(String text) {
        List<String> list = new ArrayList<String>();
        if (text == null) {
            return list;
        }
        for (String line : text.split("\\r?\\n")) {
            String s = line.trim();
            if (!s.isEmpty() && !s.startsWith("#")) {
                list.add(s);
            }
        }
        return list;
    }

    public void loadCache() {
        if (cacheFile == null || !cacheFile.isFile()) {
            return;
        }
        try {
            String text = new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8);
            blockList.setPublicDomains(DomainListParser.parse(text));
            updatedAt = cacheFile.lastModified();
        } catch (IOException e) {
        }
    }

    public int size() {
        return blockList.publicSize();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Result update(List<String> sources) {
        synchronized (updateLock) {
            Result r = new Result();
            Set<String> all = new HashSet<String>();
            for (String source : sources) {
                try {
                    Set<String> found = DomainListParser.parse(read(source));
                    all.addAll(found);
                    r.anyOk = true;
                    r.messages.add(source + ": " + found.size() + " tên miền");
                } catch (IOException e) {
                    r.messages.add(source + ": lỗi - " + e.getMessage());
                }
            }
            if (r.anyOk) {
                try {
                    writeCache(all);
                } catch (IOException e) {
                    r.messages.add("Không lưu được file " + cacheFile + ": " + e.getMessage());
                }
                blockList.setPublicDomains(all);
                updatedAt = System.currentTimeMillis();
            }
            r.domains = blockList.publicSize();
            return r;
        }
    }

    private String read(String source) throws IOException {
        String lower = source.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            HttpURLConnection c = (HttpURLConnection) new URL(source).openConnection(Proxy.NO_PROXY);
            c.setConnectTimeout(15000);
            c.setReadTimeout(60000);
            try {
                int code = c.getResponseCode();
                if (code != 200) {
                    throw new IOException("máy chủ trả mã " + code);
                }
                try (InputStream in = c.getInputStream()) {
                    return readLimited(in);
                }
            } finally {
                c.disconnect();
            }
        }
        File f = new File(source);
        if (!f.isFile()) {
            throw new IOException("không tìm thấy file");
        }
        if (f.length() > maxBytes) {
            throw new IOException("file quá lớn");
        }
        try (InputStream in = Files.newInputStream(f.toPath())) {
            return readLimited(in);
        }
    }

    private String readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            if (out.size() > maxBytes) {
                throw new IOException("dữ liệu quá lớn");
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private void writeCache(Set<String> domains) throws IOException {
        if (cacheFile == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String d : domains) {
            sb.append(d).append('\n');
        }
        File temp = new File(cacheFile.getPath() + ".tmp");
        Files.write(temp.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(temp.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public synchronized void applySchedule(List<String> sources, int hours) {
        stopAuto();
        if (!sources.isEmpty() && hours > 0) {
            long period = hours * 3600000L;
            long age = updatedAt == 0 ? Long.MAX_VALUE : System.currentTimeMillis() - updatedAt;
            long delay = age >= period ? 5000 : period - age;
            startAuto(sources, delay, period);
        }
    }

    synchronized void startAuto(final List<String> sources, long delay, long period) {
        stopAuto();
        timer = new Timer("public-blocklist", true);
        timer.schedule(new TimerTask() {
            public void run() {
                update(sources);
            }
        }, delay, period);
    }

    public synchronized void stopAuto() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
