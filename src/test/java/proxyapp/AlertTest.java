package proxyapp;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AlertTest {

    private long time = 1000000;

    private AlertMonitor monitor(AlertLog log, int threshold, long window) {
        return new AlertMonitor(log, threshold, window) {
            long now() {
                return time;
            }
        };
    }

    @Test
    public void logKeepsTheLastAlertsAndInformsListeners() {
        AlertLog log = new AlertLog(2);
        final List<String> heard = new ArrayList<String>();
        log.addListener(new AlertLog.Listener() {
            public void onAlert(AlertLog.Alert a) {
                heard.add(a.text);
            }
        });
        log.add("một");
        log.add("hai");
        log.add("ba");
        assertEquals(2, log.size());
        assertEquals("hai", log.snapshot().get(0).text);
        assertEquals("ba", log.snapshot().get(1).text);
        assertEquals(3, heard.size());
        assertTrue(log.snapshot().get(0).timeText().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        log.clear();
        assertEquals(0, log.size());
    }

    @Test
    public void tooManyBlockedRequestsInTheWindowRaiseOneAlert() {
        AlertLog log = new AlertLog(10);
        AlertMonitor m = monitor(log, 3, 60000);
        m.blocked("10.0.0.5", "an");
        m.blocked("10.0.0.5", "an");
        assertEquals(0, log.size());
        m.blocked("10.0.0.5", "an");
        assertEquals(1, log.size());
        assertTrue(log.snapshot().get(0).text, log.snapshot().get(0).text.contains("an (10.0.0.5)"));
        assertTrue(log.snapshot().get(0).text.contains("3 lần"));
        m.blocked("10.0.0.5", "an");
        m.blocked("10.0.0.5", "an");
        m.blocked("10.0.0.5", "an");
        assertEquals(1, log.size());
    }

    @Test
    public void alertCanComeAgainAfterTheWindowPasses() {
        AlertLog log = new AlertLog(10);
        AlertMonitor m = monitor(log, 2, 60000);
        m.blocked("10.0.0.5", "");
        m.blocked("10.0.0.5", "");
        assertEquals(1, log.size());
        time += 61000;
        m.blocked("10.0.0.5", "");
        m.blocked("10.0.0.5", "");
        assertEquals(2, log.size());
    }

    @Test
    public void slowBlocksOutsideTheWindowDoNotCount() {
        AlertLog log = new AlertLog(10);
        AlertMonitor m = monitor(log, 3, 60000);
        for (int i = 0; i < 10; i++) {
            m.blocked("10.0.0.5", "");
            time += 40000;
        }
        assertEquals(0, log.size());
    }

    @Test
    public void differentPeopleAreCountedSeparatelyAndNameBeatsAddress() {
        AlertLog log = new AlertLog(10);
        AlertMonitor m = monitor(log, 2, 60000);
        m.blocked("10.0.0.5", "an");
        m.blocked("10.0.0.5", "binh");
        m.blocked("10.0.0.6", "");
        assertEquals(0, log.size());
        m.blocked("10.0.0.6", "");
        assertEquals(1, log.size());
        assertTrue(log.snapshot().get(0).text.startsWith("10.0.0.6"));
    }

    @Test
    public void thresholdZeroTurnsBlockedAlertsOff() {
        AlertLog log = new AlertLog(10);
        AlertMonitor m = monitor(log, 0, 60000);
        for (int i = 0; i < 100; i++) {
            m.blocked("10.0.0.5", "");
        }
        assertEquals(0, log.size());
    }

    @Test
    public void lockAlertNamesTheAddressAndMinutes() {
        AlertLog log = new AlertLog(10);
        AlertMonitor m = monitor(log, 0, 0);
        m.locked("10.0.0.9", 5);
        assertEquals(1, log.size());
        assertTrue(log.snapshot().get(0).text.contains("10.0.0.9"));
        assertTrue(log.snapshot().get(0).text.contains("5 phút"));
    }

    @Test
    public void unknownMachineIsReportedOncePerHour() {
        AlertLog log = new AlertLog(10);
        AlertMonitor m = monitor(log, 0, 0);
        m.refusedClient("10.0.0.9");
        m.refusedClient("10.0.0.9");
        m.refusedClient("10.0.0.8");
        assertEquals(2, log.size());
        time += 3600001;
        m.refusedClient("10.0.0.9");
        assertEquals(3, log.size());
    }
}
