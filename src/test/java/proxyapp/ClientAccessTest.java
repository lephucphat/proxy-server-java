package proxyapp;

import java.net.InetAddress;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientAccessTest {

    private static InetAddress ip(String s) throws Exception {
        return InetAddress.getByName(s);
    }

    @Test
    public void emptyListAllowsEveryone() throws Exception {
        ClientAccess a = ClientAccess.parse("  ");
        assertTrue(a.isEmpty());
        assertTrue(a.allows(ip("8.8.8.8")));
    }

    @Test
    public void singleAddressAndRange() throws Exception {
        ClientAccess a = ClientAccess.parse("10.0.0.5, 192.168.1.0/24");
        assertTrue(a.allows(ip("10.0.0.5")));
        assertFalse(a.allows(ip("10.0.0.6")));
        assertTrue(a.allows(ip("192.168.1.1")));
        assertTrue(a.allows(ip("192.168.1.254")));
        assertFalse(a.allows(ip("192.168.2.1")));
    }

    @Test
    public void rangeSizesWork() throws Exception {
        assertTrue(ClientAccess.parse("172.16.0.0/12").allows(ip("172.31.255.1")));
        assertFalse(ClientAccess.parse("172.16.0.0/12").allows(ip("172.32.0.1")));
        assertTrue(ClientAccess.parse("0.0.0.0/0").allows(ip("1.2.3.4")));
        assertTrue(ClientAccess.parse("1.2.3.4/32").allows(ip("1.2.3.4")));
        assertFalse(ClientAccess.parse("1.2.3.4/32").allows(ip("1.2.3.5")));
    }

    @Test
    public void ipv6ClientIsRefusedWhenListIsNotEmpty() throws Exception {
        assertFalse(ClientAccess.parse("192.168.1.0/24").allows(ip("2001:db8::1")));
    }

    @Test
    public void separatorsCanBeMixed() throws Exception {
        ClientAccess a = ClientAccess.parse("10.0.0.1;10.0.0.2\n10.0.0.3");
        assertTrue(a.allows(ip("10.0.0.1")));
        assertTrue(a.allows(ip("10.0.0.2")));
        assertTrue(a.allows(ip("10.0.0.3")));
    }

    @Test
    public void badEntriesAreRejected() {
        String[] bad = {"abc", "1.2.3", "1.2.3.256", "1.2.3.4/33", "1.2.3.4/x", "1.2.3.4/-1", "example.com"};
        for (String s : bad) {
            try {
                ClientAccess.parse(s);
                throw new AssertionError("phải báo lỗi với: " + s);
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void detectsInternalAddresses() throws Exception {
        String[] internal = {"127.0.0.1", "10.1.2.3", "192.168.0.10", "172.16.5.5", "172.31.0.1",
            "169.254.1.1", "0.0.0.0", "::1", "fc00::1", "fd12:3456::1", "fe80::1"};
        for (String s : internal) {
            assertTrue(s, ClientAccess.isInternal(ip(s)));
        }
        String[] external = {"8.8.8.8", "1.1.1.1", "172.32.0.1", "2001:4860:4860::8888"};
        for (String s : external) {
            assertFalse(s, ClientAccess.isInternal(ip(s)));
        }
    }
}
