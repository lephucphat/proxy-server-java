package proxyapp;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrafficLogTest {

    private static LogEntry entry(String user, String target) {
        return new LogEntry(1000L, "10.0.0.5", user, "GET", "a.com", target, 200, 123, 45, false);
    }

    @Test
    public void csvHasUserColumnRightAfterClient() {
        assertEquals("time,client,user,method,host,target,status,bytes,duration_ms,blocked", LogEntry.csvHeader());
        String[] cells = entry("an", "http://a.com/").toCsv().split(",");
        assertEquals("10.0.0.5", cells[1]);
        assertEquals("an", cells[2]);
        assertEquals("GET", cells[3]);
    }

    @Test
    public void missingUserIsEmptyAndSpecialCharactersAreQuoted() {
        assertEquals("", entry(null, "x").user);
        String line = entry("a,\"b", "x").toCsv();
        assertTrue(line, line.contains(",\"a,\"\"b\","));
    }

    @Test
    public void cellsThatLookLikeSpreadsheetFormulasAreDefused() {
        String line = entry("=HYPERLINK(\"http://x\")", "+cmd").toCsv();
        assertTrue(line, line.contains(",\"'=HYPERLINK(\"\"http://x\"\")\","));
        assertTrue(line, line.contains(",'+cmd,"));
        assertTrue(entry("@sum", "-1+1").toCsv().contains(",'@sum,"));
        assertTrue(entry("binh-thuong", "http://a.com/-x").toCsv().contains(",binh-thuong,"));
        assertTrue(entry("an", "x").toCsv().contains(",200,123,45,false"));
    }

    @Test
    public void keepsOnlyTheLastEntries() {
        TrafficLog log = new TrafficLog(2);
        log.add(entry("1", "a"));
        log.add(entry("2", "b"));
        log.add(entry("3", "c"));
        List<LogEntry> list = log.snapshot();
        assertEquals(2, list.size());
        assertEquals("2", list.get(0).user);
    }

    @Test
    public void appendFileGetsHeaderOnceAndRowsAfterIt() throws Exception {
        File f = new File(System.getProperty("java.io.tmpdir"), "tl_" + System.nanoTime() + ".csv");
        TrafficLog log = new TrafficLog(10);
        log.setAppendFile(f);
        log.add(entry("an", "x"));
        log.add(entry("binh", "y"));
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals(LogEntry.csvHeader(), lines.get(0));
        assertTrue(lines.get(2).contains(",binh,"));
        TrafficLog again = new TrafficLog(10);
        again.setAppendFile(f);
        again.add(entry("chi", "z"));
        assertEquals(4, Files.readAllLines(f.toPath(), StandardCharsets.UTF_8).size());
        f.delete();
    }

    @Test
    public void oldFormatLogFileIsMovedAsideInsteadOfMixed() throws Exception {
        File f = new File(System.getProperty("java.io.tmpdir"), "tl_old_" + System.nanoTime() + ".csv");
        File old = new File(f.getPath() + ".old");
        Files.write(f.toPath(), "time,client,method,host,target,status,bytes,duration_ms,blocked\nx,y\n".getBytes(StandardCharsets.UTF_8));
        TrafficLog log = new TrafficLog(10);
        log.setAppendFile(f);
        log.add(entry("an", "x"));
        assertTrue(old.isFile());
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals(LogEntry.csvHeader(), lines.get(0));
        assertFalse(lines.get(1).startsWith("x,"));
        f.delete();
        old.delete();
    }

    @Test
    public void exportCsvWritesHeaderAndAllRows() throws Exception {
        TrafficLog log = new TrafficLog(10);
        log.add(entry("an", "x"));
        log.add(entry("", "y"));
        File f = File.createTempFile("export", ".csv");
        log.exportCsv(f);
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals(LogEntry.csvHeader(), lines.get(0));
        f.delete();
    }
}
