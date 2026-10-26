package proxyapp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebSocketProxyTest {

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private ServerSocket origin;
    private Thread originThread;
    private ProxyService service;
    private int proxyPort;

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private static String readHeaders(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int state = 0;
        while (state < 4) {
            int b = in.read();
            if (b < 0) {
                break;
            }
            out.write(b);
            if ((b == '\r' && (state == 0 || state == 2)) || (b == '\n' && (state == 1 || state == 3))) {
                state++;
            } else {
                state = b == '\r' ? 1 : 0;
            }
        }
        return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static String headerValue(String headers, String name) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith(name.toLowerCase() + ":")) {
                return line.substring(name.length() + 1).trim();
            }
        }
        return null;
    }

    private static void readFully(InputStream in, byte[] buf) throws Exception {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) {
                throw new java.io.EOFException();
            }
            off += n;
        }
    }

    @Before
    public void setUp() throws Exception {
        origin = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        originThread = new Thread() {
            public void run() {
                while (!origin.isClosed()) {
                    try (Socket s = origin.accept()) {
                        s.setSoTimeout(5000);
                        InputStream in = s.getInputStream();
                        OutputStream out = s.getOutputStream();
                        String headers = readHeaders(in);
                        String key = headerValue(headers, "Sec-WebSocket-Key");
                        if (key == null) {
                            continue;
                        }
                        byte[] sha = MessageDigest.getInstance("SHA-1").digest((key + GUID).getBytes(StandardCharsets.ISO_8859_1));
                        String reply = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                                + "Sec-WebSocket-Accept: " + Base64.getEncoder().encodeToString(sha) + "\r\n\r\n";
                        out.write(reply.getBytes(StandardCharsets.ISO_8859_1));
                        out.flush();
                        byte[] head = new byte[2];
                        readFully(in, head);
                        int len = head[1] & 0x7f;
                        byte[] mask = new byte[4];
                        readFully(in, mask);
                        byte[] payload = new byte[len];
                        readFully(in, payload);
                        for (int i = 0; i < len; i++) {
                            payload[i] ^= mask[i % 4];
                        }
                        out.write(0x81);
                        out.write(len);
                        out.write(payload);
                        out.flush();
                        Thread.sleep(200);
                    } catch (Exception e) {
                    }
                }
            }
        };
        originThread.setDaemon(true);
        originThread.start();
        proxyPort = freePort();
        service = new ProxyService(new BlockList(null, ""), new TrafficLog(100), new ProxyStats());
        ProxyConfig cfg = new ProxyConfig();
        cfg.port = proxyPort;
        service.start(cfg);
    }

    @After
    public void tearDown() throws Exception {
        service.stop();
        origin.close();
    }

    private String exchange(Socket s, String text) throws Exception {
        OutputStream out = s.getOutputStream();
        InputStream in = s.getInputStream();
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        byte[] mask = {1, 2, 3, 4};
        out.write(0x81);
        out.write(0x80 | data.length);
        out.write(mask);
        for (int i = 0; i < data.length; i++) {
            out.write(data[i] ^ mask[i % 4]);
        }
        out.flush();
        byte[] head = new byte[2];
        readFully(in, head);
        byte[] payload = new byte[head[1] & 0x7f];
        readFully(in, payload);
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static String upgradeRequest(String target, int port) {
        return "GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1:" + port + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n\r\n";
    }

    @Test
    public void webSocketThroughConnectTunnel() throws Exception {
        try (Socket s = new Socket("127.0.0.1", proxyPort)) {
            s.setSoTimeout(8000);
            int port = origin.getLocalPort();
            s.getOutputStream().write(("CONNECT 127.0.0.1:" + port + " HTTP/1.1\r\nHost: 127.0.0.1:" + port + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            String connectReply = readHeaders(s.getInputStream());
            assertTrue(connectReply, connectReply.startsWith("HTTP/1.1 200"));
            s.getOutputStream().write(upgradeRequest("/", port).getBytes(StandardCharsets.ISO_8859_1));
            String upgrade = readHeaders(s.getInputStream());
            assertTrue(upgrade, upgrade.startsWith("HTTP/1.1 101"));
            assertEquals("hello", exchange(s, "hello"));
        }
    }

    @Test
    public void webSocketUpgradeThroughPlainProxyRequest() throws Exception {
        try (Socket s = new Socket("127.0.0.1", proxyPort)) {
            s.setSoTimeout(8000);
            int port = origin.getLocalPort();
            s.getOutputStream().write(upgradeRequest("http://127.0.0.1:" + port + "/chat", port).getBytes(StandardCharsets.ISO_8859_1));
            String upgrade = readHeaders(s.getInputStream());
            assertTrue(upgrade, upgrade.startsWith("HTTP/1.1 101"));
            assertEquals("xin chao", exchange(s, "xin chao"));
        }
    }
}
