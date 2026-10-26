package proxyapp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetInfo {

    private static final String[] VIRTUAL_NAMES = {"vmware", "virtualbox", "vethernet", "hyper-v", "virtual", "bluetooth",
        "vpn", "tap-", "wsl", "docker", "loopback", "pseudo"};

    private static boolean looksVirtual(NetworkInterface nic) {
        String name = ((nic.getDisplayName() == null ? "" : nic.getDisplayName()) + " " + nic.getName()).toLowerCase();
        for (String v : VIRTUAL_NAMES) {
            if (name.contains(v)) {
                return true;
            }
        }
        return false;
    }

    public static String lanAddress() {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            String virtualFallback = null;
            String publicFallback = null;
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                boolean virtual = looksVirtual(nic);
                Enumeration<InetAddress> addresses = nic.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress a = addresses.nextElement();
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress() || a.isLinkLocalAddress()) {
                        continue;
                    }
                    if (a.isSiteLocalAddress()) {
                        if (!virtual) {
                            return a.getHostAddress();
                        }
                        if (virtualFallback == null) {
                            virtualFallback = a.getHostAddress();
                        }
                    } else if (publicFallback == null) {
                        publicFallback = a.getHostAddress();
                    }
                }
            }
            return virtualFallback != null ? virtualFallback : publicFallback;
        } catch (java.net.SocketException e) {
            return null;
        }
    }
}
