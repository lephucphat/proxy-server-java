package proxyapp;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TrafficHistoryTest {

    @Test
    public void firstSampleIsZeroThenDifferencesAreStored() {
        TrafficHistory h = new TrafficHistory(5);
        h.sample(1000, 100);
        h.sample(1500, 130);
        h.sample(1500, 130);
        h.sample(4500, 330);
        long[][] d = h.snapshot();
        assertArrayEquals(new long[]{0, 500, 0, 3000}, d[0]);
        assertArrayEquals(new long[]{0, 30, 0, 200}, d[1]);
    }

    @Test
    public void oldSamplesFallOffAndOrderStaysOldestFirst() {
        TrafficHistory h = new TrafficHistory(3);
        long total = 0;
        for (int i = 1; i <= 6; i++) {
            total += i * 10;
            h.sample(total, 0);
        }
        long[][] d = h.snapshot();
        assertArrayEquals(new long[]{40, 50, 60}, d[0]);
        assertEquals(3, h.capacity());
    }

    @Test
    public void countersGoingBackToZeroAreNotNegative() {
        TrafficHistory h = new TrafficHistory(4);
        h.sample(5000, 500);
        h.sample(5200, 520);
        h.sample(80, 8);
        long[][] d = h.snapshot();
        assertArrayEquals(new long[]{0, 200, 80}, d[0]);
        assertArrayEquals(new long[]{0, 20, 8}, d[1]);
    }

    @Test
    public void clearForgetsEverything() {
        TrafficHistory h = new TrafficHistory(4);
        h.sample(100, 1);
        h.sample(200, 2);
        h.clear();
        assertEquals(0, h.snapshot()[0].length);
        h.sample(900, 9);
        assertArrayEquals(new long[]{0}, h.snapshot()[0]);
    }

    @Test
    public void emptyHistoryGivesEmptyArrays() {
        TrafficHistory h = new TrafficHistory(10);
        assertEquals(0, h.snapshot()[0].length);
        assertEquals(0, h.snapshot()[1].length);
    }
}
