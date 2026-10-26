package proxyapp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrafficChartTest {

    private static BufferedImage paint(TrafficHistory history) {
        TrafficChart chart = new TrafficChart(history);
        chart.setSize(720, 170);
        BufferedImage img = new BufferedImage(720, 170, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        chart.paint(g);
        g.dispose();
        return img;
    }

    private static boolean hasColorNear(BufferedImage img, Color c) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Color p = new Color(img.getRGB(x, y));
                if (Math.abs(p.getRed() - c.getRed()) < 25 && Math.abs(p.getGreen() - c.getGreen()) < 25
                        && Math.abs(p.getBlue() - c.getBlue()) < 25) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void niceMaxPicksRoundCeilingAndNeverBelow1KB() {
        assertEquals(1024, TrafficChart.niceMax(0), 0.001);
        assertEquals(1024, TrafficChart.niceMax(500), 0.001);
        assertEquals(2000, TrafficChart.niceMax(1500), 0.001);
        assertEquals(5000, TrafficChart.niceMax(2001), 0.001);
        assertEquals(10000, TrafficChart.niceMax(7000), 0.001);
        assertEquals(200000, TrafficChart.niceMax(120000), 0.001);
        assertEquals(1000000, TrafficChart.niceMax(1000000), 0.001);
    }

    @Test
    public void rateIsShownInReadableUnits() {
        assertEquals("500 B/s", TrafficChart.rate(500));
        assertEquals("1.5 KB/s", TrafficChart.rate(1536));
        assertEquals("2.00 MB/s", TrafficChart.rate(2 * 1024 * 1024));
    }

    @Test
    public void emptyHistoryDrawsWithoutLinesAndWithoutErrors() {
        BufferedImage img = paint(new TrafficHistory(300));
        assertFalse(hasColorNear(img, TrafficChart.DOWN_COLOR));
        assertFalse(hasColorNear(img, TrafficChart.UP_COLOR));
    }

    @Test
    public void bothLinesAreDrawnWhenThereIsTraffic() {
        TrafficHistory h = new TrafficHistory(300);
        long down = 0;
        long up = 0;
        for (int i = 0; i < 60; i++) {
            down += 50000 + i * 1000;
            up += 20000;
            h.sample(down, up);
        }
        BufferedImage img = paint(h);
        assertTrue(hasColorNear(img, TrafficChart.DOWN_COLOR));
        assertTrue(hasColorNear(img, TrafficChart.UP_COLOR));
    }

    @Test
    public void tinyWindowIsHandled() {
        TrafficChart chart = new TrafficChart(new TrafficHistory(10));
        chart.setSize(30, 30);
        BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        chart.paint(g);
        g.dispose();
    }
}
