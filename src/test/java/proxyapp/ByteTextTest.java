package proxyapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ByteTextTest {

    @Test
    public void usesTheRightUnit() {
        assertEquals("0 B", ByteText.of(0));
        assertEquals("1023 B", ByteText.of(1023));
        assertEquals("1.0 KB", ByteText.of(1024));
        assertEquals("1.50 MB", ByteText.of(1536 * 1024));
        assertEquals("2.00 GB", ByteText.of(2L * 1024 * 1024 * 1024));
    }
}
