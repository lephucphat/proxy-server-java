package proxyapp;

import io.netty.handler.codec.http.HttpRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.littleshoot.proxy.MitmManager;

public class MitmCertManager implements MitmManager, org.littleshoot.proxy.MitmDecider {

    private final CertificateAuthority authority;
    private final SSLContext upstream;
    private volatile String[] protocols = {"TLSv1.2"};
    private volatile BlockList bypass;

    public MitmCertManager(CertificateAuthority authority) throws GeneralSecurityException {
        this(authority, SSLContext.getDefault());
    }

    public MitmCertManager(CertificateAuthority authority, SSLContext upstream) {
        this.authority = authority;
        this.upstream = upstream;
    }

    public void setBypass(BlockList list) {
        bypass = list;
    }

    public boolean shouldMitm(HttpRequest connectRequest) {
        BlockList list = bypass;
        return list == null || !list.matches(hostOf(connectRequest.getUri()));
    }

    public void setProtocols(String... names) {
        protocols = names == null || names.length == 0 ? null : names.clone();
    }

    private void applyProtocols(SSLEngine engine) {
        String[] wanted = protocols;
        if (wanted != null) {
            engine.setEnabledProtocols(wanted);
        }
    }

    public SSLEngine serverSslEngine(String peerHost, int peerPort) {
        SSLEngine engine = upstream.createSSLEngine(peerHost, peerPort);
        engine.setUseClientMode(true);
        applyProtocols(engine);
        SSLParameters params = engine.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        engine.setSSLParameters(params);
        return engine;
    }

    public SSLEngine serverSslEngine() {
        throw new IllegalStateException("Không kiểm tra được tên máy chủ khi không có SNI, từ chối kết nối");
    }

    public SSLEngine clientSslEngineFor(HttpRequest httpRequest, SSLSession serverSslSession) {
        try {
            SSLEngine engine = authority.issue(hostOf(httpRequest.getUri())).context().createSSLEngine();
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(false);
            applyProtocols(engine);
            return engine;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tạo được chứng chỉ: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được chứng chỉ: " + e.getMessage(), e);
        }
    }

    static String hostOf(String connectTarget) {
        String t = connectTarget.trim();
        if (t.startsWith("[")) {
            int end = t.indexOf(']');
            if (end > 0) {
                return t.substring(1, end);
            }
        }
        int colon = t.lastIndexOf(':');
        if (colon >= 0 && t.indexOf(':') == colon) {
            t = t.substring(0, colon);
        }
        return t;
    }
}
