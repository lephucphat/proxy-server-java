package proxyapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HintsTest {

    @Test
    public void everyLogColumnHasAnExplanation() {
        assertEquals(10, Hints.LOG_COLUMNS.length);
        for (int i = 0; i < Hints.LOG_COLUMNS.length; i++) {
            assertNotNull(Hints.logColumn(i));
            assertFalse(Hints.logColumn(i).trim().isEmpty());
        }
        assertNull(Hints.logColumn(-1));
        assertNull(Hints.logColumn(10));
        assertTrue(Hints.logColumn(3).contains("CONNECT"));
    }

    @Test
    public void everyStatsColumnHasAnExplanation() {
        assertEquals(6, Hints.STATS_COLUMNS.length);
        for (int i = 0; i < Hints.STATS_COLUMNS.length; i++) {
            assertFalse(Hints.statsColumn(i).trim().isEmpty());
        }
        assertNull(Hints.statsColumn(6));
    }

    @Test
    public void statusCodesTextExplainsTheCommonCodes() {
        String text = Hints.codesText();
        for (String code : new String[]{"200", "304", "403", "407", "429", "502"}) {
            assertTrue(code, text.contains(code));
        }
    }

    @Test
    public void tabsTextNamesEveryTab() {
        String text = Hints.tabsText();
        for (String tab : new String[]{"TỔNG QUAN", "QUY TẮC", "NHẬT KÝ", "THỐNG KÊ", "CẢNH BÁO"}) {
            assertTrue(tab, text.contains(tab));
        }
    }
}
