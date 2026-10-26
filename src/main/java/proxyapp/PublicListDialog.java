package proxyapp;

import javax.swing.JOptionPane;

public class PublicListDialog extends javax.swing.JDialog {

    private static final String SUGGESTED = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts\n"
            + "https://urlhaus.abuse.ch/downloads/hostfile/";

    private final ProxyConfig config;
    private final java.io.File configFile;
    private final PublicBlockLists lists;

    public PublicListDialog(java.awt.Frame parent, ProxyConfig config, java.io.File configFile, PublicBlockLists lists) {
        super(parent, true);
        this.config = config;
        this.configFile = configFile;
        this.lists = lists;
        initComponents();
        txtSources.setText(config.publicListUrls);
        txtHours.setText(String.valueOf(config.publicListHours));
        showInfo();
        setLocationRelativeTo(parent);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblSources = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtSources = new javax.swing.JTextArea();
        btnSuggest = new javax.swing.JButton();
        lblHours = new javax.swing.JLabel();
        txtHours = new javax.swing.JTextField();
        btnUpdate = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        lblInfo = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtResult = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Danh sách chặn công khai");

        lblSources.setText("Nguồn danh sách (mỗi dòng một địa chỉ http/https hoặc đường dẫn file):");

        txtSources.setColumns(20);
        txtSources.setRows(5);

        btnSuggest.setText("Điền nguồn gợi ý");
        btnSuggest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSuggestActionPerformed(evt);
            }
        });

        lblHours.setText("Tự cập nhật mỗi (giờ, 0 = không tự cập nhật):");

        txtHours.setText("24");

        btnUpdate.setText("Lưu và cập nhật ngay");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnClose.setText("Đóng");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        lblInfo.setText("Đang dùng 0 tên miền công khai");

        txtResult.setColumns(20);
        txtResult.setRows(5);
        txtResult.setEditable(false);

        jScrollPane1.setViewportView(txtSources);
        jScrollPane2.setViewportView(txtResult);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblSources))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnSuggest, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblHours))
            .addGroup(layout.createSequentialGroup()
                .addGap(330, 330, 330)
                .addComponent(txtHours, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(330, 330, 330)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 660, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lblSources))
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(160, 160, 160)
                .addComponent(btnSuggest, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(208, 208, 208)
                .addComponent(lblHours))
            .addGroup(layout.createSequentialGroup()
                .addGap(205, 205, 205)
                .addComponent(txtHours, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(245, 245, 245)
                .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(245, 245, 245)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(292, 292, 292)
                .addComponent(lblInfo))
            .addGroup(layout.createSequentialGroup()
                .addGap(316, 316, 316)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 490, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSuggestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSuggestActionPerformed
        String text = txtSources.getText().trim();
        java.util.List<String> have = PublicBlockLists.splitSources(text);
        for (String line : SUGGESTED.split("\n")) {
            if (!have.contains(line)) {
                text = text.isEmpty() ? line : text + "\n" + line;
            }
        }
        txtSources.setText(text);
    }//GEN-LAST:event_btnSuggestActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        final int hours;
        try {
            hours = Integer.parseInt(txtHours.getText().trim());
            if (hours < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số giờ phải là số nguyên từ 0 trở lên", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        config.publicListUrls = txtSources.getText().trim();
        config.publicListHours = hours;
        try {
            config.save(configFile);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được cấu hình: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        final java.util.List<String> sources = PublicBlockLists.splitSources(config.publicListUrls);
        if (sources.isEmpty()) {
            lists.applySchedule(sources, hours);
            txtResult.setText("Chưa có nguồn nào. Bấm \"Điền nguồn gợi ý\" hoặc tự nhập địa chỉ.");
            return;
        }
        btnUpdate.setEnabled(false);
        txtResult.setText("Đang tải, vui lòng chờ...");
        new Thread(new Runnable() {
            public void run() {
                final PublicBlockLists.Result r = lists.update(sources);
                lists.applySchedule(sources, hours);
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showResult(r);
                    }
                });
            }
        }).start();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    private void showResult(PublicBlockLists.Result r) {
        StringBuilder sb = new StringBuilder();
        for (String m : r.messages) {
            sb.append(m).append("\n");
        }
        if (!r.anyOk) {
            sb.append("Không cập nhật được, vẫn giữ danh sách cũ.\n");
        }
        txtResult.setText(sb.toString());
        showInfo();
        btnUpdate.setEnabled(true);
    }

    private void showInfo() {
        String when = lists.getUpdatedAt() == 0 ? "chưa cập nhật lần nào"
                : "cập nhật lúc " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(lists.getUpdatedAt()));
        lblInfo.setText("Đang dùng " + lists.size() + " tên miền công khai (" + when + ")");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnSuggest;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblHours;
    private javax.swing.JLabel lblInfo;
    private javax.swing.JLabel lblSources;
    private javax.swing.JTextField txtHours;
    private javax.swing.JTextArea txtResult;
    private javax.swing.JTextArea txtSources;
    // End of variables declaration//GEN-END:variables
}
