package proxyapp;

import javax.swing.JOptionPane;

public class MainFrame extends javax.swing.JFrame {

    private final java.io.File configFile = AppDir.file("proxy.properties");
    private ProxyConfig config;
    private BlockList blockList;
    private BlockList allowList;
    private BlockSchedule schedule;
    private UserStore users;
    private BlockList mitmBypass;
    private UrlRules urlRules;
    private final TrafficHistory history = new TrafficHistory(300);
    private final AlertLog alerts = new AlertLog(200);
    private PublicBlockLists publicLists;
    private final SystemProxy systemProxy = new SystemProxy();
    private volatile boolean systemProxyOn;
    private final WindowsStartup startup = new WindowsStartup();
    private TrayIconManager tray;
    private boolean trayHintShown;
    private TrafficLog trafficLog;
    private ProxyStats stats;
    private ProxyService service;
    private javax.swing.table.DefaultTableModel logModel;
    private javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel> logSorter;
    private StatsView statsView;
    private javax.swing.Timer timer;
    private final boolean firstRun = !AppDir.file("proxy.properties").isFile();
    private final String lanIp = NetInfo.lanAddress();

    public MainFrame() {
        initComponents();
        setIconImage(TrayIconManager.createImage());
        config = ProxyConfig.load(configFile);
        blockList = new BlockList(AppDir.file(config.blocklistFile));
        blockList.load();
        publicLists = new PublicBlockLists(blockList, AppDir.file(config.publicListFile));
        publicLists.loadCache();
        publicLists.applySchedule(PublicBlockLists.splitSources(config.publicListUrls), config.publicListHours);
        trafficLog = new TrafficLog(2000);
        stats = new ProxyStats();
        service = new ProxyService(blockList, trafficLog, stats);
        statsView = new StatsView(stats, alerts, tblClients, tblUsers, txtAlerts, txtTop, pnlChart);
        allowList = new BlockList(AppDir.file(config.allowlistFile), BlockList.ALLOW_DEFAULT_TEXT);
        allowList.load();
        schedule = new BlockSchedule(AppDir.file(config.scheduleFile));
        schedule.load();
        service.setAllowList(allowList);
        service.setSchedule(schedule);
        users = new UserStore(AppDir.file(config.usersFile), AppDir.file("users"));
        users.load();
        service.setUsers(users);
        mitmBypass = new BlockList(AppDir.file(config.mitmBypassFile), BlockList.BYPASS_DEFAULT_TEXT);
        mitmBypass.load();
        service.setMitmBypass(mitmBypass);
        urlRules = new UrlRules(AppDir.file(config.urlRulesFile));
        urlRules.load();
        service.setUrlRules(urlRules);
        service.setAlerts(alerts);
        alerts.addListener(new AlertLog.Listener() {
            public void onAlert(final AlertLog.Alert alert) {
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (tray != null) {
                            tray.message("Cảnh báo", alert.text);
                        }
                    }
                });
            }
        });
        logModel = new javax.swing.table.DefaultTableModel(
                new String[]{"Giờ", "Máy khách", "Người dùng", "Loại", "Tên miền", "Địa chỉ", "Mã", "Byte", "ms", "Chặn"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblLog.setModel(logModel);
        logSorter = new javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel>(logModel);
        logSorter.setSortable(6, false);
        tblLog.setRowSorter(logSorter);
        txtFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyLogFilter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyLogFilter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyLogFilter();
            }
        });
        int[] widths = {70, 90, 95, 65, 150, 330, 45, 65, 50, 55};
        for (int i = 0; i < widths.length; i++) {
            tblLog.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        trafficLog.addListener(new TrafficLog.Listener() {
            public void onEntry(final LogEntry entry) {
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        addLogRow(entry);
                    }
                });
            }
        });
        tray = new TrayIconManager();
        if (!tray.install(new TrayIconManager.Actions() {
            public void showWindow() {
                showAgain();
            }

            public void toggleProxy() {
                if (service.isRunning()) {
                    btnStopActionPerformed(null);
                } else {
                    btnStartActionPerformed(null);
                }
            }

            public void exitApp() {
                MainFrame.this.exitApp();
            }
        })) {
            tray = null;
        }
        applyConfigToFields();
        txtBlock.setText(blockList.getText());
        new Thread(new Runnable() {
            public void run() {
                try {
                    systemProxy.restoreLeftover();
                } catch (java.io.IOException ex) {
                }
                try {
                    if (SystemProxy.isSupported()) {
                        final boolean on = startup.isEnabled();
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                chkWinStart.setSelected(on);
                            }
                        });
                    }
                } catch (java.io.IOException ex) {
                }
            }
        }).start();
        styleComponents();
        timer = new javax.swing.Timer(1000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refreshStats();
            }
        });
        timer.start();
        refreshStats();
        setLocationRelativeTo(null);
        if (config.autoStartProxy) {
            btnStartActionPerformed(null);
        }
    }

    public boolean hasTray() {
        return tray != null;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlTop = new javax.swing.JPanel();
        btnStart = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        lblStatus = new javax.swing.JLabel();
        btnSystemProxy = new javax.swing.JButton();
        tabs = new javax.swing.JTabbedPane();
        pnlGeneral = new javax.swing.JPanel();
        pnlConn = new javax.swing.JPanel();
        lblPort = new javax.swing.JLabel();
        txtPort = new javax.swing.JTextField();
        chkLan = new javax.swing.JCheckBox();
        lblLimit = new javax.swing.JLabel();
        txtLimit = new javax.swing.JTextField();
        chkLogFile = new javax.swing.JCheckBox();
        pnlSecurity = new javax.swing.JPanel();
        chkAuth = new javax.swing.JCheckBox();
        lblUser = new javax.swing.JLabel();
        txtUser = new javax.swing.JTextField();
        lblPass = new javax.swing.JLabel();
        txtPass = new javax.swing.JPasswordField();
        lblAllow = new javax.swing.JLabel();
        txtAllow = new javax.swing.JTextField();
        btnUsers = new javax.swing.JButton();
        pnlSystem = new javax.swing.JPanel();
        chkTray = new javax.swing.JCheckBox();
        chkAutoProxy = new javax.swing.JCheckBox();
        chkWinStart = new javax.swing.JCheckBox();
        lblHint = new javax.swing.JLabel();
        pnlRules = new javax.swing.JPanel();
        lblBlock = new javax.swing.JLabel();
        scrollBlock = new javax.swing.JScrollPane();
        txtBlock = new javax.swing.JTextArea();
        btnSaveBlock = new javax.swing.JButton();
        btnPublic = new javax.swing.JButton();
        lblPublicHint = new javax.swing.JLabel();
        btnRules = new javax.swing.JButton();
        lblRulesHint = new javax.swing.JLabel();
        btnHttps = new javax.swing.JButton();
        lblHttpsHint = new javax.swing.JLabel();
        pnlLog = new javax.swing.JPanel();
        lblFilter = new javax.swing.JLabel();
        txtFilter = new javax.swing.JTextField();
        chkBlockedOnly = new javax.swing.JCheckBox();
        btnClearLog = new javax.swing.JButton();
        btnExport = new javax.swing.JButton();
        scrollLog = new javax.swing.JScrollPane();
        tblLog = new javax.swing.JTable();
        pnlStats = new javax.swing.JPanel();
        lblChart = new javax.swing.JLabel();
        pnlChart = new TrafficChart(history);
        lblClients = new javax.swing.JLabel();
        lblUsers = new javax.swing.JLabel();
        scrollClients = new javax.swing.JScrollPane();
        tblClients = new javax.swing.JTable();
        scrollUsers = new javax.swing.JScrollPane();
        tblUsers = new javax.swing.JTable();
        lblAlerts = new javax.swing.JLabel();
        lblTop = new javax.swing.JLabel();
        scrollAlerts = new javax.swing.JScrollPane();
        txtAlerts = new javax.swing.JTextArea();
        scrollTop = new javax.swing.JScrollPane();
        txtTop = new javax.swing.JTextArea();
        btnClearAlerts = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        pnlStatus = new javax.swing.JPanel();
        lblReq = new javax.swing.JLabel();
        lblBlocked = new javax.swing.JLabel();
        lblConn = new javax.swing.JLabel();
        lblDown = new javax.swing.JLabel();
        lblUp = new javax.swing.JLabel();
        lblFail = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Proxy Server");
        setMinimumSize(new java.awt.Dimension(900, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        btnStart.setText("Bật proxy");
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });

        btnStop.setText("Tắt proxy");
        btnStop.setEnabled(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        lblStatus.setText("● Đã dừng");

        btnSystemProxy.setText("Bật proxy hệ thống Windows");
        btnSystemProxy.setEnabled(false);
        btnSystemProxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSystemProxyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlTopLayout = new javax.swing.GroupLayout(pnlTop);
        pnlTop.setLayout(pnlTopLayout);
        pnlTopLayout.setHorizontalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTopLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(btnStart, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(btnStop, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addComponent(btnSystemProxy, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12))
        );
        pnlTopLayout.setVerticalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTopLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(btnStart, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnStop, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblStatus)
                    .addComponent(btnSystemProxy, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10))
        );

        getContentPane().add(pnlTop, java.awt.BorderLayout.NORTH);

        pnlConn.setBorder(javax.swing.BorderFactory.createTitledBorder("Kết nối"));

        lblPort.setText("Cổng:");

        txtPort.setText("8080");

        chkLan.setText("Cho phép máy khác trong mạng LAN dùng");

        lblLimit.setText("Giới hạn tốc độ (KB/s):");
        lblLimit.setToolTipText("0 = không giới hạn");

        txtLimit.setText("0");
        txtLimit.setToolTipText("0 = không giới hạn");

        chkLogFile.setText("Ghi nhật ký ra file proxy-log.csv");

        javax.swing.GroupLayout pnlConnLayout = new javax.swing.GroupLayout(pnlConn);
        pnlConn.setLayout(pnlConnLayout);
        pnlConnLayout.setHorizontalGroup(
            pnlConnLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlConnLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlConnLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlConnLayout.createSequentialGroup()
                        .addComponent(lblPort, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32)
                        .addComponent(chkLan))
                    .addGroup(pnlConnLayout.createSequentialGroup()
                        .addComponent(lblLimit, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(txtLimit, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32)
                        .addComponent(chkLogFile)))
                .addGap(12, 12, 12))
        );
        pnlConnLayout.setVerticalGroup(
            pnlConnLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlConnLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(pnlConnLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPort)
                    .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkLan))
                .addGap(8, 8, 8)
                .addGroup(pnlConnLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblLimit)
                    .addComponent(txtLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkLogFile))
                .addGap(10, 10, 10))
        );

        pnlSecurity.setBorder(javax.swing.BorderFactory.createTitledBorder("Bảo mật"));

        chkAuth.setText("Yêu cầu đăng nhập");

        lblUser.setText("Tài khoản:");

        txtUser.setText("admin");

        lblPass.setText("Mật khẩu:");

        lblAllow.setText("IP máy được phép:");
        lblAllow.setToolTipText("Để trống = tất cả. Ví dụ: 192.168.1.0/24, 10.0.0.5");

        txtAllow.setToolTipText("Để trống = tất cả. Ví dụ: 192.168.1.0/24, 10.0.0.5");

        btnUsers.setText("Tài khoản khác...");
        btnUsers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUsersActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlSecurityLayout = new javax.swing.GroupLayout(pnlSecurity);
        pnlSecurity.setLayout(pnlSecurityLayout);
        pnlSecurityLayout.setHorizontalGroup(
            pnlSecurityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSecurityLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlSecurityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkAuth)
                    .addGroup(pnlSecurityLayout.createSequentialGroup()
                        .addComponent(lblUser, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32)
                        .addComponent(lblPass, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(txtPass, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlSecurityLayout.createSequentialGroup()
                        .addComponent(lblAllow, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(txtAllow)
                        .addGap(8, 8, 8)
                        .addComponent(btnUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(12, 12, 12))
        );
        pnlSecurityLayout.setVerticalGroup(
            pnlSecurityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSecurityLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(chkAuth)
                .addGap(8, 8, 8)
                .addGroup(pnlSecurityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUser)
                    .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPass)
                    .addComponent(txtPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(pnlSecurityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAllow)
                    .addComponent(txtAllow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnUsers))
                .addGap(10, 10, 10))
        );

        pnlSystem.setBorder(javax.swing.BorderFactory.createTitledBorder("Hệ thống"));

        chkTray.setText("Đóng cửa sổ thì chạy nền ở khay hệ thống");
        chkTray.setToolTipText("Khi proxy đang chạy, bấm X chỉ ẩn cửa sổ xuống khay hệ thống");

        chkAutoProxy.setText("Tự bật proxy khi mở app");

        chkWinStart.setText("Tự mở cùng Windows");
        chkWinStart.setToolTipText("Chỉ dùng được khi chạy từ file .exe hoặc .jar");
        chkWinStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkWinStartActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlSystemLayout = new javax.swing.GroupLayout(pnlSystem);
        pnlSystem.setLayout(pnlSystemLayout);
        pnlSystemLayout.setHorizontalGroup(
            pnlSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSystemLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkTray)
                    .addComponent(chkAutoProxy)
                    .addComponent(chkWinStart))
                .addGap(12, 12, 12))
        );
        pnlSystemLayout.setVerticalGroup(
            pnlSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSystemLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(chkTray)
                .addGap(4, 4, 4)
                .addComponent(chkAutoProxy)
                .addGap(4, 4, 4)
                .addComponent(chkWinStart)
                .addGap(10, 10, 10))
        );

        lblHint.setText("Đặt proxy trình duyệt là 127.0.0.1 và cổng đã chọn");

        javax.swing.GroupLayout pnlGeneralLayout = new javax.swing.GroupLayout(pnlGeneral);
        pnlGeneral.setLayout(pnlGeneralLayout);
        pnlGeneralLayout.setHorizontalGroup(
            pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlConn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlSecurity, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlSystem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblHint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(12, 12, 12))
        );
        pnlGeneralLayout.setVerticalGroup(
            pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(pnlConn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(pnlSecurity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(pnlSystem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(lblHint)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        tabs.addTab("Tổng quan", pnlGeneral);

        lblBlock.setText("Danh sách chặn (mỗi dòng một tên miền):");

        txtBlock.setColumns(20);
        txtBlock.setRows(5);
        scrollBlock.setViewportView(txtBlock);

        btnSaveBlock.setText("Lưu danh sách chặn");
        btnSaveBlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveBlockActionPerformed(evt);
            }
        });

        btnPublic.setText("Danh sách chặn công khai...");
        btnPublic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPublicActionPerformed(evt);
            }
        });

        lblPublicHint.setText("Nạp danh sách quảng cáo, theo dõi, mã độc từ file hoặc URL");

        btnRules.setText("Chỉ cho phép & lịch chặn theo giờ...");
        btnRules.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRulesActionPerformed(evt);
            }
        });

        lblRulesHint.setText("Chỉ cho vào một số trang, hoặc chặn theo giờ trong ngày");

        btnHttps.setText("Giải mã HTTPS...");
        btnHttps.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHttpsActionPerformed(evt);
            }
        });

        lblHttpsHint.setText("Thấy đường dẫn HTTPS để chặn theo đường dẫn, từ khóa");

        javax.swing.GroupLayout pnlRulesLayout = new javax.swing.GroupLayout(pnlRules);
        pnlRules.setLayout(pnlRulesLayout);
        pnlRulesLayout.setHorizontalGroup(
            pnlRulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlRulesLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlRulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblBlock)
                    .addComponent(scrollBlock)
                    .addComponent(btnSaveBlock, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addGroup(pnlRulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnPublic, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPublicHint, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRules, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRulesHint, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnHttps, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHttpsHint, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );
        pnlRulesLayout.setVerticalGroup(
            pnlRulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlRulesLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlRulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlRulesLayout.createSequentialGroup()
                        .addComponent(lblBlock)
                        .addGap(6, 6, 6)
                        .addComponent(scrollBlock)
                        .addGap(8, 8, 8)
                        .addComponent(btnSaveBlock, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlRulesLayout.createSequentialGroup()
                        .addComponent(btnPublic, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(lblPublicHint)
                        .addGap(20, 20, 20)
                        .addComponent(btnRules, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(lblRulesHint)
                        .addGap(20, 20, 20)
                        .addComponent(btnHttps, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(lblHttpsHint)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(12, 12, 12))
        );

        tabs.addTab("Quy tắc", pnlRules);

        lblFilter.setText("Lọc:");

        txtFilter.setToolTipText("Gõ tên miền, địa chỉ hoặc tên người dùng");

        chkBlockedOnly.setText("Chỉ hiện dòng bị chặn");
        chkBlockedOnly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkBlockedOnlyActionPerformed(evt);
            }
        });

        btnClearLog.setText("Xóa nhật ký");
        btnClearLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearLogActionPerformed(evt);
            }
        });

        btnExport.setText("Xuất CSV");
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });

        scrollLog.setViewportView(tblLog);

        javax.swing.GroupLayout pnlLogLayout = new javax.swing.GroupLayout(pnlLog);
        pnlLog.setLayout(pnlLogLayout);
        pnlLogLayout.setHorizontalGroup(
            pnlLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLogLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlLogLayout.createSequentialGroup()
                        .addComponent(lblFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(txtFilter)
                        .addGap(12, 12, 12)
                        .addComponent(chkBlockedOnly)
                        .addGap(12, 12, 12)
                        .addComponent(btnClearLog, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(scrollLog))
                .addGap(12, 12, 12))
        );
        pnlLogLayout.setVerticalGroup(
            pnlLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLogLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblFilter)
                    .addComponent(txtFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkBlockedOnly)
                    .addComponent(btnClearLog)
                    .addComponent(btnExport))
                .addGap(8, 8, 8)
                .addComponent(scrollLog, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                .addGap(12, 12, 12))
        );

        tabs.addTab("Nhật ký", pnlLog);

        lblChart.setText("Lưu lượng 5 phút gần nhất (tải xuống / tải lên):");

        javax.swing.GroupLayout pnlChartLayout = new javax.swing.GroupLayout(pnlChart);
        pnlChart.setLayout(pnlChartLayout);
        pnlChartLayout.setHorizontalGroup(
            pnlChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 700, Short.MAX_VALUE)
        );
        pnlChartLayout.setVerticalGroup(
            pnlChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 170, Short.MAX_VALUE)
        );

        lblClients.setText("Theo máy khách:");

        lblUsers.setText("Theo người dùng:");

        scrollClients.setViewportView(tblClients);

        scrollUsers.setViewportView(tblUsers);

        lblAlerts.setText("Cảnh báo (mới nhất ở dưới):");

        lblTop.setText("Tên miền truy cập nhiều nhất:");

        txtAlerts.setEditable(false);
        txtAlerts.setColumns(20);
        txtAlerts.setRows(5);
        scrollAlerts.setViewportView(txtAlerts);

        txtTop.setEditable(false);
        txtTop.setColumns(20);
        txtTop.setRows(5);
        scrollTop.setViewportView(txtTop);

        btnClearAlerts.setText("Xóa cảnh báo");
        btnClearAlerts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearAlertsActionPerformed(evt);
            }
        });

        btnReset.setText("Đặt lại thống kê");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlStatsLayout = new javax.swing.GroupLayout(pnlStats);
        pnlStats.setLayout(pnlStatsLayout);
        pnlStatsLayout.setHorizontalGroup(
            pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStatsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblChart)
                    .addComponent(pnlChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlStatsLayout.createSequentialGroup()
                        .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblClients)
                            .addComponent(scrollClients, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                        .addGap(12, 12, 12)
                        .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblUsers)
                            .addComponent(scrollUsers, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)))
                    .addGroup(pnlStatsLayout.createSequentialGroup()
                        .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblAlerts)
                            .addComponent(scrollAlerts, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE))
                        .addGap(12, 12, 12)
                        .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTop)
                            .addComponent(scrollTop, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                    .addGroup(pnlStatsLayout.createSequentialGroup()
                        .addComponent(btnClearAlerts, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(12, 12, 12))
        );
        pnlStatsLayout.setVerticalGroup(
            pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStatsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(lblChart)
                .addGap(4, 4, 4)
                .addComponent(pnlChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(8, 8, 8)
                .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblClients)
                    .addComponent(lblUsers))
                .addGap(4, 4, 4)
                .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollClients, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                    .addComponent(scrollUsers, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE))
                .addGap(8, 8, 8)
                .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblAlerts)
                    .addComponent(lblTop))
                .addGap(4, 4, 4)
                .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollAlerts, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .addComponent(scrollTop, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE))
                .addGap(8, 8, 8)
                .addGroup(pnlStatsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnClearAlerts)
                    .addComponent(btnReset))
                .addGap(10, 10, 10))
        );

        tabs.addTab("Thống kê", pnlStats);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        lblReq.setText("Yêu cầu: 0");

        lblBlocked.setText("Bị chặn: 0");

        lblConn.setText("Kết nối: 0");

        lblDown.setText("Tải xuống: 0 B");

        lblUp.setText("Tải lên: 0 B");

        lblFail.setText("Sai mật khẩu: 0");

        javax.swing.GroupLayout pnlStatusLayout = new javax.swing.GroupLayout(pnlStatus);
        pnlStatus.setLayout(pnlStatusLayout);
        pnlStatusLayout.setHorizontalGroup(
            pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStatusLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(lblReq)
                .addGap(28, 28, 28)
                .addComponent(lblBlocked)
                .addGap(28, 28, 28)
                .addComponent(lblConn)
                .addGap(28, 28, 28)
                .addComponent(lblDown)
                .addGap(28, 28, 28)
                .addComponent(lblUp)
                .addGap(28, 28, 28)
                .addComponent(lblFail)
                .addGap(0, 12, Short.MAX_VALUE))
        );
        pnlStatusLayout.setVerticalGroup(
            pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStatusLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblReq)
                    .addComponent(lblBlocked)
                    .addComponent(lblConn)
                    .addComponent(lblDown)
                    .addComponent(lblUp)
                    .addComponent(lblFail))
                .addGap(6, 6, 6))
        );

        getContentPane().add(pnlStatus, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed
        try {
            readConfigFromFields();
            service.start(config);
            config.save(configFile);
            txtPass.setText("");
            updatePasswordHint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể bật proxy: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        updateRunState();
    }//GEN-LAST:event_btnStartActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        btnStop.setEnabled(false);
        new Thread(new Runnable() {
            public void run() {
                service.stop();
                restoreSystemProxy();
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateRunState();
                    }
                });
            }
        }).start();
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnHttpsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHttpsActionPerformed
        new HttpsDialog(this, config, configFile, mitmBypass, urlRules, service).setVisible(true);
    }//GEN-LAST:event_btnHttpsActionPerformed

    private void btnUsersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUsersActionPerformed
        new UsersDialog(this, users, config).setVisible(true);
    }//GEN-LAST:event_btnUsersActionPerformed

    private void btnRulesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRulesActionPerformed
        new AccessRulesDialog(this, service, allowList, schedule, config, configFile).setVisible(true);
    }//GEN-LAST:event_btnRulesActionPerformed

    private void btnSystemProxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSystemProxyActionPerformed
        if (!service.isRunning()) {
            return;
        }
        btnSystemProxy.setEnabled(false);
        final boolean turnOn = !systemProxyOn;
        final int port = service.getActiveConfig().port;
        new Thread(new Runnable() {
            public void run() {
                String error = null;
                try {
                    if (turnOn) {
                        systemProxy.enable("127.0.0.1", port);
                    } else {
                        systemProxy.restore();
                    }
                    systemProxyOn = turnOn;
                } catch (java.io.IOException ex) {
                    error = ex.getMessage();
                }
                final String message = error;
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (message != null) {
                            JOptionPane.showMessageDialog(MainFrame.this, "Không đổi được proxy hệ thống: " + message, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                        updateRunState();
                    }
                });
            }
        }).start();
    }//GEN-LAST:event_btnSystemProxyActionPerformed

    private void btnPublicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPublicActionPerformed
        new PublicListDialog(this, config, configFile, publicLists).setVisible(true);
    }//GEN-LAST:event_btnPublicActionPerformed

    private void btnSaveBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveBlockActionPerformed
        try {
            blockList.setText(txtBlock.getText());
            JOptionPane.showMessageDialog(this, "Đã lưu " + blockList.size() + " tên miền. Áp dụng ngay, không cần tắt proxy.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSaveBlockActionPerformed

    private void btnClearLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearLogActionPerformed
        trafficLog.clear();
        logModel.setRowCount(0);
    }//GEN-LAST:event_btnClearLogActionPerformed

    private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportActionPerformed
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setSelectedFile(new java.io.File("proxy-log.csv"));
        if (fc.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File saved = fc.getSelectedFile().getAbsoluteFile();
                trafficLog.exportCsv(saved);
                Object[] options = {"Mở thư mục", "Đóng"};
                int choice = JOptionPane.showOptionDialog(this, "Đã xuất nhật ký ra:\n" + saved.getPath(),
                        "Đã xuất nhật ký", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                if (choice == 0) {
                    FileLocator.show(this, saved);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không xuất được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnExportActionPerformed

    private void chkBlockedOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkBlockedOnlyActionPerformed
        applyLogFilter();
    }//GEN-LAST:event_chkBlockedOnlyActionPerformed

    private void btnClearAlertsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearAlertsActionPerformed
        alerts.clear();
        statsView.refresh();
    }//GEN-LAST:event_btnClearAlertsActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        int answer = JOptionPane.showConfirmDialog(this, "Đặt lại toàn bộ thống kê và biểu đồ về 0?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            stats.reset();
            history.clear();
            statsView.refresh();
        }
    }//GEN-LAST:event_btnResetActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (tray != null && chkTray.isSelected() && service.isRunning()) {
            saveConfigQuietly();
            setVisible(false);
            if (!trayHintShown) {
                trayHintShown = true;
                tray.message("Proxy Server", "Proxy vẫn chạy ở khay hệ thống. Bấm đúp vào biểu tượng để mở lại.");
            }
        } else {
            exitApp();
        }
    }//GEN-LAST:event_formWindowClosing

    private void chkWinStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkWinStartActionPerformed
        final boolean turnOn = chkWinStart.isSelected();
        chkWinStart.setEnabled(false);
        new Thread(new Runnable() {
            public void run() {
                String error = null;
                try {
                    if (turnOn) {
                        startup.enable(WindowsStartup.commandForThisApp());
                    } else {
                        startup.disable();
                    }
                } catch (java.io.IOException ex) {
                    error = ex.getMessage();
                }
                final String message = error;
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (message != null) {
                            chkWinStart.setSelected(!turnOn);
                            JOptionPane.showMessageDialog(MainFrame.this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                        chkWinStart.setEnabled(true);
                    }
                });
            }
        }).start();
    }//GEN-LAST:event_chkWinStartActionPerformed

    private void saveConfigQuietly() {
        try {
            readConfigFromFields();
            config.save(configFile);
        } catch (Exception ex) {
        }
    }

    private void showAgain() {
        setVisible(true);
        setExtendedState(javax.swing.JFrame.NORMAL);
        toFront();
        requestFocus();
    }

    private void exitApp() {
        saveConfigQuietly();
        restoreSystemProxy();
        service.stop();
        publicLists.stopAuto();
        if (tray != null) {
            tray.remove();
        }
        System.exit(0);
    }

    private void restoreSystemProxy() {
        if (!systemProxyOn) {
            return;
        }
        try {
            systemProxy.restore();
            systemProxyOn = false;
        } catch (final java.io.IOException ex) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(MainFrame.this, "Không khôi phục được proxy hệ thống: " + ex.getMessage()
                            + "\nHãy tắt proxy trong Cài đặt Windows > Mạng > Proxy.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void addLogRow(LogEntry e) {
        if (logModel.getRowCount() >= 2000) {
            logModel.removeRow(0);
        }
        logModel.addRow(new Object[]{e.timeText().substring(11), e.client, e.user, e.method, e.host, e.target, e.status, e.bytes, e.durationMs, e.blocked ? "Có" : ""});
        int view = tblLog.convertRowIndexToView(logModel.getRowCount() - 1);
        if (view >= 0) {
            tblLog.scrollRectToVisible(tblLog.getCellRect(view, 0, true));
        }
    }

    private void updatePasswordHint() {
        txtPass.putClientProperty("JTextField.placeholderText",
                config.hasPassword() ? "Đã lưu mật khẩu (để trống = giữ nguyên)" : "Chưa đặt mật khẩu");
        txtPass.repaint();
    }

    private void addHeaderTips(final javax.swing.JTable table, final boolean log) {
        final javax.swing.table.JTableHeader header = table.getTableHeader();
        header.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int view = header.columnAtPoint(e.getPoint());
                int model = view < 0 ? -1 : table.convertColumnIndexToModel(view);
                header.setToolTipText(log ? Hints.logColumn(model) : Hints.statsColumn(model));
            }
        });
    }

    private void setupHints() {
        lblPort.setText("Cổng proxy:");
        lblPort.setToolTipText(Hints.PORT);
        txtPort.setToolTipText(Hints.PORT);
        chkLan.setText("Cho phép máy khác trong mạng LAN dùng (điện thoại, laptop khác)");
        chkLan.setToolTipText(Hints.LAN);
        chkAuth.setText("Yêu cầu đăng nhập (bắt buộc khi bật LAN)");
        chkAuth.setToolTipText(Hints.AUTH);
        lblUser.setToolTipText(Hints.USER);
        txtUser.setToolTipText(Hints.USER);
        txtPass.setToolTipText(Hints.PASSWORD);
        lblBlock.setText("Danh sách chặn: mỗi dòng một trang, ví dụ youtube.com");
        lblBlock.setToolTipText(Hints.BLOCK_LIST);
        txtBlock.setToolTipText(Hints.BLOCK_LIST);
        btnSaveBlock.setToolTipText(Hints.SAVE_BLOCK);
        txtFilter.setToolTipText(Hints.FILTER);
        chkBlockedOnly.setToolTipText(Hints.BLOCKED_ONLY);
        lblChart.setText("Lưu lượng mạng 5 phút gần nhất (xanh = tải về, cam = gửi đi):");
        lblChart.setToolTipText(Hints.CHART);
        pnlChart.setToolTipText(Hints.CHART);
        lblClients.setText("Theo máy khách (mỗi máy dùng proxy một dòng):");
        lblUsers.setText("Theo người dùng (khi có bật đăng nhập):");
        lblAlerts.setText("Cảnh báo (máy bị chặn nhiều lần, sai mật khẩu, máy lạ):");
        lblAlerts.setToolTipText(Hints.ALERTS);
        txtAlerts.setToolTipText(Hints.ALERTS);
        lblTop.setText("Trang được truy cập nhiều nhất (đếm số yêu cầu):");
        lblTop.setToolTipText(Hints.TOP_HOSTS);
        txtTop.setToolTipText(Hints.TOP_HOSTS);
        btnClearAlerts.setToolTipText(Hints.CLEAR_ALERTS);
        btnReset.setToolTipText(Hints.RESET_STATS);
        lblReq.setToolTipText(Hints.REQUESTS);
        lblBlocked.setToolTipText(Hints.BLOCKED);
        lblConn.setToolTipText(Hints.CONNECTIONS);
        lblDown.setToolTipText(Hints.DOWNLOAD);
        lblUp.setToolTipText(Hints.UPLOAD);
        lblFail.setToolTipText(Hints.WRONG_PASSWORD);
        addHeaderTips(tblLog, true);
        addHeaderTips(tblClients, false);
        addHeaderTips(tblUsers, false);
    }

    private javax.swing.JMenuItem helpItem(String label, final String title, final String text) {
        javax.swing.JMenuItem item = new javax.swing.JMenuItem(label);
        item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                new HelpDialog(MainFrame.this, title, text).setVisible(true);
            }
        });
        return item;
    }

    private void setupHelpMenu() {
        javax.swing.JMenu menu = new javax.swing.JMenu("Trợ giúp");
        menu.add(helpItem("Giải thích các thẻ", "Giải thích các thẻ", Hints.tabsText()));
        menu.add(helpItem("Ý nghĩa mã trạng thái (200, 403, 407...)", "Ý nghĩa mã trạng thái", Hints.codesText()));
        javax.swing.JMenuItem quick = new javax.swing.JMenuItem("Hướng dẫn nhanh");
        quick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                new WelcomeDialog(MainFrame.this, config.port).setVisible(true);
            }
        });
        menu.add(quick);
        javax.swing.JMenuBar bar = new javax.swing.JMenuBar();
        bar.add(menu);
        setJMenuBar(bar);
        getRootPane().revalidate();
    }

    private void setupLogCopy() {
        javax.swing.table.DefaultTableCellRenderer tip = new javax.swing.table.DefaultTableCellRenderer() {
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setToolTipText(value == null ? null : value.toString());
                return c;
            }
        };
        tblLog.getColumnModel().getColumn(4).setCellRenderer(tip);
        tblLog.getColumnModel().getColumn(5).setCellRenderer(tip);
        final javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem copy = new javax.swing.JMenuItem("Sao chép địa chỉ");
        copy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                copySelectedAddresses();
            }
        });
        menu.add(copy);
        tblLog.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                showLogMenu(e, menu);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                showLogMenu(e, menu);
            }
        });
    }

    private void showLogMenu(java.awt.event.MouseEvent e, javax.swing.JPopupMenu menu) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = tblLog.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        if (!tblLog.isRowSelected(row)) {
            tblLog.setRowSelectionInterval(row, row);
        }
        menu.show(tblLog, e.getX(), e.getY());
    }

    private void copySelectedAddresses() {
        StringBuilder sb = new StringBuilder();
        for (int row : tblLog.getSelectedRows()) {
            int modelRow = tblLog.convertRowIndexToModel(row);
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(logModel.getValueAt(modelRow, 5));
        }
        if (sb.length() > 0) {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
        }
    }

    private void applyLogFilter() {
        logSorter.setRowFilter(LogFilter.build(txtFilter.getText(), chkBlockedOnly.isSelected()));
    }

    private void styleComponents() {
        java.awt.Font bold = btnStart.getFont().deriveFont(java.awt.Font.BOLD);
        btnStart.setFont(bold);
        btnStop.setFont(bold);
        btnStart.putClientProperty("JButton.buttonType", "roundRect");
        btnStop.putClientProperty("JButton.buttonType", "roundRect");
        btnSystemProxy.putClientProperty("JButton.buttonType", "roundRect");
        lblStatus.setFont(bold.deriveFont(14f));
        pnlTop.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE));
        pnlTop.setBackground(Theme.PANEL);
        pnlStatus.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE));
        pnlStatus.setBackground(Theme.PANEL);
        java.awt.Font small = lblHint.getFont().deriveFont(12f);
        javax.swing.JLabel[] hints = {lblHint, lblPublicHint, lblRulesHint, lblHttpsHint};
        for (javax.swing.JLabel h : hints) {
            h.setFont(small);
            h.setForeground(Theme.GRAY);
        }
        javax.swing.JButton[] cards = {btnPublic, btnRules, btnHttps};
        for (javax.swing.JButton b : cards) {
            b.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
            b.setFont(b.getFont().deriveFont(java.awt.Font.BOLD));
        }
        javax.swing.JTable[] tables = {tblLog, tblClients, tblUsers};
        for (javax.swing.JTable t : tables) {
            t.setRowHeight(24);
            t.getTableHeader().setReorderingAllowed(false);
            t.setShowGrid(false);
            t.setShowHorizontalLines(true);
            t.setGridColor(Theme.LINE);
        }
        tblLog.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        txtBlock.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        txtAlerts.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        txtTop.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        txtFilter.putClientProperty("JTextField.placeholderText", "Gõ tên miền, địa chỉ hoặc người dùng để lọc");
        txtAllow.putClientProperty("JTextField.placeholderText", "Để trống = cho tất cả máy");
        setupLogCopy();
        setupHints();
        setupHelpMenu();
        updatePasswordHint();
        updateRunState();
    }

    private void applyConfigToFields() {
        txtPort.setText(String.valueOf(config.port));
        chkLan.setSelected(config.allowLan);
        chkAuth.setSelected(config.requireAuth);
        txtUser.setText(config.username);
        txtPass.setText("");
        txtPass.setToolTipText("Để trống = giữ mật khẩu đã lưu");
        txtLimit.setText(String.valueOf(config.maxKBps));
        chkLogFile.setSelected(config.logToFile);
        txtAllow.setText(config.allowedClients);
        chkTray.setSelected(config.closeToTray);
        chkTray.setEnabled(tray != null);
        chkAutoProxy.setSelected(config.autoStartProxy);
        chkWinStart.setEnabled(SystemProxy.isSupported());
    }

    private void readConfigFromFields() {
        try {
            config.port = Integer.parseInt(txtPort.getText().trim());
            config.maxKBps = Integer.parseInt(txtLimit.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cổng và giới hạn tốc độ phải là số nguyên");
        }
        config.allowLan = chkLan.isSelected();
        config.requireAuth = chkAuth.isSelected();
        config.username = txtUser.getText().trim();
        String typed = new String(txtPass.getPassword());
        if (!typed.isEmpty()) {
            config.setPassword(typed);
        }
        config.logToFile = chkLogFile.isSelected();
        config.allowedClients = txtAllow.getText().trim();
        config.closeToTray = chkTray.isSelected();
        config.autoStartProxy = chkAutoProxy.isSelected();
        config.validate();
    }

    private void updateRunState() {
        boolean running = service.isRunning();
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        btnStart.setBackground(running ? Theme.buttonColor() : Theme.GREEN);
        btnStart.setForeground(running ? Theme.buttonTextColor() : java.awt.Color.WHITE);
        btnStop.setBackground(running ? Theme.RED : Theme.buttonColor());
        btnStop.setForeground(running ? java.awt.Color.WHITE : Theme.buttonTextColor());
        txtPort.setEnabled(!running);
        chkLan.setEnabled(!running);
        chkAuth.setEnabled(!running);
        txtUser.setEnabled(!running);
        txtPass.setEnabled(!running);
        txtLimit.setEnabled(!running);
        chkLogFile.setEnabled(!running);
        btnSystemProxy.setEnabled(running && SystemProxy.isSupported());
        btnSystemProxy.setText(systemProxyOn ? "Tắt proxy hệ thống Windows" : "Bật proxy hệ thống Windows");
        btnSystemProxy.setBackground(systemProxyOn ? Theme.GREEN : Theme.buttonColor());
        btnSystemProxy.setForeground(systemProxyOn ? java.awt.Color.WHITE : Theme.buttonTextColor());
        txtAllow.setEnabled(!running);
        refreshStats();
    }

    private static String formatBytes(long b) {
        if (b < 1024) {
            return b + " B";
        }
        if (b < 1024L * 1024) {
            return String.format("%.1f KB", b / 1024.0);
        }
        return String.format("%.2f MB", b / 1024.0 / 1024.0);
    }

    private void refreshStats() {
        history.sample(stats.getBytesToClients(), stats.getBytesFromClients());
        ProxyConfig active = service.getActiveConfig();
        if (active != null) {
            lblStatus.setText("\u25CF Đang chạy, cổng " + active.port);
            lblStatus.setForeground(Theme.GREEN);
            if (active.allowLan && lanIp != null) {
                lblHint.setText("Trình duyệt trên máy này: 127.0.0.1, cổng " + active.port + ". Điện thoại hoặc máy khác: " + lanIp + ", cổng " + active.port);
            } else {
                lblHint.setText("Trình duyệt: đặt proxy 127.0.0.1, cổng " + active.port);
            }
        } else {
            lblStatus.setText("\u25CF Đã dừng");
            lblStatus.setForeground(Theme.GRAY);
            lblHint.setText("Đặt proxy trình duyệt là 127.0.0.1 và cổng đã chọn");
        }
        if (tray != null) {
            tray.update(active != null, active != null ? "Proxy Server - đang chạy, cổng " + active.port : "Proxy Server - đã dừng");
        }
        lblReq.setText("Yêu cầu: " + stats.getRequests());
        lblBlocked.setText("Bị chặn: " + stats.getBlocked());
        lblConn.setText("Kết nối: " + stats.getActiveConnections());
        lblDown.setText("Tải xuống: " + formatBytes(stats.getBytesToClients()));
        lblUp.setText("Tải lên: " + formatBytes(stats.getBytesFromClients()));
        lblFail.setText("Sai mật khẩu: " + stats.getAuthFailures());
        if (tabs.getSelectedComponent() == pnlStats) {
            statsView.refresh();
        }
    }

    public static void main(String args[]) {
        boolean minimized = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--home") && i + 1 < args.length) {
                AppDir.set(new java.io.File(args[++i]));
            } else if (args[i].equals("--minimized")) {
                minimized = true;
            }
        }
        final boolean startHidden = minimized;
        final SingleInstance single = new SingleInstance(SingleInstance.PORT);
        if (!single.tryLock()) {
            return;
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Theme.install();
                final MainFrame frame = new MainFrame();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        if (SystemProxy.isSupported() && frame.systemProxy.hasBackup()) {
                            try {
                                frame.systemProxy.restore();
                            } catch (java.io.IOException e) {
                            }
                        }
                    }
                });
                single.setOnShow(new Runnable() {
                    public void run() {
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                frame.showAgain();
                            }
                        });
                    }
                });
                if (!(startHidden && frame.hasTray())) {
                    frame.setVisible(true);
                    if (frame.firstRun) {
                        new WelcomeDialog(frame, frame.config.port).setVisible(true);
                        frame.saveConfigQuietly();
                    }
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClearAlerts;
    private javax.swing.JButton btnClearLog;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnHttps;
    private javax.swing.JButton btnPublic;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnRules;
    private javax.swing.JButton btnSaveBlock;
    private javax.swing.JButton btnStart;
    private javax.swing.JButton btnStop;
    private javax.swing.JButton btnSystemProxy;
    private javax.swing.JButton btnUsers;
    private javax.swing.JCheckBox chkAuth;
    private javax.swing.JCheckBox chkAutoProxy;
    private javax.swing.JCheckBox chkBlockedOnly;
    private javax.swing.JCheckBox chkLan;
    private javax.swing.JCheckBox chkLogFile;
    private javax.swing.JCheckBox chkTray;
    private javax.swing.JCheckBox chkWinStart;
    private javax.swing.JLabel lblAlerts;
    private javax.swing.JLabel lblAllow;
    private javax.swing.JLabel lblBlock;
    private javax.swing.JLabel lblBlocked;
    private javax.swing.JLabel lblChart;
    private javax.swing.JLabel lblClients;
    private javax.swing.JLabel lblConn;
    private javax.swing.JLabel lblDown;
    private javax.swing.JLabel lblFail;
    private javax.swing.JLabel lblFilter;
    private javax.swing.JLabel lblHint;
    private javax.swing.JLabel lblHttpsHint;
    private javax.swing.JLabel lblLimit;
    private javax.swing.JLabel lblPass;
    private javax.swing.JLabel lblPort;
    private javax.swing.JLabel lblPublicHint;
    private javax.swing.JLabel lblReq;
    private javax.swing.JLabel lblRulesHint;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblTop;
    private javax.swing.JLabel lblUp;
    private javax.swing.JLabel lblUser;
    private javax.swing.JLabel lblUsers;
    private javax.swing.JPanel pnlChart;
    private javax.swing.JPanel pnlConn;
    private javax.swing.JPanel pnlGeneral;
    private javax.swing.JPanel pnlLog;
    private javax.swing.JPanel pnlRules;
    private javax.swing.JPanel pnlSecurity;
    private javax.swing.JPanel pnlStats;
    private javax.swing.JPanel pnlStatus;
    private javax.swing.JPanel pnlSystem;
    private javax.swing.JPanel pnlTop;
    private javax.swing.JScrollPane scrollAlerts;
    private javax.swing.JScrollPane scrollBlock;
    private javax.swing.JScrollPane scrollClients;
    private javax.swing.JScrollPane scrollLog;
    private javax.swing.JScrollPane scrollTop;
    private javax.swing.JScrollPane scrollUsers;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTable tblClients;
    private javax.swing.JTable tblLog;
    private javax.swing.JTable tblUsers;
    private javax.swing.JTextArea txtAlerts;
    private javax.swing.JTextField txtAllow;
    private javax.swing.JTextArea txtBlock;
    private javax.swing.JTextField txtFilter;
    private javax.swing.JTextField txtLimit;
    private javax.swing.JPasswordField txtPass;
    private javax.swing.JTextField txtPort;
    private javax.swing.JTextArea txtTop;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables
}
