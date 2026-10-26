package proxyapp;

import java.io.File;
import java.nio.file.Files;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockListTest {

    private BlockList make(String text) throws Exception {
        File f = Files.createTempFile("bl", ".txt").toFile();
        BlockList b = new BlockList(f);
        b.setText(text);
        f.deleteOnExit();
        return b;
    }

    @Test
    public void matchesDomainAndSubdomains() throws Exception {
        BlockList b = make("example.com\n");
        assertTrue(b.matches("example.com"));
        assertTrue(b.matches("www.example.com"));
        assertTrue(b.matches("A.B.Example.COM"));
        assertFalse(b.matches("notexample.com"));
        assertFalse(b.matches("example.org"));
    }

    @Test
    public void ignoresCommentsBlankLinesAndNormalizes() throws Exception {
        BlockList b = make("# ghi chu\n\n*.ads.net\nhttp://tracker.io/path\nsite.vn:443 # note\n");
        assertEquals(3, b.size());
        assertTrue(b.matches("x.ads.net"));
        assertTrue(b.matches("tracker.io"));
        assertTrue(b.matches("site.vn"));
    }

    @Test
    public void persistsToFileAndReloads() throws Exception {
        File f = Files.createTempFile("bl", ".txt").toFile();
        BlockList b = new BlockList(f);
        b.setText("saved.example\n");
        BlockList again = new BlockList(f);
        again.load();
        assertTrue(again.matches("saved.example"));
        f.delete();
    }

    @Test
    public void publicDomainsAreMatchedToo() throws Exception {
        BlockList b = make("mine.com\n");
        b.setPublicDomains(new java.util.HashSet<String>(java.util.Arrays.asList("pub.com")));
        assertTrue(b.matches("pub.com"));
        assertTrue(b.matches("x.pub.com"));
        assertTrue(b.matches("mine.com"));
        assertFalse(b.matches("other.com"));
        assertEquals(1, b.size());
        assertEquals(1, b.publicSize());
    }

    @Test
    public void allowListUsesItsOwnDefaultText() throws Exception {
        File f = new File(System.getProperty("java.io.tmpdir"), "al_missing_" + System.nanoTime() + ".txt");
        BlockList b = new BlockList(f, BlockList.ALLOW_DEFAULT_TEXT);
        b.load();
        assertTrue(f.exists());
        assertEquals(0, b.size());
        assertFalse(b.matches("doubleclick.net"));
        assertTrue(b.getText().contains("chỉ cho phép"));
        f.delete();
    }

    @Test
    public void createsDefaultFileWhenMissing() throws Exception {
        File f = new File(System.getProperty("java.io.tmpdir"), "bl_missing_" + System.nanoTime() + ".txt");
        BlockList b = new BlockList(f);
        b.load();
        assertTrue(f.exists());
        assertTrue(b.matches("doubleclick.net"));
        f.delete();
    }
}
