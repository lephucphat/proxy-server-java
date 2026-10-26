import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class Scenarios extends GuiRobot {

    static void shot(Component c, File file) throws Exception {
        Rectangle r = edt(() -> {
            java.awt.Point p = c.getLocationOnScreen();
            return new Rectangle(p.x, p.y, c.getWidth(), c.getHeight());
        });
        ImageIO.write(new java.awt.Robot().createScreenCapture(r), "png", file);
    }

    static boolean selected(Object o, String f) throws Exception {
        JCheckBox c = (JCheckBox) field(o, f);
        return edt(() -> c.isSelected());
    }

    static void closeDialog(JDialog d) throws Exception {
        click(buttonWithText(d, "Đóng"));
        check("Dong hop thoai '" + edt(() -> d.getTitle()) + "'", waitFor(() -> !d.isShowing(), 4000), "");
    }

    static void resize(int w, int h) throws Exception {
        edt(() -> {
            frame.setSize(w, h);
            frame.validate();
            return null;
        });
        settle(500);
    }

    static boolean fullyShown(Component c) throws Exception {
        return edt(() -> c.isShowing() && c.getWidth() > 0 && c.getHeight() > 0
                && frame.getBounds().contains(c.getLocationOnScreen().x + c.getWidth() - 1, c.getLocationOnScreen().y + c.getHeight() - 1));
    }

    static void run(File home) throws Exception {
        File shots = new File(home, "shots");
        shots.mkdirs();

        System.out.println("== 1. Bat/tat proxy va nhat ky ==");
        selectTab("Tổng quan");
        shot(frame, new File(shots, "01-tong-quan.png"));
        startProxyByClick();
        check("Nut 'Tat proxy' sang, nut 'Bat proxy' mo", edt(() -> {
            try {
                return ((JButton) field(frame, "btnStop")).isEnabled() && !((JButton) field(frame, "btnStart")).isEnabled();
            } catch (Exception e) {
                return false;
            }
        }), "");
        check("Proxy tra 403 cho trang trong danh sach chan mac dinh", httpStatus("http://doubleclick.net/") == 403, "");
        DefaultTableModel model = (DefaultTableModel) field(frame, "logModel");
        check("Nhat ky hien dong moi tren giao dien", waitFor(() -> model.getRowCount() >= 1, 5000), "");
        String blockedCell = edt(() -> String.valueOf(model.getValueAt(model.getRowCount() - 1, 9)));
        String hostCell = edt(() -> String.valueOf(model.getValueAt(model.getRowCount() - 1, 4)));
        check("Dong nhat ky ghi dung ten mien va danh dau 'Co' o cot Chan", hostCell.equals("doubleclick.net") && blockedCell.equals("Có"), hostCell + " / " + blockedCell);
        check("Thanh trang thai: 'Bi chan: 1' va 'Yeu cau: 1'", waitFor(() -> {
            try {
                return text(frame, "lblBlocked").contains("Bị chặn: 1") && text(frame, "lblReq").contains("Yêu cầu: 1");
            } catch (Exception e) {
                return false;
            }
        }, 4000), text(frame, "lblBlocked") + " / " + text(frame, "lblReq"));
        shot(frame, new File(shots, "02-dang-chay.png"));
        stopProxyByClick();

        System.out.println("== 2. Kiem tra nhap sai: bat LAN khong dang nhap ==");
        click((JCheckBox) field(frame, "chkLan"));
        check("Tich 'Cho phep LAN'", selected(frame, "chkLan"), "");
        click((JButton) field(frame, "btnStart"));
        String msg = waitOption();
        check("Hien hop thoai loi ve LAN/dang nhap", msg != null && msg.contains("LAN"), msg);
        if (msg != null) {
            pressOption(0);
        }
        click((JCheckBox) field(frame, "chkLan"));
        check("Bo tich LAN", !selected(frame, "chkLan"), "");

        System.out.println("== 3. Kiem tra nhap sai: IP duoc phep khong hop le ==");
        replaceText((JTextField) field(frame, "txtAllow"), "abc");
        click((JButton) field(frame, "btnStart"));
        msg = waitOption();
        check("Hien loi 'khong hop le' cho o IP", msg != null && msg.contains("không hợp lệ"), msg);
        if (msg != null) {
            pressOption(0);
        }
        replaceText((JTextField) field(frame, "txtAllow"), "");
        check("Proxy van dang tat sau khi bao loi", text(frame, "lblStatus").contains("Đã dừng"), text(frame, "lblStatus"));

        System.out.println("== 4. Sua danh sach chan khi proxy dang chay ==");
        startProxyByClick();
        selectTab("Quy tắc");
        shot(frame, new File(shots, "03-quy-tac.png"));
        click((Component) field(frame, "txtBlock"));
        chord(KeyEvent.VK_CONTROL, KeyEvent.VK_END);
        paste("\nvi-du-chan.test\n");
        click((JButton) field(frame, "btnSaveBlock"));
        msg = waitOption();
        check("Luu danh sach chan -> thong bao 'Da luu'", msg != null && msg.contains("Đã lưu"), msg);
        if (msg != null) {
            pressOption(0);
        }
        check("Ten mien moi bi chan ngay, khong can tat proxy", httpStatus("http://vi-du-chan.test/") == 403, "");

        System.out.println("== 5. Hop thoai danh sach chan cong khai ==");
        File pub = new File(home, "nguon-cong-khai.txt");
        Files.write(pub.toPath(), "0.0.0.0 pub-chan.test\n# ghi chu\n".getBytes(StandardCharsets.UTF_8));
        click((JButton) field(frame, "btnPublic"));
        final JDialog pd = waitDialog("công khai");
        check("Mo hop thoai danh sach chan cong khai", pd != null, "");
        if (pd != null) {
            click((JButton) field(pd, "btnSuggest"));
            String sources = text(pd, "txtSources");
            check("Nut 'Dien nguon goi y' dien 2 dia chi", sources.contains("StevenBlack") && sources.contains("urlhaus"), sources);
            replaceText((Component) field(pd, "txtSources"), pub.getAbsolutePath());
            click((JButton) field(pd, "btnUpdate"));
            check("Cap nhat tu file -> thong tin '1 ten mien'", waitFor(() -> {
                try {
                    return text(pd, "lblInfo").contains("Đang dùng 1 ");
                } catch (Exception e) {
                    return false;
                }
            }, 8000), text(pd, "lblInfo"));
            check("Ket qua liet ke nguon da doc", text(pd, "txtResult").contains("1 tên miền"), text(pd, "txtResult"));
            shot(pd, new File(shots, "04-cong-khai.png"));
            closeDialog(pd);
            check("Ten mien tu danh sach cong khai bi chan", httpStatus("http://pub-chan.test/") == 403, "");
        }

        System.out.println("== 6. Hop thoai chi cho phep va lich chan ==");
        click((JButton) field(frame, "btnRules"));
        JDialog d = waitDialog("lịch chặn");
        check("Mo hop thoai chi cho phep va lich", d != null, "");
        if (d != null) {
            click((JCheckBox) field(d, "chkAllowOnly"));
            click((JButton) field(d, "btnSave"));
            msg = waitOption();
            check("Bat 'chi cho phep' khi danh sach trong -> hoi xac nhan", msg != null && msg.contains("trống"), msg);
            if (msg != null) {
                pressOption(1);
            }
            replaceText((Component) field(d, "txtAllowList"), "vi-du-cho-phep.test");
            replaceText((Component) field(d, "txtSchedule"), "hong hong");
            click((JCheckBox) field(d, "chkAllowOnly"));
            click((JButton) field(d, "btnSave"));
            msg = waitOption();
            check("Lich sai dinh dang -> bao loi co so dong", msg != null && msg.contains("Dòng 1"), msg);
            if (msg != null) {
                pressOption(0);
            }
            replaceText((Component) field(d, "txtSchedule"), "game.test * 00:00-24:00");
            click((JButton) field(d, "btnSave"));
            msg = waitOption();
            check("Luu lich dung -> 'Da luu ... 1 dong lich'", msg != null && msg.contains("1 dòng lịch"), msg);
            if (msg != null) {
                pressOption(0);
            }
            shot(d, new File(shots, "05-lich.png"));
            closeDialog(d);
            check("Ten mien trong lich bi chan ngay", httpStatus("http://game.test/") == 403, "");
        }

        System.out.println("== 7. Hop thoai tai khoan khac ==");
        selectTab("Tổng quan");
        click((JButton) field(frame, "btnUsers"));
        d = waitDialog("Tài khoản khác");
        check("Mo hop thoai tai khoan khac", d != null, "");
        if (d != null) {
            click((JButton) field(d, "btnAdd"));
            msg = waitOption();
            check("Hop thoai hoi ten tai khoan", msg != null && msg.contains("Tên tài khoản"), msg);
            JDialog input = findOptionDialog();
            if (input != null) {
                Component tf = all(input, JTextField.class).get(0);
                click(tf);
                paste("be");
                pressOption(0);
            }
            msg = waitOption();
            check("Hop thoai hoi mat khau", msg != null && msg.contains("Mật khẩu cho be"), msg);
            input = findOptionDialog();
            if (input != null) {
                Component pf = all(input, JTextField.class).get(0);
                click(pf);
                paste("matkhau1");
                pressOption(0);
            }
            final JDialog ud = d;
            JList<?> list = (JList<?>) field(ud, "lstUsers");
            check("Tai khoan 'be' xuat hien trong danh sach", waitFor(() -> list.getModel().getSize() == 1, 5000), "");
            edt(() -> {
                list.setSelectedIndex(0);
                return null;
            });
            settle(400);
            check("Chon tai khoan -> nhan 'Luat rieng cua: be'", text(ud, "lblSelected").contains("be"), text(ud, "lblSelected"));
            click((JCheckBox) field(ud, "chkOwnRules"));
            click((JCheckBox) field(ud, "chkAllowOnly"));
            replaceText((Component) field(ud, "txtAllowList"), "hoc.test");
            click((JButton) field(ud, "btnSaveRules"));
            msg = waitOption();
            check("Luu luat rieng -> thong bao", msg != null && msg.contains("Đã lưu luật của be"), msg);
            if (msg != null) {
                pressOption(0);
            }
            shot(ud, new File(shots, "06-tai-khoan.png"));
            closeDialog(ud);
            String users = new String(Files.readAllBytes(new File(home, "users.txt").toPath()), StandardCharsets.UTF_8);
            check("File users.txt co 'be', luat rieng bat, mat khau da bam", users.startsWith("be\tpbkdf2$") && users.trim().endsWith("\t1\t1") && !users.contains("matkhau1"), users.trim().substring(0, 20));
        }

        System.out.println("== 8. The Nhat ky: loc ==");
        selectTab("Nhật ký");
        int total = edt(() -> model.getRowCount());
        JTable log = (JTable) field(frame, "tblLog");
        check("Bang nhat ky co du lieu (>= 4 dong)", total >= 4, total + " dong");
        check("Chua loc thi hien du so dong", edt(() -> log.getRowCount()) == total, "");
        replaceText((Component) field(frame, "txtFilter"), "doubleclick");
        int shown = edt(() -> log.getRowCount());
        check("Go 'doubleclick' vao o Loc -> chi con dong khop", shown >= 1 && shown < total, shown + " / " + total);
        replaceText((Component) field(frame, "txtFilter"), "khong-co-trang-nay-dau");
        check("Loc khong khop -> bang trong", edt(() -> log.getRowCount()) == 0, "");
        shot(frame, new File(shots, "07-nhat-ky-loc.png"));
        replaceText((Component) field(frame, "txtFilter"), "");
        check("Xoa o Loc -> hien lai tat ca", edt(() -> log.getRowCount()) == total, "");
        click((JCheckBox) field(frame, "chkBlockedOnly"));
        check("Tich 'Chi hien dong bi chan' -> chi dong co 'Co'", edt(() -> {
            for (int i = 0; i < log.getRowCount(); i++) {
                if (!"Có".equals(String.valueOf(log.getValueAt(i, 9)))) {
                    return false;
                }
            }
            return log.getRowCount() >= 1;
        }), "");
        click((JCheckBox) field(frame, "chkBlockedOnly"));

        System.out.println("== 9. The Thong ke ==");
        selectTab("Thống kê");
        JTable clients = (JTable) field(frame, "tblClients");
        check("Bang may khach co dong 127.0.0.1", waitFor(() -> clients.getRowCount() >= 1 && "127.0.0.1".equals(String.valueOf(clients.getValueAt(0, 0))), 6000),
                edt(() -> clients.getRowCount() + " dong"));
        check("Danh sach 'Ten mien truy cap nhieu nhat' co du lieu", waitFor(() -> {
            try {
                return text(frame, "txtTop").contains("1. ");
            } catch (Exception e) {
                return false;
            }
        }, 4000), text(frame, "txtTop"));
        settle(1200);
        shot(frame, new File(shots, "08-thong-ke.png"));
        click((JButton) field(frame, "btnReset"));
        msg = waitOption();
        check("Dat lai thong ke -> hoi xac nhan", msg != null && msg.contains("Đặt lại"), msg);
        if (msg != null) {
            pressOption(0);
        }
        check("Sau khi dat lai bang may khach trong", waitFor(() -> clients.getRowCount() == 0, 4000), "");

        System.out.println("== 10. Hop thoai giai ma HTTPS ==");
        selectTab("Quy tắc");
        click((JButton) field(frame, "btnHttps"));
        d = waitDialog("Giải mã HTTPS");
        check("Mo hop thoai giai ma HTTPS", d != null, "");
        if (d != null) {
            click((JButton) field(d, "btnExport"));
            msg = waitOption();
            check("Xuat chung chi -> thong bao co duong dan proxy-ca.crt va nut 'Mo thu muc'", msg != null && msg.contains("proxy-ca.crt")
                    && buttonWithText(findOptionDialog(), "Mở thư mục") != null, msg);
            if (msg != null) {
                pressOption(1);
            }
            check("File proxy-ca.crt va proxy-ca.p12 duoc tao", new File(home, "proxy-ca.crt").isFile() && new File(home, "proxy-ca.p12").isFile(), "");
            click((JCheckBox) field(d, "chkMitm"));
            replaceText((Component) field(d, "txtRules"), "vi-du.test keyword bimat");
            click((JButton) field(d, "btnSave"));
            msg = waitOption();
            check("Luu -> 'Da luu: 1 quy tac' va nhac bat lai proxy", msg != null && msg.contains("1 quy tắc") && msg.contains("bật lại proxy"), msg);
            if (msg != null) {
                pressOption(0);
            }
            shot(d, new File(shots, "09-https.png"));
            closeDialog(d);
            String props = new String(Files.readAllBytes(new File(home, "proxy.properties").toPath()), StandardCharsets.UTF_8);
            check("proxy.properties ghi mitmEnabled=true", props.contains("mitmEnabled=true"), "");
            check("Quy tac tu khoa ap dung ngay", httpStatus("http://vi-du.test/trang-bimat") == 403, "");
        }

        System.out.println("== 11. Co gian cua so ==");
        resize(900, 600);
        selectTab("Tổng quan");
        check("Cua so nho nhat: nut Bat/Tat proxy va o IP deu nam trong cua so",
                fullyShown((Component) field(frame, "btnStop")) && fullyShown((Component) field(frame, "btnUsers")) && fullyShown((Component) field(frame, "txtAllow")), "");
        shot(frame, new File(shots, "10-co-nho-tong-quan.png"));
        selectTab("Thống kê");
        check("Cua so nho nhat: nut Dat lai thong ke con thay", fullyShown((Component) field(frame, "btnReset")), "");
        shot(frame, new File(shots, "11-co-nho-thong-ke.png"));
        resize(1500, 820);
        selectTab("Nhật ký");
        check("Cua so lon: bang nhat ky gian ra", edt(() -> log.getParent().getWidth()) > 1300, edt(() -> log.getParent().getWidth()) + " px");
        shot(frame, new File(shots, "12-co-lon-nhat-ky.png"));
        resize(1000, 680);

        System.out.println("== 12. Hop thoai bat dau nhanh ==");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new proxyapp.WelcomeDialog(frame, PORT).setVisible(true);
            }
        });
        JDialog welcome = waitDialog("Bắt đầu nhanh");
        check("Mo hop thoai bat dau nhanh", welcome != null, "");
        if (welcome != null) {
            String steps = text(welcome, "txtSteps");
            check("Noi dung co huong dan 3 buoc va dung cong", steps.contains("Bước 3") && steps.contains("cổng " + PORT), steps.substring(0, 40));
            shot(welcome, new File(shots, "13-bat-dau-nhanh.png"));
            click(buttonWithText(welcome, "Bắt đầu dùng"));
            final JDialog w = welcome;
            check("Bam 'Bat dau dung' -> hop thoai dong", waitFor(() -> !w.isShowing(), 4000), "");
        }

        System.out.println("== 13. Dong cua so -> chay nen o khay ==");
        check("Co bieu tuong o khay he thong", edt(() -> java.awt.SystemTray.isSupported() && java.awt.SystemTray.getSystemTray().getTrayIcons().length >= 1), "");
        click((Component) field(frame, "lblStatus"));
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F4);
        robot.keyRelease(KeyEvent.VK_F4);
        robot.keyRelease(KeyEvent.VK_ALT);
        settle(1500);
        check("Nhan Alt+F4 khi proxy dang chay -> cua so an", !edt(() -> frame.isVisible()), "");
        check("Proxy van tra loi sau khi an cua so", httpStatus("http://doubleclick.net/") == 403, "");
        edt(() -> {
            try {
                java.lang.reflect.Method m = frame.getClass().getDeclaredMethod("showAgain");
                m.setAccessible(true);
                m.invoke(frame);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        settle(800);
        check("Mo lai tu khay -> cua so hien", edt(() -> frame.isVisible()), "");
        stopProxyByClick();
        File cfg = new File(home, "proxy.properties");
        check("Cau hinh duoc luu khi an cua so", cfg.length() > 0, "");
    }
}
