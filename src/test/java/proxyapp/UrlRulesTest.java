package proxyapp;

import java.io.File;
import java.nio.file.Files;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UrlRulesTest {

    private static UrlRules make(String text) throws Exception {
        File f = Files.createTempFile("urlrules", ".txt").toFile();
        f.deleteOnExit();
        UrlRules r = new UrlRules(f);
        r.setText(text);
        return r;
    }

    @Test
    public void pathRuleBlocksOnlyThatPartOfTheSite() throws Exception {
        UrlRules r = make("youtube.com path /shorts\n");
        assertNotNull(r.blockingRule("youtube.com", "/shorts/abc", false));
        assertNotNull(r.blockingRule("www.youtube.com", "/shorts?x=1", false));
        assertNotNull(r.blockingRule("youtube.com", "/SHORTS/abc", false));
        assertNull(r.blockingRule("youtube.com", "/watch?v=1", false));
        assertNull(r.blockingRule("youtube.com", "/", false));
        assertNull(r.blockingRule("vimeo.com", "/shorts/abc", false));
        assertNull(r.blockingRule("notyoutube.com", "/shorts/abc", false));
    }

    @Test
    public void pathRuleNeedsTheStartOfThePath() throws Exception {
        UrlRules r = make("a.com path /games\n");
        assertNull(r.blockingRule("a.com", "/home/games", false));
        assertNotNull(r.blockingRule("a.com", "/games", false));
        assertNotNull(r.blockingRule("a.com", "/gamesroom", false));
    }

    @Test
    public void keywordRuleLooksAtHostAndWholePath() throws Exception {
        UrlRules r = make("* keyword casino\n");
        assertNotNull(r.blockingRule("example.com", "/play/Casino-royal", false));
        assertNotNull(r.blockingRule("example.com", "/x?q=casino", false));
        assertNotNull(r.blockingRule("best-casino.net", "/", false));
        assertNull(r.blockingRule("example.com", "/hoc-tap", false));
    }

    @Test
    public void keywordOnASpecificSiteStaysOnThatSite() throws Exception {
        UrlRules r = make("reddit.com keyword nsfw\n");
        assertNotNull(r.blockingRule("www.reddit.com", "/r/nsfw", false));
        assertNull(r.blockingRule("example.com", "/r/nsfw", false));
        assertNull(r.blockingRule("reddit.com", "/r/hoc", false));
    }

    @Test
    public void tunnelsAreOnlyCheckedByHostNameForKeywords() throws Exception {
        UrlRules r = make("* keyword casino\n* keyword secret\nexample.com path /admin\n");
        assertNotNull(r.blockingRule("casino.example", "", true));
        assertNull(r.blockingRule("example.com", "/secret", true));
        assertNull(r.blockingRule("example.com", "", true));
    }

    @Test
    public void percentEncodingCannotHideAKeyword() throws Exception {
        UrlRules r = make("* keyword casino\na.com path /admin panel\n");
        assertNotNull(r.blockingRule("a.com", "/%63asino", false));
        assertNotNull(r.blockingRule("a.com", "/x?q=%43ASINO", false));
        assertNotNull(r.blockingRule("a.com", "/admin%20panel/x", false));
        assertNull(r.blockingRule("a.com", "/%zz", false));
    }

    @Test
    public void valuesMayContainSpacesAndRulesKeepTheirOrder() throws Exception {
        UrlRules r = make("* keyword xem phim lau\nb.com path /a\n* keyword a\n");
        assertEquals("* keyword xem phim lau", r.blockingRule("c.com", "/xem phim lau", false).describe());
        assertEquals("b.com path /a", r.blockingRule("b.com", "/a", false).describe());
    }

    @Test
    public void vietnameseWordsForTheKindAreAccepted() throws Exception {
        UrlRules r = make("a.com duongdan /cam\n* tukhoa xau\n");
        assertEquals(2, r.size());
        assertNotNull(r.blockingRule("a.com", "/cam/vao", false));
        assertNotNull(r.blockingRule("b.com", "/xau", false));
    }

    @Test
    public void commentsAndBlankLinesAreIgnored() throws Exception {
        UrlRules r = make("# ghi chu\n\n   \n* keyword x\n");
        assertEquals(1, r.size());
    }

    @Test
    public void badLinesAreRejectedWithTheLineNumberAndKeepOldRules() throws Exception {
        UrlRules r = make("* keyword giu-lai\n");
        String[] bad = {"a.com path", "a.com", "a.com loai x", "a.com path khong-co-gach", "a.com keyword   "};
        for (String line : bad) {
            try {
                r.setText("# ok\n" + line + "\n");
                fail("phải báo lỗi: " + line);
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().startsWith("Dòng 2"));
            }
        }
        assertEquals(1, r.size());
        assertNotNull(r.blockingRule("x.com", "/giu-lai", false));
    }

    @Test
    public void loadSkipsBadLinesAndCreatesTheDefaultFile() throws Exception {
        File f = new File(System.getProperty("java.io.tmpdir"), "ur_missing_" + System.nanoTime() + ".txt");
        UrlRules r = new UrlRules(f);
        r.load();
        assertTrue(f.exists());
        assertEquals(0, r.size());
        Files.write(f.toPath(), "* keyword a\nhong\nb.com path /b\n".getBytes("UTF-8"));
        r.load();
        assertEquals(2, r.size());
        f.delete();
    }

    @Test
    public void pathAndQueryIsTakenFromAnyKindOfAddress() {
        assertEquals("/a/b?x=1", ProxyService.pathAndQuery("https://h.com/a/b?x=1"));
        assertEquals("/a", ProxyService.pathAndQuery("http://h.com:8080/a"));
        assertEquals("/a", ProxyService.pathAndQuery("/a"));
        assertEquals("/?x=1", ProxyService.pathAndQuery("http://h.com?x=1"));
        assertEquals("/", ProxyService.pathAndQuery("http://h.com"));
    }
}
