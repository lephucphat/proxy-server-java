package proxyapp;

public class ByteText {

    public static String of(long b) {
        if (b < 1024) {
            return b + " B";
        }
        if (b < 1024L * 1024) {
            return String.format("%.1f KB", b / 1024.0);
        }
        if (b < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", b / 1024.0 / 1024.0);
        }
        return String.format("%.2f GB", b / 1024.0 / 1024.0 / 1024.0);
    }
}
