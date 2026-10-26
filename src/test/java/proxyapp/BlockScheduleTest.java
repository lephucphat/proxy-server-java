package proxyapp;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BlockScheduleTest {

    private static final LocalDateTime MON_10 = LocalDateTime.of(2026, 10, 26, 10, 0);

    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 10, 26, hour, minute).plusDays(day);
    }

    private BlockSchedule make(String text) throws Exception {
        File f = Files.createTempFile("sch", ".txt").toFile();
        f.deleteOnExit();
        BlockSchedule s = new BlockSchedule(f);
        s.setText(text);
        return s;
    }

    private static BlockList allow(String text) throws Exception {
        File f = Files.createTempFile("allow", ".txt").toFile();
        f.deleteOnExit();
        BlockList b = new BlockList(f, "");
        b.setText(text);
        return b;
    }

    @Test
    public void ruleBlocksOnlyInsideItsHoursAndDays() throws Exception {
        BlockSchedule s = make("facebook.com T2-T6 07:00-17:00\n");
        assertNotNull(s.blockingRule("facebook.com", at(0, 7, 0), null));
        assertNotNull(s.blockingRule("facebook.com", at(0, 16, 59), null));
        assertNull(s.blockingRule("facebook.com", at(0, 17, 0), null));
        assertNull(s.blockingRule("facebook.com", at(0, 6, 59), null));
        assertNotNull(s.blockingRule("facebook.com", at(4, 10, 0), null));
        assertNull(s.blockingRule("facebook.com", at(5, 10, 0), null));
        assertNull(s.blockingRule("facebook.com", at(6, 10, 0), null));
        assertNull(s.blockingRule("zalo.me", MON_10, null));
    }

    @Test
    public void subdomainsAreIncludedAndOtherNamesAreNot() throws Exception {
        BlockSchedule s = make("facebook.com * 00:00-24:00\n");
        assertNotNull(s.blockingRule("www.facebook.com", MON_10, null));
        assertNotNull(s.blockingRule("WWW.Facebook.COM", MON_10, null));
        assertNull(s.blockingRule("notfacebook.com", MON_10, null));
    }

    @Test
    public void overnightRuleCoversBothSidesOfMidnight() throws Exception {
        BlockSchedule s = make("youtube.com T2 22:00-06:00\n");
        assertNotNull(s.blockingRule("youtube.com", at(0, 22, 0), null));
        assertNotNull(s.blockingRule("youtube.com", at(0, 23, 59), null));
        assertNotNull(s.blockingRule("youtube.com", at(1, 5, 59), null));
        assertNull(s.blockingRule("youtube.com", at(1, 6, 0), null));
        assertNull(s.blockingRule("youtube.com", at(0, 5, 0), null));
        assertNull(s.blockingRule("youtube.com", at(1, 22, 30), null));
    }

    @Test
    public void dayListsRangesAndSundayWork() throws Exception {
        BlockSchedule s = make("a.com T2,T4,CN 00:00-24:00\nb.com T7-T2 00:00-24:00\nc.com cn 00:00-24:00\n");
        assertNotNull(s.blockingRule("a.com", at(0, 12, 0), null));
        assertNull(s.blockingRule("a.com", at(1, 12, 0), null));
        assertNotNull(s.blockingRule("a.com", at(2, 12, 0), null));
        assertNotNull(s.blockingRule("a.com", at(6, 12, 0), null));
        assertNotNull(s.blockingRule("b.com", at(5, 12, 0), null));
        assertNotNull(s.blockingRule("b.com", at(6, 12, 0), null));
        assertNotNull(s.blockingRule("b.com", at(0, 12, 0), null));
        assertNull(s.blockingRule("b.com", at(1, 12, 0), null));
        assertNotNull(s.blockingRule("c.com", at(6, 12, 0), null));
        assertNull(s.blockingRule("c.com", at(0, 12, 0), null));
    }

    @Test
    public void wildcardBlocksEverythingExceptAllowedSites() throws Exception {
        BlockSchedule s = make("* T2-T6 19:00-21:00\n");
        BlockList allowed = allow("hoc-tap.edu.vn\n");
        assertNotNull(s.blockingRule("game.com", at(0, 20, 0), allowed));
        assertNull(s.blockingRule("hoc-tap.edu.vn", at(0, 20, 0), allowed));
        assertNull(s.blockingRule("www.hoc-tap.edu.vn", at(0, 20, 0), allowed));
        assertNotNull(s.blockingRule("hoc-tap.edu.vn", at(0, 20, 0), null));
        assertNull(s.blockingRule("game.com", at(0, 21, 0), allowed));
    }

    @Test
    public void namedRuleIsNotCancelledByAllowList() throws Exception {
        BlockSchedule s = make("game.com * 00:00-24:00\n");
        assertNotNull(s.blockingRule("game.com", MON_10, allow("game.com\n")));
    }

    @Test
    public void commentsBlankLinesAndInlineNotesAreIgnored() throws Exception {
        BlockSchedule s = make("# ghi chu\n\n  a.com   *   08:00-09:00   # sang\n\tb.com\tT2\t10:00-11:00\n");
        assertEquals(2, s.size());
        assertNotNull(s.blockingRule("a.com", at(0, 8, 30), null));
        assertNotNull(s.blockingRule("b.com", at(0, 10, 30), null));
    }

    @Test
    public void badLinesAreRejectedWithTheLineNumber() throws Exception {
        String[] bad = {"a.com T2", "a.com T8 07:00-08:00", "a.com T2 7-8", "a.com T2 07:00-07:00",
            "a.com T2 25:00-26:00", "a.com T2 07:60-08:00", "a.com T2 24:00-05:00", "a.com T2-T9 07:00-08:00",
            "a.com T2 07:00-08:00 them", "# ok\nb.com * 07:00-08:00\nloi"};
        for (String line : bad) {
            try {
                make(line);
                fail("phải báo lỗi: " + line);
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().startsWith("Dòng "));
            }
        }
        try {
            make("# ok\nb.com * 07:00-08:00\nloi");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Dòng 3"));
        }
    }

    @Test
    public void failedSaveKeepsPreviousRulesAndFile() throws Exception {
        File f = Files.createTempFile("sch", ".txt").toFile();
        f.deleteOnExit();
        BlockSchedule s = new BlockSchedule(f);
        s.setText("a.com * 00:00-24:00\n");
        try {
            s.setText("hong hong");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(1, s.size());
        assertEquals("a.com * 00:00-24:00\n", new String(Files.readAllBytes(f.toPath()), "UTF-8"));
    }

    @Test
    public void loadIsLenientAndCreatesDefaultFile() throws Exception {
        File f = new File(System.getProperty("java.io.tmpdir"), "sch_missing_" + System.nanoTime() + ".txt");
        BlockSchedule s = new BlockSchedule(f);
        s.load();
        assertTrue(f.exists());
        assertEquals(0, s.size());
        Files.write(f.toPath(), "a.com * 00:00-24:00\nhong\nb.com T2 01:00-02:00\n".getBytes("UTF-8"));
        s.load();
        assertEquals(2, s.size());
        f.delete();
    }

    @Test
    public void ruleDescriptionShowsDaysAndHours() throws Exception {
        BlockSchedule s = make("a.com t2-t6 07:00-17:00\n");
        assertEquals("T2-T6 07:00-17:00", s.blockingRule("a.com", at(0, 8, 0), null).describe());
        assertFalse(s.blockingRule("a.com", at(0, 8, 0), null).allHosts());
    }
}
