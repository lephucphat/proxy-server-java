package proxyapp;

import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LogFilterTest {

    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    @Before
    public void setUp() {
        model = new DefaultTableModel(new String[]{"Giờ", "Máy khách", "Người dùng", "Loại", "Tên miền", "Địa chỉ", "Mã", "Byte", "ms", "Chặn"}, 0);
        model.addRow(new Object[]{"10:00:00", "192.168.1.5", "binh", "GET", "youtube.com", "https://youtube.com/watch?v=1", 200, 100, 5, ""});
        model.addRow(new Object[]{"10:00:01", "192.168.1.5", "binh", "GET", "doubleclick.net", "http://doubleclick.net/", 403, 0, 1, "Có"});
        model.addRow(new Object[]{"10:00:02", "127.0.0.1", "", "CONNECT", "example.com", "example.com:443", 200, 0, 12, ""});
        model.addRow(new Object[]{"10:00:03", "127.0.0.1", "an", "GET", "ads.example.org", "http://ads.example.org/x", 403, 0, 2, "Có"});
        sorter = new TableRowSorter<DefaultTableModel>(model);
    }

    private int rows(String text, boolean blockedOnly) {
        RowFilter<TableModel, Integer> f = LogFilter.build(text, blockedOnly);
        sorter.setRowFilter(f);
        return sorter.getViewRowCount();
    }

    @Test
    public void noTextAndNotBlockedOnlyMeansNoFilter() {
        assertNull(LogFilter.build("", false));
        assertNull(LogFilter.build("   ", false));
        assertNull(LogFilter.build(null, false));
        assertEquals(4, rows("", false));
    }

    @Test
    public void textMatchesHostUserClientAndAddressIgnoringCase() {
        assertEquals(1, rows("YouTube", false));
        assertEquals(2, rows("binh", false));
        assertEquals(2, rows("127.0.0", false));
        assertEquals(1, rows("watch?v=1", false));
        assertEquals(2, rows("example", false));
    }

    @Test
    public void textIsTrimmedAndOtherColumnsAreNotSearched() {
        assertEquals(1, rows("  youtube  ", false));
        assertEquals(0, rows("CONNECT", false));
        assertEquals(0, rows("403", false));
        assertEquals(0, rows("khong-co-gi", false));
    }

    @Test
    public void blockedOnlyKeepsJustBlockedRows() {
        assertEquals(2, rows("", true));
        assertEquals(1, rows("binh", true));
        assertEquals(1, rows("example.org", true));
        assertEquals(0, rows("youtube", true));
    }

    @Test
    public void filterFollowsRowsAddedLater() {
        javax.swing.JTable table = new javax.swing.JTable(model);
        table.setRowSorter(sorter);
        rows("example", false);
        model.addRow(new Object[]{"10:00:04", "10.0.0.1", "", "GET", "more.example.com", "http://more.example.com/", 200, 5, 3, ""});
        assertEquals(3, sorter.getViewRowCount());
    }
}
