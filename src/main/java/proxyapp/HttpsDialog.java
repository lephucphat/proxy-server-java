package proxyapp;

import javax.swing.JOptionPane;

public class HttpsDialog extends javax.swing.JDialog {

    private final ProxyConfig config;
    private final java.io.File configFile;
    private final BlockList bypass;
    private final UrlRules urlRules;
    private final ProxyService service;

    public HttpsDialog(java.awt.Frame parent, ProxyConfig config, java.io.File configFile, BlockList bypass,
            UrlRules urlRules, ProxyService service) {
        super(parent, true);
        this.config = config;
        this.configFile = configFile;
        this.bypass = bypass;
        this.urlRules = urlRules;
        this.service = service;
        initComponents();
        chkMitm.setSelected(config.mitmEnabled);
        txtBypass.setText(bypass.getText());
        txtRules.setText(urlRules.getText());
        txtInfo.setCaretPosition(0);
        txtBypass.setCaretPosition(0);
        txtRules.setCaretPosition(0);
        txtRulesHelp.setCaretPosition(0);
        setLocationRelativeTo(parent);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        chkMitm = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtInfo = new javax.swing.JTextArea();
        btnExport = new javax.swing.JButton();
        btnFolder = new javax.swing.JButton();
        lblBypass = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtBypass = new javax.swing.JTextArea();
        lblRules = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtRules = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtRulesHelp = new javax.swing.JTextArea();
        btnSave = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Giải mã HTTPS");

        chkMitm.setText("Bật giải mã HTTPS (cần cài chứng chỉ gốc vào máy khách; áp dụng khi bật lại proxy)");

        txtInfo.setColumns(20);
        txtInfo.setRows(5);
        txtInfo.setEditable(false);
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        txtInfo.setText("Giải mã HTTPS cho phép proxy thấy địa chỉ đầy đủ (đường dẫn, từ khóa) của trang HTTPS để ghi nhật ký và chặn theo quy tắc. CHỈ dùng trên thiết bị của chính bạn hoặc khi người dùng đã đồng ý. Cách làm: (1) Bấm \"Tạo / xuất chứng chỉ gốc\" để có file proxy-ca.crt. (2) Windows (Chrome, Edge): bấm đúp file, chọn Install Certificate > Current User > Place all certificates in the following store > Browse > Trusted Root Certification Authorities > Next > Finish > Yes. (3) Firefox có kho riêng: Settings > Privacy & Security > View Certificates > Authorities > Import, tick Trust this CA to identify websites. (4) Điện thoại hoặc máy khác: gửi file proxy-ca.crt sang và cài làm chứng chỉ CA, Android và iOS còn phải bật tin cậy trong cài đặt. (5) Tick ô ở trên, bấm Lưu, rồi tắt và bật lại proxy. (6) Muốn gỡ: chạy certmgr.msc, mở Trusted Root Certification Authorities > Certificates, xóa dòng ProxyApp Local CA. Khóa riêng của CA nằm trong file proxy-ca.p12: giữ kín, ai có file này có thể giả mạo mọi trang trên máy đã cài chứng chỉ. Ngân hàng, ví điện tử và ứng dụng ghim chứng chỉ nên thêm vào danh sách bên dưới để không bị giải mã.");

        btnExport.setText("Tạo / xuất chứng chỉ gốc...");
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });

        btnFolder.setText("Mở thư mục chứa chứng chỉ");
        btnFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFolderActionPerformed(evt);
            }
        });

        lblBypass.setText("Trang KHÔNG giải mã (mỗi dòng một tên miền):");

        txtBypass.setColumns(20);
        txtBypass.setRows(5);

        lblRules.setText("Chặn theo đường dẫn / từ khóa:");

        txtRules.setColumns(20);
        txtRules.setRows(5);

        txtRulesHelp.setColumns(20);
        txtRulesHelp.setRows(5);
        txtRulesHelp.setEditable(false);
        txtRulesHelp.setLineWrap(true);
        txtRulesHelp.setWrapStyleWord(true);
        txtRulesHelp.setText("Mỗi dòng: tên miền, loại, giá trị. Loại path: chặn khi đường dẫn bắt đầu bằng giá trị, ví dụ youtube.com path /shorts. Loại keyword: chặn khi tên miền + đường dẫn có chứa từ khóa, ví dụ * keyword casino (dấu * là mọi trang). Với HTTPS chỉ thấy đường dẫn khi đã bật giải mã và trang không nằm trong danh sách không giải mã; nếu không thì chỉ so từ khóa với tên miền. Quy tắc và danh sách không giải mã áp dụng ngay khi lưu.");

        btnSave.setText("Lưu và áp dụng ngay");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnClose.setText("Đóng");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        jScrollPane1.setViewportView(txtInfo);
        jScrollPane2.setViewportView(txtBypass);
        jScrollPane3.setViewportView(txtRules);
        jScrollPane4.setViewportView(txtRulesHelp);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(chkMitm))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 740, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(280, 280, 280)
                .addComponent(btnFolder, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblBypass))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(400, 400, 400)
                .addComponent(lblRules))
            .addGroup(layout.createSequentialGroup()
                .addGap(400, 400, 400)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 740, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(280, 280, 280)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 780, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(chkMitm))
            .addGroup(layout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(225, 225, 225)
                .addComponent(btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(225, 225, 225)
                .addComponent(btnFolder, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(268, 268, 268)
                .addComponent(lblBypass))
            .addGroup(layout.createSequentialGroup()
                .addGap(290, 290, 290)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(268, 268, 268)
                .addComponent(lblRules))
            .addGroup(layout.createSequentialGroup()
                .addGap(290, 290, 290)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(468, 468, 468)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(548, 548, 548)
                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(548, 548, 548)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 595, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportActionPerformed
        try {
            java.io.File keyFile = AppDir.file(config.caFile);
            java.io.File certFile = AppDir.file(config.caCertFile);
            CertificateAuthority ca = CertificateAuthority.loadOrCreate(keyFile);
            ca.exportCertificate(certFile);
            JOptionPane.showMessageDialog(this, "Đã tạo chứng chỉ gốc để cài vào máy khách:\n" + certFile.getAbsolutePath()
                    + "\n\nGiữ kín file khóa " + keyFile.getAbsolutePath() + " (không gửi cho ai).");
        } catch (java.security.GeneralSecurityException ex) {
            JOptionPane.showMessageDialog(this, "Không tạo được chứng chỉ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không tạo được chứng chỉ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnExportActionPerformed

    private void btnFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFolderActionPerformed
        java.io.File dir = AppDir.file(config.caCertFile).getAbsoluteFile().getParentFile();
        try {
            if (!java.awt.Desktop.isDesktopSupported()) {
                throw new java.io.IOException("Máy này không hỗ trợ mở thư mục từ chương trình");
            }
            java.awt.Desktop.getDesktop().open(dir);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không mở được thư mục " + dir + ": " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnFolderActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        boolean on = chkMitm.isSelected();
        try {
            urlRules.setText(txtRules.getText());
            bypass.setText(txtBypass.getText());
            config.mitmEnabled = on;
            config.save(configFile);
            String message = "Đã lưu: " + urlRules.size() + " quy tắc, " + bypass.size() + " trang không giải mã. Áp dụng ngay.";
            if (service.isRunning() && service.isMitmActive() != on) {
                message += "\nTắt rồi bật lại proxy để việc " + (on ? "bật" : "tắt") + " giải mã HTTPS có hiệu lực.";
            }
            JOptionPane.showMessageDialog(this, message);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Quy tắc sai: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnFolder;
    private javax.swing.JButton btnSave;
    private javax.swing.JCheckBox chkMitm;
    private javax.swing.JLabel lblBypass;
    private javax.swing.JLabel lblRules;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextArea txtBypass;
    private javax.swing.JTextArea txtInfo;
    private javax.swing.JTextArea txtRules;
    private javax.swing.JTextArea txtRulesHelp;
    // End of variables declaration//GEN-END:variables
}
