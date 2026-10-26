package proxyapp;

public class WelcomeDialog extends javax.swing.JDialog {

    public WelcomeDialog(java.awt.Frame parent, int port) {
        super(parent, true);
        initComponents();
        lblTitle.setFont(lblTitle.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        txtSteps.setText(steps(port));
        txtSteps.setCaretPosition(0);
        getRootPane().setDefaultButton(btnStart);
        setLocationRelativeTo(parent);
    }

    static String steps(int port) {
        String lan = NetInfo.lanAddress();
        StringBuilder sb = new StringBuilder();
        sb.append("Bước 1. Bấm \"Bật proxy\" ở góc trên bên trái.\n\n");
        sb.append("Bước 2. Đặt proxy cho trình duyệt: địa chỉ 127.0.0.1, cổng ").append(port)
                .append(". Hoặc bấm \"Bật proxy hệ thống Windows\" để Windows tự dùng (bấm lại để trả về như cũ).\n\n");
        sb.append("Bước 3. Mở một trang web, rồi xem thẻ \"Nhật ký\". Muốn chặn trang nào thì thêm vào thẻ \"Quy tắc\".\n\n");
        sb.append("Dùng cho điện thoại hoặc máy khác trong cùng Wi-Fi: vào thẻ \"Tổng quan\", tích \"Cho phép máy khác trong mạng LAN dùng\" ")
                .append("và \"Yêu cầu đăng nhập\", đặt tài khoản và mật khẩu. Trên điện thoại đặt Wi-Fi > Proxy thủ công: ");
        if (lan != null) {
            sb.append("địa chỉ ").append(lan).append(", cổng ").append(port).append(".");
        } else {
            sb.append("địa chỉ IP của máy này (xem bằng lệnh ipconfig), cổng ").append(port).append(".");
        }
        sb.append("\n\nLưu ý: chỉ dùng trên thiết bị của bạn hoặc khi người dùng đã đồng ý.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtSteps = new javax.swing.JTextArea();
        btnStart = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Bắt đầu nhanh");

        lblTitle.setText("Chào mừng đến với Proxy Server");

        txtSteps.setColumns(20);
        txtSteps.setRows(5);
        txtSteps.setEditable(false);
        txtSteps.setLineWrap(true);
        txtSteps.setWrapStyleWord(true);

        btnStart.setText("Bắt đầu dùng");
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });

        jScrollPane1.setViewportView(txtSteps);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                    .addComponent(btnStart, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblTitle)
                .addGap(12, 12, 12)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addComponent(btnStart, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed
        dispose();
    }//GEN-LAST:event_btnStartActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnStart;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea txtSteps;
    // End of variables declaration//GEN-END:variables
}
