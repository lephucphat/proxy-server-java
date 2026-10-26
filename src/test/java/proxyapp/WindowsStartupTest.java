package proxyapp;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WindowsStartupTest {

    private String root;
    private WindowsStartup startup;

    @Before
    public void setUp() {
        Assume.assumeTrue(SystemProxy.isSupported());
        root = "HKCU\\Software\\ProxyAppTest_" + System.nanoTime();
        startup = new WindowsStartup(root + "\\Run", "ProxyApp");
    }

    @After
    public void tearDown() throws Exception {
        if (root != null) {
            Registry.deleteKey(root);
        }
    }

    @Test
    public void enableWritesTheExactCommandAndDisableRemovesIt() throws Exception {
        String command = "\"C:\\Program Files\\Java\\jdk 23\\bin\\javaw.exe\" -jar \"D:\\learning java\\x.jar\""
                + " --home \"D:\\learning java\" --minimized";
        assertFalse(startup.isEnabled());
        startup.enable(command);
        assertTrue(startup.isEnabled());
        assertEquals(command, startup.getCommand());
        startup.disable();
        assertFalse(startup.isEnabled());
        assertNull(startup.getCommand());
    }

    @Test
    public void enablingAgainReplacesTheCommand() throws Exception {
        startup.enable("lenh-cu");
        startup.enable("lenh-moi");
        assertEquals("lenh-moi", startup.getCommand());
    }

    @Test
    public void disableWhenNothingIsSetDoesNotFail() throws Exception {
        startup.disable();
        assertFalse(startup.isEnabled());
    }

    @Test
    public void commandHasJavawJarHomeAndMinimizedFlag() {
        String c = WindowsStartup.commandFor(new File("D:\\app dir\\proxy.jar"), new File("C:\\jdk 23"), new File("D:\\du lieu"));
        assertEquals("\"C:\\jdk 23\\bin\\javaw.exe\" -jar \"D:\\app dir\\proxy.jar\" --home \"D:\\du lieu\" --minimized", c);
    }

    @Test
    public void packagedAppUsesItsOwnExeInTheCommand() throws Exception {
        assertEquals("\"C:\\Users\\sv\\ProxyServer\\ProxyServer.exe\" --home \"C:\\Users\\sv\\AppData\\Roaming\\ProxyServer\" --minimized",
                WindowsStartup.commandForExe(new File("C:\\Users\\sv\\ProxyServer\\ProxyServer.exe"),
                        new File("C:\\Users\\sv\\AppData\\Roaming\\ProxyServer")));
        String old = System.getProperty("jpackage.app-path");
        System.setProperty("jpackage.app-path", "D:\\Chuong trinh\\ProxyServer.exe");
        try {
            String c = WindowsStartup.commandForThisApp();
            assertTrue(c, c.startsWith("\"D:\\Chuong trinh\\ProxyServer.exe\" --home \""));
            assertTrue(c, c.endsWith("--minimized"));
            assertFalse(c, c.contains("javaw"));
        } finally {
            if (old == null) {
                System.clearProperty("jpackage.app-path");
            } else {
                System.setProperty("jpackage.app-path", old);
            }
        }
    }

    @Test
    public void commandForThisAppNeedsAJarFile() {
        try {
            WindowsStartup.commandForThisApp();
            fail("chạy từ thư mục classes thì không có file .jar");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(".jar"));
        }
    }
}
