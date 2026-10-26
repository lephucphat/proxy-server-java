package proxyapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    @Test
    public void rightPasswordMatchesWrongDoesNot() {
        String h = PasswordHasher.hash("123456");
        assertTrue(PasswordHasher.matches("123456", h));
        assertFalse(PasswordHasher.matches("123457", h));
        assertFalse(PasswordHasher.matches("", h));
    }

    @Test
    public void hashHidesPasswordAndUsesRandomSalt() {
        String a = PasswordHasher.hash("abcdef");
        String b = PasswordHasher.hash("abcdef");
        assertFalse(a.contains("abcdef"));
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.matches("abcdef", a));
        assertTrue(PasswordHasher.matches("abcdef", b));
    }

    @Test
    public void brokenStoredValueNeverMatches() {
        assertFalse(PasswordHasher.matches("x", null));
        assertFalse(PasswordHasher.matches("x", ""));
        assertFalse(PasswordHasher.matches("x", "x"));
        assertFalse(PasswordHasher.matches("x", "pbkdf2$10000$zz$zz"));
        assertFalse(PasswordHasher.matches("x", "pbkdf2$0$00$00"));
        assertFalse(PasswordHasher.matches("x", "md5$10000$00$00"));
    }
}
