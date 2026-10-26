package proxyapp;

import java.io.File;

public class ConsoleMain {

    private static boolean isInteractive() {
        java.io.Console c = System.console();
        if (c == null) {
            return false;
        }
        try {
            return (Boolean) java.io.Console.class.getMethod("isTerminal").invoke(c);
        } catch (Exception e) {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        if (!isInteractive()) {
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8"));
        }
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].equals("--home")) {
                AppDir.set(new File(args[i + 1]));
            }
        }
        for (String a : args) {
            if (a.equals("--restore-system-proxy")) {
                if (!SystemProxy.isSupported()) {
                    System.out.println("Chỉ dùng được trên Windows.");
                    return;
                }
                new SystemProxy().restore();
                System.out.println("Đã khôi phục proxy hệ thống Windows.");
                return;
            }
        }
        File cfgFile = AppDir.file("proxy.properties");
        ProxyConfig cfg = ProxyConfig.load(cfgFile);
        boolean useSystemProxy = false;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--port") && i + 1 < args.length) {
                cfg.port = Integer.parseInt(args[++i]);
            } else if (a.equals("--user") && i + 1 < args.length) {
                cfg.username = args[++i];
                cfg.requireAuth = true;
            } else if (a.equals("--pass") && i + 1 < args.length) {
                cfg.setPassword(args[++i]);
            } else if (a.equals("--lan")) {
                cfg.allowLan = true;
            } else if (a.equals("--allow") && i + 1 < args.length) {
                cfg.allowedClients = args[++i];
            } else if (a.equals("--public") && i + 1 < args.length) {
                cfg.publicListUrls = args[++i].replace(';', '\n');
            } else if (a.equals("--update-hours") && i + 1 < args.length) {
                cfg.publicListHours = Integer.parseInt(args[++i]);
            } else if (a.equals("--mitm")) {
                cfg.mitmEnabled = true;
            } else if (a.equals("--allow-only")) {
                cfg.allowOnlyMode = true;
            } else if (a.equals("--home") && i + 1 < args.length) {
                i++;
            } else if (a.equals("--system-proxy")) {
                useSystemProxy = true;
            } else if (a.equals("--limit") && i + 1 < args.length) {
                cfg.maxKBps = Integer.parseInt(args[++i]);
            } else if (a.equals("--blocklist") && i + 1 < args.length) {
                cfg.blocklistFile = args[++i];
            } else if (a.equals("--logfile") && i + 1 < args.length) {
                cfg.logFile = args[++i];
                cfg.logToFile = true;
            } else {
                System.out.println("Cách dùng: ConsoleMain [--port N] [--user U --pass P] [--lan] [--allow IP/CIDR,...] [--public nguon1;nguon2] [--update-hours N] [--home thư_mục] [--allow-only] [--mitm] [--system-proxy] [--limit KB/s] [--blocklist file] [--logfile file] | ConsoleMain --restore-system-proxy");
                return;
            }
        }
        BlockList blockList = new BlockList(AppDir.file(cfg.blocklistFile));
        blockList.load();
        PublicBlockLists publicLists = new PublicBlockLists(blockList, AppDir.file(cfg.publicListFile));
        publicLists.loadCache();
        publicLists.applySchedule(PublicBlockLists.splitSources(cfg.publicListUrls), cfg.publicListHours);
        BlockList allowList = new BlockList(AppDir.file(cfg.allowlistFile), BlockList.ALLOW_DEFAULT_TEXT);
        allowList.load();
        BlockSchedule schedule = new BlockSchedule(AppDir.file(cfg.scheduleFile));
        schedule.load();
        TrafficLog log = new TrafficLog(2000);
        ProxyStats stats = new ProxyStats();
        log.addListener(new TrafficLog.Listener() {
            public void onEntry(LogEntry e) {
                System.out.println(e.timeText() + " " + e.client + (e.user.isEmpty() ? "" : " (" + e.user + ")") + " " + e.method + " " + e.target
                        + " -> " + e.status + (e.blocked ? " [BLOCKED]" : "") + " " + e.bytes + "B " + e.durationMs + "ms");
            }
        });
        final ProxyService service = new ProxyService(blockList, log, stats);
        service.setAllowList(allowList);
        service.setSchedule(schedule);
        UserStore users = new UserStore(AppDir.file(cfg.usersFile), AppDir.file("users"));
        users.load();
        service.setUsers(users);
        AlertLog alerts = new AlertLog(200);
        alerts.addListener(new AlertLog.Listener() {
            public void onAlert(AlertLog.Alert a) {
                System.out.println(a.timeText() + " [CẢNH BÁO] " + a.text);
            }
        });
        service.setAlerts(alerts);
        BlockList mitmBypass = new BlockList(AppDir.file(cfg.mitmBypassFile), BlockList.BYPASS_DEFAULT_TEXT);
        mitmBypass.load();
        service.setMitmBypass(mitmBypass);
        UrlRules urlRules = new UrlRules(AppDir.file(cfg.urlRulesFile));
        urlRules.load();
        service.setUrlRules(urlRules);
        service.start(cfg);
        if (cfg.mitmEnabled) {
            System.out.println("[MITM] Đang giải mã HTTPS (bỏ qua " + mitmBypass.size() + " tên miền). Chứng chỉ gốc để cài vào máy khách: "
                    + AppDir.file(cfg.caCertFile).getAbsolutePath());
            System.out.println("[MITM] Khóa riêng của CA nằm ở " + AppDir.file(cfg.caFile).getAbsolutePath()
                    + ", phải giữ kín. Chỉ dùng cho thiết bị của chính bạn.");
        }
        System.out.println("Proxy đang chạy tại " + service.getListenAddress() + " - " + blockList.size() + " tên miền tự nhập, "
                + publicLists.size() + " tên miền từ danh sách công khai, "
                + schedule.size() + " dòng lịch chặn, " + users.size() + " tài khoản phụ, " + urlRules.size() + " quy tắc đường dẫn/từ khóa" + (cfg.allowOnlyMode ? ", chế độ chỉ cho phép: " + allowList.size() + " tên miền" : ""));
        final SystemProxy systemProxy = useSystemProxy && SystemProxy.isSupported() ? new SystemProxy() : null;
        if (systemProxy != null) {
            systemProxy.restoreLeftover();
            systemProxy.enable("127.0.0.1", cfg.port);
            System.out.println("Đã đặt proxy hệ thống Windows thành 127.0.0.1:" + cfg.port + " (tự khôi phục khi thoát)");
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (systemProxy != null) {
                    try {
                        systemProxy.restore();
                    } catch (java.io.IOException e) {
                        System.out.println("Không khôi phục được proxy hệ thống: " + e.getMessage());
                    }
                }
                service.stop();
            }
        });
        Thread.currentThread().join();
    }
}
