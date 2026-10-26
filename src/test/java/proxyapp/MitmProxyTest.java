package proxyapp;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MitmProxyTest {

    private CertificateAuthority proxyCa;
    private CertificateAuthority worldCa;
    private HttpsServer origin;
    private int originPort;
    private int proxyPort;
    private File tmp;
    private BlockList blockList;
    private TrafficLog log;
    private ProxyStats stats;
    private ProxyService service;

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Before
    public void setUp() throws Exception {
        proxyCa = CertificateAuthority.create();
        worldCa = CertificateAuthority.create();
        origin = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin.setHttpsConfigurator(new HttpsConfigurator(worldCa.issue("localhost").context()));
        origin.createContext("/", ex -> {
            byte[] body = ("secret-page " + ex.getRequestURI()).getBytes();
            ex.sendResponseHeaders(200, body.length);
            OutputStream os = ex.getResponseBody();
            os.write(body);
            os.close();
        });
        origin.start();
        originPort = origin.getAddress().getPort();
        proxyPort = freePort();
        tmp = Files.createTempFile("blocklist", ".txt").toFile();
        blockList = new BlockList(tmp);
        blockList.setText("# trong\n");
        log = new TrafficLog(100);
        stats = new ProxyStats();
        service = new ProxyService(blockList, log, stats);
        service.setMitm(new MitmCertManager(proxyCa, worldCa.trustingContext()));
    }

    @After
    public void tearDown() {
        service.stop();
        origin.stop(0);
        tmp.delete();
    }

    private ProxyConfig config() {
        ProxyConfig c = new ProxyConfig();
        c.port = proxyPort;
        return c;
    }

    private HttpsURLConnection open(String url, SSLContext trust) throws Exception {
        Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort));
        HttpsURLConnection c = (HttpsURLConnection) new URL(url).openConnection(p);
        c.setSSLSocketFactory(trust.getSocketFactory());
        c.setConnectTimeout(5000);
        c.setReadTimeout(10000);
        return c;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        return bo.toString("UTF-8");
    }

    @Test
    public void httpsPageIsDecryptedAndTheFullUrlIsLogged() throws Exception {
        service.start(config());
        String url = "https://localhost:" + originPort + "/secret/path?x=1";
        HttpsURLConnection c = open(url, proxyCa.trustingContext());
        assertEquals(200, c.getResponseCode());
        assertEquals("secret-page /secret/path?x=1", readAll(c.getInputStream()));
        Certificate[] seen = c.getServerCertificates();
        X509Certificate leaf = (X509Certificate) seen[0];
        assertTrue(leaf.getIssuerX500Principal().getName().contains("ProxyApp Local CA"));
        leaf.verify(proxyCa.certificate().getPublicKey());
        Thread.sleep(400);
        LogEntry get = null;
        LogEntry connect = null;
        for (LogEntry e : log.snapshot()) {
            if (e.method.equals("GET")) {
                get = e;
            }
            if (e.method.equals("CONNECT")) {
                connect = e;
            }
        }
        assertNotNull("không thấy dòng GET trong nhật ký", get);
        assertEquals(url, get.target);
        assertEquals("localhost", get.host);
        assertEquals(200, get.status);
        assertTrue(get.bytes > 0);
        assertNotNull(connect);
        assertEquals("localhost:" + originPort, connect.target);
        assertEquals(200, connect.status);
    }

    @Test
    public void everyRequestInsideTheSameTunnelIsLoggedWithItsOwnPath() throws Exception {
        service.start(config());
        SSLContext trust = proxyCa.trustingContext();
        for (String path : new String[]{"/a", "/b/c?q=2"}) {
            HttpsURLConnection c = open("https://localhost:" + originPort + path, trust);
            assertEquals(200, c.getResponseCode());
            readAll(c.getInputStream());
        }
        Thread.sleep(400);
        java.util.Set<String> targets = new java.util.HashSet<String>();
        for (LogEntry e : log.snapshot()) {
            if (e.method.equals("GET")) {
                targets.add(e.target);
            }
        }
        assertTrue(targets.toString(), targets.contains("https://localhost:" + originPort + "/a"));
        assertTrue(targets.contains("https://localhost:" + originPort + "/b/c?q=2"));
    }

    @Test
    public void clientThatDoesNotTrustTheProxyCaIsRejected() throws Exception {
        service.start(config());
        HttpsURLConnection c = open("https://localhost:" + originPort + "/", worldCa.trustingContext());
        try {
            c.getResponseCode();
            fail("phải bị từ chối vì chứng chỉ của proxy không được tin");
        } catch (SSLException expected) {
        }
    }

    @Test
    public void blockedHostNeverGetsATunnel() throws Exception {
        blockList.setText("localhost\n");
        service.start(config());
        HttpsURLConnection c = open("https://localhost:" + originPort + "/", proxyCa.trustingContext());
        try {
            c.getResponseCode();
            fail();
        } catch (java.io.IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("403"));
        }
        Thread.sleep(300);
        assertTrue(log.snapshot().get(0).blocked);
        assertEquals("CONNECT", log.snapshot().get(0).method);
    }

    @Test
    public void serverCertificateThatTheProxyDoesNotTrustGives502() throws Exception {
        service.stop();
        service = new ProxyService(blockList, log, stats);
        service.setMitm(new MitmCertManager(proxyCa));
        service.start(config());
        HttpsURLConnection c = open("https://localhost:" + originPort + "/", proxyCa.trustingContext());
        try {
            c.getResponseCode();
            fail("proxy phải từ chối máy chủ có chứng chỉ lạ");
        } catch (java.io.IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("502"));
        }
    }

    private UrlRules newRules(String text) throws Exception {
        File f = Files.createTempFile("urlrules", ".txt").toFile();
        f.deleteOnExit();
        UrlRules r = new UrlRules(f);
        r.setText(text);
        service.setUrlRules(r);
        return r;
    }

    @Test
    public void pathRuleBlocksInsideTheDecryptedTunnelButOtherPagesStayOpen() throws Exception {
        UrlRules rules = newRules("localhost path /cam\n");
        service.start(config());
        SSLContext trust = proxyCa.trustingContext();
        HttpsURLConnection ok = open("https://localhost:" + originPort + "/hoc-tap", trust);
        assertEquals(200, ok.getResponseCode());
        readAll(ok.getInputStream());
        HttpsURLConnection bad = open("https://localhost:" + originPort + "/cam/vao?x=1", trust);
        assertEquals(403, bad.getResponseCode());
        String page = readAll(bad.getErrorStream());
        assertTrue(page, page.contains("localhost path /cam"));
        rules.setText("# trong\n");
        HttpsURLConnection again = open("https://localhost:" + originPort + "/cam/vao", trust);
        assertEquals(200, again.getResponseCode());
        Thread.sleep(400);
        boolean blockedLogged = false;
        for (LogEntry e : log.snapshot()) {
            if (e.blocked && e.target.equals("https://localhost:" + originPort + "/cam/vao?x=1")) {
                blockedLogged = true;
            }
        }
        assertTrue(blockedLogged);
    }

    @Test
    public void keywordRuleBlocksInsideTheDecryptedTunnel() throws Exception {
        newRules("* keyword bimat\n");
        service.start(config());
        HttpsURLConnection bad = open("https://localhost:" + originPort + "/trang-BiMat/1", proxyCa.trustingContext());
        assertEquals(403, bad.getResponseCode());
        HttpsURLConnection ok = open("https://localhost:" + originPort + "/trang-thuong", proxyCa.trustingContext());
        assertEquals(200, ok.getResponseCode());
    }

    @Test
    public void bypassedSiteIsNotDecryptedAndTheClientSeesTheRealCertificate() throws Exception {
        MitmCertManager manager = new MitmCertManager(proxyCa, worldCa.trustingContext());
        BlockList bypass = new BlockList(null, "");
        bypass.setText("localhost\n");
        manager.setBypass(bypass);
        service.setMitm(manager);
        service.start(config());
        HttpsURLConnection c = open("https://localhost:" + originPort + "/rieng-tu", worldCa.trustingContext());
        assertEquals(200, c.getResponseCode());
        X509Certificate seen = (X509Certificate) c.getServerCertificates()[0];
        assertTrue(seen.getIssuerX500Principal().getName().contains("ProxyApp Local CA"));
        seen.verify(worldCa.certificate().getPublicKey());
        Thread.sleep(400);
        for (LogEntry e : log.snapshot()) {
            assertEquals("CONNECT", e.method);
        }
        assertEquals(1, log.snapshot().size());
        bypass.setText("# trong\n");
        HttpsURLConnection decrypted = open("https://localhost:" + originPort + "/rieng-tu", proxyCa.trustingContext());
        assertEquals(200, decrypted.getResponseCode());
        Thread.sleep(400);
        boolean sawGet = false;
        for (LogEntry e : log.snapshot()) {
            if (e.method.equals("GET")) {
                sawGet = true;
            }
        }
        assertTrue(sawGet);
    }

    @Test
    public void turningOnTheSettingCreatesTheCaFilesAndDecrypts() throws Exception {
        File dir = Files.createTempDirectory("mitm-auto").toFile();
        try {
            ProxyService auto = new ProxyService(blockList, log, stats);
            ProxyConfig c = config();
            c.mitmEnabled = true;
            c.caFile = new File(dir, "ca.p12").getPath();
            c.caCertFile = new File(dir, "ca.crt").getPath();
            auto.start(c);
            try {
                assertTrue(new File(c.caFile).isFile());
                assertTrue(new File(c.caCertFile).isFile());
                CertificateAuthority ca = CertificateAuthority.load(new File(c.caFile));
                java.security.cert.X509Certificate fromFile = (java.security.cert.X509Certificate) java.security.cert.CertificateFactory
                        .getInstance("X.509").generateCertificate(new java.io.FileInputStream(c.caCertFile));
                assertEquals(ca.certificate(), fromFile);
                Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort));
                HttpsURLConnection conn = (HttpsURLConnection) new URL("https://localhost:" + originPort + "/").openConnection(p);
                conn.setSSLSocketFactory(ca.trustingContext().getSocketFactory());
                try {
                    conn.getResponseCode();
                    fail("proxy mới không tin chứng chỉ của máy chủ thử nghiệm, phải trả 502");
                } catch (java.io.IOException expected) {
                    assertTrue(expected.getMessage(), expected.getMessage().contains("502"));
                }
            } finally {
                auto.stop();
            }
        } finally {
            for (File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }
    }

    @Test
    public void settingOffMeansNoDecryptionEvenAfterItWasOn() throws Exception {
        File dir = Files.createTempDirectory("mitm-off").toFile();
        try {
            ProxyService auto = new ProxyService(blockList, log, stats);
            ProxyConfig on = config();
            on.mitmEnabled = true;
            on.caFile = new File(dir, "ca.p12").getPath();
            on.caCertFile = new File(dir, "ca.crt").getPath();
            auto.start(on);
            auto.stop();
            ProxyConfig off = config();
            off.caFile = on.caFile;
            off.caCertFile = on.caCertFile;
            auto.start(off);
            try {
                Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort));
                HttpsURLConnection conn = (HttpsURLConnection) new URL("https://localhost:" + originPort + "/").openConnection(p);
                conn.setSSLSocketFactory(worldCa.trustingContext().getSocketFactory());
                assertEquals(200, conn.getResponseCode());
            } finally {
                auto.stop();
            }
        } finally {
            for (File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }
    }

    @Test
    public void clientThatGivesUpWhileTheProxyIsConnectingIsLogged499NotAsAServerError() throws Exception {
        service.start(config());
        String target = "localhost:" + originPort;
        for (int delay = 0; delay <= 90; delay += 6) {
            Socket s = new Socket("127.0.0.1", proxyPort);
            s.getOutputStream().write(("CONNECT " + target + " HTTP/1.1\r\nHost: " + target + "\r\n\r\n").getBytes());
            s.getOutputStream().flush();
            Thread.sleep(delay);
            s.setSoLinger(true, 0);
            s.close();
        }
        Thread.sleep(1500);
        int aborted = 0;
        for (LogEntry e : log.snapshot()) {
            assertFalse("kết nối bị máy khách bỏ giữa chừng không được ghi là lỗi 502: " + e.status, e.status == 502);
            if (e.status == 499) {
                aborted++;
            }
        }
        assertTrue("phải có kết nối bị bỏ giữa chừng được ghi 499", aborted >= 1);
    }

    @Test
    public void serviceTellsWhetherDecryptionIsActuallyRunning() throws Exception {
        assertFalse(service.isMitmActive());
        service.start(config());
        assertTrue(service.isMitmActive());
        service.stop();
        assertFalse(service.isMitmActive());
        ProxyService plain = new ProxyService(blockList, log, stats);
        plain.start(config());
        try {
            assertFalse(plain.isMitmActive());
        } finally {
            plain.stop();
        }
    }

    @Test
    public void serverCertificateForAnotherNameGives502() throws Exception {
        service.start(config());
        HttpsURLConnection c = open("https://127.0.0.1:" + originPort + "/", proxyCa.trustingContext());
        try {
            c.getResponseCode();
            fail("chứng chỉ chỉ có tên localhost, không được nhận 127.0.0.1");
        } catch (java.io.IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("502"));
        }
    }

    @Test
    public void loggedInUserIsKeptForRequestsInsideTheTunnel() throws Exception {
        ProxyConfig c = config();
        c.requireAuth = true;
        c.username = "sv";
        c.setPassword("123");
        service.start(c);
        String hp = "localhost:" + originPort;
        String login = java.util.Base64.getEncoder().encodeToString("sv:123".getBytes());
        Socket s = new Socket("127.0.0.1", proxyPort);
        s.setSoTimeout(10000);
        s.getOutputStream().write(("CONNECT " + hp + " HTTP/1.1\r\nHost: " + hp + "\r\nProxy-Authorization: Basic " + login
                + "\r\n\r\n").getBytes());
        s.getOutputStream().flush();
        StringBuilder head = new StringBuilder();
        while (!head.toString().endsWith("\r\n\r\n")) {
            int b = s.getInputStream().read();
            if (b < 0) {
                break;
            }
            head.append((char) b);
        }
        assertTrue(head.toString(), head.toString().startsWith("HTTP/1.1 200"));
        SSLSocket ssl = (SSLSocket) proxyCa.trustingContext().getSocketFactory().createSocket(s, "localhost", originPort, true);
        ssl.startHandshake();
        ssl.getOutputStream().write(("GET /inner HTTP/1.1\r\nHost: " + hp + "\r\nConnection: close\r\n\r\n").getBytes());
        ssl.getOutputStream().flush();
        StringBuilder text = new StringBuilder();
        byte[] buf = new byte[1024];
        while (!text.toString().contains("secret-page /inner")) {
            int n = ssl.getInputStream().read(buf);
            if (n < 0) {
                break;
            }
            text.append(new String(buf, 0, n));
        }
        ssl.close();
        assertTrue(text.toString(), text.toString().contains("secret-page /inner"));
        Thread.sleep(400);
        boolean innerSeen = false;
        for (LogEntry e : log.snapshot()) {
            if (e.method.equals("GET") && e.target.endsWith("/inner")) {
                assertEquals("sv", e.user);
                innerSeen = true;
            }
        }
        assertTrue(innerSeen);
    }
}
