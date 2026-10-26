package proxyapp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.net.ssl.SSLSession;
import org.littleshoot.proxy.ActivityTrackerAdapter;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class ProxyService {

    private static final AttributeKey<String> LOGGED_IN_USER = AttributeKey.valueOf("proxyapp.loggedInUser");
    private static final String DUMMY_HASH = PasswordHasher.hash("khong-ai-biet");

    private final BlockList blockList;
    private final TrafficLog log;
    private final ProxyStats stats;
    private HttpProxyServer server;
    private ProxyConfig active;
    private volatile ClientAccess clientAccess = new ClientAccess();
    private volatile LoginGuard loginGuard = new LoginGuard(0, 0);
    private volatile BlockList allowList = new BlockList(null, "");
    private volatile BlockSchedule schedule = new BlockSchedule(null);
    private volatile boolean allowOnly;
    private volatile UserStore users;
    private volatile org.littleshoot.proxy.MitmManager mitm;
    private volatile BlockList mitmBypass = new BlockList(null, "");
    private volatile UrlRules urlRules = new UrlRules(null);
    private volatile boolean mitmActive;
    private CertificateAuthority authority;
    private File authorityFile;
    private volatile AlertLog alerts = new AlertLog(200);
    private volatile AlertMonitor monitor = new AlertMonitor(alerts, 0, 0);
    private final java.util.concurrent.ConcurrentHashMap<InetSocketAddress, String> connectionUsers =
            new java.util.concurrent.ConcurrentHashMap<InetSocketAddress, String>();

    public ProxyService(BlockList blockList, TrafficLog log, ProxyStats stats) {
        this.blockList = blockList;
        this.log = log;
        this.stats = stats;
    }

    public synchronized void start(final ProxyConfig cfg) {
        if (server != null) {
            throw new IllegalStateException("Proxy đang chạy");
        }
        cfg.validate();
        log.setAppendFile(cfg.logToFile ? AppDir.file(cfg.logFile) : null);
        clientAccess = ClientAccess.parse(cfg.allowedClients);
        loginGuard = new LoginGuard(cfg.maxAuthFailures, cfg.lockMinutes * 60000L);
        allowOnly = cfg.allowOnlyMode;
        monitor = new AlertMonitor(alerts, cfg.alertBlocked, cfg.alertMinutes * 60000L);

        HttpProxyServerBootstrap b = DefaultHttpProxyServer.bootstrap()
                .withName("ProxyApp")
                .withPort(cfg.port)
                .withAllowLocalOnly(!cfg.allowLan)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new RequestFilters(originalRequest, ctx, cfg);
                    }
                })
                .plusActivityTracker(new Tracker());

        org.littleshoot.proxy.MitmManager manager = mitm;
        if (manager == null && cfg.mitmEnabled) {
            manager = buildMitm(cfg);
        }
        if (manager != null) {
            b.withManInTheMiddle(manager);
        }
        mitmActive = manager != null;
        if (cfg.maxKBps > 0) {
            long bytes = cfg.maxKBps * 1024L;
            b.withThrottling(bytes, bytes);
        }
        try {
            server = b.start();
        } catch (RuntimeException e) {
            mitmActive = false;
            if (causedByBind(e)) {
                throw new IllegalStateException("Cổng " + cfg.port + " đang được chương trình khác dùng. Hãy đổi sang cổng khác rồi bật lại.", e);
            }
            throw e;
        }
        active = cfg;
    }

    private static boolean causedByBind(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.BindException) {
                return true;
            }
            String m = t.getMessage();
            if (m != null && (m.contains("Address already in use") || m.contains("address already in use"))) {
                return true;
            }
        }
        return false;
    }

    public void setAllowList(BlockList list) {
        allowList = list;
    }

    public void setSchedule(BlockSchedule s) {
        schedule = s;
    }

    public void setUsers(UserStore store) {
        users = store;
    }

    public void setMitm(org.littleshoot.proxy.MitmManager manager) {
        mitm = manager;
    }

    public void setMitmBypass(BlockList list) {
        mitmBypass = list;
    }

    public void setUrlRules(UrlRules rules) {
        urlRules = rules;
    }

    private org.littleshoot.proxy.MitmManager buildMitm(ProxyConfig cfg) {
        try {
            File caFile = AppDir.file(cfg.caFile);
            if (authority == null || !caFile.equals(authorityFile)) {
                authority = CertificateAuthority.loadOrCreate(caFile);
                authorityFile = caFile;
            }
            authority.exportCertificate(AppDir.file(cfg.caCertFile));
            MitmCertManager manager = new MitmCertManager(authority);
            manager.setBypass(mitmBypass);
            return manager;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Không chuẩn bị được chứng chỉ giải mã HTTPS: " + e.getMessage(), e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Không chuẩn bị được chứng chỉ giải mã HTTPS: " + e.getMessage(), e);
        }
    }

    static String pathAndQuery(String url) {
        int scheme = url.indexOf("://");
        String rest = scheme >= 0 ? url.substring(scheme + 3) : url;
        if (rest.startsWith("/")) {
            return rest;
        }
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            return rest.substring(slash);
        }
        int question = rest.indexOf('?');
        return question >= 0 ? "/" + rest.substring(question) : "/";
    }

    public void setAlerts(AlertLog log) {
        alerts = log;
        monitor = new AlertMonitor(log, 0, 0);
    }

    public void setAllowOnly(boolean on) {
        allowOnly = on;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
            active = null;
        }
        mitmActive = false;
    }

    public boolean isMitmActive() {
        return mitmActive;
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    public synchronized InetSocketAddress getListenAddress() {
        return server == null ? null : server.getListenAddress();
    }

    public synchronized ProxyConfig getActiveConfig() {
        return active;
    }

    public synchronized void setThrottle(int kbps) {
        if (server != null) {
            long bytes = Math.max(0, kbps) * 1024L;
            server.setThrottle(bytes, bytes);
        }
    }

    static String hostOf(HttpRequest r) {
        String hp = org.littleshoot.proxy.impl.ProxyUtils.parseHostAndPort(r);
        if (hp == null || hp.isEmpty() || r.getUri().startsWith("/")) {
            hp = HttpHeaders.getHost(r, "");
        }
        if (hp == null) {
            return "";
        }
        hp = hp.trim();
        if (hp.startsWith("[")) {
            int end = hp.indexOf(']');
            if (end > 0) {
                return hp.substring(1, end).toLowerCase();
            }
        }
        int colon = hp.lastIndexOf(':');
        if (colon >= 0 && hp.indexOf(':') == colon) {
            hp = hp.substring(0, colon);
        }
        return hp.toLowerCase();
    }

    private static InetAddress addressOf(ChannelHandlerContext ctx) {
        if (ctx != null && ctx.channel() != null && ctx.channel().remoteAddress() instanceof InetSocketAddress) {
            return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
        }
        return null;
    }

    private static FullHttpResponse page(HttpResponseStatus status, String heading, String message, boolean keepAlive) {
        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + heading + "</title></head>"
                + "<body style=\"font-family:sans-serif;text-align:center;margin-top:80px\">"
                + "<h1>" + heading + "</h1><p>" + message + "</p></body></html>";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse r = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(body));
        r.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=utf-8");
        r.headers().set(HttpHeaders.Names.CONTENT_LENGTH, body.length);
        r.headers().set(HttpHeaders.Names.CONNECTION, keepAlive ? "keep-alive" : "close");
        return r;
    }

    private static FullHttpResponse blockedPage(String host) {
        return page(HttpResponseStatus.FORBIDDEN, "Truy cập bị chặn",
                "Proxy đã chặn trang <b>" + escape(host) + "</b>.", false);
    }

    private static FullHttpResponse loginPage() {
        FullHttpResponse r = page(HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED, "Cần đăng nhập",
                "Nhập tài khoản và mật khẩu của proxy.", true);
        r.headers().set(HttpHeaders.Names.PROXY_AUTHENTICATE, "Basic realm=\"ProxyApp\"");
        return r;
    }

    private static String[] loginOf(HttpRequest request) {
        String header = request.headers().get(HttpHeaders.Names.PROXY_AUTHORIZATION);
        if (header == null || header.length() < 6 || !header.substring(0, 6).equalsIgnoreCase("Basic ")) {
            return null;
        }
        try {
            String text = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int colon = text.indexOf(':');
            if (colon < 0) {
                return null;
            }
            return new String[]{text.substring(0, colon), text.substring(colon + 1)};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean reachesInternalNetwork(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        try {
            for (InetAddress a : InetAddress.getAllByName(host)) {
                if (ClientAccess.isInternal(a)) {
                    return true;
                }
            }
        } catch (java.net.UnknownHostException e) {
            return false;
        }
        return false;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private class Tracker extends ActivityTrackerAdapter {
        @Override
        public void clientConnected(InetSocketAddress clientAddress) {
            stats.connectionOpened();
        }

        @Override
        public void clientDisconnected(InetSocketAddress clientAddress, SSLSession sslSession) {
            stats.connectionClosed();
            if (clientAddress != null) {
                connectionUsers.remove(clientAddress);
            }
        }

        @Override
        public void bytesReceivedFromClient(FlowContext flowContext, int numberOfBytes) {
            stats.fromClient(numberOfBytes);
            countBytes(flowContext, 0, numberOfBytes);
        }

        @Override
        public void bytesSentToClient(FlowContext flowContext, int numberOfBytes) {
            stats.toClient(numberOfBytes);
            countBytes(flowContext, numberOfBytes, 0);
        }

        private void countBytes(FlowContext flowContext, long down, long up) {
            InetSocketAddress a = flowContext == null ? null : flowContext.getClientAddress();
            if (a == null || a.getAddress() == null) {
                return;
            }
            stats.bytesBy(a.getAddress().getHostAddress(), connectionUsers.get(a), down, up);
        }
    }

    private class RequestFilters extends HttpFiltersAdapter {
        private final long start = System.currentTimeMillis();
        private final ProxyConfig cfg;
        private final InetAddress clientAddress;
        private final String client;
        private final String method;
        private final String host;
        private final String target;
        private final String pathQuery;
        private final boolean tunnel;
        private String user = "";
        private int status;
        private long bytes;
        private boolean done;

        RequestFilters(HttpRequest request, ChannelHandlerContext ctx, ProxyConfig cfg) {
            super(request, ctx);
            this.cfg = cfg;
            this.clientAddress = addressOf(ctx);
            this.client = clientAddress == null ? "?" : clientAddress.getHostAddress();
            this.method = request.getMethod().name();
            this.host = hostOf(request);
            this.tunnel = HttpMethod.CONNECT.equals(request.getMethod());
            String uri = request.getUri();
            if (uri.startsWith("/") && ctx != null && ctx.pipeline().get(io.netty.handler.ssl.SslHandler.class) != null) {
                String hostHeader = HttpHeaders.getHost(request, host);
                if (hostHeader.endsWith(":443")) {
                    hostHeader = hostHeader.substring(0, hostHeader.length() - 4);
                }
                uri = "https://" + hostHeader + uri;
            }
            this.target = uri.length() > 300 ? uri.substring(0, 300) : uri;
            this.pathQuery = tunnel ? "" : pathAndQuery(uri);
        }

        private synchronized void finish(int code, boolean blocked) {
            if (done) {
                return;
            }
            done = true;
            log.add(new LogEntry(start, client, user, method, host, target, code, bytes,
                    System.currentTimeMillis() - start, blocked));
        }

        @Override
        public HttpResponse clientToProxyRequest(HttpObject httpObject) {
            if (!(httpObject instanceof HttpRequest)) {
                return null;
            }
            HttpRequest request = (HttpRequest) httpObject;
            boolean local = !cfg.allowLan || (clientAddress != null && clientAddress.isLoopbackAddress());

            if (!local && (clientAddress == null || !clientAccess.allows(clientAddress))) {
                stats.blocked();
                stats.blockedBy(client, "");
                monitor.refusedClient(client);
                finish(403, true);
                return page(HttpResponseStatus.FORBIDDEN, "Không được phép",
                        "Địa chỉ " + escape(client) + " không nằm trong danh sách máy được dùng proxy.", false);
            }

            if (cfg.requireAuth) {
                HttpResponse refused = checkLogin(request);
                if (refused != null) {
                    return refused;
                }
            }

            if (!local && cfg.blockInternal && reachesInternalNetwork(host)) {
                stats.request(host);
                stats.requestBy(client, user);
                countBlocked();
                finish(403, true);
                return page(HttpResponseStatus.FORBIDDEN, "Không được phép",
                        "Máy khách trong LAN không được truy cập mạng nội bộ qua proxy: <b>" + escape(host) + "</b>.", false);
            }

            stats.request(host);
            stats.requestBy(client, user);
            if (blockList.matches(host)) {
                countBlocked();
                finish(403, true);
                return blockedPage(host);
            }
            UrlRules.Rule urlRule = urlRules.blockingRule(host, pathQuery, tunnel);
            if (urlRule != null) {
                countBlocked();
                finish(403, true);
                return page(HttpResponseStatus.FORBIDDEN, "Bị chặn theo quy tắc",
                        "Địa chỉ <b>" + escape(host) + "</b> khớp quy tắc: " + escape(urlRule.describe()) + ".", false);
            }
            BlockSchedule rulesSchedule = schedule;
            BlockList rulesAllowList = allowList;
            boolean rulesAllowOnly = allowOnly;
            UserStore store = users;
            UserStore.Account account = cfg.requireAuth && store != null && !user.equals(cfg.username) ? store.find(user) : null;
            if (account != null && account.ownRules) {
                rulesSchedule = account.schedule;
                rulesAllowList = account.allowList;
                rulesAllowOnly = account.allowOnly;
            }
            BlockSchedule.Rule rule = rulesSchedule.blockingRule(host, java.time.LocalDateTime.now(), rulesAllowList);
            if (rule != null) {
                countBlocked();
                finish(403, true);
                return page(HttpResponseStatus.FORBIDDEN, "Đang trong giờ bị chặn",
                        "Trang <b>" + escape(host) + "</b> bị chặn theo lịch (" + escape(rule.describe()) + ").", false);
            }
            if (rulesAllowOnly && !rulesAllowList.matches(host)) {
                countBlocked();
                finish(403, true);
                return page(HttpResponseStatus.FORBIDDEN, "Không nằm trong danh sách cho phép",
                        "Chế độ chỉ cho phép đang bật, trang <b>" + escape(host) + "</b> không được truy cập.", false);
            }
            return null;
        }

        private HttpResponse checkLogin(HttpRequest request) {
            if (loginGuard.isLocked(client)) {
                stats.blocked();
                stats.blockedBy(client, "");
                finish(429, true);
                FullHttpResponse r = page(HttpResponseStatus.TOO_MANY_REQUESTS, "Tạm khóa",
                        "Sai mật khẩu quá nhiều lần. Thử lại sau " + cfg.lockMinutes + " phút.", false);
                r.headers().set("Retry-After", cfg.lockMinutes * 60);
                return r;
            }
            String loggedUser = clientChannel().attr(LOGGED_IN_USER).get();
            if (loggedUser == null) {
                String[] login = loginOf(request);
                if (login == null) {
                    finish(407, false);
                    return loginPage();
                }
                user = login[0].length() > 64 ? login[0].substring(0, 64) : login[0];
                if (!passwordIsRight(login[0], login[1])) {
                    stats.authFailure();
                    stats.authFailureBy(client, isKnownAccount(login[0]) ? login[0] : "");
                    if (loginGuard.failure(client)) {
                        monitor.locked(client, cfg.lockMinutes);
                    }
                    finish(407, false);
                    return loginPage();
                }
                loginGuard.success(client);
                loggedUser = login[0];
                clientChannel().attr(LOGGED_IN_USER).set(loggedUser);
                if (clientChannel().remoteAddress() instanceof InetSocketAddress) {
                    connectionUsers.put((InetSocketAddress) clientChannel().remoteAddress(), loggedUser);
                }
            }
            user = loggedUser;
            request.headers().remove(HttpHeaders.Names.PROXY_AUTHORIZATION);
            return null;
        }

        private void countBlocked() {
            stats.blocked();
            stats.blockedBy(client, user);
            monitor.blocked(client, user);
        }

        private boolean isKnownAccount(String name) {
            UserStore store = users;
            return cfg.username.equals(name) || (store != null && store.find(name) != null);
        }

        private boolean passwordIsRight(String name, String password) {
            if (cfg.username.equals(name)) {
                return cfg.checkPassword(password);
            }
            UserStore store = users;
            UserStore.Account account = store == null ? null : store.find(name);
            if (account == null) {
                PasswordHasher.matches(password, DUMMY_HASH);
                return false;
            }
            return account.checkPassword(password);
        }

        private io.netty.channel.Channel clientChannel() {
            return ctx.channel();
        }

        @Override
        public HttpObject serverToProxyResponse(HttpObject httpObject) {
            if (httpObject instanceof HttpResponse) {
                status = ((HttpResponse) httpObject).getStatus().code();
            }
            return httpObject;
        }

        @Override
        public HttpObject proxyToClientResponse(HttpObject httpObject) {
            if (!tunnel) {
                if (httpObject instanceof HttpResponse) {
                    status = ((HttpResponse) httpObject).getStatus().code();
                }
                if (httpObject instanceof HttpContent) {
                    bytes += ((HttpContent) httpObject).content().readableBytes();
                }
                if (httpObject instanceof LastHttpContent) {
                    finish(status == 0 ? 200 : status, false);
                }
            }
            return httpObject;
        }

        @Override
        public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
            if (tunnel) {
                finish(200, false);
            }
        }

        @Override
        public void proxyToServerConnectionFailed() {
            finish(clientChannel().isActive() ? 502 : 499, false);
        }

        @Override
        public void proxyToServerResolutionFailed(String hostAndPort) {
            finish(502, false);
        }

        @Override
        public void serverToProxyResponseTimedOut() {
            finish(504, false);
        }
    }
}
