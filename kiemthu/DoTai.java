import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import proxyapp.BlockList;
import proxyapp.ProxyConfig;
import proxyapp.ProxyService;
import proxyapp.ProxyStats;
import proxyapp.TrafficLog;

public class DoTai {

    static int originPort;
    static int proxyPort;
    static final java.util.concurrent.ConcurrentHashMap<String, AtomicLong> errorKinds =
            new java.util.concurrent.ConcurrentHashMap<String, AtomicLong>();

    static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    static long fetch(String path, boolean viaProxy) throws Exception {
        long start = System.nanoTime();
        URL url = new URL("http://127.0.0.1:" + originPort + path);
        HttpURLConnection c = (HttpURLConnection) (viaProxy
                ? url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort)))
                : url.openConnection(Proxy.NO_PROXY));
        c.setConnectTimeout(10000);
        c.setReadTimeout(20000);
        if (c.getResponseCode() != 200) {
            throw new IllegalStateException("ma " + c.getResponseCode());
        }
        byte[] buf = new byte[16384];
        try (InputStream in = c.getInputStream()) {
            while (in.read(buf) >= 0) {
            }
        }
        return (System.nanoTime() - start) / 1000;
    }

    static List<Long> run(final int threads, final int perThread, final String path, final boolean viaProxy, final AtomicLong errors) throws Exception {
        final List<Long> times = Collections.synchronizedList(new ArrayList<Long>());
        List<Thread> list = new ArrayList<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread() {
                public void run() {
                    for (int j = 0; j < perThread; j++) {
                        try {
                            times.add(fetch(path, viaProxy));
                        } catch (Exception e) {
                            errors.incrementAndGet();
                            String key = e.getClass().getSimpleName() + ": " + e.getMessage();
                            AtomicLong n = errorKinds.get(key);
                            if (n == null) {
                                errorKinds.putIfAbsent(key, new AtomicLong());
                                n = errorKinds.get(key);
                            }
                            n.incrementAndGet();
                        }
                    }
                }
            };
            list.add(t);
            t.start();
        }
        for (Thread t : list) {
            t.join();
        }
        return times;
    }

    static String stats(List<Long> times) {
        if (times.isEmpty()) {
            return "khong co mau";
        }
        List<Long> sorted = new ArrayList<Long>(times);
        Collections.sort(sorted);
        long sum = 0;
        for (long v : sorted) {
            sum += v;
        }
        return String.format("trung binh %.1f ms, p95 %.1f ms, lon nhat %.1f ms (%d yeu cau)",
                sum / (double) sorted.size() / 1000, sorted.get((int) (sorted.size() * 0.95)) / 1000.0,
                sorted.get(sorted.size() - 1) / 1000.0, sorted.size());
    }

    static long usedMb() {
        System.gc();
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
    }

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int perThread = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        int soakMinutes = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        originPort = freePort();
        proxyPort = freePort();
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", originPort), 0);
        origin.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(64));
        origin.createContext("/", ex -> {
            int size = ex.getRequestURI().getPath().equals("/big") ? 1024 * 1024 : 1024;
            ex.sendResponseHeaders(200, size);
            OutputStream os = ex.getResponseBody();
            os.write(new byte[size]);
            os.close();
        });
        origin.start();

        File tmp = File.createTempFile("dotai", ".txt");
        BlockList blockList = new BlockList(tmp);
        blockList.setText("chan.test\n");
        ProxyService service = new ProxyService(blockList, new TrafficLog(2000), new ProxyStats());
        ProxyConfig cfg = new ProxyConfig();
        cfg.port = proxyPort;
        service.start(cfg);

        AtomicLong errors = new AtomicLong();
        run(4, 20, "/small", true, errors);
        errors.set(0);
        System.out.println("Cau hinh: " + threads + " luong x " + perThread + " yeu cau");
        System.out.println("Truc tiep  (1 KB): " + stats(run(threads, perThread, "/small", false, errors)));
        System.out.println("Qua proxy  (1 KB): " + stats(run(threads, perThread, "/small", true, errors)));
        System.out.println("Truc tiep  (1 MB): " + stats(run(threads, perThread / 5, "/big", false, errors)));
        System.out.println("Qua proxy  (1 MB): " + stats(run(threads, perThread / 5, "/big", true, errors)));
        System.out.println("Loi: " + errors.get());
        long threadsBefore = ManagementFactory.getThreadMXBean().getThreadCount();
        System.out.println("RAM sau tai: " + usedMb() + " MB, so luong: " + threadsBefore);

        if (soakMinutes > 0) {
            System.out.println("Chay lien tuc " + soakMinutes + " phut");
            long end = System.currentTimeMillis() + soakMinutes * 60000L;
            long nextReport = System.currentTimeMillis() + 60000L;
            long total = 0;
            errors.set(0);
            while (System.currentTimeMillis() < end) {
                total += run(8, 25, "/small", true, errors).size();
                total += run(2, 5, "/big", true, errors).size();
                if (System.currentTimeMillis() >= nextReport) {
                    nextReport += 60000L;
                    System.out.println("  tong " + total + " yeu cau, loi " + errors.get() + ", RAM " + usedMb() + " MB, luong "
                            + ManagementFactory.getThreadMXBean().getThreadCount());
                }
            }
            System.out.println("Ket thuc: tong " + total + " yeu cau, loi " + errors.get() + ", RAM " + usedMb() + " MB, luong "
                    + ManagementFactory.getThreadMXBean().getThreadCount());
        }
        for (java.util.Map.Entry<String, AtomicLong> e : errorKinds.entrySet()) {
            System.out.println("Loai loi: " + e.getKey() + " x " + e.getValue().get());
        }
        service.stop();
        origin.stop(0);
        tmp.delete();
        System.exit(0);
    }
}
