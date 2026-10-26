package proxyapp;

import java.io.File;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SystemProxyTest {

    private String root;
    private String key;
    private File backup;
    private SystemProxy proxy;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(SystemProxy.isSupported());
        root = "HKCU\\Software\\ProxyAppTest_" + System.nanoTime();
        key = root + "\\Internet Settings";
        backup = new File(Files.createTempDirectory("sysproxy").toFile(), "backup.properties");
        proxy = new SystemProxy(key, backup, false);
    }

    @After
    public void tearDown() throws Exception {
        if (root != null) {
            reg("delete", root, "/f");
        }
        if (backup != null) {
            backup.delete();
            backup.getParentFile().delete();
        }
    }

    private static int reg(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "reg";
        System.arraycopy(args, 0, cmd, 1, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        while (p.getInputStream().read() >= 0) {
        }
        return p.waitFor();
    }

    private void seed(int enable, String server) throws Exception {
        reg("add", key, "/v", "ProxyEnable", "/t", "REG_DWORD", "/d", String.valueOf(enable), "/f");
        reg("add", key, "/v", "ProxyServer", "/t", "REG_SZ", "/d", server, "/f");
    }

    @Test
    public void enableSetsServerAndFlag() throws Exception {
        proxy.enable("127.0.0.1", 8080);
        assertEquals("127.0.0.1:8080", proxy.read("ProxyServer"));
        assertEquals("0x1", proxy.read("ProxyEnable"));
        assertTrue(proxy.isEnabled(8080));
        assertFalse(proxy.isEnabled(9999));
        assertTrue(proxy.hasBackup());
    }

    @Test
    public void restoreBringsBackPreviousSettings() throws Exception {
        seed(0, "cu.example:3128");
        proxy.enable("127.0.0.1", 8080);
        proxy.restore();
        assertEquals("cu.example:3128", proxy.read("ProxyServer"));
        assertEquals("0x0", proxy.read("ProxyEnable"));
        assertFalse(proxy.hasBackup());
        assertFalse(proxy.isEnabled(8080));
    }

    @Test
    public void restoreKeepsAnOldProxyThatWasOn() throws Exception {
        seed(1, "cong-ty.example:8000");
        proxy.enable("127.0.0.1", 8080);
        proxy.restore();
        assertEquals("cong-ty.example:8000", proxy.read("ProxyServer"));
        assertEquals("0x1", proxy.read("ProxyEnable"));
    }

    @Test
    public void restoreRemovesValuesThatDidNotExist() throws Exception {
        reg("add", key, "/v", "Khac", "/t", "REG_SZ", "/d", "x", "/f");
        proxy.enable("127.0.0.1", 8080);
        proxy.restore();
        assertNull(proxy.read("ProxyServer"));
        assertNull(proxy.read("ProxyEnable"));
        assertEquals("x", proxy.read("Khac"));
    }

    @Test
    public void enablingTwiceKeepsTheFirstBackup() throws Exception {
        seed(0, "goc.example:1");
        proxy.enable("127.0.0.1", 8080);
        proxy.enable("127.0.0.1", 9090);
        assertEquals("127.0.0.1:9090", proxy.read("ProxyServer"));
        proxy.restore();
        assertEquals("goc.example:1", proxy.read("ProxyServer"));
        assertEquals("0x0", proxy.read("ProxyEnable"));
    }

    @Test
    public void leftoverFromACrashIsRestoredByNewInstance() throws Exception {
        seed(0, "goc.example:1");
        proxy.enable("127.0.0.1", 8080);
        SystemProxy afterCrash = new SystemProxy(key, backup, false);
        afterCrash.restoreLeftover();
        assertEquals("goc.example:1", afterCrash.read("ProxyServer"));
        assertEquals("0x0", afterCrash.read("ProxyEnable"));
        assertFalse(afterCrash.hasBackup());
    }

    @Test
    public void restoreLeftoverDoesNothingWithoutBackup() throws Exception {
        seed(1, "127.0.0.1:8080");
        proxy.restoreLeftover();
        assertEquals("0x1", proxy.read("ProxyEnable"));
    }

    @Test
    public void restoreWithoutBackupTurnsOffOnlyLoopbackProxy() throws Exception {
        seed(1, "127.0.0.1:8080");
        proxy.restore();
        assertEquals("0x0", proxy.read("ProxyEnable"));
        assertEquals("127.0.0.1:8080", proxy.read("ProxyServer"));
        seed(1, "cong-ty.example:8000");
        proxy.restore();
        assertEquals("0x1", proxy.read("ProxyEnable"));
    }

    @Test
    public void missingKeyReadsAsNull() throws Exception {
        assertNull(proxy.read("ProxyServer"));
        assertFalse(proxy.isEnabled(8080));
    }

    @Test
    public void notifyingWindowsDoesNotFail() {
        new SystemProxy().notifyChange();
    }
}
