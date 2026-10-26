package proxyapp;

import java.net.InetAddress;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NetInfoTest {

    @Test
    public void lanAddressIsNullOrARealIpv4() throws Exception {
        String lan = NetInfo.lanAddress();
        if (lan == null) {
            return;
        }
        InetAddress a = InetAddress.getByName(lan);
        assertTrue(lan, a instanceof java.net.Inet4Address);
        assertFalse(lan, a.isLoopbackAddress());
    }

    @Test
    public void welcomeStepsMentionPortAndKeySteps() {
        String text = WelcomeDialog.steps(8123);
        assertTrue(text, text.contains("Bước 1"));
        assertTrue(text, text.contains("127.0.0.1, cổng 8123"));
        assertTrue(text, text.contains("cổng 8123."));
        String lan = NetInfo.lanAddress();
        if (lan != null) {
            assertTrue(text, text.contains(lan));
        }
    }
}
