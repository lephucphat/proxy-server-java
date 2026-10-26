package proxyapp;

import javax.swing.JOptionPane;

public class UsersDialog extends javax.swing.JDialog {

    private final UserStore users;
    private final ProxyConfig config;
    private final javax.swing.DefaultListModel<String> model = new javax.swing.DefaultListModel<String>();

    public UsersDialog(java.awt.Frame parent, UserStore users, ProxyConfig config) {
        super(parent, true);
        this.users = users;
        this.config = config;
        initComponents();
        lstUsers.setModel(model);
        refreshList(null);
        showSelected();
        setLocationRelativeTo(parent);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblUsers = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstUsers = new javax.swing.JList<>();
        btnAdd = new javax.swing.JButton();
        btnPassword = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        lblSelected = new javax.swing.JLabel();
        chkOwnRules = new javax.swing.JCheckBox();
        chkAllowOnly = new javax.swing.JCheckBox();
        lblAllowList = new javax.swing.JLabel();
        lblSchedule = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtAllowList = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtSchedule = new javax.swing.JTextArea();
        btnSaveRules = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Tài khoản khác và luật riêng");

        lblUsers.setText("Tài khoản khác:");

        lstUsers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstUsers.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstUsersValueChanged(evt);
            }
        });

        btnAdd.setText("Thêm tài khoản...");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnPassword.setText("Đổi mật khẩu...");
        btnPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPasswordActionPerformed(evt);
            }
        });

        btnRemove.setText("Xóa tài khoản");
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });

        lblSelected.setText("Luật riêng của: (chọn một tài khoản bên trái)");

        chkOwnRules.setText("Dùng luật riêng (thay cho luật chung ở cửa sổ chính)");

        chkAllowOnly.setText("Chế độ chỉ cho phép (riêng)");

        lblAllowList.setText("Danh sách cho phép:");

        lblSchedule.setText("Lịch chặn theo giờ:");

        txtAllowList.setColumns(20);
        txtAllowList.setRows(5);

        txtSchedule.setColumns(20);
        txtSchedule.setRows(5);

        btnSaveRules.setText("Lưu luật");
        btnSaveRules.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveRulesActionPerformed(evt);
            }
        });

        btnClose.setText("Đóng");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        jScrollPane1.setViewportView(lstUsers);
        jScrollPane2.setViewportView(txtAllowList);
        jScrollPane3.setViewportView(txtSchedule);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblUsers))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(260, 260, 260)
                .addComponent(lblSelected, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(260, 260, 260)
                .addComponent(chkOwnRules))
            .addGroup(layout.createSequentialGroup()
                .addGap(260, 260, 260)
                .addComponent(chkAllowOnly))
            .addGroup(layout.createSequentialGroup()
                .addGap(260, 260, 260)
                .addComponent(lblAllowList))
            .addGroup(layout.createSequentialGroup()
                .addGap(500, 500, 500)
                .addComponent(lblSchedule))
            .addGroup(layout.createSequentialGroup()
                .addGap(260, 260, 260)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(500, 500, 500)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(260, 260, 260)
                .addComponent(btnSaveRules, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(500, 500, 500)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 760, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lblUsers))
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(350, 350, 350)
                .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(384, 384, 384)
                .addComponent(btnPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(418, 418, 418)
                .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lblSelected))
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(chkOwnRules))
            .addGroup(layout.createSequentialGroup()
                .addGap(66, 66, 66)
                .addComponent(chkAllowOnly))
            .addGroup(layout.createSequentialGroup()
                .addGap(98, 98, 98)
                .addComponent(lblAllowList))
            .addGroup(layout.createSequentialGroup()
                .addGap(98, 98, 98)
                .addComponent(lblSchedule))
            .addGroup(layout.createSequentialGroup()
                .addGap(120, 120, 120)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(120, 120, 120)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(432, 432, 432)
                .addComponent(btnSaveRules, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(432, 432, 432)
                .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 480, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void lstUsersValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstUsersValueChanged
        if (!evt.getValueIsAdjusting()) {
            showSelected();
        }
    }//GEN-LAST:event_lstUsersValueChanged

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        String name = JOptionPane.showInputDialog(this, "Tên tài khoản (chữ không dấu, số, _ . -):", "Thêm tài khoản", JOptionPane.QUESTION_MESSAGE);
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.equalsIgnoreCase(config.username)) {
            JOptionPane.showMessageDialog(this, "Tên này trùng tài khoản chính ở cửa sổ chính", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String password = askPassword("Mật khẩu cho " + name + " (ít nhất 4 ký tự):");
        if (password == null) {
            return;
        }
        try {
            users.add(name, password);
            refreshList(name);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPasswordActionPerformed
        String name = lstUsers.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một tài khoản bên trái");
            return;
        }
        String password = askPassword("Mật khẩu mới cho " + name + " (ít nhất 4 ký tự):");
        if (password == null) {
            return;
        }
        try {
            users.setPassword(name, password);
            JOptionPane.showMessageDialog(this, "Đã đổi mật khẩu của " + name);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnPasswordActionPerformed

    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        String name = lstUsers.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một tài khoản bên trái");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Xóa tài khoản " + name + " và luật riêng của nó?",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            users.remove(name);
            refreshList(null);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnRemoveActionPerformed

    private void btnSaveRulesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveRulesActionPerformed
        String name = lstUsers.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một tài khoản bên trái");
            return;
        }
        boolean own = chkOwnRules.isSelected();
        boolean only = chkAllowOnly.isSelected();
        try {
            BlockList test = new BlockList(null, "");
            test.setText(txtAllowList.getText());
            if (own && only && test.size() == 0) {
                int answer = JOptionPane.showConfirmDialog(this,
                        "Danh sách cho phép đang trống. " + name + " sẽ không vào được trang nào. Tiếp tục?",
                        "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            users.setRules(name, own, only, txtAllowList.getText(), txtSchedule.getText());
            JOptionPane.showMessageDialog(this, "Đã lưu luật của " + name + ". Áp dụng ngay, không cần tắt proxy.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Lịch chặn sai: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSaveRulesActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    private String askPassword(String message) {
        javax.swing.JPasswordField field = new javax.swing.JPasswordField(20);
        int answer = JOptionPane.showConfirmDialog(this, new Object[]{message, field}, "Mật khẩu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return null;
        }
        return new String(field.getPassword());
    }

    private void refreshList(String select) {
        model.clear();
        for (UserStore.Account a : users.list()) {
            model.addElement(a.name);
        }
        if (select != null) {
            lstUsers.setSelectedValue(select, true);
        }
        showSelected();
    }

    private void showSelected() {
        String name = lstUsers.getSelectedValue();
        UserStore.Account a = name == null ? null : users.find(name);
        boolean has = a != null;
        chkOwnRules.setEnabled(has);
        chkAllowOnly.setEnabled(has);
        txtAllowList.setEnabled(has);
        txtSchedule.setEnabled(has);
        btnSaveRules.setEnabled(has);
        btnPassword.setEnabled(has);
        btnRemove.setEnabled(has);
        if (has) {
            lblSelected.setText("Luật riêng của: " + a.name);
            chkOwnRules.setSelected(a.ownRules);
            chkAllowOnly.setSelected(a.allowOnly);
            txtAllowList.setText(a.allowList.getText());
            txtSchedule.setText(a.schedule.getText());
            txtAllowList.setCaretPosition(0);
            txtSchedule.setCaretPosition(0);
        } else {
            lblSelected.setText("Luật riêng của: (chọn một tài khoản bên trái)");
            chkOwnRules.setSelected(false);
            chkAllowOnly.setSelected(false);
            txtAllowList.setText("");
            txtSchedule.setText("");
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnPassword;
    private javax.swing.JButton btnRemove;
    private javax.swing.JButton btnSaveRules;
    private javax.swing.JCheckBox chkAllowOnly;
    private javax.swing.JCheckBox chkOwnRules;
    private javax.swing.JLabel lblAllowList;
    private javax.swing.JLabel lblSchedule;
    private javax.swing.JLabel lblSelected;
    private javax.swing.JLabel lblUsers;
    private javax.swing.JList<String> lstUsers;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea txtAllowList;
    private javax.swing.JTextArea txtSchedule;
    // End of variables declaration//GEN-END:variables
}
