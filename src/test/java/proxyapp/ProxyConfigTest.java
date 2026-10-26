package proxyapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProxyConfigTest {

    @Test
    public void saveThenLoadKeepsValues() throws Exception {
        File f = File.createTempFile("cfg", ".properties");
        ProxyConfig c = new ProxyConfig();
        c.port = 9090;
        c.allowLan = true;
        c.requireAuth = true;
        c.username = "sv";
        c.setPassword("123");
        c.maxKBps = 64;
        c.logToFile = true;
        c.allowedClients = "192.168.1.0/24";
        c.blockInternal = false;
        c.maxAuthFailures = 3;
        c.lockMinutes = 10;
        c.publicListFile = "pub.txt";
        c.publicListUrls = "https://a.example/list\nD:\\thu muc\\b.txt";
        c.publicListHours = 12;
        c.closeToTray = false;
        c.autoStartProxy = true;
        c.allowOnlyMode = true;
        c.allowlistFile = "ok.txt";
        c.scheduleFile = "lich.txt";
        c.usersFile = "nguoi-dung.txt";
        c.alertBlocked = 7;
        c.alertMinutes = 3;
        c.mitmEnabled = true;
        c.mitmBypassFile = "bo-qua.txt";
        c.urlRulesFile = "quy-tac.txt";
        c.caFile = "ca.p12";
        c.caCertFile = "ca.crt";
        c.save(f);
        ProxyConfig d = ProxyConfig.load(f);
        assertEquals(9090, d.port);
        assertTrue(d.allowLan);
        assertTrue(d.requireAuth);
        assertEquals("sv", d.username);
        assertTrue(d.checkPassword("123"));
        assertFalse(d.checkPassword("sai"));
        assertEquals(64, d.maxKBps);
        assertTrue(d.logToFile);
        assertEquals("192.168.1.0/24", d.allowedClients);
        assertFalse(d.blockInternal);
        assertEquals(3, d.maxAuthFailures);
        assertEquals(10, d.lockMinutes);
        assertEquals("pub.txt", d.publicListFile);
        assertEquals("https://a.example/list\nD:\\thu muc\\b.txt", d.publicListUrls);
        assertEquals(12, d.publicListHours);
        assertFalse(d.closeToTray);
        assertTrue(d.autoStartProxy);
        assertTrue(d.allowOnlyMode);
        assertEquals("ok.txt", d.allowlistFile);
        assertEquals("lich.txt", d.scheduleFile);
        assertEquals("nguoi-dung.txt", d.usersFile);
        assertEquals(7, d.alertBlocked);
        assertEquals(3, d.alertMinutes);
        assertTrue(d.mitmEnabled);
        assertEquals("bo-qua.txt", d.mitmBypassFile);
        assertEquals("quy-tac.txt", d.urlRulesFile);
        assertEquals("ca.p12", d.caFile);
        assertEquals("ca.crt", d.caCertFile);
        f.delete();
    }

    @Test
    public void passwordIsNotStoredAsPlainText() throws Exception {
        File f = File.createTempFile("cfg", ".properties");
        ProxyConfig c = new ProxyConfig();
        c.setPassword("matkhau-bi-mat");
        c.save(f);
        String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertFalse(text.contains("matkhau-bi-mat"));
        assertTrue(text.contains("passwordHash=pbkdf2"));
        f.delete();
    }

    @Test
    public void oldPlainPasswordIsConvertedToHashOnLoad() throws Exception {
        File f = File.createTempFile("cfg", ".properties");
        Properties p = new Properties();
        p.setProperty("password", "cu123");
        try (OutputStream out = new FileOutputStream(f)) {
            p.store(out, null);
        }
        ProxyConfig c = ProxyConfig.load(f);
        assertTrue(c.hasPassword());
        assertTrue(c.checkPassword("cu123"));
        assertFalse(c.passwordHash.contains("cu123"));
        c.save(f);
        String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertFalse(text.contains("cu123"));
        f.delete();
    }

    @Test
    public void missingFileGivesDefaults() {
        ProxyConfig d = ProxyConfig.load(new File("khong-ton-tai.properties"));
        assertEquals(8080, d.port);
        assertFalse(d.allowLan);
        assertFalse(d.requireAuth);
        assertFalse(d.hasPassword());
        assertTrue(d.blockInternal);
        assertEquals(5, d.maxAuthFailures);
        assertEquals(5, d.lockMinutes);
        assertEquals("blocklist-public.txt", d.publicListFile);
        assertEquals("", d.publicListUrls);
        assertEquals(24, d.publicListHours);
        assertTrue(d.closeToTray);
        assertFalse(d.autoStartProxy);
        assertFalse(d.allowOnlyMode);
        assertEquals("allowlist.txt", d.allowlistFile);
        assertEquals("schedule.txt", d.scheduleFile);
        assertEquals("users.txt", d.usersFile);
        assertEquals(20, d.alertBlocked);
        assertEquals(5, d.alertMinutes);
        assertFalse(d.mitmEnabled);
        assertEquals("mitm-bypass.txt", d.mitmBypassFile);
        assertEquals("url-rules.txt", d.urlRulesFile);
        assertEquals("proxy-ca.p12", d.caFile);
        assertEquals("proxy-ca.crt", d.caCertFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeAlertThreshold() {
        ProxyConfig c = new ProxyConfig();
        c.alertBlocked = -1;
        c.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeUpdateHours() {
        ProxyConfig c = new ProxyConfig();
        c.publicListHours = -1;
        c.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBadPort() {
        ProxyConfig c = new ProxyConfig();
        c.port = 70000;
        c.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAuthWithoutUser() {
        ProxyConfig c = new ProxyConfig();
        c.requireAuth = true;
        c.username = " ";
        c.setPassword("123");
        c.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAuthWithoutPassword() {
        ProxyConfig c = new ProxyConfig();
        c.requireAuth = true;
        c.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLanWithoutAuth() {
        ProxyConfig c = new ProxyConfig();
        c.allowLan = true;
        c.validate();
    }

    @Test
    public void acceptsLanWithAuth() {
        ProxyConfig c = new ProxyConfig();
        c.allowLan = true;
        c.requireAuth = true;
        c.setPassword("123");
        c.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBadAllowedClients() {
        ProxyConfig c = new ProxyConfig();
        c.allowedClients = "abc";
        c.validate();
    }
}
