import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class GuiRobot {

    static Robot robot;
    static proxyapp.MainFrame frame;
    static int pass;
    static int fail;
    static final int PORT = 18600;

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
        } else {
            fail++;
        }
        System.out.println((ok ? "  [DAT]  " : "  [LOI]  ") + name + (detail == null || detail.isEmpty() ? "" : "  -> " + detail));
    }

    static Object field(Object o, String n) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
    }

    static <T> T edt(Callable<T> c) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return c.call();
        }
        FutureTask<T> t = new FutureTask<T>(c);
        SwingUtilities.invokeLater(t);
        return t.get(15, TimeUnit.SECONDS);
    }

    static void settle(int ms) throws Exception {
        Thread.sleep(ms);
    }

    static void click(Component c) throws Exception {
        Point p = edt(() -> {
            Point l = c.getLocationOnScreen();
            java.awt.Rectangle vr = c instanceof javax.swing.JComponent ? ((javax.swing.JComponent) c).getVisibleRect()
                    : new java.awt.Rectangle(0, 0, c.getWidth(), c.getHeight());
            return new Point(l.x + vr.x + vr.width / 2, l.y + vr.y + vr.height / 2);
        });
        robot.mouseMove(p.x, p.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        settle(350);
    }

    static void key(int vk) throws Exception {
        robot.keyPress(vk);
        robot.keyRelease(vk);
        settle(150);
    }

    static void chord(int mod, int vk) throws Exception {
        robot.keyPress(mod);
        robot.keyPress(vk);
        robot.keyRelease(vk);
        robot.keyRelease(mod);
        settle(200);
    }

    static void paste(String s) throws Exception {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
        settle(100);
        chord(KeyEvent.VK_CONTROL, KeyEvent.VK_V);
    }

    static void replaceText(Component field, String s) throws Exception {
        click(field);
        chord(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
        if (s.isEmpty()) {
            key(KeyEvent.VK_DELETE);
        } else {
            paste(s);
        }
    }

    static boolean waitFor(Callable<Boolean> cond, int ms) throws Exception {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            if (Boolean.TRUE.equals(edt(cond))) {
                return true;
            }
            Thread.sleep(100);
        }
        return Boolean.TRUE.equals(edt(cond));
    }

    static void collect(Container c, Class<?> type, List<Component> out) {
        for (Component k : c.getComponents()) {
            if (type.isInstance(k)) {
                out.add(k);
            }
            if (k instanceof Container) {
                collect((Container) k, type, out);
            }
        }
    }

    static List<Component> all(Container c, Class<?> type) throws Exception {
        return edt(() -> {
            List<Component> out = new ArrayList<Component>();
            collect(c, type, out);
            return out;
        });
    }

    static JDialog findDialog(String titlePart) throws Exception {
        return edt(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isShowing() && ((Dialog) w).getTitle() != null && ((Dialog) w).getTitle().contains(titlePart)) {
                    return (JDialog) w;
                }
            }
            return null;
        });
    }

    static JDialog waitDialog(String titlePart) throws Exception {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            JDialog d = findDialog(titlePart);
            if (d != null) {
                settle(300);
                return d;
            }
            Thread.sleep(100);
        }
        return null;
    }

    static JDialog findOptionDialog() throws Exception {
        return edt(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isShowing()) {
                    List<Component> found = new ArrayList<Component>();
                    collect((Container) w, JOptionPane.class, found);
                    if (!found.isEmpty()) {
                        return (JDialog) w;
                    }
                }
            }
            return null;
        });
    }

    static String optionText(JDialog d) throws Exception {
        return edt(() -> {
            List<Component> found = new ArrayList<Component>();
            collect(d, JOptionPane.class, found);
            JOptionPane p = (JOptionPane) found.get(0);
            Object m = p.getMessage();
            StringBuilder sb = new StringBuilder();
            if (m instanceof Object[]) {
                for (Object o : (Object[]) m) {
                    sb.append(o instanceof Component ? "[o nhap]" : String.valueOf(o)).append(' ');
                }
            } else {
                sb.append(m);
            }
            return sb.toString();
        });
    }

    static String waitOption() throws Exception {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            JDialog d = findOptionDialog();
            if (d != null) {
                settle(300);
                return optionText(d);
            }
            Thread.sleep(100);
        }
        return null;
    }

    static void pressOption(int index) throws Exception {
        JDialog d = findOptionDialog();
        List<Component> buttons = all(d, JButton.class);
        click(buttons.get(index));
        settle(300);
    }

    static JButton buttonWithText(Container c, String text) throws Exception {
        for (Component b : all(c, JButton.class)) {
            if (text.equals(edt(() -> ((JButton) b).getText()))) {
                return (JButton) b;
            }
        }
        return null;
    }

    static int httpStatus(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", PORT)));
        c.setConnectTimeout(5000);
        c.setReadTimeout(8000);
        return c.getResponseCode();
    }

    static String text(Object o, String field) throws Exception {
        Object c = field(o, field);
        if (c instanceof JLabel) {
            return edt(() -> ((JLabel) c).getText());
        }
        if (c instanceof JTextArea) {
            return edt(() -> ((JTextArea) c).getText());
        }
        if (c instanceof JTextField) {
            return edt(() -> ((JTextField) c).getText());
        }
        return String.valueOf(c);
    }

    static void dismissStrayDialogs() throws Exception {
        for (int i = 0; i < 3; i++) {
            JDialog d = findOptionDialog();
            if (d == null) {
                return;
            }
            check("Hop thoai thong bao bi ket (khong mong doi): " + optionText(d), false, "");
            List<Component> buttons = all(d, JButton.class);
            click(buttons.get(buttons.size() - 1));
        }
    }

    static void selectTab(String title) throws Exception {
        javax.swing.JTabbedPane tabs = (javax.swing.JTabbedPane) field(frame, "tabs");
        int index = edt(() -> tabs.indexOfTab(title));
        if (index < 0) {
            throw new IllegalStateException("khong co the " + title);
        }
        java.awt.Point p = edt(() -> {
            java.awt.Rectangle r = tabs.getBoundsAt(index);
            Point l = tabs.getLocationOnScreen();
            return new Point(l.x + r.x + r.width / 2, l.y + r.y + r.height / 2);
        });
        robot.mouseMove(p.x, p.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        settle(350);
        check("Bam vao the '" + title + "' -> the duoc chon", waitFor(() -> tabs.getSelectedIndex() == index, 3000), "");
    }

    static void startProxyByClick() throws Exception {
        click((JButton) field(frame, "btnStart"));
        check("Bam 'Bat proxy' -> trang thai 'dang chay'", waitFor(() -> {
            try {
                return text(frame, "lblStatus").contains("Đang chạy");
            } catch (Exception e) {
                return false;
            }
        }, 8000), text(frame, "lblStatus"));
    }

    static void stopProxyByClick() throws Exception {
        click((JButton) field(frame, "btnStop"));
        check("Bam 'Tat proxy' -> trang thai 'da dung'", waitFor(() -> {
            try {
                return text(frame, "lblStatus").contains("Đã dừng");
            } catch (Exception e) {
                return false;
            }
        }, 10000), text(frame, "lblStatus"));
    }

    public static void main(String[] args) throws Exception {
        File home = new File(args[0]).getAbsoluteFile();
        home.mkdirs();
        Files.write(new File(home, "proxy.properties").toPath(), ("port=" + PORT + "\nlogToFile=false\n").getBytes(StandardCharsets.US_ASCII));
        proxyapp.AppDir.set(home);
        robot = new Robot();
        robot.setAutoDelay(30);
        edt(() -> {
            proxyapp.Theme.install();
            frame = new proxyapp.MainFrame();
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            return null;
        });
        settle(1500);
        try {
            Scenarios.run(home);
        } catch (Throwable t) {
            fail++;
            System.out.println("  [LOI]  ngoai le: " + t);
            t.printStackTrace(System.out);
        }
        System.out.println("TONG KET: " + pass + " dat, " + fail + " loi");
        edt(() -> {
            try {
                java.lang.reflect.Method m = frame.getClass().getDeclaredMethod("exitApp");
                m.setAccessible(true);
                m.invoke(frame);
            } catch (Exception e) {
            }
            return null;
        });
        System.exit(fail == 0 ? 0 : 1);
    }
}
