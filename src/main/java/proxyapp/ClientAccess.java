package proxyapp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ClientAccess {

    private final List<int[]> ranges = new ArrayList<int[]>();

    public static ClientAccess parse(String text) {
        ClientAccess access = new ClientAccess();
        if (text == null) {
            return access;
        }
        for (String part : text.split("[,;\\s]+")) {
            String item = part.trim();
            if (!item.isEmpty()) {
                access.add(item);
            }
        }
        return access;
    }

    private void add(String item) {
        String ip = item;
        int bits = 32;
        int slash = item.indexOf('/');
        if (slash >= 0) {
            ip = item.substring(0, slash);
            try {
                bits = Integer.parseInt(item.substring(slash + 1));
            } catch (NumberFormatException e) {
                throw bad(item);
            }
        }
        Integer value = ipv4(ip);
        if (value == null || bits < 0 || bits > 32) {
            throw bad(item);
        }
        int mask = bits == 0 ? 0 : -1 << (32 - bits);
        ranges.add(new int[]{value & mask, mask});
    }

    private static IllegalArgumentException bad(String item) {
        return new IllegalArgumentException("Địa chỉ máy khách không hợp lệ: " + item
                + " (dùng dạng 192.168.1.5 hoặc 192.168.1.0/24)");
    }

    private static Integer ipv4(String s) {
        String[] parts = s.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        int value = 0;
        for (String p : parts) {
            if (!p.matches("\\d{1,3}")) {
                return null;
            }
            int n = Integer.parseInt(p);
            if (n > 255) {
                return null;
            }
            value = (value << 8) | n;
        }
        return value;
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    public boolean allows(InetAddress address) {
        if (ranges.isEmpty()) {
            return true;
        }
        if (!(address instanceof Inet4Address)) {
            return false;
        }
        byte[] b = address.getAddress();
        int value = ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
        for (int[] r : ranges) {
            if ((value & r[1]) == r[0]) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        return b.length == 16 && (b[0] & 0xfe) == 0xfc;
    }
}
