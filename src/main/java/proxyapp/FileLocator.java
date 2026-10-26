package proxyapp;

import javax.swing.JOptionPane;

public class FileLocator {

    public static void show(java.awt.Component parent, java.io.File target) {
        java.io.File dir = target.getAbsoluteFile().getParentFile();
        try {
            java.io.File file = target.getAbsoluteFile().getCanonicalFile();
            dir = file.getParentFile();
            if (SystemProxy.isSupported() && file.isFile()) {
                new ProcessBuilder("explorer.exe", "/select,", file.getPath()).start();
                return;
            }
            if (!java.awt.Desktop.isDesktopSupported()) {
                throw new java.io.IOException("Máy này không hỗ trợ mở thư mục từ chương trình");
            }
            java.awt.Desktop.getDesktop().open(dir);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(parent, "Không mở được thư mục " + dir + ": " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
