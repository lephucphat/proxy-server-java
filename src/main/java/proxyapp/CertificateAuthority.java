package proxyapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertificateAuthority {

    public static class Issued {
        public final PrivateKey key;
        public final X509Certificate[] chain;
        private final SSLContext context;

        Issued(PrivateKey key, X509Certificate[] chain, SSLContext context) {
            this.key = key;
            this.chain = chain;
            this.context = context;
        }

        public X509Certificate certificate() {
            return chain[0];
        }

        public SSLContext context() {
            return context;
        }
    }

    private static final char[] PASSWORD = "proxyapp-ca".toCharArray();
    private static final String ALIAS = "ca";
    private static final long DAY = 24L * 3600 * 1000;
    private static final int MAX_CACHE = 500;

    private final PrivateKey caKey;
    private final X509Certificate caCert;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Issued> cache = new ConcurrentHashMap<String, Issued>();
    private KeyPair leafKeys;

    private CertificateAuthority(PrivateKey caKey, X509Certificate caCert) {
        this.caKey = caKey;
        this.caCert = caCert;
    }

    public static CertificateAuthority create() throws GeneralSecurityException, IOException {
        KeyPair pair = newKeyPair();
        String machine = "may";
        try {
            machine = InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
        }
        SecureRandom random = new SecureRandom();
        String tag = String.format("%04X", random.nextInt(0x10000));
        X500Name name = new X500Name("CN=ProxyApp Local CA " + tag + " (" + cleanName(machine) + "), O=ProxyApp");
        Date from = new Date(System.currentTimeMillis() - DAY);
        Date to = new Date(System.currentTimeMillis() + 3650 * DAY);
        X509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(name, new BigInteger(64, random).abs().add(BigInteger.ONE),
                from, to, name, pair.getPublic());
        b.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        b.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        b.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(pair.getPublic()));
        X509Certificate cert = sign(b, pair.getPrivate());
        return new CertificateAuthority(pair.getPrivate(), cert);
    }

    public static CertificateAuthority loadOrCreate(File file) throws GeneralSecurityException, IOException {
        if (file.isFile()) {
            return load(file);
        }
        CertificateAuthority ca = create();
        ca.save(file);
        return ca;
    }

    public static CertificateAuthority load(File file) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(file)) {
            ks.load(in, PASSWORD);
        }
        PrivateKey key = (PrivateKey) ks.getKey(ALIAS, PASSWORD);
        Certificate cert = ks.getCertificate(ALIAS);
        if (key == null || !(cert instanceof X509Certificate)) {
            throw new IOException("File CA không đúng định dạng: " + file);
        }
        return new CertificateAuthority(key, (X509Certificate) cert);
    }

    public void save(File file) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(ALIAS, caKey, PASSWORD, new Certificate[]{caCert});
        try (OutputStream out = new FileOutputStream(file)) {
            ks.store(out, PASSWORD);
        }
    }

    public X509Certificate certificate() {
        return caCert;
    }

    public String certificatePem() throws GeneralSecurityException {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(caCert.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + body + "\n-----END CERTIFICATE-----\n";
    }

    public void exportCertificate(File file) throws GeneralSecurityException, IOException {
        java.nio.file.Files.write(file.toPath(), certificatePem().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    public SSLContext trustingContext() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setCertificateEntry(ALIAS, caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }

    public Issued issue(String host) throws GeneralSecurityException, IOException {
        String name = host.toLowerCase();
        Issued cached = cache.get(name);
        if (cached != null && cached.certificate().getNotAfter().getTime() > System.currentTimeMillis() + DAY) {
            return cached;
        }
        PrivateKey key;
        java.security.PublicKey pub;
        synchronized (this) {
            if (leafKeys == null) {
                leafKeys = newKeyPair();
            }
            key = leafKeys.getPrivate();
            pub = leafKeys.getPublic();
        }
        Date from = new Date(System.currentTimeMillis() - DAY);
        Date to = new Date(System.currentTimeMillis() + 30 * DAY);
        String commonName = name.length() <= 60 ? name : "proxyapp-site";
        X509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(caCert, new BigInteger(64, random).abs().add(BigInteger.ONE),
                from, to, new X500Principal("CN=" + cleanName(commonName)), pub);
        JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
        b.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        b.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        b.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        b.addExtension(Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(pub));
        b.addExtension(Extension.authorityKeyIdentifier, false, utils.createAuthorityKeyIdentifier(caCert));
        int type = isIpv4(name) ? GeneralName.iPAddress : GeneralName.dNSName;
        b.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(type, name)));
        X509Certificate leaf = sign(b, caKey);
        X509Certificate[] chain = {leaf, caCert};

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("leaf", key, PASSWORD, chain);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, PASSWORD);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        Issued issued = new Issued(key, chain, ctx);
        if (cache.size() >= MAX_CACHE) {
            cache.clear();
        }
        cache.put(name, issued);
        return issued;
    }

    private static KeyPair newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    private static X509Certificate sign(X509v3CertificateBuilder b, PrivateKey key) throws GeneralSecurityException, IOException {
        try {
            return new JcaX509CertificateConverter().getCertificate(
                    b.build(new JcaContentSignerBuilder("SHA256withRSA").build(key)));
        } catch (org.bouncycastle.operator.OperatorCreationException e) {
            throw new GeneralSecurityException(e);
        }
    }

    private static boolean isIpv4(String s) {
        return s.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private static String cleanName(String s) {
        return s.replaceAll("[^A-Za-z0-9.\\- ]", "");
    }
}
