package proxyapp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CertificateAuthorityTest {

    private static boolean hasName(X509Certificate c, int type, String value) throws Exception {
        Collection<List<?>> names = c.getSubjectAlternativeNames();
        if (names == null) {
            return false;
        }
        for (List<?> n : names) {
            if (((Integer) n.get(0)) == type && n.get(1).equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void caCertificateIsASelfSignedCaForSigningOnly() throws Exception {
        CertificateAuthority ca = CertificateAuthority.create();
        X509Certificate c = ca.certificate();
        assertTrue(c.getBasicConstraints() >= 0);
        assertTrue(c.getKeyUsage()[5]);
        assertFalse(c.getKeyUsage()[0]);
        assertEquals(c.getSubjectX500Principal(), c.getIssuerX500Principal());
        c.verify(c.getPublicKey());
        assertTrue(c.getSubjectX500Principal().getName().contains("ProxyApp Local CA"));
        long years = (c.getNotAfter().getTime() - System.currentTimeMillis()) / (365L * 24 * 3600 * 1000);
        assertTrue("còn " + years + " năm", years >= 9);
    }

    @Test
    public void siteCertificateIsSignedByTheCaAndNamesTheSite() throws Exception {
        CertificateAuthority ca = CertificateAuthority.create();
        CertificateAuthority.Issued issued = ca.issue("Example.COM");
        X509Certificate leaf = issued.certificate();
        leaf.verify(ca.certificate().getPublicKey());
        assertEquals(ca.certificate().getSubjectX500Principal(), leaf.getIssuerX500Principal());
        assertTrue(hasName(leaf, 2, "example.com"));
        assertEquals(-1, leaf.getBasicConstraints());
        assertTrue(leaf.getKeyUsage()[0]);
        assertTrue(leaf.getKeyUsage()[2]);
        assertFalse(leaf.getKeyUsage()[5]);
        assertEquals("1.3.6.1.5.5.7.3.1", leaf.getExtendedKeyUsage().get(0));
        assertEquals(2, issued.chain.length);
        assertEquals(ca.certificate(), issued.chain[1]);
        long days = (leaf.getNotAfter().getTime() - leaf.getNotBefore().getTime()) / (24L * 3600 * 1000);
        assertTrue("hiệu lực " + days + " ngày", days <= 32);
        assertTrue(leaf.getSubjectX500Principal().getName().contains("CN=example.com"));
        leaf.checkValidity();
    }

    @Test
    public void ipAddressGetsAnIpNameNotADnsName() throws Exception {
        CertificateAuthority ca = CertificateAuthority.create();
        X509Certificate leaf = ca.issue("192.168.1.10").certificate();
        assertTrue(hasName(leaf, 7, "192.168.1.10"));
        assertFalse(hasName(leaf, 2, "192.168.1.10"));
    }

    @Test
    public void sameSiteReusesTheCertificateAndDifferentSitesGetDifferentOnes() throws Exception {
        CertificateAuthority ca = CertificateAuthority.create();
        CertificateAuthority.Issued a = ca.issue("a.example");
        assertSame(a, ca.issue("A.example"));
        CertificateAuthority.Issued b = ca.issue("b.example");
        assertNotEquals(a.certificate().getSerialNumber(), b.certificate().getSerialNumber());
        assertFalse(hasName(b.certificate(), 2, "a.example"));
    }

    @Test
    public void caSurvivesSaveAndLoadAndCanStillSign() throws Exception {
        File f = File.createTempFile("cafile", ".p12");
        f.delete();
        CertificateAuthority first = CertificateAuthority.loadOrCreate(f);
        assertTrue(f.isFile());
        CertificateAuthority again = CertificateAuthority.loadOrCreate(f);
        assertArrayEquals(first.certificate().getEncoded(), again.certificate().getEncoded());
        again.issue("x.example").certificate().verify(first.certificate().getPublicKey());
        f.delete();
    }

    @Test
    public void brokenCaFileIsReportedNotReplaced() throws Exception {
        File f = File.createTempFile("cafile", ".p12");
        Files.write(f.toPath(), "khong phai file ca".getBytes());
        try {
            CertificateAuthority.loadOrCreate(f);
            fail();
        } catch (java.io.IOException expected) {
        }
        assertEquals("khong phai file ca", new String(Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    public void exportedPemIsAValidCertificateWithoutThePrivateKey() throws Exception {
        CertificateAuthority ca = CertificateAuthority.create();
        File f = File.createTempFile("cafile", ".crt");
        ca.exportCertificate(f);
        String pem = new String(Files.readAllBytes(f.toPath()), "US-ASCII");
        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertTrue(pem.trim().endsWith("-----END CERTIFICATE-----"));
        assertFalse(pem.contains("PRIVATE"));
        X509Certificate read = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes("US-ASCII")));
        assertEquals(ca.certificate(), read);
        f.delete();
    }

    @Test
    public void connectTargetsAreReducedToTheHostName() {
        assertEquals("example.com", MitmCertManager.hostOf("example.com:443"));
        assertEquals("example.com", MitmCertManager.hostOf("example.com"));
        assertEquals("::1", MitmCertManager.hostOf("[::1]:443"));
        assertEquals("10.0.0.1", MitmCertManager.hostOf(" 10.0.0.1:8443 "));
    }
}
