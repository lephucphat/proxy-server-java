package proxyapp;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

public class LogFilter {

    public static final int BLOCKED_COLUMN = 9;
    public static final String BLOCKED_TEXT = "Có";
    private static final int[] SEARCH_COLUMNS = {1, 2, 4, 5};

    public static RowFilter<TableModel, Integer> build(String text, final boolean blockedOnly) {
        final String needle = text == null ? "" : text.trim().toLowerCase();
        if (needle.isEmpty() && !blockedOnly) {
            return null;
        }
        return new RowFilter<TableModel, Integer>() {
            public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
                if (blockedOnly && !BLOCKED_TEXT.equals(String.valueOf(entry.getValue(BLOCKED_COLUMN)))) {
                    return false;
                }
                if (needle.isEmpty()) {
                    return true;
                }
                for (int column : SEARCH_COLUMNS) {
                    if (String.valueOf(entry.getValue(column)).toLowerCase().contains(needle)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
