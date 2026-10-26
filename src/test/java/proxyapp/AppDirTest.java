package proxyapp;

import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppDirTest {

    private File original;

    @Before
    public void setUp() {
        original = AppDir.get();
    }

    @After
    public void tearDown() {
        AppDir.set(original);
    }

    @Test
    public void relativeNamesAreResolvedAgainstTheAppFolder() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "appdir-test");
        AppDir.set(dir);
        assertEquals(new File(dir.getAbsoluteFile(), "proxy.properties"), AppDir.file("proxy.properties"));
    }

    @Test
    public void absolutePathsAreKept() {
        AppDir.set(new File(System.getProperty("java.io.tmpdir"), "appdir-test"));
        File abs = new File(System.getProperty("java.io.tmpdir"), "x.txt").getAbsoluteFile();
        assertEquals(abs, AppDir.file(abs.getPath()));
    }

    @Test
    public void packagedAppKeepsItsDataInAppDataNotNextToTheProgram() throws Exception {
        File appData = java.nio.file.Files.createTempDirectory("appdata").toFile();
        try {
            File dir = AppDir.defaultBase("C:\\Program Files\\ProxyServer\\ProxyServer.exe", appData.getPath(), "C:\\Users\\sv");
            assertEquals(new File(appData, "ProxyServer").getAbsoluteFile(), dir);
            assertFalse("chưa dùng thì chưa tạo thư mục", dir.exists());
            AppDir.set(dir);
            assertEquals(new File(dir, "proxy.properties"), AppDir.file("proxy.properties"));
            assertTrue(dir.isDirectory());
            File fallback = AppDir.defaultBase("C:\\x\\a.exe", null, appData.getPath());
            assertEquals(new File(appData, "ProxyServer").getAbsoluteFile(), fallback);
        } finally {
            new File(appData, "ProxyServer").delete();
            appData.delete();
        }
    }

    @Test
    public void notPackagedMeansTheCurrentFolder() {
        File dir = AppDir.defaultBase(null, "C:\\AppData", "C:\\Users\\sv");
        assertEquals(new File(".").getAbsoluteFile().getParentFile(), dir.getParentFile());
        assertEquals(new File(".").getAbsoluteFile().getParentFile(), AppDir.defaultBase("", "C:\\AppData", null).getParentFile());
    }

    @Test
    public void defaultIsTheCurrentFolder() {
        assertTrue(original.isAbsolute());
        assertEquals(new File(".").getAbsoluteFile().getParentFile(), original.getParentFile());
    }
}
