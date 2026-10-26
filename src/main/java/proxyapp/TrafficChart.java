package proxyapp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

public class TrafficChart extends javax.swing.JPanel {

    static final Color DOWN_COLOR = new Color(33, 112, 196);
    static final Color UP_COLOR = new Color(230, 126, 34);

    private final TrafficHistory history;

    public TrafficChart(TrafficHistory history) {
        this.history = history;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(720, 170));
    }

    static double niceMax(double value) {
        double min = 1024;
        if (value <= min) {
            return min;
        }
        double power = Math.pow(10, Math.floor(Math.log10(value)));
        double[] steps = {1, 2, 5, 10};
        for (double s : steps) {
            if (s * power >= value) {
                return s * power;
            }
        }
        return 10 * power;
    }

    static String rate(double bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return String.format("%.0f B/s", bytesPerSecond);
        }
        if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024);
        }
        return String.format("%.2f MB/s", bytesPerSecond / 1024 / 1024);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 70;
        int right = 12;
        int top = 22;
        int bottom = 22;
        int w = getWidth() - left - right;
        int h = getHeight() - top - bottom;
        if (w < 20 || h < 20) {
            g.dispose();
            return;
        }
        long[][] data = history.snapshot();
        long[] down = data[0];
        long[] up = data[1];
        int n = down.length;
        double max = 0;
        for (int i = 0; i < n; i++) {
            max = Math.max(max, Math.max(down[i], up[i]));
        }
        max = niceMax(max);

        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(225, 225, 225));
        for (int k = 0; k <= 4; k++) {
            int y = top + h - h * k / 4;
            g.drawLine(left, y, left + w, y);
            String label = rate(max * k / 4);
            g.setColor(Color.DARK_GRAY);
            g.drawString(label, left - 6 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
            g.setColor(new Color(225, 225, 225));
        }
        g.setColor(Color.GRAY);
        g.drawRect(left, top, w, h);
        int cap = history.capacity();
        String start = "-" + (cap >= 120 ? (cap / 60) + " phút" : cap + " giây");
        g.setColor(Color.DARK_GRAY);
        g.drawString(start, left, top + h + fm.getAscent() + 3);
        String now = "bây giờ";
        g.drawString(now, left + w - fm.stringWidth(now), top + h + fm.getAscent() + 3);

        if (n == 0) {
            String text = "Chưa có dữ liệu";
            g.drawString(text, left + (w - fm.stringWidth(text)) / 2, top + h / 2);
            g.dispose();
            return;
        }
        double step = cap > 1 ? (double) w / (cap - 1) : w;
        drawSeries(g, down, max, left, top, w, h, step, DOWN_COLOR, true);
        drawSeries(g, up, max, left, top, w, h, step, UP_COLOR, false);

        String downText = "Tải xuống " + rate(down[n - 1]);
        String upText = "Tải lên " + rate(up[n - 1]);
        int gap = 24;
        int total = 14 + fm.stringWidth(downText) + gap + 14 + fm.stringWidth(upText);
        int lx = left + w - total;
        g.setColor(DOWN_COLOR);
        g.fillRect(lx, 8, 10, 8);
        g.setColor(Color.DARK_GRAY);
        g.drawString(downText, lx + 14, 15);
        int ux = lx + 14 + fm.stringWidth(downText) + gap;
        g.setColor(UP_COLOR);
        g.fillRect(ux, 8, 10, 8);
        g.setColor(Color.DARK_GRAY);
        g.drawString(upText, ux + 14, 15);
        g.dispose();
    }

    private static void drawSeries(Graphics2D g, long[] values, double max, int left, int top, int w, int h,
            double step, Color color, boolean fill) {
        int n = values.length;
        Path2D.Double line = new Path2D.Double();
        double firstX = 0;
        double lastX = 0;
        for (int i = 0; i < n; i++) {
            double x = left + w - (n - 1 - i) * step;
            double y = top + h - values[i] / max * h;
            if (i == 0) {
                line.moveTo(x, y);
                firstX = x;
            } else {
                line.lineTo(x, y);
            }
            lastX = x;
        }
        if (fill) {
            Path2D.Double area = new Path2D.Double(line);
            area.lineTo(lastX, top + h);
            area.lineTo(firstX, top + h);
            area.closePath();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            g.fill(area);
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(line);
        g.setStroke(new BasicStroke(1f));
    }
}
