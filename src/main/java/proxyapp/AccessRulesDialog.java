package proxyapp;

import javax.swing.JOptionPane;

public class AccessRulesDialog extends javax.swing.JDialog {

    private final ProxyService service;
    private final BlockList allowList;
    private final BlockSchedule schedule;
    private final ProxyConfig config;
    private final java.io.File configFile;

    public AccessRulesDialog(java.awt.Frame parent, ProxyService service, BlockList allowList, BlockSchedule schedule,
            ProxyConfig config, java.io.File configFile) {
        super(parent, true);
        this.service = service;
        this.allowList = allowList;
        this.schedule = schedule;
        this.config = config;
        this.configFile = configFile;
        initComponents();
        chkAllowOnly.setSelected(config.allowOnlyMode);
        txtAllowList.setText(allowList.getText());
        txtSchedule.setText(schedule.getText());
        txtAllowList.setCaretPosition(0);
        txtSchedule.setCaretPosition(0);
        setLocationRelativeTo(parent);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        chkAllowOnly = new javax.swing.JCheckBox();
        lblAllowList = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAllowList = new javax.swing.JTextArea();
        lblSchedule = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtSchedule = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtHelp = new javax.swing.JTextArea();
        btnSave = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Chỉ cho phép và lịch chặn theo giờ");

        chkAllowOnly.setText("Chế độ chỉ cho phép: chỉ các trang trong danh sách cho phép được truy cập");

        lblAllowList.setText("Danh sách cho phép (mỗi dòng một tên miền):");

        txtAllowList.setColumns(20);
        txtAllowList.setRows(5);

        lblSchedule.setText("Lịch chặn theo giờ (tên miền, ngày, giờ):");

        txtSchedule.setColumns(20);
        txtSchedule.setRows(5);

        txtHelp.setColumns(20);
        txtHelp.setRows(5);
        txtHelp.setEditable(false);
        txtHelp.setLineWrap(true);
        txtHelp.setWrapStyleWord(true);
        txtHelp.setText("Lịch: mỗi dòng gồm tên miền, ngày, giờ. Ví dụ: facebook.com T2-T6 07:00-17:00 (chặn Facebook giờ học). Ngày dùng * (mọi ngày), T2-T6, T2,T4, CN. Giờ có thể qua nửa đêm, ví dụ 22:00-06:00. Tên miền * là chặn mọi trang trong khung giờ đó, trừ các trang trong danh sách cho phép. Danh sách chặn ở cửa sổ chính luôn được ưu tiên hơn danh sách cho phép.");

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

        jScrollPane1.setViewportView(txtAllowList);
        jScrollPane2.setViewportView(txtSchedule);
        jScrollPane3.setViewportView(txtHelp);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(chkAllowOnly))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblAllowList))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(360, 360, 360)
                .addComponent(lblSchedule))
            .addGroup(layout.createSequentialGroup()
                .addGap(360, 360, 360)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 660, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(330, 330, 330)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 700, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(chkAllowOnly))
            .addGroup(layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(lblAllowList))
            .addGroup(layout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(lblSchedule))
            .addGroup(layout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(385, 385, 385)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(495, 495, 495)
                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(495, 495, 495)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 545, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        boolean allowOnly = chkAllowOnly.isSelected();
        try {
            BlockList test = new BlockList(null, "");
            test.setText(txtAllowList.getText());
            if (allowOnly && test.size() == 0) {
                int answer = JOptionPane.showConfirmDialog(this,
                        "Danh sách cho phép đang trống. Bật chế độ này sẽ chặn mọi trang. Tiếp tục?",
                        "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            schedule.setText(txtSchedule.getText());
            allowList.setText(txtAllowList.getText());
            service.setAllowOnly(allowOnly);
            config.allowOnlyMode = allowOnly;
            config.save(configFile);
            JOptionPane.showMessageDialog(this, "Đã lưu: " + allowList.size() + " tên miền được phép, "
                    + schedule.size() + " dòng lịch chặn. Áp dụng ngay, không cần tắt proxy.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Lịch chặn sai: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnSave;
    private javax.swing.JCheckBox chkAllowOnly;
    private javax.swing.JLabel lblAllowList;
    private javax.swing.JLabel lblSchedule;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea txtAllowList;
    private javax.swing.JTextArea txtHelp;
    private javax.swing.JTextArea txtSchedule;
    // End of variables declaration//GEN-END:variables
}
