package proxyapp;

import java.awt.Color;
import java.awt.Font;
import java.util.Collections;
import javax.swing.UIManager;

public class Theme {

    public static final Color GREEN = new Color(0x2E9E5B);
    public static final Color RED = new Color(0xD9534F);
    public static final Color GRAY = new Color(0x7A8190);
    public static final Color LINE = new Color(0xD8DCE3);
    public static final Color PANEL = new Color(0xF4F6FA);

    public static void install() {
        try {
            com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#2670D6"));
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            com.formdev.flatlaf.FlatLightLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("TabbedPane.tabHeight", 38);
            UIManager.put("TabbedPane.showTabSeparators", false);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("TitledBorder.font", new Font("Segoe UI", Font.BOLD, 13));
        } catch (RuntimeException e) {
            installFallback();
        }
    }

    private static void installFallback() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
        }
    }

    public static Color buttonColor() {
        Color c = UIManager.getColor("Button.background");
        return c == null ? Color.LIGHT_GRAY : c;
    }

    public static Color buttonTextColor() {
        Color c = UIManager.getColor("Button.foreground");
        return c == null ? Color.BLACK : c;
    }
}
