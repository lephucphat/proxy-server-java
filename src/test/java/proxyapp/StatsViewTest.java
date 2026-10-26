package proxyapp;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StatsViewTest {

    private ProxyStats stats;
    private AlertLog alerts;
    private JTable clients;
    private JTable users;
    private JTextArea alertArea;
    private JTextArea topArea;
    private StatsView view;

    @Before
    public void setUp() {
        stats = new ProxyStats();
        alerts = new AlertLog(10);
        clients = new JTable();
        users = new JTable();
        alertArea = new JTextArea();
        topArea = new JTextArea();
        view = new StatsView(stats, alerts, clients, users, alertArea, topArea, new JPanel());
    }

    @Test
    public void tablesHaveTheRightColumnsAndStartEmpty() {
        assertEquals(6, clients.getColumnCount());
        assertEquals("Máy khách", clients.getColumnName(0));
        assertEquals("Người dùng", users.getColumnName(0));
        assertEquals("Tải xuống", clients.getColumnName(4));
        assertEquals(0, clients.getRowCount());
        assertFalse(clients.getModel().isCellEditable(0, 0));
        assertFalse(alertArea.isEditable());
        assertFalse(topArea.isEditable());
    }

    @Test
    public void refreshFillsClientAndUserRowsWithReadableSizes() {
        stats.requestBy("10.0.0.5", "an");
        stats.blockedBy("10.0.0.5", "an");
        stats.bytesBy("10.0.0.5", "an", 2048, 100);
        stats.requestBy("10.0.0.6", "");
        view.refresh();
        assertEquals(2, clients.getRowCount());
        assertEquals("10.0.0.5", clients.getValueAt(0, 0));
        assertEquals(1L, clients.getValueAt(0, 1));
        assertEquals(1L, clients.getValueAt(0, 2));
        assertEquals("2.0 KB", clients.getValueAt(0, 4));
        assertEquals("100 B", clients.getValueAt(0, 5));
        assertEquals(1, users.getRowCount());
        assertEquals("an", users.getValueAt(0, 0));
    }

    @Test
    public void refreshShowsAlertsOldestFirstAndTopHostsRanked() {
        alerts.add("canh bao 1");
        alerts.add("canh bao 2");
        stats.request("a.com");
        stats.request("b.com");
        stats.request("b.com");
        view.refresh();
        String text = alertArea.getText();
        assertTrue(text, text.indexOf("canh bao 1") < text.indexOf("canh bao 2"));
        assertTrue(topArea.getText(), topArea.getText().startsWith("1. b.com - 2 lần"));
        assertTrue(topArea.getText().contains("2. a.com - 1 lần"));
    }

    @Test
    public void rowsDisappearAfterTheStatisticsAreReset() {
        stats.requestBy("10.0.0.5", "an");
        view.refresh();
        assertEquals(1, clients.getRowCount());
        stats.reset();
        alerts.clear();
        view.refresh();
        assertEquals(0, clients.getRowCount());
        assertEquals(0, users.getRowCount());
        assertEquals("", alertArea.getText());
        assertEquals("", topArea.getText());
    }
}
