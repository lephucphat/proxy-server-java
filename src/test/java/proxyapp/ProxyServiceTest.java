package proxyapp;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProxyServiceTest {

    private HttpServer origin;
    private int originPort;
    private int proxyPort;
    private BlockList blockList;
    private TrafficLog log;
    private ProxyStats stats;
    private ProxyService service;
    private File tmp;
    private File usersRoot;

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Before
    public void setUp() throws Exception {
        tmp = Files.createTempFile("blocklist", ".txt").toFile();
        originPort = freePort();
        origin = HttpServer.create(new InetSocketAddress("127.0.0.1", originPort), 0);
        origin.createContext("/", ex -> {
            byte[] body = "hello-from-origin".getBytes();
            ex.sendResponseHeaders(200, body.length);
            OutputStream os = ex.getResponseBody();
            os.write(body);
            os.close();
        });
        origin.start();
        proxyPort = freePort();
        blockList = new BlockList(tmp);
        blockList.setText("blocked.test\n");
        log = new TrafficLog(100);
        stats = new ProxyStats();
        service = new ProxyService(blockList, log, stats);
    }

    @After
    public void tearDown() {
        service.stop();
        origin.stop(0);
        tmp.delete();
        deleteAll(usersRoot);
        Authenticator.setDefault(null);
    }

    private static void deleteAll(File f) {
        if (f == null) {
            return;
        }
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteAll(c);
            }
        }
        f.delete();
    }

    private ProxyConfig config() {
        ProxyConfig c = new ProxyConfig();
        c.port = proxyPort;
        return c;
    }

    private String[] get(String url) throws Exception {
        Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort));
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(p);
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        if (in != null) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
            }
        }
        return new String[]{String.valueOf(code), bo.toString("UTF-8")};
    }

    @Test
    public void forwardsAllowedRequestAndLogsIt() throws Exception {
        service.start(config());
        String[] r = get("http://127.0.0.1:" + originPort + "/hi");
        assertEquals("200", r[0]);
        assertEquals("hello-from-origin", r[1]);
        Thread.sleep(300);
        List<LogEntry> entries = log.snapshot();
        assertEquals(1, entries.size());
        LogEntry e = entries.get(0);
        assertEquals("GET", e.method);
        assertEquals("127.0.0.1", e.host);
        assertEquals(200, e.status);
        assertEquals(17, e.bytes);
        assertEquals("", e.user);
        assertFalse(e.blocked);
        assertEquals(1, stats.getRequests());
        assertEquals(0, stats.getBlocked());
        assertTrue(stats.getBytesToClients() >= 17);
    }

    @Test
    public void blocksListedDomainWith403() throws Exception {
        service.start(config());
        String[] r = get("http://blocked.test/");
        assertEquals("403", r[0]);
        assertTrue(r[1].contains("blocked.test"));
        List<LogEntry> entries = log.snapshot();
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).blocked);
        assertEquals(403, entries.get(0).status);
        assertEquals(1, stats.getBlocked());
    }

    @Test
    public void blocksSubdomainsAndEditsTakeEffectWithoutRestart() throws Exception {
        service.start(config());
        assertEquals("403", get("http://www.blocked.test/")[0]);
        blockList.setText("127.0.0.1\n");
        assertEquals("403", get("http://127.0.0.1:" + originPort + "/")[0]);
        blockList.setText("# trong\n");
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
    }

    @Test
    public void requiresCredentialsWhenAuthEnabled() throws Exception {
        ProxyConfig c = config();
        c.requireAuth = true;
        c.username = "sv";
        c.setPassword("123");
        service.start(c);
        assertEquals("407", get("http://127.0.0.1:" + originPort + "/")[0]);
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("sv", "123".toCharArray());
            }
        });
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
    }

    @Test
    public void wrongPasswordIsRejected() throws Exception {
        ProxyConfig c = config();
        c.requireAuth = true;
        c.username = "sv";
        c.setPassword("123");
        c.maxAuthFailures = 0;
        service.start(c);
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("sv", "sai".toCharArray());
            }
        });
        assertEquals("407", get("http://127.0.0.1:" + originPort + "/")[0]);
        assertTrue(stats.getAuthFailures() >= 1);
    }

    @Test
    public void connectToBlockedHostIsRefused() throws Exception {
        blockList.setText("blocked.test\n");
        service.start(config());
        java.net.Socket s = new java.net.Socket("127.0.0.1", proxyPort);
        s.setSoTimeout(5000);
        s.getOutputStream().write("CONNECT blocked.test:443 HTTP/1.1\r\nHost: blocked.test:443\r\n\r\n".getBytes());
        s.getOutputStream().flush();
        byte[] buf = new byte[256];
        int n = s.getInputStream().read(buf);
        s.close();
        assertTrue(new String(buf, 0, n).startsWith("HTTP/1.1 403"));
        Thread.sleep(200);
        assertEquals("CONNECT", log.snapshot().get(0).method);
        assertTrue(log.snapshot().get(0).blocked);
    }

    @Test
    public void connectTunnelToOriginWorks() throws Exception {
        service.start(config());
        java.net.Socket s = new java.net.Socket("127.0.0.1", proxyPort);
        s.setSoTimeout(5000);
        String hp = "127.0.0.1:" + originPort;
        s.getOutputStream().write(("CONNECT " + hp + " HTTP/1.1\r\nHost: " + hp + "\r\n\r\n").getBytes());
        s.getOutputStream().flush();
        byte[] buf = new byte[512];
        int n = s.getInputStream().read(buf);
        assertTrue(new String(buf, 0, n).startsWith("HTTP/1.1 200"));
        s.getOutputStream().write(("GET / HTTP/1.1\r\nHost: " + hp + "\r\nConnection: close\r\n\r\n").getBytes());
        s.getOutputStream().flush();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int m;
        while ((m = s.getInputStream().read(buf)) > 0) {
            bo.write(buf, 0, m);
        }
        s.close();
        assertTrue(bo.toString("UTF-8").contains("hello-from-origin"));
        Thread.sleep(200);
        LogEntry e = log.snapshot().get(0);
        assertEquals("CONNECT", e.method);
        assertEquals(200, e.status);
    }

    @Test
    public void unreachableHostGives502AndIsLogged() throws Exception {
        service.start(config());
        int dead = freePort();
        String[] r = get("http://127.0.0.1:" + dead + "/");
        assertEquals("502", r[0]);
        Thread.sleep(300);
        assertEquals(502, log.snapshot().get(0).status);
    }

    @Test
    public void topHostsCountsRequests() throws Exception {
        service.start(config());
        get("http://127.0.0.1:" + originPort + "/a");
        get("http://127.0.0.1:" + originPort + "/b");
        get("http://blocked.test/");
        assertEquals("127.0.0.1", stats.topHosts(1).get(0).getKey());
        assertEquals(2L, (long) stats.topHosts(1).get(0).getValue());
    }

    @Test
    public void stopAndRestartOnSamePort() throws Exception {
        service.start(config());
        assertTrue(service.isRunning());
        service.stop();
        assertFalse(service.isRunning());
        service.start(config());
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
    }

    @Test
    public void busyPortGivesClearMessageAndServiceStaysStopped() throws Exception {
        try (ServerSocket taken = new ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))) {
            ProxyConfig c = config();
            c.port = taken.getLocalPort();
            try {
                service.start(c);
                fail("phải báo lỗi khi cổng đang bị chiếm");
            } catch (IllegalStateException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("Cổng " + c.port + " đang được chương trình khác dùng"));
            }
            assertFalse(service.isRunning());
        }
        service.start(config());
        assertTrue(service.isRunning());
    }

    @Test
    public void bandwidthLimitSlowsDownLargeDownload() throws Exception {
        origin.createContext("/big", ex -> {
            byte[] body = new byte[120 * 1024];
            ex.sendResponseHeaders(200, body.length);
            OutputStream os = ex.getResponseBody();
            os.write(body);
            os.close();
        });
        ProxyConfig c = config();
        c.maxKBps = 30;
        service.start(c);
        long t0 = System.currentTimeMillis();
        String[] r = get("http://127.0.0.1:" + originPort + "/big");
        long took = System.currentTimeMillis() - t0;
        assertEquals("200", r[0]);
        assertEquals(120 * 1024, r[1].length());
        assertTrue("mat " + took + "ms", took > 1500);
    }

    @Test
    public void domainFromPublicListIsBlockedWith403() throws Exception {
        File list = Files.createTempFile("pub", ".txt").toFile();
        File cache = Files.createTempFile("pubcache", ".txt").toFile();
        try {
            Files.write(list.toPath(), "0.0.0.0 ads.public-list.test\n".getBytes());
            new PublicBlockLists(blockList, cache).update(java.util.Arrays.asList(list.getPath()));
            service.start(config());
            String[] r = get("http://www.ads.public-list.test/");
            assertEquals("403", r[0]);
            assertTrue(r[1].contains("ads.public-list.test"));
            Thread.sleep(200);
            assertTrue(log.snapshot().get(0).blocked);
            assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
        } finally {
            list.delete();
            cache.delete();
        }
    }

    private BlockList newAllowList(String text) throws Exception {
        File f = Files.createTempFile("allow", ".txt").toFile();
        f.deleteOnExit();
        BlockList b = new BlockList(f, "");
        b.setText(text);
        service.setAllowList(b);
        return b;
    }

    private BlockSchedule newSchedule(String text) throws Exception {
        File f = Files.createTempFile("sch", ".txt").toFile();
        f.deleteOnExit();
        BlockSchedule s = new BlockSchedule(f);
        s.setText(text);
        service.setSchedule(s);
        return s;
    }

    private static String clock(java.time.LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    @Test
    public void allowOnlyModeBlocksEverythingNotListed() throws Exception {
        newAllowList("127.0.0.1\n");
        ProxyConfig c = config();
        c.allowOnlyMode = true;
        service.start(c);
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
        String[] r = get("http://khac.test/");
        assertEquals("403", r[0]);
        assertTrue(r[1].contains("chỉ cho phép"));
        assertTrue(r[1].contains("khac.test"));
        Thread.sleep(200);
        assertTrue(log.snapshot().get(1).blocked);
        assertEquals(1, stats.getBlocked());
    }

    @Test
    public void blocklistStillWinsInAllowOnlyMode() throws Exception {
        newAllowList("127.0.0.1\n");
        blockList.setText("127.0.0.1\n");
        ProxyConfig c = config();
        c.allowOnlyMode = true;
        service.start(c);
        String[] r = get("http://127.0.0.1:" + originPort + "/");
        assertEquals("403", r[0]);
        assertTrue(r[1].contains("Proxy đã chặn trang"));
    }

    @Test
    public void allowOnlyModeAndListCanChangeWithoutRestart() throws Exception {
        BlockList allowed = newAllowList("noi-khac.test\n");
        service.start(config());
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
        service.setAllowOnly(true);
        assertEquals("403", get("http://127.0.0.1:" + originPort + "/")[0]);
        allowed.setText("127.0.0.1\n");
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
        service.setAllowOnly(false);
        allowed.setText("");
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
    }

    @Test
    public void scheduleBlocksInsideItsWindowAndEditsApplyAtOnce() throws Exception {
        BlockSchedule s = newSchedule("127.0.0.1 * 00:00-24:00\n");
        service.start(config());
        String[] r = get("http://127.0.0.1:" + originPort + "/");
        assertEquals("403", r[0]);
        assertTrue(r[1].contains("lịch"));
        Thread.sleep(200);
        assertTrue(log.snapshot().get(0).blocked);
        s.setText("# trống\n");
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
    }

    @Test
    public void scheduleWindowThatIsNotNowAllowsAndOneThatIsNowBlocks() throws Exception {
        java.time.LocalTime now = java.time.LocalTime.now();
        BlockSchedule s = newSchedule("127.0.0.1 * " + clock(now.plusHours(2)) + "-" + clock(now.plusHours(3)) + "\n");
        service.start(config());
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
        s.setText("127.0.0.1 * " + clock(now.minusHours(1)) + "-" + clock(now.plusHours(1)) + "\n");
        assertEquals("403", get("http://127.0.0.1:" + originPort + "/")[0]);
    }

    @Test
    public void wildcardScheduleLetsAllowedSitesThrough() throws Exception {
        newAllowList("127.0.0.1\n");
        newSchedule("* * 00:00-24:00\n");
        service.start(config());
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/")[0]);
        assertEquals("403", get("http://khac.test/")[0]);
    }

    private UserStore newUsers() throws Exception {
        usersRoot = Files.createTempDirectory("svc-users").toFile();
        UserStore u = new UserStore(new File(usersRoot, "users.txt"), new File(usersRoot, "users"));
        service.setUsers(u);
        return u;
    }

    private java.util.Set<String> loggedUsers() {
        java.util.Set<String> set = new java.util.HashSet<String>();
        for (LogEntry e : log.snapshot()) {
            set.add(e.user);
        }
        return set;
    }

    @Test
    public void extraAccountsCanLoginAlongsideTheMainOne() throws Exception {
        UserStore users = newUsers();
        users.add("an", "matkhau");
        service.start(authConfig());
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("an", "matkhau")));
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("sv", "123")));
        assertEquals("HTTP/1.1 407 Proxy Authentication Required", rawStatus("127.0.0.1", basic("an", "sai")));
        assertEquals("HTTP/1.1 407 Proxy Authentication Required", rawStatus("127.0.0.1", basic("ai-do", "matkhau")));
        assertEquals("HTTP/1.1 407 Proxy Authentication Required", rawStatus("127.0.0.1", basic("sv", "matkhau")));
        Thread.sleep(300);
        assertEquals(3, stats.getAuthFailures());
        java.util.Set<String> names = loggedUsers();
        assertTrue(names.toString(), names.contains("an"));
        assertTrue(names.contains("sv"));
        assertTrue(names.contains("ai-do"));
    }

    @Test
    public void everyLogRowCarriesTheNameOfTheAccountThatMadeIt() throws Exception {
        UserStore users = newUsers();
        users.add("an", "matkhau");
        service.start(authConfig());
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        rawStatus("127.0.0.1", basic("sv", "123"));
        Thread.sleep(300);
        int an = 0;
        int sv = 0;
        for (LogEntry e : log.snapshot()) {
            if (e.status == 200 && e.user.equals("an")) {
                an++;
            }
            if (e.status == 200 && e.user.equals("sv")) {
                sv++;
            }
            assertTrue(e.toCsv().contains("," + e.user + ","));
        }
        assertEquals(1, an);
        assertEquals(1, sv);
    }

    @Test
    public void accountWithOwnAllowOnlyRulesIsRestrictedOthersAreNot() throws Exception {
        UserStore users = newUsers();
        users.add("be", "matkhau");
        users.add("chi", "matkhau");
        users.setRules("be", true, true, "khac.test\n", "");
        service.start(authConfig());
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus("127.0.0.1", basic("be", "matkhau")));
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("chi", "matkhau")));
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("sv", "123")));
        Thread.sleep(300);
        boolean found = false;
        for (LogEntry e : log.snapshot()) {
            if (e.user.equals("be") && e.blocked && e.status == 403) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void accountWithOwnScheduleIsBlockedByItAndEditsApplyAtOnce() throws Exception {
        UserStore users = newUsers();
        users.add("be", "matkhau");
        users.setRules("be", true, false, "", "127.0.0.1 * 00:00-24:00\n");
        service.start(authConfig());
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus("127.0.0.1", basic("be", "matkhau")));
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("sv", "123")));
        users.setRules("be", true, false, "", "# trống\n");
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("be", "matkhau")));
        users.setRules("be", false, false, "", "127.0.0.1 * 00:00-24:00\n");
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("be", "matkhau")));
    }

    @Test
    public void ownRulesReplaceTheGlobalOnesForThatAccountOnly() throws Exception {
        UserStore users = newUsers();
        users.add("be", "matkhau");
        users.add("chi", "matkhau");
        users.setRules("be", true, false, "", "");
        newAllowList("khac.test\n");
        ProxyConfig c = authConfig();
        c.allowOnlyMode = true;
        service.start(c);
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("be", "matkhau")));
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus("127.0.0.1", basic("chi", "matkhau")));
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus("127.0.0.1", basic("sv", "123")));
    }

    @Test
    public void globalBlocklistStillAppliesToAccountsWithOwnRules() throws Exception {
        UserStore users = newUsers();
        users.add("be", "matkhau");
        users.setRules("be", true, false, "", "");
        blockList.setText("127.0.0.1\n");
        service.start(authConfig());
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus("127.0.0.1", basic("be", "matkhau")));
    }

    private UrlRules newUrlRules(String text) throws Exception {
        File f = Files.createTempFile("urlrules", ".txt").toFile();
        f.deleteOnExit();
        UrlRules r = new UrlRules(f);
        r.setText(text);
        service.setUrlRules(r);
        return r;
    }

    @Test
    public void pathRuleBlocksPlainHttpPagesAndOthersPass() throws Exception {
        UrlRules rules = newUrlRules("127.0.0.1 path /cam\n");
        service.start(config());
        String[] bad = get("http://127.0.0.1:" + originPort + "/cam/vao?x=1");
        assertEquals("403", bad[0]);
        assertTrue(bad[1].contains("127.0.0.1 path /cam"));
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/hoc")[0]);
        Thread.sleep(300);
        boolean logged = false;
        for (LogEntry e : log.snapshot()) {
            if (e.blocked && e.target.contains("/cam/vao")) {
                logged = true;
            }
        }
        assertTrue(logged);
        assertEquals(1, stats.getBlocked());
        rules.setText("# trống\n");
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/cam/vao")[0]);
    }

    @Test
    public void keywordRuleBlocksPlainHttpAndTheNameOfAHttpsSite() throws Exception {
        newUrlRules("* keyword bimat\n* keyword casino\n");
        service.start(config());
        assertEquals("403", get("http://127.0.0.1:" + originPort + "/trang-BiMat")[0]);
        assertEquals("200", get("http://127.0.0.1:" + originPort + "/binh-thuong")[0]);
        java.net.Socket s = new java.net.Socket("127.0.0.1", proxyPort);
        s.setSoTimeout(5000);
        s.getOutputStream().write("CONNECT casino-vui.test:443 HTTP/1.1\r\nHost: casino-vui.test:443\r\n\r\n".getBytes());
        s.getOutputStream().flush();
        byte[] buf = new byte[256];
        int n = s.getInputStream().read(buf);
        s.close();
        assertTrue(new String(buf, 0, n).startsWith("HTTP/1.1 403"));
    }

    @Test
    public void urlRulesComeAfterTheBlocklistAndCountAsBlocked() throws Exception {
        blockList.setText("127.0.0.1\n");
        newUrlRules("* keyword x\n");
        service.start(config());
        String[] r = get("http://127.0.0.1:" + originPort + "/x");
        assertEquals("403", r[0]);
        assertTrue(r[1].contains("Proxy đã chặn trang"));
    }

    private ProxyStats.Row rowOf(java.util.List<ProxyStats.Row> rows, String name) {
        for (ProxyStats.Row r : rows) {
            if (r.name.equals(name)) {
                return r;
            }
        }
        return null;
    }

    @Test
    public void statisticsAreKeptPerClientAndPerUser() throws Exception {
        UserStore users = newUsers();
        users.add("an", "matkhau");
        service.start(authConfig());
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        rawStatus("127.0.0.1", basic("sv", "123"));
        rawStatus("127.0.0.1", basic("an", "sai"));
        rawStatus("127.0.0.1", basic("ma-quai", "xxx"));
        Thread.sleep(500);
        ProxyStats.Row client = rowOf(stats.clientRows(), "127.0.0.1");
        assertEquals(3, client.requests);
        assertEquals(2, client.authFailures);
        assertTrue(client.down > 0);
        assertTrue(client.up > 0);
        ProxyStats.Row an = rowOf(stats.userRows(), "an");
        assertEquals(2, an.requests);
        assertEquals(1, an.authFailures);
        assertTrue("byte của an: " + an.down, an.down > 0);
        assertEquals(1, rowOf(stats.userRows(), "sv").requests);
        assertNull(rowOf(stats.userRows(), "ma-quai"));
    }

    @Test
    public void blockedRequestsAreCountedForTheClientAndTheUser() throws Exception {
        UserStore users = newUsers();
        users.add("an", "matkhau");
        blockList.setText("127.0.0.1\n");
        service.start(authConfig());
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        assertEquals(2, rowOf(stats.userRows(), "an").blocked);
        assertEquals(2, rowOf(stats.clientRows(), "127.0.0.1").blocked);
    }

    @Test
    public void lockingAnAddressRaisesAnAlert() throws Exception {
        AlertLog alerts = new AlertLog(10);
        service.setAlerts(alerts);
        ProxyConfig c = authConfig();
        c.maxAuthFailures = 3;
        service.start(c);
        for (int i = 0; i < 3; i++) {
            rawStatus("127.0.0.1", basic("sv", "sai"));
        }
        assertEquals(1, alerts.size());
        assertTrue(alerts.snapshot().get(0).text.contains("127.0.0.1"));
        assertTrue(alerts.snapshot().get(0).text.contains("khóa"));
        rawStatus("127.0.0.1", basic("sv", "123"));
        assertEquals(1, alerts.size());
    }

    @Test
    public void manyBlockedRequestsRaiseAnAlertNamingTheUser() throws Exception {
        UserStore users = newUsers();
        users.add("an", "matkhau");
        AlertLog alerts = new AlertLog(10);
        service.setAlerts(alerts);
        blockList.setText("127.0.0.1\n");
        ProxyConfig c = authConfig();
        c.alertBlocked = 3;
        c.alertMinutes = 5;
        service.start(c);
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        assertEquals(0, alerts.size());
        rawStatus("127.0.0.1", basic("an", "matkhau"));
        assertEquals(1, alerts.size());
        assertTrue(alerts.snapshot().get(0).text, alerts.snapshot().get(0).text.contains("an (127.0.0.1)"));
        assertTrue(alerts.snapshot().get(0).text.contains("3 lần"));
    }

    @Test
    public void blockedAlertsCanBeSwitchedOff() throws Exception {
        AlertLog alerts = new AlertLog(10);
        service.setAlerts(alerts);
        blockList.setText("127.0.0.1\n");
        ProxyConfig c = config();
        c.alertBlocked = 0;
        service.start(c);
        for (int i = 0; i < 5; i++) {
            rawStatus("127.0.0.1", "");
        }
        assertEquals(0, alerts.size());
        assertEquals(5, rowOf(stats.clientRows(), "127.0.0.1").blocked);
    }

    @Test
    public void unknownMachineTryingTheProxyRaisesAnAlert() throws Exception {
        java.net.InetAddress lan = lanAddress();
        org.junit.Assume.assumeNotNull(lan);
        AlertLog alerts = new AlertLog(10);
        service.setAlerts(alerts);
        ProxyConfig c = authConfig();
        c.allowLan = true;
        c.allowedClients = "10.254.254.0/24";
        service.start(c);
        rawStatus(lan.getHostAddress(), basic("sv", "123"));
        rawStatus(lan.getHostAddress(), basic("sv", "123"));
        assertEquals(1, alerts.size());
        assertTrue(alerts.snapshot().get(0).text.contains(lan.getHostAddress()));
        assertEquals(2, rowOf(stats.clientRows(), lan.getHostAddress()).blocked);
    }

    private static String basic(String user, String pass) {
        String text = user + ":" + pass;
        return "Proxy-Authorization: Basic " + java.util.Base64.getEncoder().encodeToString(text.getBytes()) + "\r\n";
    }

    private String rawStatus(String proxyHost, String extraHeader) throws Exception {
        String hp = "127.0.0.1:" + originPort;
        String request = "GET http://" + hp + "/ HTTP/1.1\r\nHost: " + hp + "\r\n" + extraHeader
                + "Connection: close\r\n\r\n";
        try (java.net.Socket s = new java.net.Socket(proxyHost, proxyPort)) {
            s.setSoTimeout(5000);
            s.getOutputStream().write(request.getBytes());
            s.getOutputStream().flush();
            byte[] buf = new byte[512];
            int n = s.getInputStream().read(buf);
            String text = new String(buf, 0, n);
            return text.substring(0, text.indexOf("\r\n"));
        }
    }

    private ProxyConfig authConfig() {
        ProxyConfig c = config();
        c.requireAuth = true;
        c.username = "sv";
        c.setPassword("123");
        return c;
    }

    private static java.net.InetAddress lanAddress() throws Exception {
        java.util.Enumeration<java.net.NetworkInterface> all = java.net.NetworkInterface.getNetworkInterfaces();
        while (all != null && all.hasMoreElements()) {
            java.net.NetworkInterface ni = all.nextElement();
            if (!ni.isUp() || ni.isLoopback()) {
                continue;
            }
            java.util.Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                java.net.InetAddress a = addrs.nextElement();
                if (a instanceof java.net.Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                    return a;
                }
            }
        }
        return null;
    }

    @Test
    public void requestsWithoutLoginAreAnswered407AndLogged() throws Exception {
        service.start(authConfig());
        assertEquals("HTTP/1.1 407 Proxy Authentication Required", rawStatus("127.0.0.1", ""));
        Thread.sleep(200);
        assertEquals(1, log.snapshot().size());
        assertEquals(407, log.snapshot().get(0).status);
        assertEquals(0, stats.getRequests());
        assertEquals(0, stats.getAuthFailures());
    }

    @Test
    public void wrongPasswordIsCountedAndLogged() throws Exception {
        service.start(authConfig());
        assertEquals("HTTP/1.1 407 Proxy Authentication Required", rawStatus("127.0.0.1", basic("sv", "sai")));
        assertEquals("HTTP/1.1 407 Proxy Authentication Required", rawStatus("127.0.0.1", basic("khac", "123")));
        Thread.sleep(200);
        assertEquals(2, stats.getAuthFailures());
        assertEquals(2, log.snapshot().size());
        assertEquals(407, log.snapshot().get(1).status);
    }

    @Test
    public void correctLoginIsAcceptedAndHeaderIsNotForwarded() throws Exception {
        origin.createContext("/echo", ex -> {
            String h = ex.getRequestHeaders().getFirst("Proxy-Authorization");
            byte[] body = (h == null ? "none" : "leaked").getBytes();
            ex.sendResponseHeaders(200, body.length);
            OutputStream os = ex.getResponseBody();
            os.write(body);
            os.close();
        });
        service.start(authConfig());
        java.net.Socket s = new java.net.Socket("127.0.0.1", proxyPort);
        s.setSoTimeout(5000);
        String hp = "127.0.0.1:" + originPort;
        s.getOutputStream().write(("GET http://" + hp + "/echo HTTP/1.1\r\nHost: " + hp + "\r\n" + basic("sv", "123")
                + "Connection: close\r\n\r\n").getBytes());
        s.getOutputStream().flush();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int m;
        String text = "";
        while (!text.endsWith("none") && !text.endsWith("leaked") && (m = s.getInputStream().read(buf)) > 0) {
            bo.write(buf, 0, m);
            text = bo.toString("UTF-8");
        }
        s.close();
        assertTrue(text.startsWith("HTTP/1.1 200"));
        assertTrue(text.endsWith("none"));
    }

    @Test
    public void addressIsLockedAfterTooManyWrongPasswords() throws Exception {
        ProxyConfig c = authConfig();
        c.maxAuthFailures = 3;
        service.start(c);
        for (int i = 0; i < 3; i++) {
            assertEquals("HTTP/1.1 407 Proxy Authentication Required", rawStatus("127.0.0.1", basic("sv", "sai")));
        }
        assertEquals("HTTP/1.1 429 Too Many Requests", rawStatus("127.0.0.1", basic("sv", "123")));
        assertEquals("HTTP/1.1 429 Too Many Requests", rawStatus("127.0.0.1", ""));
        Thread.sleep(200);
        assertEquals(3, stats.getAuthFailures());
        assertEquals(429, log.snapshot().get(3).status);
        assertTrue(log.snapshot().get(3).blocked);
    }

    @Test
    public void rightPasswordBetweenMistakesResetsTheCount() throws Exception {
        ProxyConfig c = authConfig();
        c.maxAuthFailures = 3;
        service.start(c);
        rawStatus("127.0.0.1", basic("sv", "sai"));
        rawStatus("127.0.0.1", basic("sv", "sai"));
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("sv", "123")));
        rawStatus("127.0.0.1", basic("sv", "sai"));
        rawStatus("127.0.0.1", basic("sv", "sai"));
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("sv", "123")));
    }

    @Test
    public void lanClientCannotReachLocalhostThroughProxy() throws Exception {
        java.net.InetAddress lan = lanAddress();
        org.junit.Assume.assumeNotNull(lan);
        ProxyConfig c = authConfig();
        c.allowLan = true;
        service.start(c);
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus(lan.getHostAddress(), basic("sv", "123")));
        Thread.sleep(200);
        assertTrue(log.snapshot().get(0).blocked);
        assertEquals(1, stats.getBlocked());
    }

    @Test
    public void lanClientCanReachLocalhostWhenInternalBlockIsOff() throws Exception {
        java.net.InetAddress lan = lanAddress();
        org.junit.Assume.assumeNotNull(lan);
        ProxyConfig c = authConfig();
        c.allowLan = true;
        c.blockInternal = false;
        service.start(c);
        assertEquals("HTTP/1.1 200 OK", rawStatus(lan.getHostAddress(), basic("sv", "123")));
    }

    @Test
    public void localClientKeepsAccessEvenWhenLanIsOnAndListIsSet() throws Exception {
        ProxyConfig c = authConfig();
        c.allowLan = true;
        c.allowedClients = "10.254.254.0/24";
        service.start(c);
        assertEquals("HTTP/1.1 200 OK", rawStatus("127.0.0.1", basic("sv", "123")));
    }

    @Test
    public void lanClientOutsideTheAllowedListIsRefusedBeforeLogin() throws Exception {
        java.net.InetAddress lan = lanAddress();
        org.junit.Assume.assumeNotNull(lan);
        ProxyConfig c = authConfig();
        c.allowLan = true;
        c.blockInternal = false;
        c.allowedClients = "10.254.254.0/24";
        service.start(c);
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus(lan.getHostAddress(), basic("sv", "123")));
        assertEquals("HTTP/1.1 403 Forbidden", rawStatus(lan.getHostAddress(), ""));
        assertEquals(0, stats.getAuthFailures());
    }

    @Test
    public void lanClientInsideTheAllowedListCanUseProxy() throws Exception {
        java.net.InetAddress lan = lanAddress();
        org.junit.Assume.assumeNotNull(lan);
        ProxyConfig c = authConfig();
        c.allowLan = true;
        c.blockInternal = false;
        c.allowedClients = lan.getHostAddress() + "/32";
        service.start(c);
        assertEquals("HTTP/1.1 200 OK", rawStatus(lan.getHostAddress(), basic("sv", "123")));
    }
}
