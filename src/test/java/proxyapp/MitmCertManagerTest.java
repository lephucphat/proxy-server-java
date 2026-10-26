package proxyapp;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import javax.net.ssl.SSLEngine;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MitmCertManagerTest {

    private MitmCertManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new MitmCertManager(CertificateAuthority.create());
    }

    private SSLEngine clientEngine(String target) {
        return manager.clientSslEngineFor(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, target), null);
    }

    @Test
    public void bothLegsUseTls12ByDefault() {
        assertArrayEquals(new String[]{"TLSv1.2"}, manager.serverSslEngine("example.com", 443).getEnabledProtocols());
        assertArrayEquals(new String[]{"TLSv1.2"}, clientEngine("example.com:443").getEnabledProtocols());
    }

    @Test
    public void protocolsCanBeChangedOrLeftToTheJdk() {
        manager.setProtocols("TLSv1.2", "TLSv1.3");
        assertEquals(2, manager.serverSslEngine("example.com", 443).getEnabledProtocols().length);
        manager.setProtocols();
        assertTrue(manager.serverSslEngine("example.com", 443).getEnabledProtocols().length >= 1);
    }

    @Test
    public void upstreamEngineChecksTheServerNameAndActsAsClient() {
        SSLEngine e = manager.serverSslEngine("example.com", 443);
        assertTrue(e.getUseClientMode());
        assertEquals("HTTPS", e.getSSLParameters().getEndpointIdentificationAlgorithm());
        assertEquals("example.com", e.getPeerHost());
    }

    @Test
    public void clientEngineActsAsServerWithoutAskingForACertificate() {
        SSLEngine e = clientEngine("shop.example.com:443");
        assertFalse(e.getUseClientMode());
        assertFalse(e.getNeedClientAuth());
    }

    @Test
    public void connectionWithoutServerNameIsRefusedInsteadOfSkippingTheCheck() {
        try {
            manager.serverSslEngine();
            fail();
        } catch (IllegalStateException expected) {
        }
    }
}
