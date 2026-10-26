package proxyapp;

import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DomainListParserTest {

    @Test
    public void readsHostsFormat() {
        Set<String> d = DomainListParser.parse("# Title\n127.0.0.1 localhost\n0.0.0.0 ads.example.com\n"
                + "127.0.0.1\ttracker.example.net # ghi chu\n0.0.0.0 a.example.org b.example.org\n");
        assertEquals(4, d.size());
        assertTrue(d.contains("ads.example.com"));
        assertTrue(d.contains("tracker.example.net"));
        assertTrue(d.contains("a.example.org"));
        assertTrue(d.contains("b.example.org"));
    }

    @Test
    public void readsPlainDomainsAndAdblockRules() {
        Set<String> d = DomainListParser.parse("evil.com\n||bad.net^\n||worse.org^$third-party\n"
                + "! comment\n[Adblock Plus 2.0]\nUPPER.Example.COM\nhttp://site.vn/path\n");
        assertEquals(5, d.size());
        assertTrue(d.contains("evil.com"));
        assertTrue(d.contains("bad.net"));
        assertTrue(d.contains("worse.org"));
        assertTrue(d.contains("upper.example.com"));
        assertTrue(d.contains("site.vn"));
    }

    @Test
    public void skipsNamesThatAreNotRealDomains() {
        Set<String> d = DomainListParser.parse("localhost\nlocalhost.localdomain\nbroadcasthost\n"
                + "ip6-localhost\n0.0.0.0\n1.2.3.4\n::1 ip6-loopback\n::1\nkhong hop le\nnoDot\n"
                + "bad_char$.com\n\n   \n");
        assertTrue(d.toString(), d.isEmpty());
    }

    @Test
    public void removesDuplicatesAndKeepsSubdomains() {
        Set<String> d = DomainListParser.parse("a.com\nA.COM\n0.0.0.0 a.com\nwww.a.com\n");
        assertEquals(2, d.size());
        assertFalse(d.contains("localhost"));
    }

    @Test
    public void emptyTextGivesEmptySet() {
        assertTrue(DomainListParser.parse("").isEmpty());
    }
}
