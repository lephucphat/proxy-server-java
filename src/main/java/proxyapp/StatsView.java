package proxyapp;

import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

public class StatsView {

    private final ProxyStats stats;
    private final AlertLog alerts;
    private final DefaultTableModel clientModel;
    private final DefaultTableModel userModel;
    private final JTextArea alertArea;
    private final JTextArea topArea;
    private final JComponent chart;

    public StatsView(ProxyStats stats, AlertLog alerts, JTable clients, JTable users, JTextArea alertArea,
            JTextArea topArea, JComponent chart) {
        this.stats = stats;
        this.alerts = alerts;
        this.alertArea = alertArea;
        this.topArea = topArea;
        this.chart = chart;
        clientModel = newModel("Máy khách");
        userModel = newModel("Người dùng");
        clients.setModel(clientModel);
        users.setModel(userModel);
        int[] widths = {110, 60, 60, 55, 80, 80};
        for (int i = 0; i < widths.length; i++) {
            clients.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            users.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        clients.setRowHeight(24);
        users.setRowHeight(24);
        topArea.setEditable(false);
        alertArea.setEditable(false);
    }

    private static DefaultTableModel newModel(String firstColumn) {
        return new DefaultTableModel(new String[]{firstColumn, "Yêu cầu", "Bị chặn", "Sai MK", "Tải xuống", "Tải lên"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static void fill(DefaultTableModel model, List<ProxyStats.Row> rows) {
        model.setRowCount(0);
        for (ProxyStats.Row r : rows) {
            model.addRow(new Object[]{r.name, r.requests, r.blocked, r.authFailures, ByteText.of(r.down), ByteText.of(r.up)});
        }
    }

    public void refresh() {
        fill(clientModel, stats.clientRows());
        fill(userModel, stats.userRows());
        chart.repaint();
        StringBuilder sb = new StringBuilder();
        for (AlertLog.Alert a : alerts.snapshot()) {
            sb.append(a.timeText()).append("  ").append(a.text).append("\n");
        }
        setIfChanged(alertArea, sb.toString(), true);
        sb = new StringBuilder();
        int rank = 1;
        for (java.util.Map.Entry<String, Long> e : stats.topHosts(8)) {
            sb.append(rank++).append(". ").append(e.getKey()).append(" - ").append(e.getValue()).append(" lần\n");
        }
        setIfChanged(topArea, sb.toString(), false);
    }

    private static void setIfChanged(JTextArea area, String text, boolean scrollToEnd) {
        if (!text.equals(area.getText())) {
            area.setText(text);
            area.setCaretPosition(scrollToEnd ? text.length() : 0);
        }
    }
}
