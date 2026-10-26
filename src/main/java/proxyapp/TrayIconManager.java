package proxyapp;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class TrayIconManager {

    public interface Actions {
        void showWindow();

        void toggleProxy();

        void exitApp();
    }

    private TrayIcon icon;
    private MenuItem toggleItem;

    public static boolean isSupported() {
        return SystemTray.isSupported();
    }

    public static Image createImage() {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(33, 112, 196));
        g.fillOval(1, 1, size - 2, size - 2);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        int w = g.getFontMetrics().stringWidth("P");
        g.drawString("P", (size - w) / 2, 24);
        g.dispose();
        return img;
    }

    public boolean install(final Actions actions) {
        if (!isSupported()) {
            return false;
        }
        PopupMenu menu = new PopupMenu();
        MenuItem showItem = new MenuItem("Mở cửa sổ");
        toggleItem = new MenuItem("Bật proxy");
        MenuItem exitItem = new MenuItem("Thoát");
        showItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actions.showWindow();
            }
        });
        toggleItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actions.toggleProxy();
            }
        });
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actions.exitApp();
            }
        });
        menu.add(showItem);
        menu.add(toggleItem);
        menu.addSeparator();
        menu.add(exitItem);
        icon = new TrayIcon(createImage(), "Proxy Server", menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actions.showWindow();
            }
        });
        try {
            SystemTray.getSystemTray().add(icon);
        } catch (AWTException e) {
            icon = null;
            return false;
        }
        return true;
    }

    public void update(boolean running, String tooltip) {
        if (icon != null) {
            toggleItem.setLabel(running ? "Tắt proxy" : "Bật proxy");
            icon.setToolTip(tooltip);
        }
    }

    public void message(String title, String text) {
        if (icon != null) {
            icon.displayMessage(title, text, TrayIcon.MessageType.INFO);
        }
    }

    public void remove() {
        if (icon != null) {
            SystemTray.getSystemTray().remove(icon);
            icon = null;
        }
    }
}
