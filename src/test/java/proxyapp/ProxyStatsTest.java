package proxyapp;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProxyStatsTest {

    @Test
    public void countsPerClientAndPerUser() {
        ProxyStats s = new ProxyStats();
        s.requestBy("10.0.0.5", "an");
        s.requestBy("10.0.0.5", "an");
        s.requestBy("10.0.0.6", "binh");
        s.blockedBy("10.0.0.5", "an");
        s.authFailureBy("10.0.0.7", "");
        s.bytesBy("10.0.0.5", "an", 1000, 50);
        s.bytesBy("10.0.0.5", "an", 500, 0);
        List<ProxyStats.Row> clients = s.clientRows();
        assertEquals(3, clients.size());
        assertEquals("10.0.0.5", clients.get(0).name);
        assertEquals(2, clients.get(0).requests);
        assertEquals(1, clients.get(0).blocked);
        assertEquals(1500, clients.get(0).down);
        assertEquals(50, clients.get(0).up);
        List<ProxyStats.Row> users = s.userRows();
        assertEquals(2, users.size());
        assertEquals("an", users.get(0).name);
        assertEquals(2, users.get(0).requests);
        assertEquals(1500, users.get(0).down);
    }

    @Test
    public void emptyOrMissingUserDoesNotCreateAUserRow() {
        ProxyStats s = new ProxyStats();
        s.requestBy("10.0.0.5", "");
        s.requestBy("10.0.0.5", null);
        s.bytesBy("10.0.0.5", null, 10, 10);
        s.blockedBy("10.0.0.5", "");
        s.authFailureBy("10.0.0.5", "");
        assertEquals(0, s.userRows().size());
        assertEquals(1, s.clientRows().size());
        assertEquals(2, s.clientRows().get(0).requests);
    }

    @Test
    public void failedLoginIsCountedPerClientAndPerRealUser() {
        ProxyStats s = new ProxyStats();
        s.authFailureBy("10.0.0.5", "an");
        s.authFailureBy("10.0.0.5", "");
        assertEquals(2, s.clientRows().get(0).authFailures);
        assertEquals(1, s.userRows().get(0).authFailures);
    }

    @Test
    public void rowsAreSortedByTrafficThenRequestsThenName() {
        ProxyStats s = new ProxyStats();
        s.requestBy("b", "");
        s.requestBy("a", "");
        s.requestBy("a", "");
        s.requestBy("c", "");
        s.bytesBy("c", "", 100, 0);
        List<ProxyStats.Row> rows = s.clientRows();
        assertEquals("c", rows.get(0).name);
        assertEquals("a", rows.get(1).name);
        assertEquals("b", rows.get(2).name);
    }

    @Test
    public void resetClearsEverythingIncludingRows() {
        ProxyStats s = new ProxyStats();
        s.request("a.com");
        s.requestBy("10.0.0.5", "an");
        s.reset();
        assertEquals(0, s.getRequests());
        assertTrue(s.clientRows().isEmpty());
        assertTrue(s.userRows().isEmpty());
    }

    @Test
    public void tooManyDifferentKeysAreIgnoredInsteadOfGrowingForever() {
        ProxyStats s = new ProxyStats();
        for (int i = 0; i < 6000; i++) {
            s.requestBy("10.0." + (i / 250) + "." + (i % 250), "");
        }
        assertEquals(5000, s.clientRows().size());
        s.requestBy("10.0.0.0", "");
        assertEquals(2, s.clientRows().stream().filter(r -> r.name.equals("10.0.0.0")).findFirst().get().requests);
    }
}
